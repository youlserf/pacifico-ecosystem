package com.pacifico.issuance.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing an issued insurance policy.
 * <p>
 * This class persists the final policy details after successful risk assessment 
 * and premium calculation.
 */
@Entity
@Table(name = "policies")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    
    private Long quoteId;
    
    @Column(unique = true)
    private String policyNumber;
    
    private String dni;
    private BigDecimal finalPremium;
    private LocalDateTime issuedAt;
}
