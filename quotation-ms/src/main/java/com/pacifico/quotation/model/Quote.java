package com.pacifico.quotation.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing an insurance quotation.
 * <p>
 * This class stores the details of a quotation request and its associated risk assessment.
 * It uses {@link NamedEntityGraph} to optimize database fetching and avoid N+1 issues.
 */
@Entity
@Table(name = "quotes")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@NamedEntityGraph(name = "Quote.all", attributeNodes = {
    @NamedAttributeNode("dni"),
    @NamedAttributeNode("status")
})
public class Quote implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    
    private String dni;
    private Integer age;
    private BigDecimal carValue;
    private Double probabilityScore;
    private String riskLevel;
    private String status;
    private LocalDateTime createdAt;
}
