package mlanett.tpe_agent;

import net.bytebuddy.asm.Advice;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Advice executed when the ThreadPoolExecutor transitions to TERMINATED.
 */
public final class ThreadPoolExecutorTerminatedAdvice {

    private ThreadPoolExecutorTerminatedAdvice() {
        // Prevent instantiation
    }

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(@Advice.This ThreadPoolExecutor executor) {
        ThreadPoolExecutorRegistry.getInstance().unregister(executor);
    }
}
