package com.pacifico.issuance.config;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;

@Configuration
public class ObservationConfig {

    @Bean
    public ObservationPredicate noActuatorHealth() {
        return (name, context) -> {
            if ("http.server.requests".equals(name) && context instanceof ServerRequestObservationContext) {
                ServerRequestObservationContext serverContext = (ServerRequestObservationContext) context;
                return !serverContext.getCarrier().getRequestURI().startsWith("/actuator");
            }
            return true;
        };
    }
}
