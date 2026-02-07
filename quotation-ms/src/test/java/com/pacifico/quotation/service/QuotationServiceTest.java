package com.pacifico.quotation.service;

import com.pacifico.insurance.event.InsurancePolicyEvent;
import com.pacifico.quotation.dto.RiskCacheEntry;
import com.pacifico.quotation.exception.HighRiskException;
import com.pacifico.quotation.model.Quote;
import com.pacifico.quotation.repository.QuoteRepository;
import com.pacifico.risk.RiskInferenceServiceGrpc;
import com.pacifico.risk.RiskRequest;
import com.pacifico.risk.RiskResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuotationServiceTest {

    @Mock
    private RiskInferenceServiceGrpc.RiskInferenceServiceBlockingStub riskStub;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private QuoteRepository quoteRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private QuotationService quotationService;

    private static final String DNI = "12345678";
    private static final int AGE = 25;
    private static final double CAR_VALUE = 40000.0;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Should successfully orchestrate quotation when risk is low")
    void shouldSuccessfullyOrchestrateQuotation() {
        // GIVEN
        when(valueOperations.get(anyString())).thenReturn(null);
        
        RiskResponse riskResponse = RiskResponse.newBuilder()
                .setProbabilityScore(0.5)
                .setRiskLevel("MEDIUM")
                .build();
        when(riskStub.evaluateRisk(any(RiskRequest.class))).thenReturn(riskResponse);
        
        Quote savedQuote = Quote.builder()
                .id(1L)
                .dni(DNI)
                .age(AGE)
                .carValue(java.math.BigDecimal.valueOf(CAR_VALUE))
                .probabilityScore(0.5)
                .riskLevel("MEDIUM")
                .status("APPROVED")
                .build();
        when(quoteRepository.save(any(Quote.class))).thenReturn(savedQuote);

        // WHEN
        Quote result = quotationService.orchestrateQuotation(DNI, AGE, CAR_VALUE);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getDni()).isEqualTo(DNI);
        verify(riskStub, times(1)).evaluateRisk(any(RiskRequest.class));
        verify(redisTemplate.opsForValue(), times(1)).set(anyString(), any(RiskCacheEntry.class), any(Duration.class));
        verify(kafkaTemplate, times(1)).send(any(org.apache.kafka.clients.producer.ProducerRecord.class));
    }

    @Test
    @DisplayName("Should use cache when risk is already inferred")
    void shouldUseCacheWhenRiskIsInferred() {
        // GIVEN
        RiskCacheEntry cacheEntry = new RiskCacheEntry(0.2, "LOW");
        when(valueOperations.get(anyString())).thenReturn(cacheEntry);
        
        Quote savedQuote = Quote.builder()
                .id(1L)
                .dni(DNI)
                .age(AGE)
                .carValue(java.math.BigDecimal.valueOf(CAR_VALUE))
                .probabilityScore(0.2)
                .riskLevel("LOW")
                .build();
        when(quoteRepository.save(any(Quote.class))).thenReturn(savedQuote);

        // WHEN
        quotationService.orchestrateQuotation(DNI, AGE, CAR_VALUE);

        // THEN
        verify(riskStub, never()).evaluateRisk(any(RiskRequest.class));
        verify(quoteRepository, times(1)).save(any(Quote.class));
    }

    @Test
    @DisplayName("Should throw HighRiskException when score exceeds threshold")
    void shouldThrowExceptionWhenRiskIsHigh() {
        // GIVEN
        when(valueOperations.get(anyString())).thenReturn(null);
        
        RiskResponse riskResponse = RiskResponse.newBuilder()
                .setProbabilityScore(0.85)
                .setRiskLevel("HIGH")
                .build();
        when(riskStub.evaluateRisk(any(RiskRequest.class))).thenReturn(riskResponse);

        // WHEN & THEN
        assertThatThrownBy(() -> quotationService.orchestrateQuotation(DNI, AGE, CAR_VALUE))
                .isInstanceOf(HighRiskException.class)
                .hasMessageContaining("High risk detected");
        
        verify(quoteRepository, never()).save(any(Quote.class));
        verify(kafkaTemplate, never()).send(anyString(), any());
    }

    @Test
    @DisplayName("Should handle concurrent requests using Virtual Threads simulator")
    void shouldHandleConcurrentRequests() throws Exception {
        // GIVEN
        RiskResponse riskResponse = RiskResponse.newBuilder().setProbabilityScore(0.1).setRiskLevel("LOW").build();
        when(riskStub.evaluateRisk(any(RiskRequest.class))).thenReturn(riskResponse);
        Quote mockQuote = Quote.builder()
                .id(1L)
                .dni(DNI)
                .age(AGE)
                .carValue(java.math.BigDecimal.valueOf(CAR_VALUE))
                .probabilityScore(0.1)
                .riskLevel("LOW")
                .build();
        when(quoteRepository.save(any(Quote.class))).thenReturn(mockQuote);

        int concurrentTasks = 50;
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CompletableFuture<?>[] futures = new CompletableFuture[concurrentTasks];

        // WHEN
        for (int i = 0; i < concurrentTasks; i++) {
            futures[i] = CompletableFuture.runAsync(() -> 
                quotationService.orchestrateQuotation(DNI, AGE, CAR_VALUE), executor);
        }

        CompletableFuture.allOf(futures).get();

        // THEN
        verify(quoteRepository, times(concurrentTasks)).save(any(Quote.class));
        executor.shutdown();
    }
}
