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

import java.io.UnsupportedEncodingException;

public final class URI {

    private URI() {
    }

    public static String decodePath(String path) {
        return decodePath(path, "UTF-8");
    }

    /* Decode a URI path.
    * @param path The path the encode
    * @param buf StringBuilder to encode path into
    */
    public static String decodePath(String path, String charset) {
        if (path == null)
            return null;
        char[] chars = null;
        int n = 0;
        byte[] bytes = null;
        int b = 0;

        int len = path.length();

        for (int i = 0; i < len; i++) {
            char c = path.charAt(i);

            if (c == '%' && (i + 2) < len) {
                if (chars == null) {
                    chars = new char[len];
                    bytes = new byte[len];
                    path.getChars(0, i, chars, 0);
                }
                bytes[b++] = (byte) (0xff & parseInt(path, i + 1, 2, 16));
                i += 2;
                continue;
            } else if (bytes == null) {
                n++;
                continue;
            }

            if (b > 0) {
                String s;
                try {
                    s = new String(bytes, 0, b, charset);
                } catch (UnsupportedEncodingException e) {
                    s = new String(bytes, 0, b);
                }
                s.getChars(0, s.length(), chars, n);
                n += s.length();
                b = 0;
            }

            chars[n++] = c;
        }

        if (chars == null)
            return path;

        if (b > 0) {
            String s;
            try {
                s = new String(bytes, 0, b, charset);
            } catch (UnsupportedEncodingException e) {
                s = new String(bytes, 0, b);
            }
            s.getChars(0, s.length(), chars, n);
            n += s.length();
        }

        return new String(chars, 0, n);
    }

    private static int parseInt(String s, int offset, int length, int base) throws NumberFormatException {
        int value = 0;
        if (length < 0)
            length = s.length() - offset;
        for (int i = 0; i < length; i++) {
            char c = s.charAt(offset + i);
            int digit = c - '0';
            if (digit < 0 || digit >= base || digit >= 10) {
                digit = 10 + c - 'A';
                if (digit < 10 || digit >= base)
                    digit = 10 + c - 'a';
            }
            if (digit < 0 || digit >= base)
                throw new NumberFormatException(s.substring(offset, offset + length));
            value = value * base + digit;
        }
        return value;
    }
}



