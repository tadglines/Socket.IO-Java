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
package com.glines.socketio.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ServiceConfigurationError;

/**
 * @author Mathieu Carbou <mathieu.carbou@gmail.com>
 * @param <S> The type of the service to be loaded by this loader
 */
public final class ServiceClassLoader<S> implements Iterable<Class<S>> {

    private static final String PREFIX = "META-INF/services/";
    private final Class<S> service;
    private final Loader loader;
    private LinkedHashMap<String, Class<S>> providers = new LinkedHashMap<String, Class<S>>();
    private LazyIterator lookupIterator;

    public void reload() {
        providers.clear();
        lookupIterator = new LazyIterator(service, loader);
    }

    private ServiceClassLoader(Class<S> svc, Loader loader) {
        this.service = svc;
        this.loader = loader;
        reload();
    }

    private static void fail(Class service, String msg, Throwable cause) throws ServiceConfigurationError {
        throw new ServiceConfigurationError(service.getName() + ": " + msg, cause);
    }

    private static void fail(Class service, String msg) throws ServiceConfigurationError {
        throw new ServiceConfigurationError(service.getName() + ": " + msg);
    }

    private static void fail(Class service, URL u, int line, String msg) throws ServiceConfigurationError {
        fail(service, u + ":" + line + ": " + msg);
    }

    private int parseLine(Class service, URL u, BufferedReader r, int lc, List<String> names) throws IOException, ServiceConfigurationError {
        String ln = r.readLine();
        if (ln == null) return -1;
        int ci = ln.indexOf('#');
        if (ci >= 0) ln = ln.substring(0, ci);
        ln = ln.trim();
        int n = ln.length();
        if (n != 0) {
            if ((ln.indexOf(' ') >= 0) || (ln.indexOf('\t') >= 0))
                fail(service, u, lc, "Illegal configuration-file syntax");
            int cp = ln.codePointAt(0);
            if (!Character.isJavaIdentifierStart(cp))
                fail(service, u, lc, "Illegal provider-class name: " + ln);
            for (int i = Character.charCount(cp); i < n; i += Character.charCount(cp)) {
                cp = ln.codePointAt(i);
                if (!Character.isJavaIdentifierPart(cp) && (cp != '.'))
                    fail(service, u, lc, "Illegal provider-class name: " + ln);
            }
            if (!providers.containsKey(ln) && !names.contains(ln))
                names.add(ln);
        }
        return lc + 1;
    }

    private Iterator<String> parse(Class service, URL u) throws ServiceConfigurationError {
        InputStream in = null;
        BufferedReader r = null;
        ArrayList<String> names = new ArrayList<String>();
        try {
            in = u.openStream();
            r = new BufferedReader(new InputStreamReader(in, "utf-8"));
            int lc = 1;
            while ((lc = parseLine(service, u, r, lc, names)) >= 0) ;
        } catch (IOException x) {
            fail(service, "Error reading configuration file", x);
        } finally {
            try {
                if (r != null) r.close();
                if (in != null) in.close();
            } catch (IOException y) {
                fail(service, "Error closing configuration file", y);
            }
        }
        return names.iterator();
    }

    private class LazyIterator implements Iterator<Class<S>> {
        final Class<? super S> service;
        final Loader loader;
        Iterator<URL> configs = null;
        Iterator<String> pending = null;
        String nextName = null;

        private LazyIterator(Class<? super S> service, Loader loader) {
            this.service = service;
            this.loader = loader;
        }

        public boolean hasNext() {
            if (nextName != null) {
                return true;
            }
            if (configs == null) {
                String fullName = PREFIX + service.getName();
                configs = loader.getResources(fullName).iterator();
            }
            while ((pending == null) || !pending.hasNext()) {
                if (!configs.hasNext()) {
                    return false;
                }
                pending = parse(service, configs.next());
            }
            nextName = pending.next();
            return true;
        }

        @SuppressWarnings({"unchecked"})
        public Class<S> next() {
            if (!hasNext())
                throw new NoSuchElementException();
            String cn = nextName;
            nextName = null;
            try {
                Class<S> p = (Class<S>) loader.loadClass(cn);
                providers.put(cn, p);
                return p;
            } catch (RuntimeException x) {
                fail(service,
                        "Provider " + cn + " could not be instantiated: " + x,
                        x);
            }
            throw new Error();        // This cannot happen
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    public Iterator<Class<S>> iterator() {
        return new Iterator<Class<S>>() {
            Iterator<Map.Entry<String, Class<S>>> knownProviders = providers.entrySet().iterator();

            public boolean hasNext() {
                return knownProviders.hasNext() || lookupIterator.hasNext();
            }

            public Class<S> next() {
                if (knownProviders.hasNext())
                    return knownProviders.next().getValue();
                return lookupIterator.next();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static <S> ServiceClassLoader<S> load(Class<S> service, Loader loader) {
        return new ServiceClassLoader<S>(service, loader);
    }

    public static <S> ServiceClassLoader<S> load(Class<S> service) {
        return new ServiceClassLoader<S>(service, new DefaultLoader());
    }

    public String toString() {
        return "ServiceClassLoader[" + service.getName() + "]";
    }

}