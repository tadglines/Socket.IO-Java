/**
 * The MIT License
 * Copyright (c) 2010 Tad Glines
 *
 * Contributors: Ovea.com, Mycila.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.glines.socketio.server;

import com.glines.socketio.annotation.Handle;
import com.glines.socketio.util.DefaultLoader;
import com.glines.socketio.util.ServiceClassLoader;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class AnnotationTransportHandlerProvider implements TransportHandlerProvider {

    private static final Logger LOGGER = Logger.getLogger(AnnotationTransportHandlerProvider.class.getName());

    private final Map<TransportType, Class<TransportHandler>> handlerClasses = new EnumMap<TransportType, Class<TransportHandler>>(TransportType.class);

    @Override
    public TransportHandler get(Class<?> handlerType, TransportType transportType) {
        Class<TransportHandler> handlerClass = handlerClasses.get(transportType);
        if (handlerClass == null)
            throw new IllegalArgumentException("No TransportHandler found for transport " + transportType);
        if (!handlerType.isAssignableFrom(handlerClass))
            throw new IllegalArgumentException("TransportHandler " + handlerClass.getName() + " is not of required type " + handlerType.getName() + " for transport " + transportType);
        return load(handlerClass);
    }

    @Override
    public boolean isSupported(TransportType type) {
        return handlerClasses.containsKey(type);
    }

    @Override
    public void init() {
        handlerClasses.clear();
        ServiceClassLoader<TransportHandler> serviceClassLoader = ServiceClassLoader.load(
                TransportHandler.class,
                new DefaultLoader(Thread.currentThread().getContextClassLoader()));
        Iterator<Class<TransportHandler>> it = serviceClassLoader.iterator();
        while (it.hasNext()) {
            Class<TransportHandler> transportHandlerClass;
            try {
                transportHandlerClass = it.next();
            } catch (Throwable e) {
                LOGGER.log(Level.INFO, "Unable to load transport hander class. Error: " + e.getMessage(), e);
                continue;
            }
            // try to load it to see if it is available
            if (load(transportHandlerClass) != null) {
                Handle handle = transportHandlerClass.getAnnotation(Handle.class);
                if (handle != null) {
                    for (TransportType type : handle.value()) {
                        handlerClasses.put(type, transportHandlerClass);
                    }
                }
            }
        }
    }

    private static TransportHandler load(Class<TransportHandler> c) {
        try {
            Constructor<TransportHandler> ctor = c.getConstructor();
            return ctor.newInstance();
        } catch (InvocationTargetException e) {
            LOGGER.log(Level.INFO, "Unable to load transport handler class " + c.getName() + ". Error: " + e.getTargetException().getMessage(), e.getTargetException());
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Unable to load transport handler class " + c.getName() + ". Error: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Map<TransportType, Class<TransportHandler>> listAll() {
        return handlerClasses;
    }

}
