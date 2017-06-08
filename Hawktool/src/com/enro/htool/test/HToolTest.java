package com.enro.htool.test;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.enro.htool.common.HToolConstants;
import com.enro.htool.main.HTool;

public class HToolTest{
	
	private final static Logger logger = Logger.getLogger(HToolTest.class.getName());

    public HToolTest()
    {
    }

    public static void main(String a[])
    {
        String transport[] = {
            "agent_name=bw6-demo-box", "hawk_domain=bw-demo-domain", "hawk_transport=tibrv", "rv_service=7474", "rv_network=;", "rv_daemon=tcp:7474"
        };
        String microagents[] = {
        		HToolConstants.BWMANM/*,HToolConstants.REMANM*/
        };
        
        HTool hTool = new HTool(transport);
        
        String [] rb = {"<?xml version=\"1.0\" encoding=\"UTF-8\" ?><ruleBase><version>5.2.0</version><name><![CDATA[SampleMemRB]]></name><schedule></schedule><author>psyncopate on host bw6-demo-box(10.0.0.5) at 23:54 Mon, Jun 5, 2017</author><lastModification>psyncopate on host bw6-demo-box(10.0.0.5) at 23:59 Mon, Jun 5, 2017</lastModification><comment><![CDATA[]]></comment><rule><name><![CDATA[COM.TIBCO.ADAPTER.bwengine.%%TIBCO_DOMAIN%%.%%TIBCO_DEPLOYMENT%%.%%TIBCO_COMPONENT_INSTANCE%%:GetMemoryUsage():60]]></name><schedule></schedule><overRuling>0</overRuling><dataSource><microAgentName><![CDATA[COM.TIBCO.ADAPTER.bwengine.%%TIBCO_DOMAIN%%.%%TIBCO_DEPLOYMENT%%.%%TIBCO_COMPONENT_INSTANCE%%]]></microAgentName><methodName>GetMemoryUsage</methodName><interval>60000</interval></dataSource><test><name><![CDATA[(PercentUsed >= 90)]]></name><schedule></schedule><operator class=\"COM.TIBCO.hawk.config.rbengine.rulebase.operators.GreaterThanOrEqualTo\" ><operator class=\"COM.TIBCO.hawk.config.rbengine.rulebase.operators.RuleData\" ><dataObject class=\"java.lang.String\" ><![CDATA[PercentUsed]]></dataObject></operator><dataObject class=\"java.lang.Long\"  value=\"90\" /></operator><consequenceAction><name><![CDATA[sendAlertHigh(alertMsg=Memory usage is  ${PercentUsed} )]]></name><schedule></schedule><microAgentName>COM.TIBCO.hawk.microagent.RuleBaseEngine</microAgentName><methodName>sendAlertMessage</methodName><dataElement name=\"message\"><dataObject class=\"COM.TIBCO.hawk.config.rbengine.rulebase.util.AlertHigh\" ><![CDATA[Memory usage is  ${PercentUsed} ]]></dataObject></dataElement><properties></properties><performOnceOnly/><escalationTime>0</escalationTime></consequenceAction><trueConditionPolicy><trueCountThreshold>1</trueCountThreshold></trueConditionPolicy><clearOn><clearOnFirstFalse/></clearOn></test></rule></ruleBase>"};        
        String [] nm = {"SampleMemRB.hrb"};
        String [] ma = hTool.getMAgents(microagents);
        
        logger.log(Level.FINE,hTool.processRulebaseTemplates("bw-demo-domain", ma,rb,nm)+
        		" rulebases were sent to Hawk agents in the domain");
        
    }
}
