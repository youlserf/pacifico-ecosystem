package com.pacifico.quotation.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class HighRiskException extends RuntimeException {
    public HighRiskException(String message) {
        super(message);
    }
}
