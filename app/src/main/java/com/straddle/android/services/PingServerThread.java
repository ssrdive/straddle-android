package com.straddle.android.services;

import com.straddle.android.api.API;

import java.io.DataOutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

public class PingServerThread extends Thread {
    Socket pingSocket;
    DataOutputStream pingOutput;
    String number;

    public PingServerThread(Socket pingSocket, DataOutputStream pingOutput, String number) {
        this.pingSocket = pingSocket;
        this.pingOutput = pingOutput;
        this.number = number;
    }

    @Override
    public void run() {
        try {
            pingSocket = new Socket(new API().getServerIP(), new API().getPingServerPort());
            pingOutput = new DataOutputStream(pingSocket.getOutputStream());

            for(;;) {
                pingOutput.writeUTF("PING~"+number+"~"+getLocalIPAddress());
                TimeUnit.SECONDS.sleep(2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public String getLocalIPAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
