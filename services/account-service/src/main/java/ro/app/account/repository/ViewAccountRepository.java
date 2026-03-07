package ro.app.account.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.app.account.model.view.ViewAccount;

public interface ViewAccountRepository extends JpaRepository<ViewAccount, Long> {

}
