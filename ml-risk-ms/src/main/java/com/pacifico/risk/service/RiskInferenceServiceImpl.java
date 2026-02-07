package com.pacifico.risk.service;

import com.pacifico.risk.RiskInferenceServiceGrpc;
import com.pacifico.risk.RiskRequest;
import com.pacifico.risk.RiskResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Implementation of the {@link RiskInferenceServiceGrpc} for high-performance risk scoring.
 * <p>
 * This service acts as a mock for a Machine Learning model (e.g., ONNX, TensorFlow).
 * It uses Project Loom's Virtual Threads to provide non-blocking execution for 
 * inference tasks, ensuring that the gRPC server threads are not held up during 
 * computation-heavy operations.
 */
@GrpcService
public class RiskInferenceServiceImpl extends RiskInferenceServiceGrpc.RiskInferenceServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(RiskInferenceServiceImpl.class);
    
    /**
     * Dedicated executor for inference tasks using Virtual Threads.
     * This allows scaling highly concurrent risk assessment requests with minimal resource overhead.
     */
    private final ExecutorService inferenceExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Evaluates the risk score for a specific customer based on automotive profile data.
     * <p>
     * The processing is offloaded to the {@code inferenceExecutor} to maintain gRPC responsiveness.
     *
     * @param request The risk evaluation request containing customer DNI and vehicle data.
     * @param responseObserver Observer for the risk inference results.
     */
    @Override
    public void evaluateRisk(RiskRequest request, StreamObserver<RiskResponse> responseObserver) {
        inferenceExecutor.submit(() -> {
            try {
                logger.info("Evaluating risk for DNI: {}", request.getDni());
                
                // MOCK ML Logic (Simulating ONNX Runtime)
                double score = calculateMockScore(request);
                String riskLevel = determineRiskLevel(score);

                RiskResponse response = RiskResponse.newBuilder()
                        .setProbabilityScore(score)
                        .setRiskLevel(riskLevel)
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (Exception e) {
                logger.error("Error during inference", e);
                responseObserver.onError(e);
            }
        });
    }

    private double calculateMockScore(RiskRequest request) {
        // Simple mock logic: age and car value impact risk
        double score = (request.getAge() < 25 ? 0.4 : 0.1) + (request.getCarValue() > 50000 ? 0.4 : 0.1);
        return Math.min(score, 1.0);
    }

    private String determineRiskLevel(double score) {
        if (score < 0.3) return "LOW";
        if (score < 0.7) return "MEDIUM";
        return "HIGH";
    }
}
