package com.pacifico.issuance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pacifico.insurance.event.InsurancePolicyEvent;
import com.pacifico.issuance.model.Policy;
import com.pacifico.issuance.repository.PolicyRepository;
import com.pacifico.issuance.websocket.IssuanceWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

/**
 * Service responsible for the final issuance of insurance policies.
 * <p>
 * This service consumes {@link InsurancePolicyEvent} from Kafka,
 * persists the policy data to PostgreSQL, and pushes real-time updates 
 * to the customer via WebSockets.
 */
@Service
public class IssuanceService {

    private static final Logger logger = LoggerFactory.getLogger(IssuanceService.class);
    private final PolicyRepository policyRepository;
    private final IssuanceWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    public IssuanceService(PolicyRepository policyRepository, 
                           IssuanceWebSocketHandler webSocketHandler,
                           ObjectMapper objectMapper) {
        this.policyRepository = policyRepository;
        this.webSocketHandler = webSocketHandler;
        this.objectMapper = objectMapper;
    }

    /**
     * Consumes policy issuance events from Kafka.
     * <p>
     * This method handles the asynchronous transition from quotation to formal policy.
     * It generates a unique policy number, saves the entity, and triggers a WebSocket 
     * notification for the front-end.
     *
     * @param event The event containing quotation and premium data.
     */
    @KafkaListener(topics = "insurance.policy.issuance", groupId = "issuance-group")
    public void consume(InsurancePolicyEvent event) {
        logger.info("Received Kafka event for DNI: {}", event.getDni());
        try {
            Long quoteId = event.getQuoteId();
            String dni = event.getDni().toString();
            Double finalPremium = event.getFinalPremium();

            // 1. Generate Policy Number PAC-2026-XXXX
            String policyNumber = "PAC-2026-" + (1000 + this.random.nextInt(9000));

            // 2. Persist to DB
            Policy policy = Policy.builder()
                    .quoteId(quoteId)
                    .policyNumber(policyNumber)
                    .dni(dni)
                    .finalPremium(BigDecimal.valueOf(finalPremium))
                    .issuedAt(LocalDateTime.now())
                    .build();
            
            policyRepository.save(policy);
            logger.info("Policy {} saved for DNI: {}", policyNumber, dni);

            // 3. Push to WebSocket
            String pushPayload = objectMapper.writeValueAsString(Map.of(
                "policyNumber", policyNumber,
                "dni", dni,
                "finalPremium", finalPremium,
                "status", "ISSUED"
            ));
            webSocketHandler.sendToUser(dni, pushPayload);

        } catch (Exception e) {
            logger.error("Error processing issuance event", e);
        }
    }
}
