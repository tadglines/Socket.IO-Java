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
