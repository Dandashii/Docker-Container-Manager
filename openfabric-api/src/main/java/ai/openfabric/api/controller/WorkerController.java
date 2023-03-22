package ai.openfabric.api.controller;

import ai.openfabric.api.model.Worker;
import ai.openfabric.api.repository.WorkerRepository;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.CpuStatsConfig;
import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.InvocationBuilder;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Duration;

@RestController
@RequestMapping("${node.api.path}/worker")
public class WorkerController {

    private Worker worker;
    private WorkerRepository repository;
    private DockerClient dockerClient;
    private DockerClientConfig config;
    private DockerHttpClient httpClient;

    public WorkerController(WorkerRepository repository) {
        this.repository = repository;
        this.config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        this.httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
        this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
    }



    @GetMapping(path = "/getWorkers")
    public Page<Worker> getWorkers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size ) {

        Pageable pageRequest = PageRequest.of(page, size);

        return repository.findAll(pageRequest);
    }

    @PostMapping("/startWorker/{containerName}")
    public ResponseEntity<String> startDocker(@PathVariable String containerName) throws DockerException {
        worker = repository.findByName(containerName);

        if(worker != null) {
            if (worker.isRunning()) {
                return ResponseEntity.ok("The docker container: " + worker.getName() + " is already running!");
            } else {
                dockerClient.startContainerCmd(containerName).exec();

                worker.setStatus("running");
                worker.onUpdate();

                try {
                    repository.save(worker);
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating the docker's status in the database" + e.getMessage());
                }

                return ResponseEntity.ok("The docker container: " + worker.getName() + " has been started successfully");
            }
        } else {
            try {
                dockerClient.startContainerCmd(containerName).exec();

                worker = new Worker();

                worker.setName(containerName);
                worker.setStatus("running");

                try {
                    repository.save(worker);
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating the worker's status due to: " + e.getMessage());
                }

                return ResponseEntity.ok("Docker container has started successfully");
            } catch (DockerException e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error starting the worker due to: " + e.getMessage());
            }
        }
    }

    @PostMapping("/stopWorker/{containerName}")
    public ResponseEntity<String> stopDockerContainer(@PathVariable String containerName) {
        worker = repository.findByName(containerName);

        if(worker != null) {
            if (worker.isRunning()) {
                worker.setStatus("stopped");
                worker.onUpdate();

                try {
                    repository.save(worker);
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating the worker's status due to: " + e.getMessage());
                }

                try {
                    dockerClient.stopContainerCmd(containerName).exec();
                    return ResponseEntity.ok("The docker container: " + worker.getName() + " has been stopped!");
                } catch (DockerException e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to stop the worker due to: " + e.getMessage());
                }

            } else {
                return ResponseEntity.ok("The worker: " + worker.getName() + " is already stopped!!");
            }
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("The worker: " + containerName + " doesnt exist!");
        }
    }

    @PostMapping("/getWorkerStats/{containerName}")
    public Statistics getWorkerStats(@PathVariable String containerName) {
        InvocationBuilder.AsyncResultCallback<Statistics> callback = new InvocationBuilder.AsyncResultCallback<>();
        dockerClient.statsCmd(containerName).exec(callback);
        Statistics stats = new Statistics();

        try {
            stats = callback.awaitResult();
            callback.close();
        } catch (RuntimeException | IOException e) {
            throw new RuntimeException(e.getMessage());
        }

        return stats;
    }

    @PostMapping("/getWorkerInfo/{containerName}")
    public Worker getWorkerInfo(@PathVariable String containerName) {
        return repository.findByName(containerName);
    }
}
