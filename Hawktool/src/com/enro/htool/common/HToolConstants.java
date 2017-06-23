package com.enro.htool.common;

public class HToolConstants {
	// MicroAgent names
	public static final String BWMANM = "COM.TIBCO.ADAPTER.bwengine";					// BW MicroAgent
	public static final String HEMANM = "COM.TIBCO.hawk.microagent.HawkEventService";	// Hawk Event MicroAgent
	public static final String REMANM = "COM.TIBCO.hawk.microagent.RuleBaseEngine"; 	// Rulebase Engine MicroAgent
	public static final String SERVMA = "COM.TIBCO.hawk.hma.Services";					// Services MicroAgent
	
	// Rulebase replacement expressions
	public static final String CMPEXP = "%%TIBCO_COMPONENT_INSTANCE%%";
	public static final String DEPEXP = "%%TIBCO_DEPLOYMENT%%";
	public static final String DOMEXP = "%%TIBCO_DOMAIN%%";
	public static final String EAGEXP = "%%TIBCO_EVENT_AGENT%%";
	public static final String TAGEXP = "%%TIBCO_AGENT%%";
	public static final String SCREXP = "%%TIBCO_AGENT_SCRIPT%%";
	
	// Misc parameters
	public static final int INTVL = 15 * 1000;
	public static final int NUMAT = 10;
}
