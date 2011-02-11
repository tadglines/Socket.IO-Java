package com.glines.socketio.server.transport;

import com.glines.socketio.server.SocketIOConfig;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Mathieu Carbou
 */
public interface DataHandler {

    void init(SocketIOConfig config);

    boolean isConnectionPersistent();

    void onStartSend(HttpServletResponse response) throws IOException;

    void onWriteData(ServletResponse response, String data) throws IOException;

    void onFinishSend(ServletResponse response) throws IOException;

    void onConnect(HttpServletRequest request, HttpServletResponse response) throws IOException;

}
