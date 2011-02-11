package com.glines.socketio.server;

import com.glines.socketio.util.DefaultLoader;
import com.glines.socketio.util.ServiceClassLoader;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mathieu Carbou
 */
public final class ClasspathTransportDiscovery implements TransportDiscovery {

    private static final Logger LOGGER = Logger.getLogger(ClasspathTransportDiscovery.class.getName());

    @Override
    public Iterable<Transport> discover() {
        List<Transport> transports = new LinkedList<Transport>();
        // discover transports lazily
        ServiceClassLoader<Transport> serviceClassLoader = ServiceClassLoader.load(
                Transport.class,
                new DefaultLoader(Thread.currentThread().getContextClassLoader()));
        Iterator<Class<Transport>> it = serviceClassLoader.iterator();
        while (it.hasNext()) {
            Class<Transport> transportClass;
            try {
                transportClass = it.next();
            } catch (Throwable e) {
                LOGGER.log(Level.INFO, "Unable to load transport class: Error: " + e.getMessage(), e);
                continue;
            }
            try {
                Constructor<Transport> ctor = transportClass.getConstructor();
                transports.add(ctor.newInstance());
            } catch (InvocationTargetException e) {
                LOGGER.log(Level.INFO, "Unable to load transport class " + transportClass.getName() + ". Error: " + e.getTargetException().getMessage(), e.getTargetException());
            } catch (Throwable e) {
                LOGGER.log(Level.INFO, "Unable to load transport class " + transportClass.getName() + ". Error: " + e.getMessage(), e);
            }
        }
        return transports;
    }
}
