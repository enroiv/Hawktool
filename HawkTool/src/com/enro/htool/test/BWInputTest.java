package com.enro.htool.test;

import java.util.Map;

import com.enro.htool.main.HTool;

public class BWInputTest {
	public static String receiveStrArr(String [] mAgents, String [] hawkTransportParams){
		HTool hTool = new HTool();
		return hTool.process(mAgents,hawkTransportParams);
	}
	
	public static String receiveMap(Map<String,String> input){
		return "ok";
	}
}
