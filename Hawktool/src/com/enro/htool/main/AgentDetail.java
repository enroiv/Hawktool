package com.enro.htool.main;

import java.util.ArrayList;
import java.util.List;

import COM.TIBCO.hawk.console.hawkeye.AgentInstance;
import COM.TIBCO.hawk.talon.MicroAgentID;

public class AgentDetail {
	private AgentInstance agentInstance = null;
	private MicroAgentID rbe = null;				// Rulebase Engine MicroAgent
	private MicroAgentID srv = null;				// Services MicroAgent
	private MicroAgentID hev = null;				// Hawk Event MicroAgent
	private List<MicroAgentID> mas = new ArrayList<MicroAgentID>();
	
	public void addMA(MicroAgentID maid){
		mas.add(maid);
	}
	
	public List<MicroAgentID> getMAs(){
		return mas;
	}

	public MicroAgentID getRbe() {
		return rbe;
	}

	public void setRbe(MicroAgentID rbe) {
		this.rbe = rbe;
	}

	public MicroAgentID getSrv() {
		return srv;
	}

	public void setSrv(MicroAgentID srv) {
		this.srv = srv;
	}

	public AgentInstance getAgentInstance() {
		return agentInstance;
	}

	public void setAgentInstance(AgentInstance agentInstance) {
		this.agentInstance = agentInstance;
	}

	public MicroAgentID getHev() {
		return hev;
	}

	public void setHev(MicroAgentID hev) {
		this.hev = hev;
	}
}
