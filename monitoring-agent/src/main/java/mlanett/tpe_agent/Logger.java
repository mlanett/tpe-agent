package mlanett.tpe_agent;

final class Logger {
    private final String module;

    Logger(String module) { this.module = module; }

    void debug(String message) {
        System.out.printf("DEBUG [%s] %s%n", this.module, message);
    }

    void info(String message) {
        System.out.printf("INFO [%s] %s%n", this.module, message);
    }

    void warn(String message, Throwable t) {
        if (t == null) {
            System.err.printf("WARN [%s] %s%n", this.module, message);
        }
        else {
            System.err.printf("ERROR [%s] %s error=%s%n", this.module, message, t);
            t.printStackTrace(System.err);
        }
    }
}
