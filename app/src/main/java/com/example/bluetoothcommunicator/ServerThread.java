package com.example.bluetoothcommunicator;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

class ServerThread extends Thread {
    private final BluetoothServerSocket serverSocket;
    private final MessageHandler messageHandler;
    private OutputStream outputStream;

    @SuppressLint("MissingPermission")
    public ServerThread(BluetoothAdapter bluetoothAdapter, UUID appUUID, MessageHandler messageHandler) {
        BluetoothServerSocket tmp = null;
        this.messageHandler = messageHandler;
        try {
            tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("MyApp", appUUID);
        } catch (IOException e) {
            System.out.println("ServerThread Constructor IOException: " + e.getMessage());
        }
        serverSocket = tmp;
    }

    public void run() {
        BluetoothSocket socket = null;
        while (true) {
            try {
                socket = serverSocket.accept();
                if (socket != null) {
                    System.out.println("SERVER RECEIVED CONNECTION");
                    manageConnectedSocket(socket);
                    // Comment out the line below if you want to accept multiple connections
                    // serverSocket.close();
                    // break;
                }
            } catch (IOException e) {
                System.out.println("ServerThread Accept IOException: " + e.getMessage());
                break;
            }
        }
    }

    private void manageConnectedSocket(BluetoothSocket socket) {
        InputStream inputStream = null;

        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    messageHandler.handleMessage(incomingMessage, false);
                    System.out.println("ReceivedDUDU: " + incomingMessage);

                } catch (IOException e) {
                    System.out.println("ServerThread Read IOException: " + e.getMessage());
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("ServerThread Stream Initialization IOException: " + e.getMessage());
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
                socket.close();
            } catch (IOException e) {
                System.out.println("ServerThread Final IOException: " + e.getMessage());
            }
        }
    }

    public void sendMessage(String message) {
        try {
            if (outputStream != null) {
                outputStream.write(message.getBytes());
                System.out.println("Message sent: " + message);
            }
        } catch (IOException e) {
            System.out.println("ServerThread Send IOException: " + e.getMessage());
        }
    }
}