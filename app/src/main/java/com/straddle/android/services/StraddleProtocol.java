package com.straddle.android.services;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.straddle.android.utils.SQLiteHelper;
import com.straddle.android.utils.Utils;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class StraddleProtocol extends Service {
    DatagramSocket serverSocket;
    SQLiteDatabase db;
    Utils utils;

    @Override
    public void onCreate() {
        super.onCreate();
        db = new SQLiteHelper(getApplicationContext()).getReadableDatabase();
        utils = new Utils();

        Thread thread = new Thread(){
            public void run() {
                try {
                    serverSocket = new DatagramSocket(7070);
                    for(;;) {
                        DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
                        serverSocket.receive(receivePacket);

                        InetAddress packetIP = receivePacket.getAddress();

                        String data = new String(receivePacket.getData(), receivePacket.getOffset(), receivePacket.getLength());
                        String[] dataArr = data.split("~");

                        switch (dataArr[0]) {
                            case "MESSAGE":
                                ContentValues values = new ContentValues();
                                values.put("message_id", dataArr[2]);
                                values.put("from_user", dataArr[1]);
                                values.put("message", dataArr[4]);
                                values.put("timestamp", dataArr[3]);
                                long saved = db.insert("received_message", null, values);
                                Log.d("SAVE STATUS", Long.toString(saved));
                                if (saved == -1) {

                                    // Failed to save message
                                }

                                // Send RECEIVED packet for the message
                                String sendString = "RECEIVED"
                                        +"~"+dataArr[2]
                                        +"~"+utils.dateTime();

                                byte[] messageBytes = sendString.getBytes();
                                DatagramPacket sendPacket = new DatagramPacket(messageBytes,
                                        messageBytes.length, packetIP, 7070);
                                serverSocket.send(sendPacket);
                                break;
                            case "RECEIVED":
                                db.execSQL("UPDATE sent_message SET sent = 1, sent_timestamp = \"" + dataArr[2] + "\" WHERE id = " + dataArr[1]);
                                break;
                        }
                        Intent newIntent = new Intent("eventName");
                        newIntent.putExtra("data", data); // You can add additional data to the intent...
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(newIntent);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        serverSocket.close();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
