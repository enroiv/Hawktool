package com.enro.htool.main;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.enro.htool.common.HToolUtil;

public class RBReplicator {
	
	private final static Logger logger = Logger.getLogger(RBReplicator.class.getName());
	private Properties properties;
	private String path;
	private HTool hTool;
	private String [] reqMicroAgents;
	private String [] domMicroAgents;
	private Map<String,Map<String,String>> maRulebases;
	private String domain;
	
	public RBReplicator(String path) {
		this.path = path;
	}
	
	public void init() throws Exception{
		setProps(HToolUtil.getProperties(path));
		domain = properties.getProperty("hawk_domain");
		Map<String,Properties> maDetail	= HToolUtil.getMADetail(path);
		maRulebases = HToolUtil.getMARulebases(properties.getProperty("path"),maDetail);

		reqMicroAgents = maRulebases.keySet().toArray(new String[maRulebases.keySet().size()]);
		
		hTool = new HTool(properties);	
		
		domMicroAgents = hTool.getMAgents(reqMicroAgents);
	}

	private static void showUsage(){
		System.err.println("Usage: RBReplicator [conf_file]");
		System.exit(1);
	}
	
	private void cleanup() {
		hTool.cleanup();
	}

	private void setProps(Properties properties) {
		this.properties = properties;
		
	}	
	
	public void showMA() {
		
		if(domMicroAgents==null){
			logger.log(Level.WARNING,"No MicroAgents were found");
			return;
		}
		
		for(String domMicroAgent : domMicroAgents){
			logger.log(Level.INFO,"Found MicroAgent: " + domMicroAgent);
		}
		
		hTool.cleanup();
    	System.exit(0);
	}

	private static void processRulebase(HTool hTool,String domain, String [] mas, Map<String, String> templateData) {
		
		Set<Entry<String,String>> eSet = templateData.entrySet();
        Iterator<Entry<String, String>> it = eSet.iterator();
        List<String> allKeys = new ArrayList<String>();
        List<String> allVals = new ArrayList<String>();
        
        while(it.hasNext()){
        	Entry<String,String> entry = it.next();
        	allKeys.add(entry.getKey());
        	allVals.add(entry.getValue());
        }
        
        String [] rb = allVals.toArray(new String[allKeys.size()]);        
        String [] nm = allKeys.toArray(new String[allVals.size()]);
        
        logger.log(Level.FINE,hTool.processRulebaseTemplates(domain, mas,rb,nm)+
        		" rulebases were sent to Hawk agents in the domain");
		
	}
	
	public void processRulebases() {
		
		if(domMicroAgents == null){
			logger.log(Level.WARNING,"No appropriate MicroAgents were found in the domain");
			return;
		}
		
		for(String ma : reqMicroAgents){
			logger.log(Level.INFO,"Processing Rulebases for MicroAgent: " + ma);
			
			Map<String,String> templateData = maRulebases.get(ma);
			if(null != templateData && templateData.size()>0){
				processRulebase(hTool,domain, HToolUtil.filter(domMicroAgents, ma),templateData);
			}
		}
	}

	public static void main(String [] a){
		
		if(a.length == 0) showUsage();
		
		try{
			
			RBReplicator rep = new RBReplicator(a[0]);
			rep.init();
			
			if(HToolUtil.filter(a, "showdet").length > 0) rep.showDetail();
			if(HToolUtil.filter(a, "showma").length > 0) rep.showMA();
				
			rep.processRulebases();
			rep.cleanup();
			
		} catch(Exception e){
			e.printStackTrace();
		}
		
	}

	private void showDetail() {
		hTool.showDomainDetails();		
	}
}
