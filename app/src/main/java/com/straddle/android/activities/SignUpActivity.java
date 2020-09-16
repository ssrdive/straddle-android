package com.straddle.android.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity implements View.OnClickListener {
    private SharedPreferences userDetails;

    Spinner countryCode;
    Button sendPin;
    Button next;
    Button verifyPin;
    EditText number;
    EditText pin;
    TextView disclaimer;

    private ProgressDialog dialog;

    private RequestQueue mQueue;
    private Utils utils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        userDetails = getSharedPreferences("user_details", MODE_PRIVATE);

        ArrayList<String> arrCode = new ArrayList<String>(Arrays.asList(getResources().getStringArray(R.array.country_code)));

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                R.layout.support_simple_spinner_dropdown_item, arrCode);
        countryCode = findViewById(R.id.countryCode);
        countryCode.setAdapter(dataAdapter);

        mQueue = Volley.newRequestQueue(this);

        utils = new Utils();

        number = findViewById(R.id.number);
        pin = findViewById(R.id.pin);

        sendPin = findViewById(R.id.sendPin);
        sendPin.setOnClickListener(this);

        next = findViewById(R.id.next);
        next.setOnClickListener(this);

        verifyPin = findViewById(R.id.verifyPin);
        verifyPin.setOnClickListener(this);

        disclaimer = findViewById(R.id.disclaimer);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sendPin:
                sendPin.setEnabled(false);
                number.setEnabled(false);
                sendPin();
                break;
            case R.id.next:
                Intent intent = new Intent(this, ConversationsActivity.class);
                startActivity(intent);
                break;
            case R.id.verifyPin:
                verifyPin();
                break;
        }
    }
    
    public void sendPin() {
        final String number = this.number.getText().toString();
        if (utils.isInternetAvailable(getApplicationContext()) == false) {
            AlertDialog failureAlert = new AlertDialog.Builder(SignUpActivity.this).create();
            failureAlert.setTitle("Failed to send verification PIN");
            failureAlert.setMessage("You are not connected to internet.");
            failureAlert.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            failureAlert.show();
            this.sendPin.setEnabled(true);
            return;
        } else {
            dialog = new ProgressDialog(this);
            dialog.setTitle("Verification PIN");
            dialog.setMessage("Sending verification PIN please wait...");
            dialog.setCancelable(false);
            dialog.show();

            String url = new API().getApiLink() + "/signup";

            StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    dialog.dismiss();
                    sendPin.setEnabled(true);
                    pin.setVisibility(View.VISIBLE);
                    verifyPin.setVisibility(View.VISIBLE);
                    disclaimer.setVisibility(View.VISIBLE);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    dialog.dismiss();
                    error.printStackTrace();
                    enableSendPINControls();
                    AlertDialog failureAlert = new AlertDialog.Builder(SignUpActivity.this).create();
                    failureAlert.setTitle("Failed to send verification PIN");
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
                    params.put("number", number);
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

    public void enableSendPINControls() {
        number.setEnabled(true);
        sendPin.setEnabled(true);
    }

    public void verifyPin() {
        final String number = this.number.getText().toString();
        final String pin = this.pin.getText().toString();
        if (utils.isInternetAvailable(getApplicationContext()) == false) {
            AlertDialog failureAlert = new AlertDialog.Builder(SignUpActivity.this).create();
            failureAlert.setTitle("Failed verify PIN");
            failureAlert.setMessage("You are not connected to internet.");
            failureAlert.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            failureAlert.show();
            this.sendPin.setEnabled(true);
            return;
        } else {
            dialog = new ProgressDialog(this);
            dialog.setTitle("PIN Verification");
            dialog.setMessage("Verifying PIN please wait...");
            dialog.setCancelable(false);
            dialog.show();

            String url = new API().getApiLink() + "/verifyPin";

            StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    dialog.dismiss();
                    SharedPreferences.Editor userEditor = userDetails.edit();

                    userEditor.putString("country_code", "94");
                    userEditor.putString("number", number);
                    userEditor.putString("hash", response);

                    userEditor.apply();

                    getApplicationContext().deleteDatabase("straddle.db");

                    Toast.makeText(getApplicationContext(), "PIN Verification Successful", Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(getApplicationContext(), ConversationsActivity.class);
                    startActivity(intent);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    dialog.dismiss();
                    error.printStackTrace();
                    enableSendPINControls();
                    AlertDialog failureAlert = new AlertDialog.Builder(SignUpActivity.this).create();
                    failureAlert.setTitle("Failed to send verification PIN");
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
                    params.put("number", number);
                    params.put("pin", pin);
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
}
