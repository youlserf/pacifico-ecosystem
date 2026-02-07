package com.pacifico.risk.config;

import io.grpc.*;
import io.micrometer.tracing.Tracer;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * gRPC Server Interceptor for distributed tracing propagation.
 * <p>
 * This interceptor captures the current traceId from the Micrometer Tracer 
 * and populates the SLF4J MDC (Mapped Diagnostic Context), ensuring that 
 * the TraceID is automatically included in all log statements throughout 
 * the request lifecycle.
 */
@GrpcGlobalServerInterceptor
public class TraceIdInterceptor implements ServerInterceptor {

    @Autowired
    private Tracer tracer;

    @Override
    public <I, O> ServerCall.Listener<I> interceptCall(
            ServerCall<I, O> call,
            Metadata headers,
            ServerCallHandler<I, O> next) {
        
        MDC.put("traceId", tracer.currentSpan() != null ? tracer.currentSpan().context().traceId() : "N/A");
        
        try {
            return next.startCall(call, headers);
        } finally {
            // MDC clear is handled by the framework usually, but for gRPC async 
            // it's better to be careful. However, Micrometer Tracing usually 
            // handles this. This Interceptor ensures visibility in logs.
        }
    }
}
