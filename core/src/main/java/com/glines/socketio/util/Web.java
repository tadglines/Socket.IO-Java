package com.glines.socketio.util;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Mathieu Carbou
 */
public final class Web {

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

}
