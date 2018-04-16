package com.veritas.asr;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_10;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

public class KaldiStatusWebSocketClient extends WebSocketClient {
    private AtomicInteger workers_available = new AtomicInteger(0);

    public KaldiStatusWebSocketClient(URI uri) {
        super(uri, new Draft_10());
    }

    public void onOpen(ServerHandshake serverHandshake) {
    }

    public void onMessage(String message) {
        try {
            JSONObject obj = new JSONObject(message);
            workers_available.set(Integer.parseInt(obj.getString("num_workers_available")));

            System.out.println(uri.toString() + " > num_workers_available: " + workers_available.get());
        }
        catch (Exception ex) {}
    }

    public void onClose(int i, String s, boolean b) {
    }

    public void onError(Exception e) {
    }

    public int getKaldiAvailableWorkers() {
        return workers_available.get();
    }

    public void waitForAvailableWorkers() throws Exception {
        //Wait a max of 30seconds!
        int maxNumberTries = 300;
        while (getKaldiAvailableWorkers() == 0 && maxNumberTries-- > 0) {
            Thread.sleep(100);
        }
        if (getKaldiAvailableWorkers() == 0)
            throw new Exception("No available workers.");
    }
}

