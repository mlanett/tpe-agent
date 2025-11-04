package com.mlanett.tpe.monitoring;

import net.bytebuddy.asm.Advice;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Advice executed when a ThreadPoolExecutor constructor completes.
 */
public final class ThreadPoolExecutorConstructorAdvice {

    private ThreadPoolExecutorConstructorAdvice() {
        // Prevent instantiation
    }

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(@Advice.This ThreadPoolExecutor executor) {
        ThreadPoolExecutorRegistry.getInstance().register(executor);
    }
}
