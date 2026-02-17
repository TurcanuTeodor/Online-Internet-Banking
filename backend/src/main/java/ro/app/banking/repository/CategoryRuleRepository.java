package ro.app.banking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ro.app.banking.model.entity.CategoryRule;
import ro.app.banking.model.enums.TransactionCategory;

@Repository
public interface CategoryRuleRepository extends JpaRepository<CategoryRule, Long> {
    Optional<CategoryRule> findByKeywordIgnoreCase(String keyword);
    List<CategoryRule> findByCategory(TransactionCategory category);
}
