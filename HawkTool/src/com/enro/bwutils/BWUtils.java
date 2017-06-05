package com.enro.bwutils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
 
/*
* Only methods declared as public and static are loaded.
* The input parameters and return values must be of one of the types
* String,char,Boolean,boolean,int,float,double,short,long,
* Integer,Float,Double,Short,Long.
* The return value of the function cannot be void.
* The method cannot be a constructor.
* The method cannot explicitly throw an exception. Runtime exceptions are
allowed, however.
* Method names cannot be overloaded in a class or any imported classes in a
single Java Custom Function resource. You can load methods of the same
name into separate classes in separate Java Custom Function resources and
use the Prefix field to differentiate between the methods.
* If you make references to any imported class files, these classes must be
available in the classpath configured for TIBCO ActiveMatrix BusinessWorks.
The easiest way to make the imported classes available is to place them in the
TIBCO/bw/2.0/lib directory.
* Inner classes are not supported.
*/
 
public class BWUtils {
               
                /**
                * The following is a two-dimensional array that provides the
                * online help for functions in this class. Declare an array
                * named HELP_STRINGS.
                */
                public static final String[][] HELP_STRINGS ={
                                {
                                                "bwutils",
                                                "Prints base BWUtils class name",
                                                "bwutils()",
                                                "String &lt;&lt; BWUtils &gt;&gt;"
                                },
                                {
                                                "strReplace",
                                                "Replaces all occurrences of &lt;&lt; matchStr &gt;&gt; with &lt;&lt; replaceStr &gt;&gt; in &lt;&lt; sourceStr &gt;&gt; (case-sensitive).",
                                                "strReplace(\"Homeowner\",\"meow\",\"t dog eating contest win\")",
                                                "Hot dog eating contest winner"
                                },
                                {
                                                "envDetail",
                                                "Return an XML representation of the environment details, as reported by the JVM.",
                                                "envDetail()",
                                                "XML String"
                                },
                                {
                                                "addXMLSchema",
                                                "Adds an xsd schema qualifier to an XML String.",
                                                "addXMLSchema(\"XML String\",\"schema\",\"pfx\")",
                                                "XML String with added schema qualifier"
                                }
                };
               
                public static String bwutils(){
                                return BWUtils.class.getSimpleName();
                }
               
                public static String addXMLSchema(String src, String schema, String pfx){
                               
                               
                                // Input already has a schema. Return unchanged.
                                if (Pattern.compile("<\\w+:").matcher(src).find()) return src;
                               
                                Pattern tagP = Pattern.compile("<[^\\?!]..*?>|<!\\[[cC][dD][aA][tT][aA]\\[..*?]]>");
                                Pattern dtaP = Pattern.compile("..*[cC][dD][aA][tT][aA]..*");
                                Matcher tagM = tagP.matcher(src);
                               
                                String [] strs = tagP.split(src);
                                StringBuilder sb = new StringBuilder();
                                boolean pfxAdded = false;
                               
                                for(int i=0;tagM.find();i++){
                                                String matchedTag = tagM.group();
                                                sb.append(((i < strs.length) ? strs[i] : ""));
                                               
                                                if(!dtaP.matcher(matchedTag).matches()){
                                                               
                                                                String s1 = strReplace(matchedTag,"<","<"+pfx+":");
                                                                if(!(matchedTag.contains("/") || pfxAdded)){
                                                                                s1 = strReplace(s1,">"," xmlns:"+pfx+"=\""+schema+"\">");
                                                                                pfxAdded = true;
                                                                }
                                                                sb.append(s1);
                                                }
                                                else{
                                                                sb.append(matchedTag);
                                                }
                                }
                               
                                return strReplace(sb.toString(),pfx+":/","/"+pfx+":");
                }
               
                public static String strReplace(String src,String from,String to){
                                return Pattern.compile(from).matcher(src).replaceAll(to);
                }
               
                public static String envDetail(){
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                String ret;
                                try {
                                                System.getProperties().storeToXML(baos, "");
                                                ret = baos.toString();
                                } catch (IOException e) {
                                                ret = e.getMessage();
                                } finally{
                                                try {
                                                                baos.flush();
                                                                baos.close();
                                                } catch (IOException e) {
                                                                ret = e.getMessage();
                                                }
                                }
                                return ret;
                }
               
                public static String tokenizeRegex(String src, String exp){
                               
                                String strings [] = Pattern.compile(exp).split(src);
                                StringBuilder sb = new StringBuilder();
                                String sep = "\u2660";
                               
                                for(String s : strings){
                                                sb.append(s);
                                                sb.append(sep);
                                }
                               
                                return sb.toString();
                }
               
                public static void main(String [] a){}
}