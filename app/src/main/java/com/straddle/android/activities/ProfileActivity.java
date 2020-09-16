package com.straddle.android.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    EditText firstName, lastName, displayName, dob, status;

    private ProgressDialog dialog;
    private Utils utils;
    private SharedPreferences userDetails;
    private RequestQueue mQueue;

    private Button updateProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        firstName = findViewById(R.id.firstName);
        lastName = findViewById(R.id.lastName);
        displayName = findViewById(R.id.displayName);
        dob = findViewById(R.id.dob);
        status = findViewById(R.id.status);
        updateProfile = findViewById(R.id.updateProfile);

        updateProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                udpateData();
            }
        });

        utils = new Utils();
        userDetails = getSharedPreferences("user_details", MODE_PRIVATE);
        mQueue = Volley.newRequestQueue(this);

        loadProfileData();
    }

    public void udpateData() {
        dialog = new ProgressDialog(this);
        dialog.setTitle("Updating Profile");
        dialog.setMessage("Updating profile please wait...");
        dialog.setCancelable(false);
        dialog.show();

        String url = new API().getApiLink() + "/updateProfile";

        StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                dialog.dismiss();
                Toast.makeText(getApplicationContext(), "Profile udpated", Toast.LENGTH_SHORT).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dialog.dismiss();
                error.printStackTrace();
                AlertDialog failureAlert = new AlertDialog.Builder(ProfileActivity.this).create();
                failureAlert.setTitle("Failed to update profile");
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
                params.put("number", userDetails.getString("number", ""));
                params.put("first_name", firstName.getText().toString());
                params.put("last_name", lastName.getText().toString());
                params.put("display_name", displayName.getText().toString());
                params.put("dob", dob.getText().toString());
                params.put("status", status.getText().toString());
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

    public void loadProfileData() {
        dialog = new ProgressDialog(this);
        dialog.setTitle("Loading profile details");
        dialog.setMessage("Loading profile details from server...");
        dialog.setCancelable(false);
        dialog.show();

        String url = new API().getApiLink() + "/getDetails/" + userDetails.getString("number", "");

        StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                dialog.dismiss();

                try {
                    JSONObject res = new JSONObject(response);

                    firstName.setText(res.getString("first_name"));
                    lastName.setText(res.getString("last_name"));
                    displayName.setText(res.getString("display_name"));
                    dob.setText(res.getString("dob"));
                    status.setText(res.getString("status"));
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                AlertDialog failureAlert = new AlertDialog.Builder(ProfileActivity.this).create();
                failureAlert.setTitle("Failed to load prfile details");
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
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/x-www-form-urlencoded");
                return params;
            }
        };

        mQueue.add(request);

    }
}
