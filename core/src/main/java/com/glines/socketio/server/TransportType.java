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
