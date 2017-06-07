package com.enro.htool.main;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.enro.htool.common.HToolConstants;

import COM.TIBCO.hawk.console.hawkeye.AgentInstance;
import COM.TIBCO.hawk.console.hawkeye.AgentManager;
import COM.TIBCO.hawk.console.hawkeye.AgentMonitor;
import COM.TIBCO.hawk.console.hawkeye.AgentMonitorEvent;
import COM.TIBCO.hawk.console.hawkeye.AgentMonitorListener;
import COM.TIBCO.hawk.console.hawkeye.ErrorExceptionEvent;
import COM.TIBCO.hawk.console.hawkeye.ErrorExceptionListener;
import COM.TIBCO.hawk.console.hawkeye.MicroAgentListMonitorEvent;
import COM.TIBCO.hawk.console.hawkeye.MicroAgentListMonitorListener;
import COM.TIBCO.hawk.console.hawkeye.TIBHawkConsole;
import COM.TIBCO.hawk.console.hawkeye.TIBHawkConsoleFactory;
import COM.TIBCO.hawk.talon.MethodInvocation;
import COM.TIBCO.hawk.talon.MicroAgentData;
import COM.TIBCO.hawk.talon.MicroAgentException;
import COM.TIBCO.hawk.talon.MicroAgentID;

public class HToolConsole implements AgentMonitorListener,
MicroAgentListMonitorListener, ErrorExceptionListener{
	
	private TIBHawkConsole console;
	private AgentMonitor agentMonitor;
	private AgentManager agentManager;
	
	private String agentName;
	
	private Map<String,Map<String,MicroAgentID>> agentDetail = new HashMap<String,Map<String,MicroAgentID>>();
	
	private static final boolean DEBUG = false;
	
	public HToolConsole(Properties props) throws Exception{
		
		String agentNm = props.getProperty("agent_name");
		
		// if agent name is not specified use the local host name
		if (agentNm == null) {
			try {
				java.net.InetAddress hostInetInfo = java.net.InetAddress.getLocalHost();
				agentNm = hostInetInfo.getHostName();
				props.put("agent_name", agentNm);
			} catch (java.net.UnknownHostException uhe) {}
		}
		
		agentName = agentNm;

		// Create the TIBHawkConsole Instance
		console = TIBHawkConsoleFactory.getInstance().createHawkConsole(props);

		agentMonitor = console.getAgentMonitor();
		agentManager = console.getAgentManager();

		// create and add listener for console errors
		agentMonitor.addErrorExceptionListener(this);

		// create and add listener for agents
		agentMonitor.addAgentMonitorListener(this);

		// create and add listener for microagents
		agentMonitor.addMicroAgentListMonitorListener(this);

		agentMonitor.initialize();
		agentManager.initialize();
	}
	
	public final void cleanUp() {
		agentMonitor.removeErrorExceptionListener(this);
		agentMonitor.removeAgentMonitorListener(this);
		agentMonitor.removeMicroAgentListMonitorListener(this);

		agentMonitor.shutdown();
		agentManager.shutdown();

		System.exit(0);
	}
	
	public String getAgentName() {
		return agentName;
	}
	
	public Map<String,Map<String,MicroAgentID>> getAgentDetails(){
		return agentDetail;
	}
	
	public MicroAgentID getRulebaseEngineMicroagentID(String agNm) {
		return getMicroAgentID(((null==agNm || agNm.length()==0)?"":agNm),HToolConstants.REMANM);
	}
	
	public MicroAgentID getMicroAgentID(String agNm,String maNm){
		String agent = (agNm.length()==0) ? agentName : agNm;
		return agentDetail.get(agent).get(maNm);
	}
	
	public MicroAgentData invoke(MicroAgentID maid, MethodInvocation mi)
			throws MicroAgentException {
		AgentManager am = console.getAgentManager();
		return (am.invoke(maid, mi));
	}
	
	private void addMicroagent(AgentInstance ai, MicroAgentID [] mIDs) {
		
		Map<String,MicroAgentID> maidDtl = new HashMap<String,MicroAgentID>();
		
		if (!ai.getAgentID().getName().equals(agentName)){
			//TODO: Process other agents
			println("Found external agent: " + agentName);
		} else{
			//TODO: Process local agent
			println("Found agent: " + agentName);
		}
		
		for(MicroAgentID mID : mIDs){
			
			log("addMicroagent:  " + mID.getAgent().getName() + ":" + mID.getName());
			maidDtl.put(mID.getName(),mID);
			
		}
		
		agentDetail.put(ai.getAgentID().getName(), maidDtl);
	}
	
	void removeMicroagent(MicroAgentID mID) {
		String agNm = mID.getAgent().getName();
		log("removeMicroagent:  " + agNm + ":" + mID.getName());
		Map<String,MicroAgentID> maidDtl = agentDetail.get(agNm);
		maidDtl.remove(mID.getName());
		agentDetail.put(agNm, maidDtl);
	}
	
	/*AgentMonitorListener methods */
	@Override
	public synchronized void onAgentAlive(AgentMonitorEvent event) {
		
		println("onAgentAlive");
		
		AgentInstance agntInst = event.getAgentInstance();
		addMicroagent(agntInst,agntInst.getStatusMicroAgents());		
	}

	

	@Override
	public synchronized void onAgentExpired(AgentMonitorEvent event) {
		
		println("onAgentExpired");
		
		AgentInstance agntInst = event.getAgentInstance();

		if (!agntInst.getAgentID().getName().equals(agentName))
			return;
		println("Loosing agent: " + agentName);

		MicroAgentID[] mIDs = agntInst.getStatusMicroAgents();
		for (int i = 0; i < mIDs.length; i++) {
			removeMicroagent(mIDs[i]);
		}
	}
	
	/* MicroAgentListMonitorListener methods */
	@Override
	public synchronized void onMicroAgentAdded(MicroAgentListMonitorEvent event) {
		println("onMicroAgentAdded");
		
		MicroAgentID [] mIDs = {event.getMicroAgentID()};
		addMicroagent(event.getAgentInstance(),mIDs);
	}

	@Override
	public synchronized void onMicroAgentRemoved(MicroAgentListMonitorEvent event) {
		println("onMicroAgentRemoved");
		MicroAgentID mID = event.getMicroAgentID();
		removeMicroagent(mID);
	}	
	
	/*ErrorExceptionListener methods */
	@Override
	public synchronized void onErrorExceptionEvent(ErrorExceptionEvent event) {
		println("onErrorExceptionEvent: event=" + event);
	}
	
	void println(String s) {
		System.out.println(s);
	}
	
	void log(String s) {
		if (!DEBUG)
			return;
		System.out.println(s);
	}
	
}
