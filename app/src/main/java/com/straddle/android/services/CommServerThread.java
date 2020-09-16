package com.straddle.android.services;

import com.straddle.android.api.API;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class CommServerThread extends Thread {
    Socket commSocket;
    DataOutputStream commOutput;
    DataInputStream commInput;

    public CommServerThread(Socket commSocket, DataOutputStream commOutput, DataInputStream commInput) {
        this.commSocket = commSocket;
        this.commOutput = commOutput;
        this.commInput = commInput;
    }

    public void run() {
        try {
            commSocket = new Socket(new API().getServerIP(), new API().getCommServerPort());
            commOutput = new DataOutputStream(commSocket.getOutputStream());
            commInput = new DataInputStream(commSocket.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
