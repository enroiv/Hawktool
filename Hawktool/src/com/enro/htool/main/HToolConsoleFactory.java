package com.enro.htool.main;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enro.htool.common.ConfReader;

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
