package com.enro.htool.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
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
import com.enro.htool.main.RBTemplate;

public class HToolUtil {
	
	private static final Logger logger = Logger.getLogger(HToolUtil.class.getName());
	
	public static String[] filter(String [] s, String it){
		Set<String> set = new HashSet<String>();
		
		for(String ss : s){
			if(ss.contains(it)) set.add(ss);
		}
		
		return set.toArray(new String[set.size()]);
	}
	
	public static Properties getPropas(String params[]){
        Properties p = new Properties();
        
        for(int i = 0; i < params.length; i++){
            String entry = params[i];
            String keyval[] = entry.split("=");
            p.setProperty(keyval[0], keyval[1]);
        }

        return p;
    }
	
	public static Properties getProperties(String conf) throws Exception {
		
		ConfReader cr = new ConfReader(conf);
		Properties props = cr.getSection("General");
		String transport = cr.getString("General", "hawk_transport", "none");
		
		if(transport.contentEquals("none")){
			System.err.println("Invalid configuration specified");
			System.exit(1);
		} else {
			props.putAll(cr.getSection(transport));
		}
		
		props.putAll(cr.getSection("Template"));
		props.putAll(cr.getSection("SSL"));
		
		return props;
	}
	
	public static Map<String,Properties> getMADetail(String conf) throws Exception{
		ConfReader cr = new ConfReader(conf);
		Map<String,Properties> maDetail = new HashMap<String,Properties>();
		String [] mags = cr.getMASections();
		
		for(String ma : mags){
			maDetail.put(ma, cr.getSection(ma));
		}
		
		return maDetail;
	}
	
	private static Map<String, String> getTemplates(String path, String prefix) {
		
		Map<String,String> templates = new HashMap<String,String>();
		BufferedReader br = null;
		final String pfx = prefix.toLowerCase()+".";
		
		try{
			
			File folder = new File(path);
			File[] files = folder.listFiles(new FilenameFilter() {
			    public boolean accept(File dir, String name) {
			        return name.toLowerCase().endsWith(".hrb") &&
			        		name.toLowerCase().startsWith(pfx);
			    }
			});
			
			if(null!=files && files.length>0){
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
					templates.put(BWUtils.strReplace(BWUtils.strReplace(file.getName(),"\\.[hH][rR][bB]$",""), pfx, ""),sb.toString());
				}
			}
		} catch(ArrayIndexOutOfBoundsException ee){
			logger.log(Level.SEVERE,"Unable to read from templates directory!!");
			System.exit(1);
		}
		catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
		
		return templates;
	}
	
	public static Map<String, Map<String, String>> getMARulebases(String path, Map<String, Properties> maDetail) {
		
		Map<String, Map<String, String>> maRulebases = new HashMap<String,Map<String,String>>();
		
		Set<Entry<String,Properties>> set = maDetail.entrySet();
		Iterator<Entry<String,Properties>> it = set.iterator();
		
		while(it.hasNext()){
			Entry<String,Properties> entry = it.next();
			String ma = entry.getKey();
			ma = ma.substring(ma.indexOf("ma:")+"ma:".length());
			String pfx = entry.getValue().getProperty("prefix");
			
			Map<String,String> maTemplates = getTemplates(path,pfx);
			maRulebases.put(ma, maTemplates);
		}
		
		return maRulebases;
	}
	
	public static String[] getMAServices(Map<String, Properties> maDetail) {
		
		Set<Entry<String,Properties>> set = maDetail.entrySet();
		Iterator<Entry<String,Properties>> it = set.iterator();
		String [] rslt = null;
		
		while(it.hasNext()){
			Entry<String,Properties> entry = it.next();
			String ma = entry.getKey();
			ma = ma.substring(ma.indexOf("ma:")+"ma:".length());
			String svc = entry.getValue().getProperty("service_list");
			
			if(null != svc && svc.length() > 0){
				rslt = svc.split(",");
				break;
			}
		}
		
		return rslt;
	}
	
	public static Hashtable<String,String> processSSLProperties(Properties props){
		
		Hashtable<String,String> sslProps = new Hashtable<String,String>();
		
		String ssl_vendor = props.getProperty("ssl_vendor");
		String ssl_trace = props.getProperty("ssl_trace");
		String ssl_trusted = props.getProperty("ssl_trusted");
		String ssl_expected_hostname = props.getProperty("ssl_expected_hostname");
		String ssl_ciphers = props.getProperty("ssl_ciphers");
		String ssl_verify_host_name = props.getProperty("ssl_no_verify_host_name");
		String ssl_verify_host = props.getProperty("ssl_no_verify_host");
		String ssl_identity = props.getProperty("ssl_identity");
		String ssl_password = props.getProperty("ssl_identity");
		String ssl_private_key = props.getProperty("ssl_identity");
		
		if (ssl_vendor != null){
			logger.log(Level.INFO, "ssl_vendor: " + ssl_vendor);
			sslProps.put(com.tibco.tibjms.TibjmsSSL.VENDOR, ssl_vendor);
		}
		
		if (ssl_trace != null){
			logger.log(Level.INFO, "ssl_trace: " + ssl_trace);
			sslProps.put(com.tibco.tibjms.TibjmsSSL.TRACE, ssl_trace);
		}
		
		if (ssl_trusted != null){
			logger.log(Level.INFO, "ssl_trusted: " + ssl_trusted);
			sslProps.put(com.tibco.tibjms.TibjmsSSL.TRUSTED_CERTIFICATES,ssl_trusted);
		}
		
		if (ssl_expected_hostname != null){
			logger.log(Level.INFO, "ssl_expected_hostname: " + ssl_expected_hostname);
			sslProps.put(com.tibco.tibjms.TibjmsSSL.EXPECTED_HOST_NAME,ssl_expected_hostname);
		}
		
		if (ssl_ciphers != null){
			logger.log(Level.INFO, "ssl_ciphers: " + ssl_ciphers);
			sslProps.put(com.tibco.tibjms.TibjmsSSL.CIPHER_SUITES,ssl_ciphers);
		}
		
		if (ssl_verify_host_name != null){
			logger.log(Level.INFO, "ssl_verify_host_name: " + ssl_verify_host_name);
			sslProps.put(com.tibco.tibjms.TibjmsSSL.ENABLE_VERIFY_HOST_NAME,ssl_verify_host_name);
		}
		
		if (ssl_verify_host != null){
			logger.log(Level.INFO, "ssl_verify_host: " + ssl_verify_host);
			sslProps.put(com.tibco.tibjms.TibjmsSSL.ENABLE_VERIFY_HOST,ssl_verify_host);
		}

		// set client identity if specified. ssl_private_key may be null
		// if identity is PKCS12, JKS or EPF. 'j2se' only supports
		// PKCS12 and JKS. 'entrust61' also supports PEM/PKCS8 combination.
		if (ssl_identity != null) {
			
			logger.log(Level.INFO, "ssl_identity: " + ssl_identity);
			sslProps.put(com.tibco.tibjms.TibjmsSSL.IDENTITY,ssl_identity);
			
			sslProps.put(com.tibco.tibjms.TibjmsSSL.PASSWORD,ssl_password);

			if (ssl_private_key != null) {
				sslProps.put(com.tibco.tibjms.TibjmsSSL.PRIVATE_KEY,ssl_private_key);
			}
		}
		
		return sslProps;
	}

	public static List<RBTemplate> readTemplates(String templatePath, String microAgent,
			Properties microAgentProps) {
		
		List<RBTemplate> templates = new ArrayList<RBTemplate>();
		BufferedReader br = null;
		final String pfx = microAgentProps.getProperty("prefix").toLowerCase()+".";
		
		try{
			
			File folder = new File(templatePath);
			File[] files = folder.listFiles(new FilenameFilter() {
			    public boolean accept(File dir, String name) {
			        return name.toLowerCase().endsWith(".hrb") &&
			        		name.toLowerCase().startsWith(pfx);
			    }
			});
			
			if(null!=files && files.length>0){
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
					
					RBTemplate rbt = new RBTemplate(microAgent,BWUtils.strReplace(BWUtils.strReplace(file.getName(),"\\.[hH][rR][bB]$",""), pfx, ""),
							sb.toString(),
							microAgentProps);
					templates.add(rbt);
				}
			}
		} catch(ArrayIndexOutOfBoundsException ee){
			logger.log(Level.SEVERE,"Unable to read from templates directory!!");
		} catch(Exception e){
			e.printStackTrace();
		}
		
		return templates;
		
	}
	
}
