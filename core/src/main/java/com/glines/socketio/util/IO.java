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

import java.io.*;

/**
 * @author Mathieu Carbou
 */
public final class IO {
    private IO() {
    }

    public static void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        int len;
        while ((len = is.read(buffer)) >= 0)
            os.write(buffer, 0, len);
    }

    public static void copy(Reader in, Writer out) throws IOException {
        char[] buffer = new char[64 * 1024];
        int len;
        while ((len = in.read(buffer)) >= 0)
            out.write(buffer, 0, len);
    }

    public static String toString(InputStream in) throws IOException {
        return toString(in, null);
    }

    public static String toString(Reader in) throws IOException {
        StringWriter writer = new StringWriter();
        copy(in, writer);
        return writer.toString();
    }

    public static String toString(InputStream in, String encoding) throws IOException {
        InputStreamReader reader = encoding == null ? new InputStreamReader(in) : new InputStreamReader(in, encoding);
        return toString(reader);
    }

}
