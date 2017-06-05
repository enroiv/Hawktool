package com.enro.htool.test;

import com.enro.htool.main.HTool;

public class HToolTest {
	public static void main(String [] a){
		
		String transport [] ={"agent_name=bw6-demo-box",
		"hawk_domain=bw-demo-domain",
		"hawk_transport=tibrv",
		"rv_service=7474",
		"rv_network=;",
		"rv_daemon=tcp:7474"};
		String [] microagents = {"COM.TIBCO.ADAPTER.bwengine"};
		
		HTool hTool = new HTool();
		hTool.process(microagents, transport);
		
	}
}
