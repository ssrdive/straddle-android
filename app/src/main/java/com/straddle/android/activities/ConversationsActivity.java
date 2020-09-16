package com.straddle.android.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.straddle.android.R;
import com.straddle.android.services.STMessage;
import com.straddle.android.utils.SQLiteHelper;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class ConversationsActivity extends AppCompatActivity implements View.OnClickListener {
    private SharedPreferences userDetails;

    SQLiteDatabase db;

    TextView myNumber;
    EditText mobileNo;
    Button message, start, stop;
    TextView privateIPAddress;
    TextView publicIPAddress;

    LinearLayout chatList;

    RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        db = new SQLiteHelper(getApplicationContext()).getReadableDatabase();

        startService(new Intent(this, STMessage.class));

        userDetails = getSharedPreferences("user_details", MODE_PRIVATE);

        queue = Volley.newRequestQueue(this);

        //        LocalBroadcastManager.getInstance(this).registerReceiver(aLBReceiver,
        //                new IntentFilter("eventName"));

        message = findViewById(R.id.message);
        message.setOnClickListener(this);

        start = findViewById(R.id.startButton);
        start.setOnClickListener(this);

        stop = findViewById(R.id.stopButton);
        stop.setOnClickListener(this);

        mobileNo = findViewById(R.id.mobileNo);

        myNumber = findViewById(R.id.myNumber);
        myNumber.setText(userDetails.getString("country_code", "") + userDetails.getString("number", ""));

        privateIPAddress = findViewById(R.id.privateIPAddress);
        getLocalIPAddress();

        publicIPAddress = findViewById(R.id.publicIPAddress);
        getPublicIPAddress();

        chatList = findViewById(R.id.chatList);
        loadConversations();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.example_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item1:
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                return true;
            case R.id.item2:
                Toast.makeText(this, "Item 2 Selected", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void loadConversations() {
        final Cursor res = db.rawQuery("SELECT user, message, MAX(timestamp) as time\n" +
                "FROM (SELECT from_user as user, message, timestamp\n" +
                "FROM received_message \n" +
                "UNION\n" +
                "SELECT to_user as user, message, timestamp\n" +
                "from sent_message) convs\n" +
                "GROUP BY user", null);
        res.moveToFirst();
        while (res.isAfterLast() == false) {
            TextView number = new TextView(this);
            final String userNumber = res.getString(res.getColumnIndex("user"));
            number.setText(userNumber);
            number.setTextSize(24);
            chatList.addView(number);

            number.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
                    intent.putExtra("MOBILE_NO", userNumber);
                    startActivity(intent);
                }
            });

            TextView time = new TextView(this);
            time.setText(res.getString(res.getColumnIndex("time")));
            time.setTextSize(12);
            chatList.addView(time);

            TextView message = new TextView(this);
            message.setText(res.getString(res.getColumnIndex("message")));
            message.setTextSize(18);
            chatList.addView(message);

            res.moveToNext();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.message:
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("MOBILE_NO", mobileNo.getText().toString());
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
//            Toast.makeText(getApplicationContext(), data, Toast.LENGTH_SHORT).show();
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

    public void getPublicIPAddress() {
        String url = "https://checkip.amazonaws.com/";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        publicIPAddress.setText("Public IP Address: "+ response.replace("\n", ""));
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("IP Set Error", error.toString());
                publicIPAddress.setText("Public IP Address: FAILED");
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
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
