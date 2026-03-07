package ro.app.transaction.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.app.transaction.model.view.ViewTransaction;

public interface ViewTransactionRepository extends JpaRepository<ViewTransaction, Long> {
}
