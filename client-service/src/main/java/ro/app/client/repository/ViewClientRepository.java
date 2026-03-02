package ro.app.client.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.app.client.model.view.ViewClient;

public interface ViewClientRepository extends JpaRepository<ViewClient, Long> {
}
