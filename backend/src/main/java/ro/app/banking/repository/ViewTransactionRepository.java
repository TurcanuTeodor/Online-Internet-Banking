package ro.app.banking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.app.banking.model.view.ViewTransaction;

public interface ViewTransactionRepository extends JpaRepository<ViewTransaction, Long> {
    
}
