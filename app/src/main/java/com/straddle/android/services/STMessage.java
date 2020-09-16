package com.straddle.android.services;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.IBinder;
import android.os.StrictMode;

import com.straddle.android.api.API;
import com.straddle.android.utils.SQLiteHelper;
import com.straddle.android.utils.Utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class STMessage extends Service {
    DatagramSocket serverSocket;
    SQLiteDatabase db;
    Utils utils;

    // PingServer connection socket
    Socket pingSocket;
    DataOutputStream pingOutput;

    // CommServer connection socket
    Socket commSocket;
    DataOutputStream commOutput;
    DataInputStream commInput;

    SharedPreferences userDetails;

    IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public STMessage getServerInstance() {
            return STMessage.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        db = new SQLiteHelper(getApplicationContext()).getReadableDatabase();
        utils = new Utils();

        userDetails = getApplicationContext().getSharedPreferences("user_details", getApplicationContext().MODE_PRIVATE);
        String number = userDetails.getString("country_code", "") + userDetails.getString("number", "");

//        CommServerThread commServerThread = new CommServerThread(commSocket, commOutput, commInput);
//        commServerThread.start();

        Thread thread = new Thread() {
            public void run() {
                try {
                    commSocket = new Socket(new API().getServerIP(), new API().getCommServerPort());
                    commOutput = new DataOutputStream(commSocket.getOutputStream());
                    commInput = new DataInputStream(commSocket.getInputStream());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();

        PingServerThread pingServerThread = new PingServerThread(pingSocket, pingOutput, number);
        pingServerThread.start();

        PeerServerThread peerServerThread = new PeerServerThread(getApplicationContext(), db,
                STMessage.this, pingSocket);
        peerServerThread.start();

//        Thread thread = new Thread(){
//            public void run() {
//                try {
//                    serverSocket = new DatagramSocket(7070);
//                    for(;;) {
//                        DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
//                        serverSocket.receive(receivePacket);
//
//                        InetAddress packetIP = receivePacket.getAddress();
//
//                        String data = new String(receivePacket.getData(), receivePacket.getOffset(), receivePacket.getLength());
//                        String[] dataArr = data.split("~");
//
//                        switch (dataArr[0]) {
//                            case "MESSAGE":
//                                ContentValues values = new ContentValues();
//                                values.put("message_id", dataArr[2]);
//                                values.put("from_user", dataArr[1]);
//                                values.put("message", dataArr[4]);
//                                values.put("timestamp", dataArr[3]);
//                                long saved = db.insert("received_message", null, values);
//                                Log.d("SAVE STATUS", Long.toString(saved));
//                                if (saved == -1) {
//
//                                    // Failed to save message
//                                }
//
//                                // Send RECEIVED packet for the message
//                                String sendString = "RECEIVED"
//                                        +"~"+dataArr[2]
//                                        +"~"+utils.dateTime();
//                                sendPacket(sendString, packetIP.toString());
//
////                                byte[] messageBytes = sendString.getBytes();
////                                DatagramPacket sendPacket = new DatagramPacket(messageBytes,
////                                        messageBytes.length, packetIP, 7070);
////                                serverSocket.send(sendPacket);
//                                break;
//                            case "RECEIVED":
//                                db.execSQL("UPDATE sent_message SET sent = 1, sent_timestamp = \"" + dataArr[2] + "\" WHERE id = " + dataArr[1]);
//                                break;
//                            case "READ":
//                                db.execSQL("UPDATE sent_message SET read_timestamp = \""+dataArr[1]+"\" WHERE id IN ("+dataArr[2]+")");
//                                break;
//                        }
//                        Intent newIntent = new Intent("eventName");
//                        newIntent.putExtra("data", data); // You can add additional data to the intent...
//                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(newIntent);
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        };
//
//        thread.start();
    }

    public String getPeerIP(String number) {
        try {
            commOutput.writeUTF("IPREQUEST~" + number);
            String received = commInput.readUTF();
            String receivedArr[] = received.split("~");
            switch (receivedArr[0]) {
                case "SUCCESS":
                    return receivedArr[1];
                default:
                    return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void sendPacket(String payload, String peerIP, String category) {
        byte[] messageBytes = payload.getBytes();
        try {
            DatagramSocket serverSocket = new DatagramSocket();
            DatagramPacket sendPacket;
            switch (category) {
                case "PEER":
                    sendPacket = new DatagramPacket(messageBytes,
                            messageBytes.length, InetAddress.getByName(peerIP), 7070);
                    serverSocket.send(sendPacket);
                    break;
                case "SERVER":
                    pingOutput.writeUTF(payload);
                    break;
                case "DUAL":
                    sendPacket = new DatagramPacket(messageBytes,
                            messageBytes.length, InetAddress.getByName(peerIP), 7070);
                    serverSocket.send(sendPacket);
                    pingOutput.writeUTF(payload);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
