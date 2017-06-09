package com.enro.htool.common;

import java.util.Hashtable;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HToolUtil {
	
	private static final Logger logger = Logger.getLogger(HToolUtil.class.getName());
	
	public static Properties getProps(String params[]){
        Properties p = new Properties();
        
        for(int i = 0; i < params.length; i++){
            String entry = params[i];
            String keyval[] = entry.split("=");
            p.setProperty(keyval[0], keyval[1]);
        }

        return p;
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
}
