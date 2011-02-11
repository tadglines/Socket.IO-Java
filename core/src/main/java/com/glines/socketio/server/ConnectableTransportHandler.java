package com.glines.socketio.server;

import com.glines.socketio.server.transport.DataHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public interface ConnectableTransportHandler {
    void connect(HttpServletRequest request, HttpServletResponse response) throws IOException;
    void setDataHandler(DataHandler dataHandler);
}
