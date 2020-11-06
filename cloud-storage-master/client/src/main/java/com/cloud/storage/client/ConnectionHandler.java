package com.cloud.storage.client;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.IOException;
import java.net.Socket;

public class ConnectionHandler {
    private static ConnectionHandler ourInstance = new ConnectionHandler();

    public static ConnectionHandler getInstance() {
        return ourInstance;
    }

    private Socket socket;
    private ObjectEncoderOutputStream out;
    private ObjectDecoderInputStream in;

    public boolean isConnected() {
        return socket != null && !socket.isClosed();
    }

    private ConnectionHandler() {
    }

    public ObjectEncoderOutputStream getOut() {
        return out;
    }

    public void connect() throws IOException {
        socket = new Socket("localhost", 36663);
        in = new ObjectDecoderInputStream(socket.getInputStream());
        out = new ObjectEncoderOutputStream(socket.getOutputStream());
    }

    public void sendData (Object data) throws IOException {
        out.writeObject(data);
        out.flush();
    }

    public Object readData() throws IOException,ClassNotFoundException {
        return in.readObject();
    }

    public void close() {
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
