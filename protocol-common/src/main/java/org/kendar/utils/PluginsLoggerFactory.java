package org.kendar.utils;

import org.kendar.di.annotations.TpmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TpmService
public class PluginsLoggerFactory {
    public Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
}
