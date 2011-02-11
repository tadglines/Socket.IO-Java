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

import javax.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.util.Random;

/**
 * @author Mathieu Carbou
 */
public final class Web {

    private static final char[] BASE64_ALPHABET ="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray();
    private static Random random = new SecureRandom();

    private Web() {
    }

    public static String extractSessionId(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path != null && path.length() > 0 && !"/".equals(path)) {
            if (path.startsWith("/")) path = path.substring(1);
            String[] parts = path.split("/");
            if (parts.length >= 2) {
                return parts[1] == null ? null : (parts[1].length() == 0 ? null : parts[1]);
            }
        }
        return null;
    }

    public static String generateRandomString(int length) {
        StringBuilder result = new StringBuilder(length);
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        for (byte aByte : bytes) {
            result.append(BASE64_ALPHABET[aByte & 0x3F]);
        }
        return result.toString();
    }

}
