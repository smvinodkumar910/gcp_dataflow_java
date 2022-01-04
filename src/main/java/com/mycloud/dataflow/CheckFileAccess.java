package com.mycloud.dataflow;

import com.mycloud.configuration.AppProperties;
import com.exasol.jdbc.EXADriver;

public class CheckFileAccess {

    public static void main(String[] args) {

        AppProperties app = new AppProperties();
        app.getProperty("gcp").forEach((K,V)->{
            System.out.println("Key :"+K+" Value :"+V);
        });
        
    }
    
}
