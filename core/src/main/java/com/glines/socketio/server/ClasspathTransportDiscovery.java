package com.glines.socketio.server;

import com.glines.socketio.util.DefaultLoader;
import com.glines.socketio.util.ServiceClassLoader;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
        for (Class<Transport> transportClass : serviceClassLoader) {
            try {
                Constructor<Transport> ctor = transportClass.getConstructor();
                transports.add(ctor.newInstance());
            } catch (InvocationTargetException e) {
                LOGGER.log(Level.WARNING, "Unable to load transport class " + transportClass.getName() + " : " + e.getTargetException().getMessage());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Unable to load transport class " + transportClass.getName() + " : " + e.getMessage());
            }
        }
        return transports;
    }
}
