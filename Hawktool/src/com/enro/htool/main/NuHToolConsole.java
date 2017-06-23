package com.enro.htool.main;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class NuHToolConsole implements AgentMonitorListener,
MicroAgentListMonitorListener, ErrorExceptionListener{
	
	private List<Dispatcher> dispatchers = new ArrayList<Dispatcher>();
	private static Logger logger = LoggerFactory.getLogger(NuHToolConsole.class);
	private TIBHawkConsole console = null;

	public NuHToolConsole(Properties props) throws Exception {
		
		String agentNm = props.getProperty("agent_name");
		
		// if agent name is not specified use the local host name
		if (agentNm == null) {
			try {
				java.net.InetAddress hostInetInfo = java.net.InetAddress.getLocalHost();
				agentNm = hostInetInfo.getHostName();
				props.put("agent_name", agentNm);
			} catch (java.net.UnknownHostException uhe) {}
		}
		
		// Create the TIBHawkConsole Instance
		console = TIBHawkConsoleFactory.getInstance().createHawkConsole(props);

		AgentMonitor agentMonitor = console.getAgentMonitor();
		
		// create and add listener for console errors
		agentMonitor.addErrorExceptionListener(this);

		// create and add listener for agents
		agentMonitor.addAgentMonitorListener(this);

		// create and add listener for microagents
		agentMonitor.addMicroAgentListMonitorListener(this);


	}

	public void addDispatcher(Dispatcher dispatcher) {
		this.dispatchers.add(dispatcher);
	}

	public void start() throws Exception {
		console.getAgentMonitor().initialize();
		console.getAgentManager().initialize();
		
	}

	public void end() {
		console.getAgentManager().shutdown();
		console.getAgentMonitor().shutdown();
		
	}
	
	public MicroAgentData invoke(MicroAgentID maid, MethodInvocation mi)
			throws MicroAgentException {
		AgentManager am = console.getAgentManager();
		return (am.invoke(maid, mi));
	}

	/*
	 * (non-Javadoc)
	 * AgentMonitorListener methods
	 * @see COM.TIBCO.hawk.console.hawkeye.AgentMonitorListener#onAgentAlive(COM.TIBCO.hawk.console.hawkeye.AgentMonitorEvent)
	 */
	@Override
	public void onAgentAlive(AgentMonitorEvent e) {
		AgentInstance agentInstance = e.getAgentInstance();
		AgentDetail agentDetail = new AgentDetail();
		agentDetail.setAgentInstance(agentInstance);
		
		for(MicroAgentID maid : agentInstance.getStatusMicroAgents()){
			
			if(maid.getName().contentEquals(HToolConstants.REMANM)) {
				agentDetail.setRbe(maid);
				continue;
			}
			
			if(maid.getName().contentEquals(HToolConstants.SERVMA)) agentDetail.setSrv(maid);
			agentDetail.addMA(maid);
		}
		
		for(Dispatcher d : this.dispatchers){
			logger.debug(d.getName() + " is now dispatching.");
			logger.debug("Processing all microagents  at " + agentInstance.getAgentID().getName());
			d.dispatch(agentDetail);
		}
	}

	@Override
	public void onAgentExpired(AgentMonitorEvent e) {
		logger.info(e.toString());
	}
	
	/*
	 * (non-Javadoc)
	 * MicroAgentListMonitorListener methods
	 * @see COM.TIBCO.hawk.console.hawkeye.MicroAgentListMonitorListener#onMicroAgentAdded(COM.TIBCO.hawk.console.hawkeye.MicroAgentListMonitorEvent)
	 */
	@Override
	public void onMicroAgentAdded(MicroAgentListMonitorEvent e) {
		
		AgentInstance agentInstance = e.getAgentInstance();
		MicroAgentID microAgent = e.getMicroAgentID();
		
		AgentDetail agentDetail = new AgentDetail();
		agentDetail.setAgentInstance(agentInstance);
		
		for(MicroAgentID maid : agentInstance.getStatusMicroAgents()){
			if(maid.getName().contentEquals(HToolConstants.REMANM)) agentDetail.setRbe(maid);
			if(maid.getName().contentEquals(HToolConstants.SERVMA)) agentDetail.setSrv(maid);
		}
		
		agentDetail.addMA(microAgent);
		
		for(Dispatcher d : this.dispatchers){
			logger.debug(d.getName() + " is now dispatching.");
			logger.debug("Processing for " + microAgent.getName() + " at " + agentInstance.getAgentID().getName());
			d.dispatch(agentDetail);
		}
	}

	@Override
	public void onMicroAgentRemoved(MicroAgentListMonitorEvent e) {
		logger.info(e.toString());
	}
	
	/*
	 * (non-Javadoc)
	 * ErrorExceptionListener methods
	 * @see COM.TIBCO.hawk.console.hawkeye.ErrorExceptionListener#onErrorExceptionEvent(COM.TIBCO.hawk.console.hawkeye.ErrorExceptionEvent)
	 */
	@Override
	public void onErrorExceptionEvent(ErrorExceptionEvent e) {
		logger.error(e.toString());
		
	}
	
}
