package com.pacifico.issuance.repository;

import com.pacifico.issuance.model.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<Policy, Long> {
}
