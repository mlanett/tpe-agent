package mlanett.tpe_agent;

/**
 * Immutable snapshot of ThreadPoolExecutor metrics.
 * This interface is on the bootstrap classpath and visible to all classloaders.
 */
public interface IThreadPoolMetrics {
    
    /**
     * @return the approximate number of threads that are actively executing tasks
     */
    int getActiveCount();
    
    /**
     * @return the approximate total number of tasks that have completed execution
     */
    long getCompletedTaskCount();
    
    /**
     * @return the core number of threads
     */
    int getCorePoolSize();
    
    /**
     * @return the largest number of threads that have ever simultaneously been in the pool
     */
    int getLargestPoolSize();
    
    /**
     * @return the maximum allowed number of threads
     */
    int getMaximumPoolSize();
    
    /**
     * @return the current number of threads in the pool
     */
    int getPoolSize();
    
    /**
     * @return the approximate total number of tasks that have ever been scheduled for execution
     */
    long getTaskCount();
    
    /**
     * @return the number of tasks currently queued for execution
     */
    int getQueuedCount();
    
    /**
     * @return true if this executor has been shut down
     */
    boolean isShutdown();
    
    /**
     * @return true if all tasks have completed following shut down
     */
    boolean isTerminated();
}
