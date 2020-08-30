package com.straddle.android.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.straddle.android.R;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

public class SocketTestActivity extends AppCompatActivity implements View.OnClickListener {

    private Button refreshIP;
    private TextView privateIPAddress;
    private TextView publicIPAddress;
    private EditText message;
    private EditText serverIP;
    private Button sendMessage;
    private Button startServerSocket;
    private Button startClientSocket;

    RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.activity_socket_test);

        queue = Volley.newRequestQueue(this);

        refreshIP = findViewById(R.id.refreshIP);
        privateIPAddress = findViewById(R.id.privateIPAddress);
        publicIPAddress = findViewById(R.id.publicIPAddress);
        message = findViewById(R.id.message);
        serverIP = findViewById(R.id.serverIP);
        sendMessage = findViewById(R.id.sendMessage);
        startServerSocket = findViewById(R.id.startServerSocket);
        startClientSocket = findViewById(R.id.startClientSocket);

        refreshIP.setOnClickListener(this);
        startServerSocket.setOnClickListener(this);
        startClientSocket.setOnClickListener(this);

        getLocalIPAddress();
        getPublicIPAddress();

    }

    public void getLocalIPAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
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
                        publicIPAddress.setText("Public IP Address: "+ response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("IP Set Error", error.toString());
                publicIPAddress.setText("That didn't work!");
            }
        });

// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.refreshIP:
                getLocalIPAddress();
                getPublicIPAddress();
                break;
            case R.id.startServerSocket:
                try {
                    ServerSocket ss = new ServerSocket(8001);
                    Log.d("SOCK_ADDR", ss.getInetAddress().toString());
                    Socket s = ss.accept();

                    Log.d("CONN_ADDR", s.getInetAddress().toString());

                    InputStreamReader in = new InputStreamReader(s.getInputStream());
                    BufferedReader bf = new BufferedReader(in);
                    PrintWriter pr = new PrintWriter(s.getOutputStream());

                    String str = bf.readLine();
                    Log.d("CLIENT_MSG", str);
                    Toast.makeText(this, str, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.startClientSocket:
                try {
                    Socket s = new Socket(serverIP.getText().toString(), 8001);
                    PrintWriter pr = new PrintWriter(s.getOutputStream());

                    pr.println(message.getText().toString());
                    pr.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }
}
