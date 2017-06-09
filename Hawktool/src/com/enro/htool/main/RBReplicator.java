package com.enro.htool.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import com.enro.bwutils.BWUtils;
import com.enro.htool.common.HToolConstants;

import COM.TIBCO.hawk.utilities.misc.HawkConstants;

public class RBReplicator {
	
	private final static Logger logger = Logger.getLogger(RBReplicator.class.getName());
	
	private static Properties getDefaultProperties() {
		
		Properties props = new Properties();
		String agentNm;
		
		logger.warning("Using default properties!");
		
		try {
			java.net.InetAddress hostInetInfo = java.net.InetAddress.getLocalHost();
			agentNm = hostInetInfo.getHostName();
			props.put("agent_name", agentNm);
			
			props.put("hawk_domain","bw-demo-domain");	// This will ONLY work if your domain is actually called "bw-demo-domain" :-P
			props.put("hawk_transport","tibrv");
			props.put("rv_service","7474");
			props.put("rv_network",";");
			props.put("rv_daemon","tcp:7474");
			
		} catch (java.net.UnknownHostException uhe) {
			logger.log(Level.SEVERE,"Usage: java RBReplicator <propFile> <RuleBase Templates Dir>");
			logger.log(Level.SEVERE,"Unable to use default properties. Make sure to provide a valid Hawk properties file");
			logger.log(Level.SEVERE,uhe.getLocalizedMessage());
			System.exit(1);
		}
		
		return props;
	}
	
	private static Properties getProperties(String[] a) {
		
		Properties props;
		
		try {
			props = new Properties();
			InputStream is = new FileInputStream(a[0]);
			props.load(is);
			is.close();

		} catch (Exception e) {
			props = getDefaultProperties();
		}
		
		return props;
	}

	private static Map<String, String> getTemplates(String[] a) {
		
		Map<String,String> templates = new HashMap<String,String>();
		BufferedReader br = null;
		
		try{
			
			File folder = new File(a[1]);
			File[] files = folder.listFiles(new FilenameFilter() {
			    public boolean accept(File dir, String name) {
			        return name.toLowerCase().endsWith(".hrb");
			    }
			});
			
			logger.log(Level.FINE,files.length + " rulebases were found");
			
			for(File file : files){
				
				FileReader fr = new FileReader(file);
				StringBuilder sb = new StringBuilder();
				
				XMLStreamReader xmlSR = XMLInputFactory.newInstance().createXMLStreamReader(fr);
				String encoding = xmlSR.getCharacterEncodingScheme();
				logger.log(Level.CONFIG,"Rulebase "+ file.getName() +" reported encoding as: " + encoding);
				
				br = new BufferedReader(new InputStreamReader(new FileInputStream(file),encoding));
				
				String sCurrentLine = br.readLine();
				while (sCurrentLine != null) {
		            sb.append(sCurrentLine);
		            sCurrentLine = br.readLine();
		        }
				
				br.close();
				templates.put(BWUtils.strReplace(file.getName(),"\\.[hH][rR][bB]$",""),sb.toString());
			}
			
		} catch(ArrayIndexOutOfBoundsException ee){
			logger.log(Level.SEVERE,"Invalid templates directory!!");
			logger.log(Level.SEVERE,"Usage: java RBReplicator <propFile> <RuleBase Templates Dir>");
			System.exit(1);
		}
		catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
		
		return templates;
	}
	
	public static void main(String [] a){
		
		Properties props = getProperties(a);
		Map<String,String> templates = getTemplates(a);
		
		HTool hTool = new HTool(props);
		
		String microagents[] = {HToolConstants.BWMANM};
        String [] ma = hTool.getMAgents(microagents);
        
        Set<Entry<String,String>> eSet = templates.entrySet();
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
        
        logger.log(Level.FINE,hTool.processRulebaseTemplates(props.getProperty("hawk_domain","bw-demo-domain"), ma,rb,nm)+
        		" rulebases were sent to Hawk agents in the domain");
        
        hTool.cleanup();
		
	}
}
