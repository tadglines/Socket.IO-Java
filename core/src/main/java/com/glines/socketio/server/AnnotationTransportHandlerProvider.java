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
final class AnnotationTransportHandlerProvider implements TransportHandlerProvider {

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
