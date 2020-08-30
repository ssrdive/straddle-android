package com.straddle.android.utils;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NumberFormatter {
    public String format(double value) {
        DecimalFormat formatter = new DecimalFormat("#,###.00");
        if (value == 0)
            return "0";
        else
            return formatter.format(value);
    }

    public String format(String value) {
        DecimalFormat formatter = new DecimalFormat("#,###.00");
        if (Float.parseFloat(value) == 0)
            return value;
        else
            return formatter.format(Float.parseFloat(value));
    }
}
