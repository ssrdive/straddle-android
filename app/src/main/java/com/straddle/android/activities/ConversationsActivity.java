package com.straddle.android.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.straddle.android.R;
import com.straddle.android.services.STMessage;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class ConversationsActivity extends AppCompatActivity implements View.OnClickListener {

    EditText peerIP, mobileNo, myNumber;
    Button message, start, stop;
    TextView privateIPAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);

        startService(new Intent(this, STMessage.class));

//        LocalBroadcastManager.getInstance(this).registerReceiver(aLBReceiver,
//                new IntentFilter("eventName"));

        peerIP = findViewById(R.id.peerIP);

        message = findViewById(R.id.message);
        message.setOnClickListener(this);

        start = findViewById(R.id.startButton);
        start.setOnClickListener(this);

        stop = findViewById(R.id.stopButton);
        stop.setOnClickListener(this);

        mobileNo = findViewById(R.id.mobileNo);
        myNumber = findViewById(R.id.myNumber);

        privateIPAddress = findViewById(R.id.privateIPAddress);
        getLocalIPAddress();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.message:
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("PEER_IP", peerIP.getText().toString());
                intent.putExtra("MOBILE_NO", mobileNo.getText().toString());
                intent.putExtra("MY_NUMBER", myNumber.getText().toString());
                startActivity(intent);
                break;
            case R.id.startButton:
//                startService(new Intent(this, STMessage.class));
//                LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
//                    @Override
//                    public void onReceive(Context context, Intent intent) {
//                        String data = intent.getStringExtra("data");
//                        Toast.makeText(getApplicationContext(), data, Toast.LENGTH_LONG).show();
//                    }
//                }, new IntentFilter("eventName"));
//                LocalBroadcastManager.getInstance(this).registerReceiver(aLBReceiver,
//                        new IntentFilter("eventName"));
                break;
            case R.id.stopButton:
//                stopService(new Intent(this, STMessage.class));
                break;
        }
    }

    private BroadcastReceiver aLBReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String data = intent.getStringExtra("data");
            Toast.makeText(getApplicationContext(), data, Toast.LENGTH_SHORT).show();
            Log.d("FROM_CONV", data);
        }
    };

    public void getLocalIPAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        privateIPAddress.setText("Private IP Address: " + inetAddress.getHostAddress());
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
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
