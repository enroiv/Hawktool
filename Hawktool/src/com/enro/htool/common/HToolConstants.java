package com.enro.htool.common;

public class HToolConstants {
	// Hawk Event Service MicroAgent
	public static final String SERVMA = "COM.TIBCO.hawk.hma.Services";
	
	// JMS plugin MicroAgent
	//public static final String JMSMA = "COM.TIBCO.hawk.tibjms.HawkController";
	
	// BW MicroAgent
	public static final String BWMANM = "COM.TIBCO.ADAPTER.bwengine";
	
	// Rulebase Engine MicroAgent
	public static final String REMANM = "COM.TIBCO.hawk.microagent.RuleBaseEngine";
	
	// Rulebase replacement expressions
	public static final String DOMEXP = "%%TIBCO_DOMAIN%%";
	public static final String DEPEXP = "%%TIBCO_DEPLOYMENT%%";
	public static final String CMPEXP = "%%TIBCO_COMPONENT_INSTANCE%%";
	
	// Misc parameters
	public static final int INTVL = 15 * 1000;
	public static final int NUMAT = 10;
}
