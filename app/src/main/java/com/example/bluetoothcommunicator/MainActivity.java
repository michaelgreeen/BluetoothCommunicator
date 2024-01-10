package com.example.bluetoothcommunicator;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.activity.ComponentActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends ComponentActivity {
    private final int requestEnableBt = 1;
    private BluetoothAdapter bluetoothAdapter;
    private Spinner pairedDevicesSpinner;
    private ListView chatHistoryListView;
    private EditText messageEditText;
    private TextView uuidText;
    private Button sendButton;
    private ArrayList<String> pairedDevicesList = new ArrayList<>();
    private final int bluetoothPermissionsRequestCode = 101;
    private UUID appUUID;

    private Button connectButton;
    private Button awaitButton;
    private ArrayAdapter<String> chatAdapter;
    private ArrayList<String> chatMessages;
    private String selectedDevice = null;
    private ServerThread serverThread;
    private ClientThread clientThread;
    private MediaPlayer messageNotificationSound;


    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appUUID = UUID.fromString("03f87aee-633b-488c-bf36-725ea9f2cb69");

        pairedDevicesSpinner = findViewById(R.id.pairedDevicesSpinner);
        chatHistoryListView = findViewById(R.id.chatHistoryListView);
        messageEditText = findViewById(R.id.messageEditText);
        uuidText = findViewById(R.id.uuidTextView);
        uuidText.setText(appUUID.toString());
        chatMessages = new ArrayList<>();
        chatAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, chatMessages);
        chatHistoryListView.setAdapter(chatAdapter);
        messageNotificationSound = MediaPlayer.create(this, R.raw.notification_sound);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            return;
        } else if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, requestEnableBt);
        }

        checkBluetoothPermissions();

        connectButton = findViewById(R.id.connectButton);
        awaitButton = findViewById(R.id.awaitButton);
        sendButton = findViewById(R.id.sendButton);

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedDevice != null) {
                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(selectedDevice);
                    clientThread = new ClientThread(device, appUUID, bluetoothAdapter, (message, isSending) -> handleMessage(message, false));
                    clientThread.start();
                }
            }
        });

        awaitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serverThread = new ServerThread(bluetoothAdapter, appUUID, (message, isSending) -> handleMessage(message, false));
                serverThread.start();
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = messageEditText.getText().toString();
                sendMessage(message);
                messageEditText.setText("");
            }
        });

        setupBluetoothDevicesList();
    }

    private void sendMessage(String message) {
        handleMessage("Me: " + message, true);
        if (clientThread != null) {
            clientThread.sendMessage(message);
        }
        if (serverThread != null) {
            serverThread.sendMessage(message);
        }
    }
    private void handleMessage(final String message, boolean isSending) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                System.out.println(message);
                chatMessages.add(message);

                if (chatMessages.size() > 5) {
                    chatMessages.remove(0);
                }

                chatAdapter.notifyDataSetChanged();

                if (messageNotificationSound != null && !isSending) {
                    messageNotificationSound.start();
                }
            }
        });
    }

    private void checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            checkRuntimePermissions();
        } else {
            checkRuntimePermissions();
            setupBluetoothDevicesList();
        }
    }

    private void checkRuntimePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    bluetoothPermissionsRequestCode
            );
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.BLUETOOTH_SCAN},
                    bluetoothPermissionsRequestCode
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == bluetoothPermissionsRequestCode && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupBluetoothDevicesList();
        }
    }

    @SuppressLint("MissingPermission")
    private void setupBluetoothDevicesList() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter != null ? bluetoothAdapter.getBondedDevices() : null;
        if (pairedDevices != null) {
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesList.add(device.getName() + "\n[" + device.getAddress() + "]");
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, pairedDevicesList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pairedDevicesSpinner.setAdapter(adapter);

        pairedDevicesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String item = parent.getItemAtPosition(position).toString();
                selectedDevice = item.substring(item.indexOf("[") + 1, item.indexOf("]"));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedDevice = null;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageNotificationSound != null) {
            messageNotificationSound.release();
            messageNotificationSound = null;
        }
    }
}
