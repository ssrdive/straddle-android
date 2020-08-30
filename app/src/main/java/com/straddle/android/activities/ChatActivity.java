package com.straddle.android.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.straddle.android.R;
import com.straddle.android.utils.SQLiteHelper;
import com.straddle.android.utils.Utils;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener {

    DatagramSocket clientSocket;

    SQLiteDatabase db;

    String peerIP, peerMobileNo, myNumber;

    TextView peerIP_tv, mobileNo_tv, myNumber_tv;
    Button sendMessage;
    EditText message;
    LinearLayout messagesView;
    ScrollView scrollMessages;

    Utils utils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        utils = new Utils();

        db = new SQLiteHelper(getApplicationContext()).getReadableDatabase();

        Bundle extras = getIntent().getExtras();
        peerIP = extras.getString("PEER_IP");
        peerMobileNo = extras.getString("MOBILE_NO");
        myNumber = extras.getString("MY_NUMBER");

        try {
            clientSocket = new DatagramSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }

        peerIP_tv = findViewById(R.id.peerIP_tv);
        peerIP_tv.setText(peerIP);

        mobileNo_tv = findViewById(R.id.mobileNo_tv);
        mobileNo_tv.setText(peerMobileNo);

        myNumber_tv = findViewById(R.id.myNumber_tv);
        myNumber_tv.setText(myNumber);

        sendMessage = findViewById(R.id.sendMessage);
        sendMessage.setOnClickListener(this);

        message = findViewById(R.id.message);

        messagesView = findViewById(R.id.messagesView);

        scrollMessages = findViewById(R.id.scrollMessages);

        getMessages();
        moveToBottom();
    }

    private BroadcastReceiver aLBReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String data = intent.getStringExtra("data");
            String[] dataArr = data.split("~");

            switch (dataArr[0]) {
                case "MESSAGE":
                    if(dataArr[1].equals(peerMobileNo)) {
                        addMessage(Integer.parseInt(dataArr[2]), "from", dataArr[4], dataArr[3]);
                        moveToBottom();
                    } else {
                        Toast.makeText(getApplicationContext(), data, Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    public void getMessages() {
        Cursor res = db.rawQuery("SELECT * FROM (SELECT id, message, timestamp, \"from\" AS " +
                "type FROM received_message " +
                "WHERE from_user = "+peerMobileNo+" UNION SELECT id, message, timestamp, \"to\" AS " +
                "type FROM sent_message WHERE to_user = "+peerMobileNo+") M " +
                "ORDER BY timestamp ASC", null);
        res.moveToFirst();
        while (res.isAfterLast() == false) {
            addMessage(Integer.parseInt(res.getString(res.getColumnIndex("id"))),
                    res.getString(res.getColumnIndex("type")),
                    res.getString(res.getColumnIndex("message")),
                    res.getString(res.getColumnIndex("timestamp")));
            res.moveToNext();
        }
    }

    public void sendReadReceipts() {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sendMessage:
                if(message.getText().toString().equals(""))
                    return;

                ContentValues values = new ContentValues();
                values.put("to_user", peerMobileNo);
                values.put("message", message.getText().toString());
                values.put("timestamp", utils.dateTime());
                long saved = db.insert("sent_message", null, values);
                if (saved == -1) {
                    // Failed to save message
                    // TO DO
                }

                String sendString = "MESSAGE"
                        +"~"+myNumber
                        +"~"+saved
                        +"~"+utils.dateTime()
                        +"~"+message.getText().toString();

                byte[] messageBytes = sendString.getBytes();
                try {
                    DatagramPacket sendPacket = new DatagramPacket(messageBytes,
                            messageBytes.length, InetAddress.getByName(peerIP), 7070);
                    clientSocket.send(sendPacket);
                    addMessage((int) saved, "to", message.getText().toString(), utils.dateTime());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                moveToBottom();
                message.setText("");
                break;
            default:
                Toast.makeText(this, String.valueOf(v.getId()), Toast.LENGTH_LONG).show();
                break;
        }
    }

    public void moveToBottom() {
        scrollMessages.postDelayed(new Runnable() {
            @Override
            public void run() {
                scrollMessages.fullScroll(ScrollView.FOCUS_DOWN);
            }
        },100);
    }

    public void addMessage(int id, String type, String message, String timestamp) {
        TextView tvMsg = new TextView(this);
        tvMsg.setText(message);
        tvMsg.setTextSize(18);

        TextView tvTime = new TextView(this);
        tvTime.setText(utils.dateTime());
        tvTime.setTextSize(8);

        if(type.equals("to")) {
            tvMsg.setTextColor(Color.parseColor("#5ebd6e"));
            tvMsg.setGravity(Gravity.RIGHT);
            tvTime.setGravity(Gravity.RIGHT);
            messagesView.addView(tvMsg);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setGravity(Gravity.RIGHT);
            layout.addView(tvTime);

            TextView tvStatus = new TextView(this);
            tvStatus.setId(id);
            tvStatus.setText(" \u2014 Sending");
            tvStatus.setOnClickListener(this);
            tvStatus.setTextSize(10);
            layout.addView(tvStatus);

            messagesView.addView(layout);
        } else {
            tvMsg.setTextColor(Color.parseColor("#4287f5"));
            messagesView.addView(tvMsg);
            messagesView.addView(tvTime);
        }
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(aLBReceiver);
        super.onPause();
    }

    @Override
    public void onResume() {
        LocalBroadcastManager.getInstance(this).registerReceiver(aLBReceiver,
                new IntentFilter("eventName"));
        super.onResume();
    }
}
