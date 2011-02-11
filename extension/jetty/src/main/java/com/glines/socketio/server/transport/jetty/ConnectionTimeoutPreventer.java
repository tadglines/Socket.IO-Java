package com.glines.socketio.server.transport.jetty;

/**
* @author Mathieu Carbou
*/
interface ConnectionTimeoutPreventer {
    void connectionActive();
}
