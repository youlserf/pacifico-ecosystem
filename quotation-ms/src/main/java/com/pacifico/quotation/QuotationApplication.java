package com.pacifico.quotation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import net.devh.boot.grpc.client.autoconfigure.GrpcClientMetricAutoConfiguration;

@SpringBootApplication(exclude = {GrpcClientMetricAutoConfiguration.class})
@EnableJpaRepositories
public class QuotationApplication {
    public static void main(String[] args) {
        SpringApplication.run(QuotationApplication.class, args);
    }

}
