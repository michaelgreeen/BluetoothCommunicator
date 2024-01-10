package com.example.bluetoothcommunicator;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

class ClientThread extends Thread {
    private final BluetoothSocket socket;
    private final BluetoothAdapter bluetoothAdapter;
    private final MessageHandler messageHandler;
    private OutputStream outputStream;

    @SuppressLint("MissingPermission")
    public ClientThread(BluetoothDevice device, UUID appUUID, BluetoothAdapter bluetoothAdapter, MessageHandler messageHandler) {
        BluetoothSocket tmp = null;
        this.bluetoothAdapter = bluetoothAdapter;
        this.messageHandler = messageHandler;
        try {
            tmp = device.createRfcommSocketToServiceRecord(appUUID);
        } catch (IOException e) {
            System.out.println("ClientThread Constructor IOException: " + e.getMessage());
        }
        socket = tmp;
    }

    @SuppressLint("MissingPermission")
    public void run() {
        bluetoothAdapter.cancelDiscovery();

        try {
            System.out.println("Trying to connect");
            socket.connect();
            System.out.println("CLIENT CONNECTED");
            manageConnectedSocket(socket);
        } catch (IOException connectException) {
            System.out.println("ClientThread Connection IOException: " + connectException.getMessage());
            try {
                socket.close();
            } catch (IOException closeException) {
                System.out.println("ClientThread Close IOException: " + closeException.getMessage());
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
                    System.out.println(incomingMessage);

                } catch (IOException e) {
                    System.out.println("ClientThread Read IOException: " + e.getMessage());
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("ClientThread Stream Initialization IOException: " + e.getMessage());
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
                socket.close();
            } catch (IOException e) {
                System.out.println("ClientThread Final IOException: " + e.getMessage());
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
            System.out.println("ClientThread Send IOException: " + e.getMessage());
        }
    }
}