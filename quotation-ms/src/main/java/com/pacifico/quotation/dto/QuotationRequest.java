package com.pacifico.quotation.dto;

import jakarta.validation.constraints.*;

/**
 * Data Transfer Object for quotation creation requests.
 * <p>
 * Enforces validation rules for customer data using Jakarta Bean Validation.
 */
public record QuotationRequest(
    @NotBlank(message = "DNI is required")
    @Pattern(regexp = "^[0-9]{8}$", message = "DNI must be 8 digits")
    String dni,

    @Min(value = 18, message = "Age must be at least 18")
    @Max(value = 99, message = "Age must be less than 100")
    int age,

    @Positive(message = "Car value must be positive")
    double carValue
) {}
