/*
 * Copyright (c) 2025 x_Kiva_x
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 */
package org.kiva.mchangehighlighter.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

@SuppressWarnings("unused")
public class MLogger {
    private final Logger logger;

    public MLogger(Logger mc_logger) {
        this.logger = mc_logger;
    }
    public MLogger(String name) {
        this(LogManager.getLogger(name));
    }

    private String edit(Level level, String message) {
        if (level == Level.DEBUG) return String.format("[DEBUG-%s]: %s", logger.getName(), message);
        return String.format("[%s]: %s", logger.getName(), message);
    }

    private void log(Level level, String message, Object... args) {if (!logger.isEnabled(level)) return; logger.log(level, edit(level, message), args);}
    public void trace(String message, Object... args) {log(Level.TRACE, message, args);}
    public void debug(String message, Object... args) {
        log(Level.DEBUG, message, args);
    }
    public void info(String message, Object... args) {
        log(Level.INFO, message, args);
    }
    public void warn(String message, Object... args) {log(Level.WARN, message, args);}
    public void error(String message, Object... args) {log(Level.ERROR, message, args);}
    public void fatal(String message, Object... args) {log(Level.FATAL, message, args);}
}
