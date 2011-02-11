package com.glines.socketio.client.common;

import com.glines.socketio.common.DisconnectReason;

/**
* @author Mathieu Carbou (mathieu.carbou@gmail.com)
*/
public interface SocketIOConnectionListener {
    void onConnect();
    void onDisconnect(DisconnectReason reason, String errorMessage);
    void onMessage(int messageType, String message);
}
