package mlanett.tpe_agent.util;

public final class Logger {
    private final String module;

    public Logger(String module) { this.module = module; }

    public void debug(String message) {
        System.out.printf("DEBUG [%s] %s%n", module, message);
    }

    public void info(String message) {
        System.out.printf("INFO [%s] %s%n", module, message);
    }

    public void warn(String message) {
        System.err.printf("WARN [%s] %s%n", module, message);
    }

    public void error(String message, Throwable t) {
        System.err.printf("ERROR [%s] %s error=%s%n", module, message, t);
        if (t != null) {
            t.printStackTrace(System.err);
        }
    }
}
