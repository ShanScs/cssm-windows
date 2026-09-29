package org.slf4j;

public class Logger {
    private final String name;
    public Logger(String name) { this.name = name; }
    public void trace(String msg) {}
    public void trace(String format, Object... args) {}
    public void debug(String msg) {}
    public void debug(String format, Object... args) {}
    public void info(String msg) {}
    public void info(String format, Object... args) {}
    public void warn(String msg) {}
    public void warn(String format, Object... args) {}
    public void error(String msg) {}
    public void error(String format, Object... args) {}
    public void error(String msg, Throwable t) {}
    public boolean isTraceEnabled() { return false; }
    public boolean isDebugEnabled() { return false; }
    public boolean isInfoEnabled() { return false; }
    public boolean isWarnEnabled() { return false; }
    public boolean isErrorEnabled() { return false; }
}
