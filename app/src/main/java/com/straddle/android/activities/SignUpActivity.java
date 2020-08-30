package com.straddle.android.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.straddle.android.R;
import com.straddle.android.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;

public class SignUpActivity extends AppCompatActivity implements View.OnClickListener {

    Spinner countryCode;
    Button verifyNumber;
    Button next;

    private RequestQueue mQueue;
    private Utils utils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        ArrayList<String> arrCode = new ArrayList<String>(Arrays.asList(getResources().getStringArray(R.array.country_code)));

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                R.layout.support_simple_spinner_dropdown_item, arrCode);
        countryCode = findViewById(R.id.countryCode);
        countryCode.setAdapter(dataAdapter);

        mQueue = Volley.newRequestQueue(this);

        verifyNumber = findViewById(R.id.verifyNumber);
        next = findViewById(R.id.next);
        verifyNumber.setOnClickListener(this);
        next.setOnClickListener(this);



    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.verifyNumber:
                verifyNumber.setEnabled(false);
                break;
            case R.id.next:
                Intent intent = new Intent(this, ConversationsActivity.class);
                startActivity(intent);
                break;
        }
    }
}
