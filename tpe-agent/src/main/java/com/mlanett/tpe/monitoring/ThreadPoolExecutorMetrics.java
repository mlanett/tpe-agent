package com.mlanett.tpe.monitoring;

import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Summary of ThreadPoolExecutor metrics.
 */
public final class ThreadPoolExecutorMetrics {
    private final int queueSize;
    private final int activeCount;
    private final long completedTaskCount;
    private final long taskCount;
    private final int corePoolSize;
    private final int maximumPoolSize;
    private final int poolSize;

    public ThreadPoolExecutorMetrics(
            int queueSize,
            int activeCount,
            long completedTaskCount,
            long taskCount,
            int corePoolSize,
            int maximumPoolSize,
            int poolSize) {
        this.queueSize = queueSize;
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

    public int getQueueSize() {
        return queueSize;
    }

    public int getActiveCount() {
        return activeCount;
    }

    public long getCompletedTaskCount() {
        return completedTaskCount;
    }

    public long getTaskCount() {
        return taskCount;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

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
        return queueSize == that.queueSize &&
               activeCount == that.activeCount &&
               completedTaskCount == that.completedTaskCount &&
               taskCount == that.taskCount &&
               corePoolSize == that.corePoolSize &&
               maximumPoolSize == that.maximumPoolSize &&
               poolSize == that.poolSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(queueSize, activeCount, completedTaskCount, taskCount,
                           corePoolSize, maximumPoolSize, poolSize);
    }
}
