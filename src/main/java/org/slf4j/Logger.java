package org.slf4j;

public interface Logger {
    void trace(String msg);
    void trace(String format, Object... args);
    void debug(String msg);
    void debug(String format, Object... args);
    void info(String msg);
    void info(String format, Object... args);
    void warn(String msg);
    void warn(String format, Object... args);
    void error(String msg);
    void error(String format, Object... args);
    void error(String msg, Throwable t);
    boolean isTraceEnabled();
    boolean isDebugEnabled();
    boolean isInfoEnabled();
    boolean isWarnEnabled();
    boolean isErrorEnabled();
}
