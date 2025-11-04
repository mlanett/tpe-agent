import mlanett.tpe_agent.ThreadPoolExecutorMetrics;
import mlanett.tpe_agent.ThreadPoolExecutorRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstration of ThreadPoolExecutor monitoring agent.
 * Creates multiple thread pools with different workloads and periodically logs their status.
 */
public class Demo {
    private static final int RUN_DURATION_SECONDS = 10;
    private static final int MONITORING_INTERVAL_SECONDS = 2;
    private static final AtomicInteger threadCounter = new AtomicInteger(0);

    private final List<ThreadPoolExecutor> executors = new ArrayList<>();
    private volatile boolean running = true;

    public static void main(String[] args) {
        Demo demo = new Demo();
        try {
            demo.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Demo interrupted");
        }
    }

    private void run() throws InterruptedException {
        System.out.println("=== ThreadPoolExecutor Monitoring Demo ===\n");
        System.out.println("Starting demo with " + RUN_DURATION_SECONDS + " second duration");
        System.out.println("Monitoring interval: " + MONITORING_INTERVAL_SECONDS + " seconds\n");

        // Create thread pools with different configurations
        ThreadPoolExecutor fastPool = createPool("fast-pool", 2, 4, 10);
        ThreadPoolExecutor slowPool = createPool("slow-pool", 3, 5, 20);
        ThreadPoolExecutor burstPool = createPool("burst-pool", 1, 5, 15);

        executors.add(fastPool);
        executors.add(slowPool);
        executors.add(burstPool);

        // Start workload generators
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::generateFastWorkload, 0, 100, TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(this::generateSlowWorkload, 0, 500, TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(this::generateBurstWorkload, 0, 300, TimeUnit.MILLISECONDS);

        // Start monitoring loop
        Thread monitoringThread = new Thread(this::monitoringLoop);
        monitoringThread.start();

        // Run for specified duration
        Thread.sleep(RUN_DURATION_SECONDS * 1000L);
        running = false;

        // Shutdown
        System.out.println("\n=== Shutting down ===");
        scheduler.shutdown();
        monitoringThread.join(2000);

        for (ThreadPoolExecutor executor : executors) {
            executor.shutdown();
        }

        // Wait for all tasks to complete
        boolean allTerminated = true;
        for (ThreadPoolExecutor executor : executors) {
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    allTerminated = false;
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
            }
        }

        // Final status report
        printStatusReport();
        System.out.println("\n=== Demo complete ===");
    }

    private ThreadPoolExecutor createPool(String namePrefix, int coreSize, int maxSize, int queueCapacity) {
        ThreadFactory factory = new ThreadFactory() {
            final String prefix = namePrefix;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, prefix + "-" + threadCounter.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        };

        return new ThreadPoolExecutor(
            coreSize,
            maxSize,
            60L,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(queueCapacity),
            factory,
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    private void generateFastWorkload() {
        if (!running) return;
        ThreadPoolExecutor executor = executors.get(0);
        if (executor.isShutdown()) return;

        executor.submit(() -> {
            try {
                Thread.sleep(100 + (long)(Math.random() * 100)); // 100-200ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void generateSlowWorkload() {
        if (!running) return;
        ThreadPoolExecutor executor = executors.get(1);
        if (executor.isShutdown()) return;

        executor.submit(() -> {
            try {
                Thread.sleep(500 + (long)(Math.random() * 500)); // 500-1000ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void generateBurstWorkload() {
        if (!running) return;
        ThreadPoolExecutor executor = executors.get(2);
        if (executor.isShutdown()) return;

        // Occasionally submit multiple tasks at once
        int taskCount = Math.random() > 0.7 ? 3 : 1;
        for (int i = 0; i < taskCount; i++) {
            executor.submit(() -> {
                try {
                    Thread.sleep(200 + (long)(Math.random() * 400)); // 200-600ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }

    private void monitoringLoop() {
        while (running) {
            try {
                Thread.sleep(MONITORING_INTERVAL_SECONDS * 1000L);
                printStatusReport();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void printStatusReport() {
        Map<String, ThreadPoolExecutorMetrics> snapshot = 
            ThreadPoolExecutorRegistry.getInstance().snapshot();

        if (snapshot.isEmpty()) {
            System.out.println("[No thread pools discovered yet]");
            return;
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.printf("%-20s %8s %8s %12s %12s %6s %6s %6s%n",
            "Pool Name", "Queue", "Active", "Completed", "Total", "Core", "Max", "Size");
        System.out.println("-".repeat(80));

        for (Map.Entry<String, ThreadPoolExecutorMetrics> entry : snapshot.entrySet()) {
            ThreadPoolExecutorMetrics metrics = entry.getValue();
            System.out.printf("%-20s %8d %8d %12d %12d %6d %6d %6d%n",
                entry.getKey(),
                metrics.getQueueSize(),
                metrics.getActiveCount(),
                metrics.getCompletedTaskCount(),
                metrics.getTaskCount(),
                metrics.getCorePoolSize(),
                metrics.getMaximumPoolSize(),
                metrics.getPoolSize());
        }

        System.out.println("=".repeat(80));
    }
}

