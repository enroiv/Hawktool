package com.enro.htool.main;

import java.util.Hashtable;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enro.htool.common.ConfReader;
import com.enro.htool.common.HToolUtil;

import COM.TIBCO.hawk.utilities.misc.HawkConstants;

public class HToolConsoleFactory {
	
	private static Logger logger = LoggerFactory.getLogger(HToolConsoleFactory.class);

	public static NuHToolConsole getInstance(ConfReader reader) {
					
					
		Properties props = reader.getSection("General");
		String transport = reader.getString("General", "hawk_transport", "none");
		
		if(transport.contentEquals("none")){
			System.err.println("Invalid configuration specified");
			System.exit(1);
		} else {
			props.putAll(reader.getSection(transport));
			
			// In case SSL was specified, attempt to read it
			props.putAll(reader.getSection("SSL"));
			
			Hashtable <String,String> sslProps = HToolUtil.processSSLProperties(props);
    		
			boolean hasSSL = (sslProps.size() > 0);
    		if(hasSSL){
    			logger.info((hasSSL ? "" : "No ") + "SSL details were loaded.");
    			props.put(HawkConstants.HAWK_SSL_PARAMS, sslProps);
    		}
		}
		
		props.putAll(reader.getSection("Template"));
		NuHToolConsole console = null;
		
		try{
			console = new NuHToolConsole(props);
		} catch(Exception e){
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		
		return console; 
	}

}
