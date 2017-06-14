package com.enro.htool.main;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
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
import COM.TIBCO.hawk.talon.DataElement;
import COM.TIBCO.hawk.talon.MethodInvocation;
import COM.TIBCO.hawk.talon.MicroAgentData;
import COM.TIBCO.hawk.talon.MicroAgentException;
import COM.TIBCO.hawk.talon.MicroAgentID;
import COM.TIBCO.hawk.talon.TabularData;

public class HToolConsole implements AgentMonitorListener,
MicroAgentListMonitorListener, ErrorExceptionListener{
	
	private final static Logger logger = Logger.getLogger(HToolConsole.class.getName());
	
	private boolean isInitialized = false;
	private TIBHawkConsole console;
	private AgentMonitor agentMonitor;
	private AgentManager agentManager;
	
	private Properties props;
	private String agentName;
	
	/*
	 * This map contains all agents in the domain along with their associated MicroAgents:
	 * AG1			MA1_Name	MA1_ID
	 * 				MA2_Name	MA2_ID
	 * 				MAn_Name	MAn_ID
	 * 
	 * AGn			MA1_Name	MA1_ID
	 * 				MA2_Name	MA2_ID
	 * 				MAn_Name	MAn_ID				
	 */
	private Map<String,Map<String,MicroAgentID>> agentDetail = null;
	
	/*
	 * This map contains the associated Rulebase Engine MicroAgent for every other MicroAgent
	 * in the domain (so the application will know where to push the rulebases to)
	 * MA1_Name@AGnm		RBE_ID
	 * MA2_Name@AGnm		RBE_ID
	 * MAn_Name@Agnm		RBE_ID
	 */
	private Map<String,MicroAgentID> rbeDetail = new HashMap<String,MicroAgentID>();
	
	/*
	 * This map contains the associated Services MicroAgent for every other MicroAgent
	 * in the domain
	 * MA1_Name@AGnm		SRV_ID
	 * MA2_Name@AGnm		SRV_ID
	 * MAn_Name@Agnm		SRV_ID
	 */
	private Map<String,MicroAgentID> srvDetail = new HashMap<String,MicroAgentID>();
	
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
	
	private void addMicroagents(AgentInstance ai, MicroAgentID [] mIDs) {
		
		Map<String,MicroAgentID> maidDtl = new HashMap<String,MicroAgentID>();
		MicroAgentID rbeMAID = null;
		
		String aiNm = ai.getAgentID().getName();
		if (!aiNm.equals(agentName)){
			logger.log(Level.INFO,agentName + " found external agent: " + aiNm);
		} else{
			logger.log(Level.INFO,"Found local agent: " + aiNm);
		}
		
		// First pass to get the RulebaseEngine and Services ID for the agent
		for(MicroAgentID mID : mIDs){
			if(mID.getName().contentEquals(HToolConstants.REMANM)) rbeMAID = mID;
			if(mID.getName().contentEquals(HToolConstants.SERVMA)) srvDetail.put(mID.getName()+"@"+aiNm, mID);
		}
		
		// Second pass to fill the MicroAgent ID by name and RBE by name maps
		for(MicroAgentID mID : mIDs){
			String mNm = mID.getName();
			maidDtl.put(mNm,mID);
			rbeDetail.put(mNm+"@"+aiNm,rbeMAID);
		}
		
		if(null == agentDetail) agentDetail = new HashMap<String,Map<String,MicroAgentID>>();
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
		addMicroagents(agntInst,agntInst.getStatusMicroAgents());		
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
		addMicroagents(event.getAgentInstance(),mIDs);
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

	public MicroAgentID [] getRBEMicroAgentsFor(String microAgent,String service) {
		
		Set<String> matching = rbeDetail.keySet();
		Set<String> matched = new HashSet<String>();
		Set<MicroAgentID> extracted = new HashSet<MicroAgentID>();
		
		for(String s : matching){
			if(s.contains(microAgent)) matched.add(s);
		}
		
		for(String e : matched){
			MicroAgentID maid = rbeDetail.get(e);
			if(null != maid) extracted.add(maid);
		}
		
		MicroAgentID [] maids = extracted.toArray(new MicroAgentID[extracted.size()]);
		
		return filterByService(maids,service);
	}
	
	private MicroAgentID[] filterByService(MicroAgentID[] maids, String service) {
		
		// No service was specified. Return full array
		if (null == service) return maids;
		
		logger.log(Level.FINE,"Looking for agents implementing "+service);
		
		// Return only those MicroAgents which implement the requested service
		Set<MicroAgentID> s = new HashSet<MicroAgentID>();
		
		for(MicroAgentID maid : maids){
			String agentNm = maid.getAgent().getName();
			
			logger.log(Level.FINE,"Inspecting "+agentNm);
			
			// Get the associated Service MA for this MicroAgent and check if it implements the requested service
			MicroAgentID srvMa = srvDetail.get(HToolConstants.SERVMA+"@"+agentNm);
			if(getServiceMicroAgentsFor(srvMa,service)) {
				s.add(maid);
				logger.log(Level.FINE,agentNm+" implements "+service);
			}
		}
		
		return s.toArray(new MicroAgentID[s.size()]);
	}

	public boolean getServiceMicroAgentsFor(MicroAgentID srvMaid, String service) {
		
		boolean rslt = false;
		
		// null MicroAgents don't implement anything!!
		if(null==srvMaid) return rslt;
		
		try{
			DataElement[] dataElements = new DataElement[1];
			dataElements[0] = new DataElement("Service", "");
			MethodInvocation mi = new MethodInvocation("getServiceStatus", dataElements);
			TabularData td = (TabularData) invoke(srvMaid, mi).getData();
			Object [][] fullTable = td.getAllData();
			
			for(int i=0;i<fullTable.length;i++){
				String str = fullTable[i][0].toString();
				
				if(str.contains(service)){
					rslt = true;
					break;
				}
			}
		} catch(Exception ex){
			ex.printStackTrace();
		}
		
		return rslt;
	}
	
	public void showDomainDetails(){
		
		System.out.println("Hawk Agent Detail");
		
		Set<Entry<String,Map<String,MicroAgentID>>> s = agentDetail.entrySet();
		Iterator<Entry<String,Map<String,MicroAgentID>>> it = s.iterator();
		while(it.hasNext()){
			Entry<String,Map<String,MicroAgentID>> e = it.next();
			System.out.println("Agent "+e.getKey());
			Map<String,MicroAgentID> m = e.getValue();
			Set<Entry<String,MicroAgentID>> ss = m.entrySet();
			Iterator<Entry<String,MicroAgentID>> ii = ss.iterator();
			while(ii.hasNext()){
				Entry <String,MicroAgentID> ee = ii.next();
				System.out.println("\tMicroAgent: "+ee.getKey());
			}
			System.out.println("\n");
		}
		
		System.out.println("\n\nRulebase Engine Detail");
		
		Set<Entry<String,MicroAgentID>> sss = rbeDetail.entrySet();
		Iterator<Entry<String,MicroAgentID>> iii = sss.iterator();
		while(iii.hasNext()){
			Entry<String,MicroAgentID> eee = iii.next();
			MicroAgentID mID = eee.getValue();
			System.out.println("Micro Agent: "+eee.getKey()+"\tRBE: "+mID.getAgent().getName());
		}
		
		System.out.println("\n");
	}
	
}
