package com.hamiltonjewelers.ns_sf_connector.worker;

import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import com.hamiltonjewelers.ns_sf_connector.service.SyncJobService;
import com.hamiltonjewelers.ns_sf_connector.utils.WorkerIdGenerator;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class WorkerManager {
    private final SyncJobService syncJobService;
    private final int workerCount;
    private final int claimLimit;
    private final ExecutorService executor;

    public WorkerManager(SyncJobService syncJobService,
                         @Value("${app.worker.count:4}") int workerCount,
                         @Value("${app.worker.claim.limit:10}") int claimLimit) {
        this.syncJobService = syncJobService;
        this.workerCount = Math.max(1, workerCount);
        this.claimLimit = Math.max(1, claimLimit);
        this.executor = Executors.newFixedThreadPool(workerCount);
    }

    @PostConstruct
    public void start() {
        String baseWorkerId = WorkerIdGenerator.getWorkerId();

        for (int i = 0; i < workerCount; i++) {
            final String workerId = String.format("%s-%d", baseWorkerId, i);

            System.out.println("Launching Worker: " + workerId);
            executor.submit(new WorkerRunnbale(workerId));
        }
    }

    @PreDestroy
    public void shutdown() {
        System.out.println("Shutting down worker pool...");
        executor.shutdown();
        System.out.println("Worker pool shutdown initiated.");
    }

    private class WorkerRunnbale implements Runnable {
        private final String workerId;

        private WorkerRunnbale(String workerId) {
            this.workerId = workerId;
        }

        @Override
        public void run() {
            System.out.println("Starting worker " + workerId);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Instant start = Instant.now();
                    System.out.printf("[%s] Attempting to claim up to %d jobs...%n", workerId, claimLimit);
                    List<SyncJob> claimed = syncJobService.claimJobs(claimLimit, workerId);

                    System.out.printf("worker %s claimed job %s...%n", workerId,
                            claimed);

                    if (claimed.isEmpty()) {
                        System.out.printf("[%s] No jobs available. Sleeping " +
                                        "for 5 second...%n",
                                workerId);

                        Thread.sleep(Duration.ofSeconds(5).toMillis());

                        continue;
                    }

                    System.out.printf("[%s] Claimed %d jobs...%n", workerId,
                            claimed.size());

                    for (SyncJob job : claimed) {
                        System.out.printf("[%s] Starting job %s (record: %s, " +
                                        "source: %s, status: %s, attempts: " +
                                        "%s)%n",
                                workerId,
                                job.getId(),
                                job.getRecordType(),
                                job.getSourceSystem(),
                                job.getStatus(),
                                job.getAttemptCount());
                        try {
                            // TODO: perform actual sync work here
                            System.out.printf("[%s] Processing job %s...%n", workerId, job.getId());
                            // Simulate work (remove in real code)
                            Thread.sleep(500); // simulate some work

                            System.out.printf("[%s] Successfully processed job %s%n", workerId, job.getId());
                            // TODO: update job status to COMPLETED via syncJobService
                        } catch (Exception e) {
                            System.out.printf("[%s] FAILED processing job %s: %s%n",
                                    workerId, job.getId(), e.getMessage());
                            // TODO: handle failure (increment attempts, set next retry time)
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Worker " + workerId + " is interrupted");

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            System.out.println("Worker " + workerId + " stopped!");
        }
    }
}
