package ai.openfabric.api.repository;

import ai.openfabric.api.model.Worker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface WorkerRepository extends CrudRepository<Worker, String> {
    Optional<Worker> findById(String id);
    Page<Worker> findAll(Pageable pageable);
}
