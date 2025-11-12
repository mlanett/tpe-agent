package mlanett.tpe_agent;

import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Summary of ThreadPoolExecutor metrics.
 */
public final class ThreadPoolExecutorMetrics {
    private final int queuedCount;
    private final int activeCount;
    private final long completedTaskCount;
    private final long taskCount;
    private final int corePoolSize;
    private final int maximumPoolSize;
    private final int poolSize;

    public ThreadPoolExecutorMetrics(
            int queuedCount,
            int activeCount,
            long completedTaskCount,
            long taskCount,
            int corePoolSize,
            int maximumPoolSize,
            int poolSize) {
        this.queuedCount = queuedCount;
        this.activeCount = activeCount;
        this.completedTaskCount = completedTaskCount;
        this.taskCount = taskCount;
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.poolSize = poolSize;
    }

    public static ThreadPoolExecutorMetrics of(ThreadPoolExecutor executor) {
        return new ThreadPoolExecutorMetrics(
            executor.getQueue().size(),
            executor.getActiveCount(),
            executor.getCompletedTaskCount(),
            executor.getTaskCount(),
            executor.getCorePoolSize(),
            executor.getMaximumPoolSize(),
            executor.getPoolSize()
        );
    }

    /**
     * The number of tasks waiting in the queue.
     */
    public int getQueuedCount() {
        return queuedCount;
    }

    /**
     * The approximate number of threads that are actively executing tasks.
     */
    public int getActiveCount() {
        return activeCount;
    }

    /**
     * The approximate total number of tasks that have completed execution.
     */
    public long getCompletedTaskCount() {
        return completedTaskCount;
    }

    /**
     * The approximate total number of tasks that have ever been scheduled for execution (e.g. completed + active + queued).
     */
    public long getTaskCount() {
        return taskCount;
    }

    /**
     * The number of threads to keep in the pool, even if they are idle, unless allowCoreThreadTimeOut is set.
     */
    public int getCorePoolSize() {
        return corePoolSize;
    }

    /**
     * The maximum number of threads allowed in the pool.
     */
    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    /**
     * The current number of threads in the pool.
     */
    public int getPoolSize() {
        return poolSize;
    }

    @Override
    public String toString() {
        return "ThreadPoolExecutor[queue=" + queueSize +
               ", active=" + activeCount +
               ", completed=" + completedTaskCount +
               ", total=" + taskCount +
               ", core=" + corePoolSize +
               ", max=" + maximumPoolSize +
               ", current=" + poolSize + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThreadPoolExecutorMetrics that = (ThreadPoolExecutorMetrics) o;
        return queuedCount == that.queuedCount &&
               activeCount == that.activeCount &&
               completedTaskCount == that.completedTaskCount &&
               taskCount == that.taskCount &&
               corePoolSize == that.corePoolSize &&
               maximumPoolSize == that.maximumPoolSize &&
               poolSize == that.poolSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(queuedCount, activeCount, completedTaskCount, taskCount, corePoolSize, maximumPoolSize, poolSize);
    }
}
