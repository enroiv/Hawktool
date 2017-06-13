package com.enro.htool.common;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfReader {

   private Pattern  _section  = Pattern.compile( "^\\s*\\[([^]]*)\\]\\s*" );
   private Pattern  _keyValue = Pattern.compile( "^\\s*([^=]*)=(.*)" );
   private Pattern  _blnk_cmt    = Pattern.compile("^\\s*#(.*)$|^$");
   
   private Map< String,Map< String,String >>  _entries  = new HashMap<>();

   public ConfReader( String path ) throws IOException {
      load( path );
   }

   public void load( String path ) throws IOException {
      
	   try( BufferedReader br = new BufferedReader( new FileReader( path ))) {
         
    	 String line;
         String section = null;
         
         while(( line = br.readLine()) != null ) {
        	 
        	 Matcher m = _blnk_cmt.matcher(line);
        	 if(m.matches()) continue;
        	 
        	 m = _section.matcher( line );
        	 
        	 if( m.matches()) {
        		 section = m.group( 1 ).trim();
        	 }
        	 else if( section != null ) {

        		 m = _keyValue.matcher( line );
        		 
        		 if( m.matches()) {
        			 String key   = m.group( 1 ).trim();
        			 String value = m.group( 2 ).trim();
        			 Map< String, String > kv = _entries.get( section );
                  
        			 if( kv == null ) {
        				 _entries.put( section, kv = new HashMap<>());   
        			 }
        			 
        			 kv.put( key, value );
        		 }
        	 }
         } 
      }
   }

   public String getString( String section, String key, String defaultvalue ) {
      Map< String, String > kv = _entries.get( section );
      if( kv == null ) {
         return defaultvalue;
      }
      return kv.get( key );
   }

   public int getInt( String section, String key, int defaultvalue ) {
      Map< String, String > kv = _entries.get( section );
      if( kv == null ) {
         return defaultvalue;
      }
      return Integer.parseInt( kv.get( key ));
   }

   public float getFloat( String section, String key, float defaultvalue ) {
      Map< String, String > kv = _entries.get( section );
      if( kv == null ) {
         return defaultvalue;
      }
      return Float.parseFloat( kv.get( key ));
   }

   public double getDouble( String section, String key, double defaultvalue ) {
      Map< String, String > kv = _entries.get( section );
      if( kv == null ) {
         return defaultvalue;
      }
      return Double.parseDouble( kv.get( key ));
   }
   
   public Properties getSection(String section){
	   Properties props = new Properties();
	   
	   Map<String,String> sectionEntries = _entries.get(section);
	   
	   if(null != sectionEntries){
		   props = new Properties();
		   props.putAll(sectionEntries);
	   }
	   
	   return props;
   }
   
   public String [] getMASections(){
	   Set<String> ss = _entries.keySet();
	   Set<String> nu = new HashSet<String>();
	   
	   for(String ma : ss){
		   if(ma.contains("ma:")){
			   nu.add(ma);
		   }
	   }
	   
	   return nu.toArray(new String[nu.size()]);
   }
   
   public static void main(String [] a){
	   try {
		ConfReader cr = new ConfReader("res/SampleProps.conf");
		System.out.println(cr.getString("General","agent_name","")+" "+
		cr.getString("General","hawk_domain","")+" "+
		cr.getString("General","hawk_transport","")+" "+
		cr.getString("tibrv","rv_service","")+" "+
		cr.getString("tibrv","rv_network","")+" "+
		cr.getString("tibrv","rv_daemon","")+" "+
		cr.getString("Template","path",""));
		
		String [] microAgs = cr.getMASections();
		for(String ma : microAgs){
			System.out.println(cr.getString(ma,"prefix",""));
		}
			
	} catch (IOException e) {
		e.printStackTrace();
	}
   }
}