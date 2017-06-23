package com.enro.htool.main;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RBTemplate {
	
	private static Logger logger = LoggerFactory.getLogger(RBTemplate.class);
	
	private String maID;
	private String tmpltID;
	private String rbXML;
	private Properties templateProps;
	
	public RBTemplate(String ma, String tmplt, String xml,Properties props){
		this.setMaID(ma);
		this.setTmpltID(tmplt);
		this.setRbXML(xml);
		this.setTemplateProps(props);
	}
	
	public String processTemplate(){
		
		logger.debug("Processing base template");
		return "";
	}

	public String getMaID() {
		return maID;
	}

	public void setMaID(String maID) {
		this.maID = maID;
	}

	public String getTmpltID() {
		return tmpltID;
	}

	public void setTmpltID(String tmpltID) {
		this.tmpltID = tmpltID;
	}

	public String getRbXML() {
		return rbXML;
	}

	public void setRbXML(String rbXML) {
		this.rbXML = rbXML;
	}
	
	public Properties getTemplateProps() {
		return templateProps;
	}

	public void setTemplateProps(Properties templateProps) {
		this.templateProps = templateProps;
	}
	
	@Override
	public String toString(){
		//int idx = rbXML.indexOf("?>");
		return tmpltID + "[" + maID + "]";//+: ..." + rbXML.substring(idx+2, idx+150) + "...";
	}
}
