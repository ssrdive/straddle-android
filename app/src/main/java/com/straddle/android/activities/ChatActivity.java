package com.straddle.android.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
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
import com.straddle.android.services.STMessage;
import com.straddle.android.utils.SQLiteHelper;
import com.straddle.android.utils.Utils;

import java.net.DatagramSocket;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener {

    private SharedPreferences userDetails;

    SQLiteDatabase db;

    String peerIP, peerMobileNo, myNumber;

    TextView peerIP_tv, mobileNo_tv, myNumber_tv, showProfile;
    Button sendMessage;
    EditText message;
    LinearLayout messagesView;
    ScrollView scrollMessages;

    Utils utils;

    boolean mBounded;
    STMessage stMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        utils = new Utils();

        db = new SQLiteHelper(getApplicationContext()).getReadableDatabase();

        userDetails = getSharedPreferences("user_details", MODE_PRIVATE);

        Bundle extras = getIntent().getExtras();
//        peerIP = extras.getString("PEER_IP");
        peerMobileNo = extras.getString("MOBILE_NO");
        myNumber = userDetails.getString("country_code", "") + userDetails.getString("number", "");

        peerIP_tv = findViewById(R.id.peerIP_tv);

        mobileNo_tv = findViewById(R.id.mobileNo_tv);
        mobileNo_tv.setText(peerMobileNo);

        myNumber_tv = findViewById(R.id.myNumber_tv);
        myNumber_tv.setText(myNumber);

        sendMessage = findViewById(R.id.sendMessage);
        sendMessage.setOnClickListener(this);

        showProfile = findViewById(R.id.showProfile);
        showProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), UserProfileActivity.class);
                intent.putExtra("number", peerMobileNo.substring(2));
                startActivity(intent);
            }
        });

        message = findViewById(R.id.message);

        messagesView = findViewById(R.id.messagesView);

        scrollMessages = findViewById(R.id.scrollMessages);

        getMessages();
        moveToBottom();

    }

    private void getPeerIPAddress() {
        this.peerIP = stMessage.getPeerIP(peerMobileNo);
        if(peerIP != null) {
            peerIP_tv.setText(peerIP);
            sendMessage.setEnabled(true);
        }

    }

    private BroadcastReceiver aLBReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String data = intent.getStringExtra("data");
            String[] dataArr = data.split("~");

            switch (dataArr[0]) {
                case "MESSAGE":
                    if(dataArr[1].equals(peerMobileNo)) {
                        addMessage(Integer.parseInt(dataArr[2]), "from", dataArr[4], dataArr[3], "","", "");
                        moveToBottom();
                        String dateTime = utils.dateTime();
                        db.execSQL("UPDATE received_message SET read_timestamp = \""
                                + dateTime + "\" WHERE message_id = " + dataArr[2]);
                        String sendString = "READ"
                                +"~"+dateTime
                                +"~"+dataArr[2];
                        stMessage.sendPacket(sendString, peerIP, "PEER");

                    } else {
                        Toast.makeText(getApplicationContext(), data, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case "RECEIVED":
                    TextView msgDeliveredStatus = findViewById(Integer.parseInt(dataArr[1]));
                    msgDeliveredStatus.setText("Delivered on " + dataArr[2]);
                    break;
                case "READ":
                    String[] ids = dataArr[2].split(",");
                    for(String id : ids) {
                        TextView msgReadStatus = findViewById(Integer.parseInt(id));
                        if (msgReadStatus != null)
                            msgReadStatus.setText("Read on " + dataArr[1]);
                    }
                    break;
            }
        }
    };

    public void getMessages() {
        Cursor res = db.rawQuery("SELECT * FROM (SELECT id, message, timestamp, \"from\" AS " +
                "type, \"\" AS sent, \"\" AS sent_timestamp, \"\" AS read_timestamp FROM received_message " +
                "WHERE from_user = "+peerMobileNo+" UNION SELECT id, message, timestamp, \"to\" AS " +
                "type, sent, sent_timestamp, read_timestamp FROM sent_message WHERE to_user = "+peerMobileNo+") M " +
                "ORDER BY timestamp ASC", null);
        res.moveToFirst();
        while (res.isAfterLast() == false) {
            addMessage(Integer.parseInt(res.getString(res.getColumnIndex("id"))),
                    res.getString(res.getColumnIndex("type")),
                    res.getString(res.getColumnIndex("message")),
                    res.getString(res.getColumnIndex("timestamp")),
                    res.getString(res.getColumnIndex("sent")),
                    res.getString(res.getColumnIndex("sent_timestamp")),
                    res.getString(res.getColumnIndex("read_timestamp")));
            res.moveToNext();
        }
    }

    public void sendReadReceipts() {
        Cursor res = db.rawQuery("SELECT id, message_id " +
                "FROM received_message " +
                "WHERE read_timestamp IS NULL AND from_user = " + peerMobileNo, null);
        String localIDs = "";
        String peerIDs = "";
        String dateTime = utils.dateTime();
        if(res.getCount() != 0) {
            res.moveToFirst();
            while(res.isAfterLast() == false) {
                localIDs = localIDs + res.getString(res.getColumnIndex("id"));
                peerIDs = peerIDs + res.getString(res.getColumnIndex("message_id"));
                if(res.isLast() == false) {
                    localIDs = localIDs + ",";
                    peerIDs = peerIDs + ",";
                }
                res.moveToNext();
            }

            db.execSQL("UPDATE received_message SET read_timestamp = \""
                    + dateTime + "\" WHERE id IN (" + localIDs + ")");

            String sendString = "READ"
                    +"~"+dateTime
                    +"~"+peerIDs;
            stMessage.sendPacket(sendString, peerIP, "PEER");
        }
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

                String dateTime = utils.dateTime();
                String sendString = "MESSAGE"
                        +"~"+myNumber
                        +"~"+saved
                        +"~"+dateTime
                        +"~"+message.getText().toString();
                stMessage.sendPacket(sendString, peerIP, "PEER");
                addMessage((int) saved, "to", message.getText().toString(), dateTime,
                        "0", "", "");
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

    public void addMessage(int id, String type, String message,
                           String timestamp, String sent, String sentTimestamp, String readTimestamp) {
        TextView tvMsg = new TextView(this);
        tvMsg.setText(message);
        tvMsg.setTextSize(18);

        TextView tvTime = new TextView(this);
        tvTime.setText(timestamp);
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

            TextView tvDash = new TextView(this);
            tvDash.setTextSize(10);
            tvDash.setText(" \u2014 ");

            TextView tvStatus = new TextView(this);
            tvStatus.setId(id);
            tvStatus.setOnClickListener(this);
            tvStatus.setTextSize(10);

            if(readTimestamp != null && !readTimestamp.equals("")) {
                tvStatus.setText("Read on " + readTimestamp);
            } else if(readTimestamp != null && sent.equals("1")) {
                tvStatus.setText("Delivered on " + sentTimestamp);
            } else {
                tvStatus.setText("Sending");
            }

            layout.addView(tvDash);
            layout.addView(tvStatus);
            messagesView.addView(layout);
        } else {
            tvMsg.setTextColor(Color.parseColor("#4287f5"));
            messagesView.addView(tvMsg);
            messagesView.addView(tvTime);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBounded = false;
            stMessage = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBounded = true;
            STMessage.LocalBinder mLocalBinder = (STMessage.LocalBinder) service;
            stMessage = mLocalBinder.getServerInstance();
            getPeerIPAddress();
            sendReadReceipts();
        }
    };

    @Override
    public void onResume() {
        LocalBroadcastManager.getInstance(this).registerReceiver(aLBReceiver,
                new IntentFilter("eventName"));
        Intent mIntent = new Intent(this, STMessage.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);
        super.onResume();
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(aLBReceiver);
        if(mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();

        if(mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }
    }
}
