package com.straddle.android.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

import com.straddle.android.R;
import com.straddle.android.activities.ChatActivity;
import com.straddle.android.api.API;
import com.straddle.android.utils.Utils;

public class PeerServerThread extends Thread {
    Context context;
    SQLiteDatabase db;
    STMessage stMessage;
    Socket serverSock;

    public PeerServerThread(Context context, SQLiteDatabase db, STMessage stMessage,
                            Socket serverSock) {
        this.context = context;
        this.db = db;
        this.stMessage = stMessage;
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
                final String data = new String(receivePacket.getData(), receivePacket.getOffset(), receivePacket.getLength());
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
                        if (saved == -1) { // TO DO Failed to save message
                        }

                        int reqCode = 1;
                        Intent intent = new Intent(context, ChatActivity.class);
                        intent.putExtra("MOBILE_NO", dataArr[1]);
                        showNotification(context, dataArr[1], dataArr[4], intent, reqCode);

                        String sendString = "RECEIVED"
                            +"~"+dataArr[2]
                            +"~"+utils.dateTime();
                        stMessage.sendPacket(sendString, packetIP.getHostAddress(), "PEER");

                        break;
                    case "RECEIVED":
                        db.execSQL("UPDATE sent_message SET sent = 1, sent_timestamp = \"" + dataArr[2] + "\" WHERE id = " + dataArr[1]);
                        break;
                    case "READ":
                        db.execSQL("UPDATE sent_message SET read_timestamp = \""+dataArr[1]+"\" WHERE id IN ("+dataArr[2]+")");
                        break;
                }
                Intent newIntent = new Intent("eventName");
                newIntent.putExtra("data", data);
                LocalBroadcastManager.getInstance(context).sendBroadcast(newIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showNotification(Context context, String title, String message, Intent intent, int reqCode) {
        PendingIntent pendingIntent = PendingIntent.getActivity(context, reqCode, intent, PendingIntent.FLAG_ONE_SHOT);
        String CHANNEL_ID = "channel_name";// The id of the channel.
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Channel Name";// The user-visible name of the channel.
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            notificationManager.createNotificationChannel(mChannel);
        }
        notificationManager.notify(reqCode, notificationBuilder.build()); // 0 is the request code, it should be unique id

        Log.d("showNotification", "showNotification: " + reqCode);
    }
}
