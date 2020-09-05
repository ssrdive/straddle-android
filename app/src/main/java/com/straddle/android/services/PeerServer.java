package com.straddle.android.services;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import com.straddle.android.utils.Utils;

public class PeerServer extends Thread {
    Context context;
    SQLiteDatabase db;

    public PeerServer(Context context, SQLiteDatabase db) {
        this.context = context;
        this.db = db;
    }

    @Override
    public void run() {
        try {
            DatagramSocket serverSocket = new DatagramSocket(7070);
            Utils utils = new Utils();
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
                    case "READ":
                        db.execSQL("UPDATE sent_message SET read_timestamp = \""+dataArr[1]+"\" WHERE id IN ("+dataArr[2]+")");
                        break;
                }
                Intent newIntent = new Intent("eventName");
                newIntent.putExtra("data", data); // You can add additional data to the intent...
                LocalBroadcastManager.getInstance(context).sendBroadcast(newIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
