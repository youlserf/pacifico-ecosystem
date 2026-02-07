package com.pacifico.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * Entry point for the API Gateway service.
 * <p>
 * This gateway acts as the single entry point for the Pacifico Insurance Ecosystem,
 * providing routing, security, and global logging.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    private static final Logger logger = LoggerFactory.getLogger(ApiGatewayApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    /**
     * Global filter for logging incoming requests and outgoing responses.
     * <p>
     * Provides visibility into transaction flow at the network edge.
     */
    @Bean
    @Order(-1)
    public GlobalFilter loggingFilter() {
        return (exchange, chain) -> {
            logger.info(">>> Gateway Routing Request: {} {}", 
                exchange.getRequest().getMethod(), 
                exchange.getRequest().getURI());
            return chain.filter(exchange).then(Mono.fromRunnable(() -> 
                logger.info("<<< Gateway Response Status: {}", exchange.getResponse().getStatusCode())
            ));
        };
    }
}
