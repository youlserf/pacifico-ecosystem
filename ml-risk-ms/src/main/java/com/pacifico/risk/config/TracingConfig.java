package com.pacifico.risk.config;

import io.grpc.ServerInterceptor;
import io.micrometer.observation.ObservationPredicate;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.core.instrument.binder.grpc.ObservationGrpcServerInterceptor;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfig {

    @Bean
    public ObservationPredicate noActuatorHealth() {
        return (name, context) -> {
            if ("http.server.requests".equals(name) && context instanceof org.springframework.http.server.observation.ServerRequestObservationContext serverContext) {
                return !serverContext.getCarrier().getRequestURI().startsWith("/actuator");
            }
            return true;
        };
    }

    @Bean
    @GrpcGlobalServerInterceptor
    public ServerInterceptor observationGrpcServerInterceptor(ObservationRegistry observationRegistry) {
        return new ObservationGrpcServerInterceptor(observationRegistry);
    }
}
