package com.enro.htool.main;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

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
	
	private final static Logger logger = Logger.getLogger(HToolConsole.class.getName());
	
	private boolean isInitialized = false;
	private TIBHawkConsole console;
	private AgentMonitor agentMonitor;
	private AgentManager agentManager;
	
	private Properties props;
	private String agentName;
	
	private Map<String,Map<String,MicroAgentID>> agentDetail = null;
	
	public HToolConsole(Properties prps) throws Exception{
		
		String agentNm = prps.getProperty("agent_name");
		
		// if agent name is not specified use the local host name
		if (agentNm == null) {
			try {
				java.net.InetAddress hostInetInfo = java.net.InetAddress.getLocalHost();
				agentNm = hostInetInfo.getHostName();
				prps.put("agent_name", agentNm);
			} catch (java.net.UnknownHostException uhe) {}
		}
		
		agentName = agentNm;
		props = prps;
		
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
		
		isInitialized = true;
		
	}
	
	public void initialize() throws Exception{
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
		
		isInitialized = true;
	}
	
	public boolean isInit(){
		return isInitialized;
	}
	
	public final void cleanUp() {
		agentMonitor.removeErrorExceptionListener(this);
		agentMonitor.removeAgentMonitorListener(this);
		agentMonitor.removeMicroAgentListMonitorListener(this);

		agentMonitor.shutdown();
		agentManager.shutdown();

		console = null;
		isInitialized = false;
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
		
		String aiNm = ai.getAgentID().getName();
		if (!aiNm.equals(agentName)){
			//TODO: Process other agents
			logger.log(Level.INFO,agentName + " found external agent: " + aiNm);
		} else{
			//TODO: Process local agent
			logger.log(Level.INFO,"Found local agent: " + aiNm);
		}
		
		for(MicroAgentID mID : mIDs){
			maidDtl.put(mID.getName(),mID);
			
			if(mID.getName().equalsIgnoreCase(HToolConstants.REMANM)){
				System.out.println("Adding "+mID.getName()+ " to " + aiNm);
			}
		}
		
		if(null == agentDetail){
			agentDetail = new HashMap<String,Map<String,MicroAgentID>>();
		}
		
		agentDetail.put(aiNm, maidDtl);
	}
	
	void removeMicroagent(MicroAgentID mID) {
		String agNm = mID.getAgent().getName();
		Map<String,MicroAgentID> maidDtl = agentDetail.get(agNm);
		maidDtl.remove(mID.getName());
		agentDetail.put(agNm, maidDtl);
	}
	
	/*AgentMonitorListener methods */
	@Override
	public synchronized void onAgentAlive(AgentMonitorEvent event) {
		AgentInstance agntInst = event.getAgentInstance();
		logger.log(Level.INFO,"Hawk agent <" + agntInst.getAgentID().getName() + "> was found");
		addMicroagent(agntInst,agntInst.getStatusMicroAgents());		
	}

	@Override
	public synchronized void onAgentExpired(AgentMonitorEvent event) {
		
		AgentInstance agntInst = event.getAgentInstance();
		
		logger.log(Level.INFO,agntInst.getAgentID().getName() + " has expired");

		if (!agntInst.getAgentID().getName().equals(agentName))
			return;

		MicroAgentID[] mIDs = agntInst.getStatusMicroAgents();
		for (int i = 0; i < mIDs.length; i++) {
			removeMicroagent(mIDs[i]);
		}
	}
	
	/* MicroAgentListMonitorListener methods */
	@Override
	public synchronized void onMicroAgentAdded(MicroAgentListMonitorEvent event) {
		MicroAgentID [] mIDs = {event.getMicroAgentID()};
		addMicroagent(event.getAgentInstance(),mIDs);
	}

	@Override
	public synchronized void onMicroAgentRemoved(MicroAgentListMonitorEvent event) {
		MicroAgentID mID = event.getMicroAgentID();
		removeMicroagent(mID);
	}	
	
	/*ErrorExceptionListener methods */
	@Override
	public synchronized void onErrorExceptionEvent(ErrorExceptionEvent event) {
		logger.log(Level.SEVERE,"onErrorExceptionEvent: event=" + event);
	}
	
}
