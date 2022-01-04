package com.mycloud.configuration;

import java.util.HashMap;
import java.util.Map;

import com.mycloud.Utilities.FileReader;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONObject;

/*
To encrypt Application properties file / 
*/

public class AppProperties{

    private static Logger logger = LoggerFactory.getLogger(AppProperties.class);

    private JSONObject appProp;

    public AppProperties(){

        try{
            
            String propString = new FileReader().readFile("application.json");
            appProp = new JSONObject(propString);
            logger.info("Sucessesfully setted the properties");

        }catch(Exception e){
            e.printStackTrace();
        }

    }

    public Map<String,String> getProperty(String properyName){
        Map<String,String> props = new HashMap<String,String>();
        JSONObject propJson = this.appProp.getJSONObject(properyName);
        String[] propNames =JSONObject.getNames(propJson);
        for(String propName:propNames){
            props.put(propName, propJson.getString(propName));
        }
        logger.info("returning "+properyName+" properties");
        return props;
    }

    public static void main(String[] args) {

        AppProperties app = new AppProperties();
        app.getProperty("gcp").forEach((K,V)->{
            System.out.println("Key : "+K +" Value : "+V);
        });
    } 
}