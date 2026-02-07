package com.pacifico.gateway.config;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;

@Configuration
public class ObservationConfig {

    @Bean
    public ObservationPredicate noActuatorHealth() {
        return (name, context) -> {
            if (context instanceof ServerRequestObservationContext serverContext) {
                return !serverContext.getCarrier().getURI().getPath().startsWith("/actuator");
            }
            return true;
        };
    }
}
