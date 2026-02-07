package com.pacifico.risk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import net.devh.boot.grpc.server.autoconfigure.GrpcServerMetricAutoConfiguration;

@SpringBootApplication(exclude = {GrpcServerMetricAutoConfiguration.class})
public class MlRiskApplication {

    public static void main(String[] args) {
        SpringApplication.run(MlRiskApplication.class, args);
    }

}
