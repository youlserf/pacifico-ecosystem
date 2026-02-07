package com.pacifico.quotation.controller;

import com.pacifico.quotation.dto.QuotationRequest;
import com.pacifico.quotation.model.Quote;
import com.pacifico.quotation.service.QuotationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * GraphQL Controller for handling quotation-related operations.
 * <p>
 * This controller serves as the entry point for GraphQL mutations,
 * delegating business logic to {@link QuotationService}.
 */
@Controller
public class QuotationController {

    @Autowired
    private QuotationService quotationService;

    @MutationMapping
    public Map<String, Object> createQuote(@Valid @Argument QuotationRequest input) {
        Quote quote = quotationService.orchestrateQuotation(input.dni(), input.age(), input.carValue());
        return Map.of(
            "quoteId", quote.getId().toString(),
            "status", "SUCCESS",
            "message", "Quotation processed and approved"
        );
    }
}
