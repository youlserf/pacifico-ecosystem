package com.pacifico.quotation.dto;

import java.io.Serializable;
/**
 * DTO representing a cached risk assessment result.
 * <p>
 * This record is used to store risk data in Redis to avoid redundant gRPC calls.
 *
 * @param probabilityScore The inferred probability of risk.
 * @param riskLevel The categorized risk level (e.g., LOW, MEDIUM, HIGH).
 */
public record RiskCacheEntry(double probabilityScore, String riskLevel) implements Serializable {
}
