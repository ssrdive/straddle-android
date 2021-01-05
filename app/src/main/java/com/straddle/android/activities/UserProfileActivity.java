package com.straddle.android.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

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

public class UserProfileActivity extends AppCompatActivity {

    TextView number, firstName, lastName, displayName, dob, status;

    private ProgressDialog dialog;
    private Utils utils;
    private RequestQueue mQueue;

    String userNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        number = findViewById(R.id.number);
        firstName = findViewById(R.id.firstName);
        lastName = findViewById(R.id.lastName);
        displayName = findViewById(R.id.displayName);
        dob = findViewById(R.id.dob);
        status = findViewById(R.id.status);

        utils = new Utils();
        mQueue = Volley.newRequestQueue(this);

        Intent intent = getIntent();
        userNumber = intent.getStringExtra("number");

        number.setText("94" + userNumber);

        loadProfileData();
    }

    public void loadProfileData() {
        dialog = new ProgressDialog(this);
        dialog.setTitle("Loading profile details");
        dialog.setMessage("Loading profile details from server...");
        dialog.setCancelable(false);
        dialog.show();

        String url = new API().getApiLink() + "/getDetails/" + userNumber;

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
                AlertDialog failureAlert = new AlertDialog.Builder(UserProfileActivity.this).create();
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
