package com.orthowatch.repository;

import com.orthowatch.model.RiskRule;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskRuleRepository extends JpaRepository<RiskRule, UUID> {
  Optional<RiskRule> findByRuleName(String ruleName);
}
