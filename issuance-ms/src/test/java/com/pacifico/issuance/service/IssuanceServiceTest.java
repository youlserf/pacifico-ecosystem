package com.pacifico.issuance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pacifico.insurance.event.InsurancePolicyEvent;
import com.pacifico.issuance.model.Policy;
import com.pacifico.issuance.repository.PolicyRepository;
import com.pacifico.issuance.websocket.IssuanceWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssuanceServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private IssuanceWebSocketHandler webSocketHandler;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private IssuanceService issuanceService;

    @Test
    @DisplayName("Should process Kafka event and issue policy")
    void shouldProcessEventAndIssuePolicy() throws Exception {
        // GIVEN
        InsurancePolicyEvent event = InsurancePolicyEvent.newBuilder()
                .setQuoteId(123L)
                .setDni("11223344")
                .setApprovedRiskScore(0.25)
                .setFinalPremium(500.50)
                .build();
        
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"status\":\"ISSUED\"}");

        // WHEN
        issuanceService.consume(event);

        // THEN
        ArgumentCaptor<Policy> policyCaptor = ArgumentCaptor.forClass(Policy.class);
        verify(policyRepository, times(1)).save(policyCaptor.capture());
        
        Policy savedPolicy = policyCaptor.getValue();
        assertThat(savedPolicy.getDni()).isEqualTo("11223344");
        assertThat(savedPolicy.getQuoteId()).isEqualTo(123L);
        assertThat(savedPolicy.getFinalPremium()).isEqualByComparingTo(BigDecimal.valueOf(500.50));
        assertThat(savedPolicy.getPolicyNumber()).startsWith("PAC-2026-");

        verify(webSocketHandler, times(1)).sendToUser(eq("11223344"), anyString());
    }
}
