package com.pacifico.risk.service;

import com.pacifico.risk.RiskRequest;
import com.pacifico.risk.RiskResponse;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RiskInferenceServiceImplTest {

    private RiskInferenceServiceImpl riskService;

    @BeforeEach
    void setUp() {
        riskService = new RiskInferenceServiceImpl();
    }

    @Test
    @DisplayName("Should return LOW risk for young customer with low car value")
    void shouldReturnLowRisk() {
        // GIVEN
        RiskRequest request = RiskRequest.newBuilder()
                .setDni("12345678")
                .setAge(30)
                .setCarValue(10000.0)
                .build();
        StreamObserver<RiskResponse> responseObserver = mock(StreamObserver.class);

        // WHEN
        riskService.evaluateRisk(request, responseObserver);

        // THEN - Using ArgumentCaptor because evaluations are in a Virtual Thread executor
        ArgumentCaptor<RiskResponse> responseCaptor = ArgumentCaptor.forClass(RiskResponse.class);
        
        // Wait a bit for the async execution
        verify(responseObserver, timeout(1000)).onNext(responseCaptor.capture());
        verify(responseObserver, timeout(1000)).onCompleted();

        RiskResponse response = responseCaptor.getValue();
        assertThat(response.getProbabilityScore()).isLessThan(0.3);
        assertThat(response.getRiskLevel()).isEqualTo("LOW");
    }

    @Test
    @DisplayName("Should return HIGH risk for young customer with expensive car")
    void shouldReturnHighRisk() {
        // GIVEN
        RiskRequest request = RiskRequest.newBuilder()
                .setDni("87654321")
                .setAge(20) // Risk + 0.4
                .setCarValue(60000.0) // Risk + 0.4
                .build();
        StreamObserver<RiskResponse> responseObserver = mock(StreamObserver.class);

        // WHEN
        riskService.evaluateRisk(request, responseObserver);

        // THEN
        ArgumentCaptor<RiskResponse> responseCaptor = ArgumentCaptor.forClass(RiskResponse.class);
        verify(responseObserver, timeout(1000)).onNext(responseCaptor.capture());
        
        RiskResponse response = responseCaptor.getValue();
        assertThat(response.getProbabilityScore()).isGreaterThanOrEqualTo(0.8);
        assertThat(response.getRiskLevel()).isEqualTo("HIGH");
    }
}
