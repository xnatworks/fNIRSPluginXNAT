/*
 * GENERATED FILE
 * Created on Sat Jul 12 17:05:20 EDT 2025
 *
 */

/**
 * @author XDAT
 *
 */

function fnirs_fnirsPipelineAssessorData(){
this.xsiType="fnirs:fnirsPipelineAssessorData";

	this.getSchemaElementName=function(){
		return "fnirsPipelineAssessorData";
	}

	this.getFullSchemaElementName=function(){
		return "fnirs:fnirsPipelineAssessorData";
	}
this.extension=dynamicJSLoad('xnat_imageAssessorData','generated/xnat_imageAssessorData.js');

	this.Pipelinerun=null;


	function getPipelinerun() {
		return this.Pipelinerun;
	}
	this.getPipelinerun=getPipelinerun;


	function setPipelinerun(v){
		this.Pipelinerun=v;
	}
	this.setPipelinerun=setPipelinerun;

	this.Pipelinerundatetime=null;


	function getPipelinerundatetime() {
		return this.Pipelinerundatetime;
	}
	this.getPipelinerundatetime=getPipelinerundatetime;


	function setPipelinerundatetime(v){
		this.Pipelinerundatetime=v;
	}
	this.setPipelinerundatetime=setPipelinerundatetime;

	this.Userthatran=null;


	function getUserthatran() {
		return this.Userthatran;
	}
	this.getUserthatran=getUserthatran;


	function setUserthatran(v){
		this.Userthatran=v;
	}
	this.setUserthatran=setUserthatran;

	this.Scanused=null;


	function getScanused() {
		return this.Scanused;
	}
	this.getScanused=getScanused;


	function setScanused(v){
		this.Scanused=v;
	}
	this.setScanused=setScanused;

	this.Paramsused=null;


	function getParamsused() {
		return this.Paramsused;
	}
	this.getParamsused=getParamsused;


	function setParamsused(v){
		this.Paramsused=v;
	}
	this.setParamsused=setParamsused;


	this.getProperty=function(xmlPath){
			if(xmlPath.startsWith(this.getFullSchemaElementName())){
				xmlPath=xmlPath.substring(this.getFullSchemaElementName().length + 1);
			}
			if(xmlPath=="imageAssessorData"){
				return this.Imageassessordata ;
			} else 
			if(xmlPath.startsWith("imageAssessorData")){
				xmlPath=xmlPath.substring(17);
				if(xmlPath=="")return this.Imageassessordata ;
				if(xmlPath.startsWith("[")){
					if (xmlPath.indexOf("/")>-1){
						var optionString=xmlPath.substring(0,xmlPath.indexOf("/"));
						xmlPath=xmlPath.substring(xmlPath.indexOf("/")+1);
					}else{
						var optionString=xmlPath;
						xmlPath="";
					}
					
					var options = loadOptions(optionString);//omUtils.js
				}else{xmlPath=xmlPath.substring(1);}
				if(this.Imageassessordata!=undefined)return this.Imageassessordata.getProperty(xmlPath);
				else return null;
			} else 
			if(xmlPath=="pipelineRun"){
				return this.Pipelinerun ;
			} else 
			if(xmlPath=="pipelineRunDateTime"){
				return this.Pipelinerundatetime ;
			} else 
			if(xmlPath=="userThatRan"){
				return this.Userthatran ;
			} else 
			if(xmlPath=="scanUsed"){
				return this.Scanused ;
			} else 
			if(xmlPath=="paramsUsed"){
				return this.Paramsused ;
			} else 
			if(xmlPath=="meta"){
				return this.Meta ;
			} else 
			{
				return this.extension.getProperty(xmlPath);
			}
	}


	this.setProperty=function(xmlPath,value){
			if(xmlPath.startsWith(this.getFullSchemaElementName())){
				xmlPath=xmlPath.substring(this.getFullSchemaElementName().length + 1);
			}
			if(xmlPath=="imageAssessorData"){
				this.Imageassessordata=value;
			} else 
			if(xmlPath.startsWith("imageAssessorData")){
				xmlPath=xmlPath.substring(17);
				if(xmlPath=="")return this.Imageassessordata ;
				if(xmlPath.startsWith("[")){
					if (xmlPath.indexOf("/")>-1){
						var optionString=xmlPath.substring(0,xmlPath.indexOf("/"));
						xmlPath=xmlPath.substring(xmlPath.indexOf("/")+1);
					}else{
						var optionString=xmlPath;
						xmlPath="";
					}
					
					var options = loadOptions(optionString);//omUtils.js
				}else{xmlPath=xmlPath.substring(1);}
				if(this.Imageassessordata!=undefined){
					this.Imageassessordata.setProperty(xmlPath,value);
				}else{
						if(options && options.xsiType){
							this.Imageassessordata= instanciateObject(options.xsiType);//omUtils.js
						}else{
							this.Imageassessordata= instanciateObject("xnat:imageAssessorData");//omUtils.js
						}
						if(options && options.where)this.Imageassessordata.setProperty(options.where.field,options.where.value);
						this.Imageassessordata.setProperty(xmlPath,value);
				}
			} else 
			if(xmlPath=="pipelineRun"){
				this.Pipelinerun=value;
			} else 
			if(xmlPath=="pipelineRunDateTime"){
				this.Pipelinerundatetime=value;
			} else 
			if(xmlPath=="userThatRan"){
				this.Userthatran=value;
			} else 
			if(xmlPath=="scanUsed"){
				this.Scanused=value;
			} else 
			if(xmlPath=="paramsUsed"){
				this.Paramsused=value;
			} else 
			if(xmlPath=="meta"){
				this.Meta=value;
			} else 
			{
				return this.extension.setProperty(xmlPath,value);
			}
	}

	/**
	 * Sets the value for a field via the XMLPATH.
	 * @param v Value to Set.
	 */
	this.setReferenceField=function(xmlPath,v) {
			this.extension.setReferenceField(xmlPath,v);
	}

	/**
	 * Gets the value for a field via the XMLPATH.
	 * @param v Value to Set.
	 */
	this.getReferenceFieldName=function(xmlPath) {
			return this.extension.getReferenceFieldName(xmlPath);
	}

	/**
	 * Returns whether or not this is a reference field
	 */
	this.getFieldType=function(xmlPath){
		if (xmlPath=="pipelineRun"){
			return "field_data";
		}else if (xmlPath=="pipelineRunDateTime"){
			return "field_data";
		}else if (xmlPath=="userThatRan"){
			return "field_data";
		}else if (xmlPath=="scanUsed"){
			return "field_data";
		}else if (xmlPath=="paramsUsed"){
			return "field_data";
		}
		else{
			return this.extension.getFieldType(xmlPath);
		}
	}


	this.toXML=function(xmlTxt,preventComments){
		xmlTxt+="<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		xmlTxt+="\n<fnirs:fnirsPipelineAssessorData";
		xmlTxt+=this.getXMLAtts();
		xmlTxt+=" xmlns:arc=\"http://nrg.wustl.edu/arc\"";
		xmlTxt+=" xmlns:cat=\"http://nrg.wustl.edu/catalog\"";
		xmlTxt+=" xmlns:fnirs=\"http://nrg.wustl.edu/fnirs\"";
		xmlTxt+=" xmlns:icr=\"http://icr.ac.uk/icr\"";
		xmlTxt+=" xmlns:pipe=\"http://nrg.wustl.edu/pipe\"";
		xmlTxt+=" xmlns:prov=\"http://www.nbirn.net/prov\"";
		xmlTxt+=" xmlns:scr=\"http://nrg.wustl.edu/scr\"";
		xmlTxt+=" xmlns:val=\"http://nrg.wustl.edu/val\"";
		xmlTxt+=" xmlns:wrk=\"http://nrg.wustl.edu/workflow\"";
		xmlTxt+=" xmlns:xdat=\"http://nrg.wustl.edu/security\"";
		xmlTxt+=" xmlns:xnat=\"http://nrg.wustl.edu/xnat\"";
		xmlTxt+=" xmlns:xnat_a=\"http://nrg.wustl.edu/xnat_assessments\"";
		xmlTxt+=" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"";
		xmlTxt+=">";
		xmlTxt+=this.getXMLBody(preventComments)
		xmlTxt+="\n</fnirs:fnirsPipelineAssessorData>";
		return xmlTxt;
	}


	this.getXMLComments=function(preventComments){
		var str ="";
		if((preventComments==undefined || !preventComments) && this.hasXMLComments()){
		}
		return str;
	}


	this.getXMLAtts=function(){
		var attTxt = this.extension.getXMLAtts();
		return attTxt;
	}


	this.getXMLBody=function(preventComments){
		var xmlTxt=this.getXMLComments(preventComments);
		xmlTxt+=this.extension.getXMLBody(preventComments);
		if (this.Pipelinerun!=null){
			xmlTxt+="\n<fnirs:pipelineRun";
			xmlTxt+=">";
			xmlTxt+=this.Pipelinerun.replace(/>/g,"&gt;").replace(/</g,"&lt;");
			xmlTxt+="</fnirs:pipelineRun>";
		}
		if (this.Pipelinerundatetime!=null){
			xmlTxt+="\n<fnirs:pipelineRunDateTime";
			xmlTxt+=">";
			xmlTxt+=this.Pipelinerundatetime;
			xmlTxt+="</fnirs:pipelineRunDateTime>";
		}
		if (this.Userthatran!=null){
			xmlTxt+="\n<fnirs:userThatRan";
			xmlTxt+=">";
			xmlTxt+=this.Userthatran.replace(/>/g,"&gt;").replace(/</g,"&lt;");
			xmlTxt+="</fnirs:userThatRan>";
		}
		if (this.Scanused!=null){
			xmlTxt+="\n<fnirs:scanUsed";
			xmlTxt+=">";
			xmlTxt+=this.Scanused.replace(/>/g,"&gt;").replace(/</g,"&lt;");
			xmlTxt+="</fnirs:scanUsed>";
		}
		if (this.Paramsused!=null){
			xmlTxt+="\n<fnirs:paramsUsed";
			xmlTxt+=">";
			xmlTxt+=this.Paramsused.replace(/>/g,"&gt;").replace(/</g,"&lt;");
			xmlTxt+="</fnirs:paramsUsed>";
		}
		return xmlTxt;
	}


	this.hasXMLComments=function(){
	}


	this.hasXMLBodyContent=function(){
		if (this.Pipelinerun!=null) return true;
		if (this.Pipelinerundatetime!=null) return true;
		if (this.Userthatran!=null) return true;
		if (this.Scanused!=null) return true;
		if (this.Paramsused!=null) return true;
		if(this.hasXMLComments())return true;
		if(this.extension.hasXMLBodyContent())return true;
		return false;
	}
}
