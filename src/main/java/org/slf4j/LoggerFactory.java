package org.slf4j;

public class LoggerFactory {
    private static final Logger NOP = new Logger() {
        public void trace(String msg) {}
        public void trace(String format, Object arg) {}
        public void trace(String format, Object arg1, Object arg2) {}
        public void trace(String format, Object... args) {}
        public void trace(String msg, Throwable t) {}
        public void debug(String msg) {}
        public void debug(String format, Object arg) {}
        public void debug(String format, Object arg1, Object arg2) {}
        public void debug(String format, Object... args) {}
        public void debug(String msg, Throwable t) {}
        public void info(String msg) {}
        public void info(String format, Object arg) {}
        public void info(String format, Object arg1, Object arg2) {}
        public void info(String format, Object... args) {}
        public void info(String msg, Throwable t) {}
        public void warn(String msg) {}
        public void warn(String format, Object arg) {}
        public void warn(String format, Object arg1, Object arg2) {}
        public void warn(String format, Object... args) {}
        public void warn(String msg, Throwable t) {}
        public void error(String msg) {}
        public void error(String format, Object arg) {}
        public void error(String format, Object arg1, Object arg2) {}
        public void error(String format, Object... args) {}
        public void error(String msg, Throwable t) {}
        public boolean isTraceEnabled() { return false; }
        public boolean isDebugEnabled() { return false; }
        public boolean isInfoEnabled() { return false; }
        public boolean isWarnEnabled() { return false; }
        public boolean isErrorEnabled() { return false; }
    };

    public static Logger getLogger(Class<?> clazz) {
        return NOP;
    }
    public static Logger getLogger(String name) {
        return NOP;
    }
}
