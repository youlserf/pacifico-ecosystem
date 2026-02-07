package com.pacifico.quotation.repository;

import com.pacifico.quotation.model.Quote;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repository interface for {@link Quote} entities.
 * <p>
 * Provides standard CRUD operations and custom optimized fetching using Entity Graphs.
 */
public interface QuoteRepository extends JpaRepository<Quote, Long> {
    
    @EntityGraph(value = "Quote.all", type = EntityGraph.EntityGraphType.LOAD)
    Optional<Quote> findById(Long id);
}
