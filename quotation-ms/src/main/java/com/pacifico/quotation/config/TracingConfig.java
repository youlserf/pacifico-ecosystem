package com.pacifico.quotation.config;

import io.grpc.ClientInterceptor;
import io.micrometer.observation.ObservationPredicate;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.core.instrument.binder.grpc.ObservationGrpcClientInterceptor;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
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
    @GrpcGlobalClientInterceptor
    public ClientInterceptor observationGrpcClientInterceptor(ObservationRegistry observationRegistry) {
        return new ObservationGrpcClientInterceptor(observationRegistry);
    }
}
