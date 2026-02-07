package com.pacifico.quotation.service;

import com.pacifico.insurance.event.InsurancePolicyEvent;
import com.pacifico.quotation.dto.RiskCacheEntry;
import com.pacifico.quotation.exception.HighRiskException;
import com.pacifico.quotation.model.Quote;
import com.pacifico.quotation.repository.QuoteRepository;
import com.pacifico.risk.RiskInferenceServiceGrpc;
import com.pacifico.risk.RiskRequest;
import com.pacifico.risk.RiskResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Core service for orchestrating the insurance quotation lifecycle.
 * <p>
 * This service implements the Cache-Aside pattern using Redis to optimize risk assessment,
 * communicates with the ML Risk Service via gRPC for high-performance scoring,
 * and publishes events to Kafka for downstream policy issuance.
 * <p>
 * It leverages Project Loom's Virtual Threads (when running on Java 21) 
 * to handle multiple simultaneous quotation requests efficiently.
 */
@Service
public class QuotationService {

    private static final Logger logger = LoggerFactory.getLogger(QuotationService.class);

    @GrpcClient("ml-risk-ms")
    private RiskInferenceServiceGrpc.RiskInferenceServiceBlockingStub riskStub;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private QuoteRepository quoteRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private static final String RISK_CACHE_PREFIX = "risk_cache:";
    private static final String KAFKA_TOPIC = "insurance.policy.issuance";

    /**
     * Orchestrates the complete quotation process for a customer.
     * <p>
     * The flow includes:
     * 1. Checking risk cache in Redis (Cache-Aside).
     * 2. Inferred risk score via gRPC if not cached.
     * 3. Functional evaluation of risk thresholds.
     * 4. Persistence of approved quotes in PostgreSQL.
     * 5. Async event publication to Kafka for policy issuance.
     *
     * @param dni The national ID of the customer.
     * @param age The age of the customer.
     * @param carValue The appraised value of the vehicle.
     * @return The generated and persisted {@link Quote}.
     * @throws HighRiskException if the calculated risk score exceeds the acceptable threshold (0.80).
     */
    public Quote orchestrateQuotation(String dni, int age, double carValue) {
        logger.info("Orchestrating quotation for DNI: {}", dni);

        // 1. Check Redis Cache
        RiskCacheEntry cachedRisk = (RiskCacheEntry) redisTemplate.opsForValue().get(RISK_CACHE_PREFIX + dni);

        RiskResponse riskResponse;
        if (cachedRisk != null) {
            logger.info("Cache hit for DNI: {}", dni);
            riskResponse = RiskResponse.newBuilder()
                    .setProbabilityScore(cachedRisk.probabilityScore())
                    .setRiskLevel(cachedRisk.riskLevel())
                    .build();
        } else {
            logger.info("Cache miss for DNI: {}. Calling gRPC...", dni);
            // 2. gRPC Call
            RiskRequest request = RiskRequest.newBuilder()
                    .setDni(dni)
                    .setAge(age)
                    .setCarValue(carValue)
                    .build();
            
            riskResponse = riskStub.evaluateRisk(request);
            
            // Store in Redis (TTL 10m)
            RiskCacheEntry entry = new RiskCacheEntry(riskResponse.getProbabilityScore(), riskResponse.getRiskLevel());
            redisTemplate.opsForValue().set(RISK_CACHE_PREFIX + dni, entry, Duration.ofMinutes(10));
        }

        final RiskResponse finalRisk = riskResponse;
        
        // 3. Functional Logic
        return Optional.of(riskResponse)
                .filter(res -> res.getProbabilityScore() < 0.80)
                .map(res -> saveToPostgres(dni, age, carValue, res))
                .map(quote -> {
                    sendToKafka(quote);
                    return quote;
                })
                .orElseThrow(() -> new HighRiskException("High risk detected: " + finalRisk.getProbabilityScore()));
    }

    private Quote saveToPostgres(String dni, int age, double carValue, RiskResponse risk) {
        Quote quote = Quote.builder()
                .dni(dni)
                .age(age)
                .carValue(BigDecimal.valueOf(carValue))
                .probabilityScore(risk.getProbabilityScore())
                .riskLevel(risk.getRiskLevel())
                .status("APPROVED")
                .createdAt(LocalDateTime.now())
                .build();
        return quoteRepository.save(quote);
    }

    private void sendToKafka(Quote quote) {
        logger.info("Sending quote to Kafka: {}", quote.getId());
        double finalPremium = quote.getCarValue().doubleValue() * 0.05 * (1 + quote.getProbabilityScore());
        
        InsurancePolicyEvent event = InsurancePolicyEvent.newBuilder()
                .setQuoteId(quote.getId())
                .setDni(quote.getDni())
                .setApprovedRiskScore(quote.getProbabilityScore())
                .setFinalPremium(finalPremium)
                .build();

        ProducerRecord<String, Object> kafkaRecord = new ProducerRecord<>(KAFKA_TOPIC, quote.getDni(), event);
        kafkaTemplate.send(kafkaRecord);
    }
}
