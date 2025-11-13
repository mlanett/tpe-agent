package mlanett.tpe_agent.advice;

import net.bytebuddy.asm.Advice;

import java.util.concurrent.ThreadPoolExecutor;

import mlanett.tpe_agent.ThreadPoolExecutorRegistry;

/**
 * Advice executed when shutdown or shutdownNow completes.
 */
public final class ThreadPoolExecutorShutdownAdvice {

    private ThreadPoolExecutorShutdownAdvice() {
        // Prevent instantiation
    }

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(@Advice.This ThreadPoolExecutor executor) {
        ThreadPoolExecutorRegistry.getInstance().unregister(executor);
    }
}
