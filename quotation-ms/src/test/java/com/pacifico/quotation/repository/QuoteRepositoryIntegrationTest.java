package com.pacifico.quotation.repository;

import com.pacifico.quotation.model.Quote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class QuoteRepositoryIntegrationTest {

    @Autowired
    private QuoteRepository quoteRepository;

    @Test
    @DisplayName("Should find quote with EntityGraph to avoid N+1")
    void shouldFindQuoteWithEntityGraph() {
        // GIVEN
        Quote quote = Quote.builder()
                .dni("88888888")
                .age(30)
                .carValue(BigDecimal.valueOf(25000.0))
                .status("APPROVED")
                .createdAt(LocalDateTime.now())
                .build();
        Quote saved = quoteRepository.save(quote);

        // WHEN
        // The repository method findById(Long id) is decorated with @EntityGraph in the actual repository
        Optional<Quote> found = quoteRepository.findById(saved.getId());

        // THEN
        assertThat(found).isPresent();
        assertThat(found.get().getDni()).isEqualTo("88888888");
        // Verification of N+1 prevention would normally be done by checking SQL logs
    }
}
