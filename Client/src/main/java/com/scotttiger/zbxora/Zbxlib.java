package com.scotttiger.zbxora;

import java.sql.Timestamp;

import java.util.ArrayList;

public class Zbxlib {
    public Zbxlib(ArrayList output) {
        super();
    }


    public void output(String host, String key,String values) {        
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String line = host + " " + key + " " + timestamp.toString() + " " + values + "\n";
        
    }

}
