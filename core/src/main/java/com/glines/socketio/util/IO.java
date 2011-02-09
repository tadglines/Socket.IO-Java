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
