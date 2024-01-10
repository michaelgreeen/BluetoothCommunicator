package com.example.bluetoothcommunicator;
public interface MessageHandler {
    void handleMessage(String message, boolean isSending);
}