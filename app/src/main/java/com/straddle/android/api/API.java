package com.straddle.android.api;

public class API {
    private final String API_LINK = "https://www.straddle.dev/api";
    private final String SERVER_IP = "206.189.39.61";
    private final int PING_SERVER_PORT = 5056;
    private final int COMM_SERVER_PORT = 5058;

    public String getApiLink() {
        return this.API_LINK;
    }

    public String getServerIP() { return this.SERVER_IP; }

    public int getPingServerPort() {
        return this.PING_SERVER_PORT;
    }

    public int getCommServerPort() { return this.COMM_SERVER_PORT; }
}
