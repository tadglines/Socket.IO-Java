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

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public enum TransportType {

    WEB_SOCKET("websocket"),
    FLASH_SOCKET("flashsocket"),
    HTML_FILE("htmlfile"),
    JSONP_POLLING("jsonp-polling"),
    XHR_MULTIPART("xhr-multipart"),
    XHR_POLLING("xhr-polling"),
    UNKNOWN("");

    private final String name;

    TransportType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static TransportType from(String name) {
        for (TransportType type : values()) {
            if (type.name.equals(name))
                return type;
        }
        return UNKNOWN;
    }
}
