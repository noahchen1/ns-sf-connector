package com.hamiltonjewelers.ns_sf_connector.service.sync.job;

import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import com.hamiltonjewelers.ns_sf_connector.utils.WorkerIdGenerator;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@ConditionalOnProperty(name = "app.worker.enabled", havingValue = "true", matchIfMissing = true)
public class SyncJobWorker {
    private final SyncJobService syncJobService;
    private final SyncJobDispatcher dispatcher;
    private final int workerCount;
    private final int claimLimit;
    private final ExecutorService executor;

    public SyncJobWorker(SyncJobService syncJobService,
                         SyncJobDispatcher dispatcher,
                         @Value("${app.worker.count:4}") int workerCount,
                         @Value("${app.worker.claim.limit:10}") int claimLimit) {
        this.syncJobService = syncJobService;
        this.dispatcher = dispatcher;
        this.workerCount = Math.max(1, workerCount);
        this.claimLimit = Math.max(1, claimLimit);
        this.executor = Executors.newFixedThreadPool(this.workerCount);
    }

    @PostConstruct
    public void start() {
        String baseWorkerId = WorkerIdGenerator.getWorkerId();

        for (int i = 0; i < workerCount; i++) {
            final String workerId = String.format("%s-%d", baseWorkerId, i);

            System.out.println("Launching Worker: " + workerId);
            executor.submit(new WorkerRunnable(workerId));
        }
    }

    @PreDestroy
    public void shutdown() {
        System.out.println("Shutting down worker pool...");
        executor.shutdown();
        System.out.println("Worker pool shutdown initiated.");
    }

    private class WorkerRunnable implements Runnable {
        private final String workerId;

        private WorkerRunnable(String workerId) {
            this.workerId = workerId;
        }

        @Override
        public void run() {
            System.out.println("Starting worker " + workerId);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    System.out.printf("[%s] Attempting to claim up to %d jobs...%n", workerId, claimLimit);
                    List<SyncJob> claimed = syncJobService.claim(claimLimit, workerId);

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
                            System.out.printf("[%s] Processing job %s...%n", workerId, job.getId());
                            dispatcher.dispatch(job);
                            boolean completed = syncJobService.complete(job.getId(), workerId);
                            System.out.printf(
                                    completed
                                            ? "[%s] Successfully processed job %s%n"
                                            : "[%s] Job %s was superseded during processing%n",
                                    workerId,
                                    job.getId()
                            );
                        } catch (Exception e) {
                            System.out.printf("[%s] FAILED processing job %s: %s%n",
                                    workerId, job.getId(), e.getMessage());
                            syncJobService.fail(job.getId(), workerId, e);
                        }
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    System.out.printf("[%s] Worker loop failed: %s%n", workerId, e.getMessage());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            System.out.println("Worker " + workerId + " stopped!");
        }
    }
}
