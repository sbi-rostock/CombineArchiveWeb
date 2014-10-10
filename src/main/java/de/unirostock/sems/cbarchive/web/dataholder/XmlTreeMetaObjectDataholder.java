package de.unirostock.sems.cbarchive.web.dataholder;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.transform.TransformerException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.Utils;
import de.unirostock.sems.cbarchive.meta.DefaultMetaDataObject;
import de.unirostock.sems.cbarchive.meta.MetaDataHolder;
import de.unirostock.sems.cbarchive.meta.MetaDataObject;
import de.unirostock.sems.cbarchive.web.exception.CombineArchiveWebException;

public class XmlTreeMetaObjectDataholder extends MetaObjectDataholder {

	private Element xmlTree = null;
	/** In case the String cannot parsed to a XmlElement */
	private String exceptionMessage = null; 
	
	public XmlTreeMetaObjectDataholder(MetaDataObject metaObject, ArchiveEntryDataholder parent) {
		super(metaObject, parent);
		type = MetaObjectDataholder.TYPE_XML;
		xmlTree = metaObject.getXmlDescription();
	}
	
	public XmlTreeMetaObjectDataholder(String id, String type, boolean changed) {
		super(id, type, changed);
	}
	
	public XmlTreeMetaObjectDataholder() {
		super(null, null, false);
	}
	
	@JsonIgnore
	public Element getXmlTree() {
		return xmlTree;
	}
	
	@JsonIgnore
	public void setXmlTree(Element xmlTree) {
		this.xmlTree = xmlTree;
	}
	
	public String getXmlString() {
		try {
			Document doc = new Document().setRootElement(xmlTree.clone());
			return Utils.prettyPrintDocument( doc );
		} catch (IOException | TransformerException e) {
			LOGGER.error(e, "Cannot transform Xml Element to String.");
			return "";
		}
	}
	
	public void setXmlString(String xmlString) {
		
		try {
			SAXBuilder builder = new SAXBuilder ();
			Document doc = (Document) builder.build( new StringReader(xmlString) );
			xmlTree = doc.getRootElement().clone();
			exceptionMessage = null;
		} catch (JDOMException | IOException e) {
			LOGGER.error(e, "Cannot transform String to Xml Element");
			xmlTree = null;
			exceptionMessage = e.getMessage();
		}
	}
	
	@JsonIgnore
	public String getExceptionMessage() {
		return exceptionMessage;
	}

	@JsonIgnore
	@Override
	public void update(MetaObjectDataholder newMetaObject) throws CombineArchiveWebException {
		
		if( newMetaObject instanceof XmlTreeMetaObjectDataholder == false ) {
			// not the correct class type, check if data type is valid at least
			if( newMetaObject.type.equals(MetaObjectDataholder.TYPE_XML) == false )
				// data type is also not valid -> exception
				throw new IllegalArgumentException("Wrong data type");
		}
		
		// apply changes
		Element newXmltree = ((XmlTreeMetaObjectDataholder) newMetaObject).getXmlTree();
		if( newXmltree != null ) {
			MetaDataHolder parentMeta = parent.getArchiveEntry();
			
			// remove the old meta object
			parentMeta.removeDescription(metaObject);
			
			// generates the new one
			xmlTree = newXmltree;
			try {
				LOGGER.warn( Utils.prettyPrintDocument(new Document(xmlTree.clone())));
			} catch (IOException | TransformerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			metaObject = getCombineArchiveMetaObject();
			// add it
			parentMeta.addDescription(metaObject);
		}
		else {
			String message = ((XmlTreeMetaObjectDataholder) newMetaObject).exceptionMessage;
			if( message == null || message.isEmpty() )
				message = "Could not parse String to an Xml Element";
			
			throw new CombineArchiveWebException(message);
		}
		
	}

	@Override
	public MetaDataObject getCombineArchiveMetaObject() {
		this.metaObject = new DefaultMetaDataObject(xmlTree);
		// update the id
		generateId();
		return metaObject;
	}

}
