/*
 * GENERATED FILE
 * Created on Sat Jul 12 17:05:20 EDT 2025
 *
 */
package org.nrg.xdat.bean;
import org.apache.log4j.Logger;
import org.nrg.xdat.bean.base.BaseElement;

import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author XDAT
 *
 *//*
 ******************************** 
 * DO NOT MODIFY THIS FILE 
 *
 ********************************/
@SuppressWarnings({"unchecked","rawtypes"})
public class FnirsFnirspipelineassessordataBean extends XnatImageassessordataBean implements java.io.Serializable, org.nrg.xdat.model.FnirsFnirspipelineassessordataI {
	public static final Logger logger = Logger.getLogger(FnirsFnirspipelineassessordataBean.class);
	public static final String SCHEMA_ELEMENT_NAME="fnirs:fnirsPipelineAssessorData";

	public String getSchemaElementName(){
		return "fnirsPipelineAssessorData";
	}

	public String getFullSchemaElementName(){
		return "fnirs:fnirsPipelineAssessorData";
	}

	//FIELD

	private String _Pipelinerun=null;

	/**
	 * @return Returns the pipelineRun.
	 */
	public String getPipelinerun(){
		return _Pipelinerun;
	}

	/**
	 * Sets the value for pipelineRun.
	 * @param v Value to Set.
	 */
	public void setPipelinerun(String v){
		_Pipelinerun=v;
	}

	//FIELD

	private Date _Pipelinerundatetime=null;

	/**
	 * @return Returns the pipelineRunDateTime.
	 */
	public Date getPipelinerundatetime(){
		return _Pipelinerundatetime;
	}

	/**
	 * Sets the value for pipelineRunDateTime.
	 * @param v Value to Set.
	 */
	public void setPipelinerundatetime(Date v){
		_Pipelinerundatetime=v;
	}

	/**
	 * Sets the value for pipelineRunDateTime.
	 * @param v Value to Set.
	 */
	public void setPipelinerundatetime(Object v){
		throw new IllegalArgumentException();
	}

	/**
	 * Sets the value for pipelineRunDateTime.
	 * @param v Value to Set.
	 */
	public void setPipelinerundatetime(String v)  {
		_Pipelinerundatetime=formatDateTime(v);
	}

	//FIELD

	private String _Userthatran=null;

	/**
	 * @return Returns the userThatRan.
	 */
	public String getUserthatran(){
		return _Userthatran;
	}

	/**
	 * Sets the value for userThatRan.
	 * @param v Value to Set.
	 */
	public void setUserthatran(String v){
		_Userthatran=v;
	}

	//FIELD

	private String _Scanused=null;

	/**
	 * @return Returns the scanUsed.
	 */
	public String getScanused(){
		return _Scanused;
	}

	/**
	 * Sets the value for scanUsed.
	 * @param v Value to Set.
	 */
	public void setScanused(String v){
		_Scanused=v;
	}

	//FIELD

	private String _Paramsused=null;

	/**
	 * @return Returns the paramsUsed.
	 */
	public String getParamsused(){
		return _Paramsused;
	}

	/**
	 * Sets the value for paramsUsed.
	 * @param v Value to Set.
	 */
	public void setParamsused(String v){
		_Paramsused=v;
	}

	/**
	 * Sets the value for a field via the XMLPATH.
	 * @param v Value to Set.
	 */
	public void setDataField(String xmlPath,String v) throws BaseElement.UnknownFieldException{
		if (xmlPath.equals("pipelineRun")){
			setPipelinerun(v);
		}else if (xmlPath.equals("pipelineRunDateTime")){
			setPipelinerundatetime(v);
		}else if (xmlPath.equals("userThatRan")){
			setUserthatran(v);
		}else if (xmlPath.equals("scanUsed")){
			setScanused(v);
		}else if (xmlPath.equals("paramsUsed")){
			setParamsused(v);
		}
		else{
			super.setDataField(xmlPath,v);
		}
	}

	/**
	 * Sets the value for a field via the XMLPATH.
	 * @param v Value to Set.
	 */
	public void setReferenceField(String xmlPath,BaseElement v) throws BaseElement.UnknownFieldException{
			super.setReferenceField(xmlPath,v);
	}

	/**
	 * Gets the value for a field via the XMLPATH.
	 * @param v Value to Set.
	 */
	public Object getDataFieldValue(String xmlPath) throws BaseElement.UnknownFieldException{
		if (xmlPath.equals("pipelineRun")){
			return getPipelinerun();
		}else if (xmlPath.equals("pipelineRunDateTime")){
			return getPipelinerundatetime();
		}else if (xmlPath.equals("userThatRan")){
			return getUserthatran();
		}else if (xmlPath.equals("scanUsed")){
			return getScanused();
		}else if (xmlPath.equals("paramsUsed")){
			return getParamsused();
		}
		else{
			return super.getDataFieldValue(xmlPath);
		}
	}

	/**
	 * Gets the value for a field via the XMLPATH.
	 * @param v Value to Set.
	 */
	public Object getReferenceField(String xmlPath) throws BaseElement.UnknownFieldException{
			return super.getReferenceField(xmlPath);
	}

	/**
	 * Gets the value for a field via the XMLPATH.
	 * @param v Value to Set.
	 */
	public String getReferenceFieldName(String xmlPath) throws BaseElement.UnknownFieldException{
			return super.getReferenceFieldName(xmlPath);
	}

	/**
	 * Returns whether or not this is a reference field
	 */
	public String getFieldType(String xmlPath) throws BaseElement.UnknownFieldException{
		if (xmlPath.equals("pipelineRun")){
			return BaseElement.field_data;
		}else if (xmlPath.equals("pipelineRunDateTime")){
			return BaseElement.field_data;
		}else if (xmlPath.equals("userThatRan")){
			return BaseElement.field_data;
		}else if (xmlPath.equals("scanUsed")){
			return BaseElement.field_data;
		}else if (xmlPath.equals("paramsUsed")){
			return BaseElement.field_data;
		}
		else{
			return super.getFieldType(xmlPath);
		}
	}

	/**
	 * Returns arraylist of all fields
	 */
	public ArrayList getAllFields() {
		ArrayList all_fields=new ArrayList();
		all_fields.add("pipelineRun");
		all_fields.add("pipelineRunDateTime");
		all_fields.add("userThatRan");
		all_fields.add("scanUsed");
		all_fields.add("paramsUsed");
		all_fields.addAll(super.getAllFields());
		return all_fields;
	}


	public String toString(){
		java.io.StringWriter sw = new java.io.StringWriter();
		try{this.toXML(sw,true);}catch(java.io.IOException e){}
		return sw.toString();
	}


	public void toXML(java.io.Writer writer,boolean prettyPrint) throws java.io.IOException{
		writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		writer.write("\n<fnirs:fnirsPipelineAssessorData");
		TreeMap map = new TreeMap();
		map.putAll(getXMLAtts());
		map.put("xmlns:arc","http://nrg.wustl.edu/arc");
		map.put("xmlns:cat","http://nrg.wustl.edu/catalog");
		map.put("xmlns:fnirs","http://nrg.wustl.edu/fnirs");
		map.put("xmlns:icr","http://icr.ac.uk/icr");
		map.put("xmlns:pipe","http://nrg.wustl.edu/pipe");
		map.put("xmlns:prov","http://www.nbirn.net/prov");
		map.put("xmlns:scr","http://nrg.wustl.edu/scr");
		map.put("xmlns:val","http://nrg.wustl.edu/val");
		map.put("xmlns:wrk","http://nrg.wustl.edu/workflow");
		map.put("xmlns:xdat","http://nrg.wustl.edu/security");
		map.put("xmlns:xnat","http://nrg.wustl.edu/xnat");
		map.put("xmlns:xnat_a","http://nrg.wustl.edu/xnat_assessments");
		map.put("xmlns:xsi","http://www.w3.org/2001/XMLSchema-instance");
		java.util.Iterator iter =map.keySet().iterator();
		while(iter.hasNext()){
			String key = (String)iter.next();
			writer.write(" " + key + "=\"" + map.get(key) + "\"");
		}
		int header = 0;
		if (prettyPrint)header++;
		writer.write(">");
		addXMLBody(writer,header);
		if (prettyPrint)header--;
		writer.write("\n</fnirs:fnirsPipelineAssessorData>");
	}


	protected void addXMLAtts(java.io.Writer writer) throws java.io.IOException{
		TreeMap map = this.getXMLAtts();
		java.util.Iterator iter =map.keySet().iterator();
		while(iter.hasNext()){
			String key = (String)iter.next();
			writer.write(" " + key + "=\"" + map.get(key) + "\"");
		}
	}


	protected TreeMap getXMLAtts() {
		TreeMap map = super.getXMLAtts();
		return map;
	}


	protected boolean addXMLBody(java.io.Writer writer, int header) throws java.io.IOException{
		super.addXMLBody(writer,header);
		//REFERENCE FROM fnirsPipelineAssessorData -> imageAssessorData
		if (_Pipelinerun!=null){
			writer.write("\n" + createHeader(header++) + "<fnirs:pipelineRun");
			writer.write(">");
			writer.write(ValueParser(_Pipelinerun,"string"));
			writer.write("</fnirs:pipelineRun>");
			header--;
		}
		if (_Pipelinerundatetime!=null){
			writer.write("\n" + createHeader(header++) + "<fnirs:pipelineRunDateTime");
			writer.write(">");
			writer.write(ValueParser(_Pipelinerundatetime,"dateTime"));
			writer.write("</fnirs:pipelineRunDateTime>");
			header--;
		}
		if (_Userthatran!=null){
			writer.write("\n" + createHeader(header++) + "<fnirs:userThatRan");
			writer.write(">");
			writer.write(ValueParser(_Userthatran,"string"));
			writer.write("</fnirs:userThatRan>");
			header--;
		}
		if (_Scanused!=null){
			writer.write("\n" + createHeader(header++) + "<fnirs:scanUsed");
			writer.write(">");
			writer.write(ValueParser(_Scanused,"string"));
			writer.write("</fnirs:scanUsed>");
			header--;
		}
		if (_Paramsused!=null){
			writer.write("\n" + createHeader(header++) + "<fnirs:paramsUsed");
			writer.write(">");
			writer.write(ValueParser(_Paramsused,"string"));
			writer.write("</fnirs:paramsUsed>");
			header--;
		}
	return true;
	}


	protected boolean hasXMLBodyContent(){
		if (_Pipelinerun!=null) return true;
		if (_Pipelinerundatetime!=null) return true;
		if (_Userthatran!=null) return true;
		if (_Scanused!=null) return true;
		if (_Paramsused!=null) return true;
		if(super.hasXMLBodyContent())return true;
		return false;
	}
}
