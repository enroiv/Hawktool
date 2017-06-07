package com.enro.htool.common;

import java.util.Properties;

public class HToolUtil {
	public static Properties getProps(String params[]){
        Properties p = new Properties();
        
        for(int i = 0; i < params.length; i++){
            String entry = params[i];
            String keyval[] = entry.split("=");
            p.setProperty(keyval[0], keyval[1]);
        }

        return p;
    }
}
