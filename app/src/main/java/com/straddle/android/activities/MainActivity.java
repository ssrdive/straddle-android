package com.straddle.android.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.straddle.android.R;
import com.straddle.android.api.API;
import com.straddle.android.utils.Utils;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Handler handler;
    private SharedPreferences userDetails;
    private RequestQueue mQueue;

    private ProgressDialog dialog;

    private Utils utils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler();
        userDetails = getSharedPreferences("user_details", MODE_PRIVATE);
        if (userDetails.contains("hash")) {
            mQueue = Volley.newRequestQueue(this);
            utils = new Utils();
            checkToken();
        } else {
            handler.postDelayed(new Runnable() {
                public void run() {
                    Intent intent = new Intent(getApplicationContext(), SignUpActivity.class);
                    startActivity(intent);
                }
            }, 800);
        }
    }

    public void checkToken() {
        if (utils.isInternetAvailable(getApplicationContext()) == false) {
            AlertDialog failureAlert = new AlertDialog.Builder(MainActivity.this).create();
            failureAlert.setTitle("Failed to verify identity");
            failureAlert.setMessage("You are not connected to internet.");
            failureAlert.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            failureAlert.show();
            return;
        } else {
            dialog = new ProgressDialog(this);
            dialog.setTitle("Verification identity");
            dialog.setMessage("Verifying identity hash with server...");
            dialog.setCancelable(false);
            dialog.show();

            String url = new API().getApiLink() + "/verifyHash";

            StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    dialog.dismiss();

                    Toast.makeText(getApplicationContext(), "Hash Identity Verification Successful", Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(getApplicationContext(), ConversationsActivity.class);
                    startActivity(intent);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    error.printStackTrace();
                    AlertDialog failureAlert = new AlertDialog.Builder(MainActivity.this).create();
                    failureAlert.setTitle("Failed to verify identity hash");
                    failureAlert.setMessage(error.toString());
                    failureAlert.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    failureAlert.show();
                }
            }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("country_code", "94");
                    params.put("number", userDetails.getString("number", ""));
                    params.put("hash", userDetails.getString("hash", ""));
                    return params;
                }

                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("Content-Type", "application/x-www-form-urlencoded");
                    return params;
                }
            };

            mQueue.add(request);
        }
    }

//    public String getDeviceIMEI() {
//        String deviceUniqueIdentifier = null;
//        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
//        if (null != tm) {
//            deviceUniqueIdentifier = tm.getDeviceId();
//        }
//        if (null == deviceUniqueIdentifier || 0 == deviceUniqueIdentifier.length()) {
//            deviceUniqueIdentifier = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
//        }
//        return deviceUniqueIdentifier;
//    }
}
