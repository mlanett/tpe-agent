package mlanett.tpe_agent;

import mlanett.tpe_agent.IThreadPoolMetrics;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Implementation of IThreadPoolMetrics that wraps a ThreadPoolExecutor.
 * This is a simple immutable snapshot of the executor's state.
 */
final class ThreadPoolMetrics implements IThreadPoolMetrics {
    
    private final int activeCount;
    private final long completedTaskCount;
    private final int corePoolSize;
    private final int largestPoolSize;
    private final int maximumPoolSize;
    private final int poolSize;
    private final long taskCount;
    private final int queuedCount;
    private final boolean shutdown;
    private final boolean terminated;
    
    private ThreadPoolMetrics(
            int activeCount,
            long completedTaskCount,
            int corePoolSize,
            int largestPoolSize,
            int maximumPoolSize,
            int poolSize,
            long taskCount,
            int queuedCount,
            boolean shutdown,
            boolean terminated) {
        this.activeCount = activeCount;
        this.completedTaskCount = completedTaskCount;
        this.corePoolSize = corePoolSize;
        this.largestPoolSize = largestPoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.poolSize = poolSize;
        this.taskCount = taskCount;
        this.queuedCount = queuedCount;
        this.shutdown = shutdown;
        this.terminated = terminated;
    }
    
    /**
     * Create a snapshot of the given ThreadPoolExecutor's metrics.
     */
    static ThreadPoolMetrics of(ThreadPoolExecutor executor) {
        return new ThreadPoolMetrics(
            executor.getActiveCount(),
            executor.getCompletedTaskCount(),
            executor.getCorePoolSize(),
            executor.getLargestPoolSize(),
            executor.getMaximumPoolSize(),
            executor.getPoolSize(),
            executor.getTaskCount(),
            executor.getQueue().size(),
            executor.isShutdown(),
            executor.isTerminated()
        );
    }
    
    @Override
    public int getActiveCount() {
        return activeCount;
    }
    
    @Override
    public long getCompletedTaskCount() {
        return completedTaskCount;
    }
    
    @Override
    public int getCorePoolSize() {
        return corePoolSize;
    }
    
    @Override
    public int getLargestPoolSize() {
        return largestPoolSize;
    }
    
    @Override
    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }
    
    @Override
    public int getPoolSize() {
        return poolSize;
    }
    
    @Override
    public long getTaskCount() {
        return taskCount;
    }
    
    @Override
    public int getQueuedCount() {
        return queuedCount;
    }
    
    @Override
    public boolean isShutdown() {
        return shutdown;
    }
    
    @Override
    public boolean isTerminated() {
        return terminated;
    }
}

