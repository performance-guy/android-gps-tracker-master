package com.ods.salman;


public class Constants {
    //DEV
    public static final String APIEndpoint = "http://api.virnt.com/";

    // PRO
//    public static final String APIEndpoint = "http://test.ygaps.com/";

    public static final long expire_token = 24*60*60 ;


    public static final String FIRST_TIME = "first_time";
    public static final String TOKEN = "token";
    public static final String DATASOURCE_VARIABLE = "location";
    public static final String VARIABLE_ID = "variable";
    public static final String PUSH_TIME = "push_time";
    public static final String SERVICE_RUNNING = "service_running";

    public static final int NOTIFICATION_ID = 1337;
    public static class VARIABLE_CONTEXT {
        public static final String LATITUDE = "lat";
        public static final String LONGITUDE = "lng";
    }
}
