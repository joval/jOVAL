// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.engine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import oval.schemas.common.GeneratorType;
import oval.schemas.common.MessageType;
import oval.schemas.systemcharacteristics.core.CollectedObjectsType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.ObjectFactory;
import oval.schemas.systemcharacteristics.core.ObjectType;
import oval.schemas.systemcharacteristics.core.OvalSystemCharacteristics;
import oval.schemas.systemcharacteristics.core.ReferenceType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.core.SystemDataType;
import oval.schemas.systemcharacteristics.core.SystemInfoType;
import oval.schemas.systemcharacteristics.core.VariableValueType;

import com.sun.xml.internal.txw2.output.IndentingXMLStreamWriter;

import org.joval.intf.plugin.IPlugin;
import org.joval.intf.oval.ISystemCharacteristics;
import org.joval.oval.OvalException;
import org.joval.util.JOVALSystem;
import org.joval.xml.EmptyNamespaceContext;

/**
 * The purpose of this class is to mirror the apparent relational storage structure used by Ovaldi to generate the system-
 * characteristics file.  That file appears to maintain a table of objects and a separate table of item containing data about
 * those objects.  This class also maintains separate structures for the purpose of serializing them to the proper format,
 * but it also provides direct access to the item data given the object ID, so that it is computationally useful as well.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SystemCharacteristics implements ISystemCharacteristics {
    /**
     * Load a SystemCharacteristics from a File.
     */
    public SystemCharacteristics(File f) throws OvalException {
	this(getOvalSystemCharacteristics(f));
    }

    // Internal

    static final OvalSystemCharacteristics getOvalSystemCharacteristics(File f) throws OvalException {
	try {
	    JAXBContext ctx = JAXBContext.newInstance(JOVALSystem.getOvalProperty(JOVALSystem.OVAL_PROP_SYSTEMCHARACTERISTICS));
	    Unmarshaller unmarshaller = ctx.createUnmarshaller();
	    Object rootObj = unmarshaller.unmarshal(f);
	    if (rootObj instanceof OvalSystemCharacteristics) {
		return (OvalSystemCharacteristics)rootObj;
	    } else {
		throw new OvalException(JOVALSystem.getMessage("ERROR_SC_BAD_FILE", f));
	    }
	} catch (JAXBException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_SC_PARSE"), e);
	    throw new OvalException(e);
	}
    }

    private OvalSystemCharacteristics osc;
    private Hashtable<String, ObjectType> objectTable;
    private Hashtable<BigInteger, JAXBElement<? extends ItemType>> itemTable;
    private Hashtable<String, VariableValueType> variableTable;
    private IPlugin plugin;
    private ObjectFactory coreFactory;

    /**
     * Create an empty SystemCharacteristics for scanning.
     */
    SystemCharacteristics(IPlugin plugin) {
	objectTable = new Hashtable<String, ObjectType>();
	itemTable = new Hashtable<BigInteger, JAXBElement<? extends ItemType>>();
	variableTable = new Hashtable<String, VariableValueType>();
	coreFactory = new ObjectFactory();
	this.plugin = plugin;
    }

    /**
     * Create a SystemCharacteristics from an OvalSystemCharacteristics (i.e., from a parsed File).
     */
    SystemCharacteristics(OvalSystemCharacteristics osc) {
	this.osc = osc;
	coreFactory = new ObjectFactory();

	itemTable = new Hashtable<BigInteger, JAXBElement<? extends ItemType>>();
	for (JAXBElement<? extends ItemType> item : osc.getSystemData().getItem()) {
	    storeItem(item);
	}

	variableTable = new Hashtable<String, VariableValueType>();
	objectTable = new Hashtable<String, ObjectType>();
	for (ObjectType objectType : osc.getCollectedObjects().getObject()) {
	    String id = objectType.getId();
	    List<MessageType> messages = objectType.getMessage();
	    if (messages.size() == 0) {
		setObject(id, objectType.getComment(), objectType.getVersion(), objectType.getFlag(), null);
	    } else {
		String comment = objectType.getComment();
		BigInteger version = objectType.getVersion();
		FlagEnumeration flag = objectType.getFlag();
		for (MessageType message : messages) {
		    setObject(id, comment, version, flag, message);
		}
	    }

	    for (ReferenceType referenceType : objectType.getReference()) {
		relateItem(id, referenceType.getItemRef());
	    }

	    for (VariableValueType variableValueType : objectType.getVariableValue()) {
		storeVariable(variableValueType);
		relateVariable(id, variableValueType.getVariableId());
	    }
	}
    }

    /**
     * Test whether an ObjectType with the specified ID is present.
     */
    boolean containsObject(String objectId) {
	return objectTable.containsKey(objectId);
    }

    /**
     * Return a filtered OvalSystemCharacteristics, containing only objects and items pertaining to the specified variables
     * and objects.
     */
    OvalSystemCharacteristics getOvalSystemCharacteristics(List<String> variables, List<BigInteger> itemIds) {
	OvalSystemCharacteristics filteredSc = coreFactory.createOvalSystemCharacteristics();
	if (osc == null) {
	    osc = getOvalSystemCharacteristics();
	}
	filteredSc.setGenerator(osc.getGenerator());
	filteredSc.setSystemInfo(osc.getSystemInfo());

	//
	// Add only objects whose items and variables are all specified.
	//
	CollectedObjectsType collectedObjectsType = coreFactory.createCollectedObjectsType();
	List <ObjectType>objects = collectedObjectsType.getObject();
	for (ObjectType objectType : objectTable.values()) {
	    boolean add = true;
	    for (ReferenceType referenceType : objectType.getReference()) {
		if (!itemIds.contains(referenceType.getItemRef())) {
		    add = false;
		    break;
		}
	    }
	    if (add) {
		for (VariableValueType variableValueType : objectType.getVariableValue()) {
		    if (!variables.contains(variableValueType.getVariableId())) {
			add = false;
			break;
		    }
		}
		if (add) {
		    objects.add(objectTable.get(objectType));
		}
	    }
	}
	filteredSc.setCollectedObjects(collectedObjectsType);

	//
	// Add only items in the list.
	//
	SystemDataType systemDataType = coreFactory.createSystemDataType();
	for (BigInteger itemId : itemIds) {
	    systemDataType.getItem().add(itemTable.get(itemId));
	}
	filteredSc.setSystemData(systemDataType);
	return filteredSc;
    }

    // Implement ISystemCharacteristics

    public OvalSystemCharacteristics getOvalSystemCharacteristics() {
	if (osc == null) {
	    osc = createOvalSystemCharacteristics();
	}
	return osc;
    }

    /**
     * Store the ItemType in the itemTable and return the ID used to store it.  The plugin should retain this ID in case the
     * item is shared by multiple objects.
     */
    public synchronized BigInteger storeItem(JAXBElement<? extends ItemType> wrappedItem) {
	ItemType item = wrappedItem.getValue();
	BigInteger itemId = null;
	if (item.isSetId()) {
	    itemId = item.getId();
	    if (!itemTable.containsKey(itemId)) {
		itemTable.put(itemId, wrappedItem);
	    }
	} else {
	    itemId = new BigInteger(new Integer(itemTable.size()).toString());
	    item.setId(itemId);
	    itemTable.put(itemId, wrappedItem);
	}
	return itemId;
    }

    /**
     * Add some information about an object to the store, without relating it to a variable or an item.
     */
    public void setObject(String objectId, String comment, BigInteger version, FlagEnumeration flag, MessageType message) {
	ObjectType objectType = objectTable.get(objectId);
	if (objectType == null) {
	    objectType = coreFactory.createObjectType();
	    objectType.setId(objectId);
	    objectTable.put(objectId, objectType);
	}
	if (comment != null) {
	    objectType.setComment(comment);
	}
	if (version != null) {
	    objectType.setVersion(version);
	}
	objectType.setFlag(flag);
	if (message != null) {
	    objectType.getMessage().add(message);
	}
    }

    /**
     * Fetch an existing ObjectType or create a new ObjectType and store it in the objectTable, and create a relation between
     * the ObjectType and ItemType.
     */
    public void relateItem(String objectId, BigInteger itemId) throws NoSuchElementException {
	ItemType item = itemTable.get(itemId).getValue();
	if (item == null) {
	    throw new NoSuchElementException(JOVALSystem.getMessage("ERROR_REF_ITEM", itemId.toString()));
	}
	ObjectType objectType = objectTable.get(objectId);
	if (objectType == null) {
	    throw new NoSuchElementException(JOVALSystem.getMessage("ERROR_REF_OBJECT", objectId));
	}
	ReferenceType referenceType = coreFactory.createReferenceType();
	referenceType.setItemRef(itemId);
	objectType.getReference().add(referenceType);
    }

    public void storeVariable(VariableValueType variableValueType) {
	if (!variableTable.containsKey(variableValueType.getVariableId())) {
	    variableTable.put(variableValueType.getVariableId(), variableValueType);
	}
    }

    /**
     * Add a variable reference to an ObjectType; the object is stored if it does not already exist.
     */
    public void relateVariable(String objectId, String variableId) throws NoSuchElementException {
	VariableValueType variable = variableTable.get(variableId);
	if (variable == null) {
	    throw new NoSuchElementException(JOVALSystem.getMessage("ERROR_REF_VARIABLE", variableId));
	}
	ObjectType objectType = objectTable.get(objectId);
	if (objectType == null) {
	    throw new NoSuchElementException(JOVALSystem.getMessage("ERROR_REF_OBJECT", objectId));
	}
	objectType.getVariableValue().add(variable);
    }

    public String getVariableValue(String variableId) {
	return (String)variableTable.get(variableId).getValue();
    }

    public List<VariableValueType> getVariablesByObjectId(String id) throws NoSuchElementException {
	ObjectType objectType = objectTable.get(id);
	if (objectType == null) {
	    throw new NoSuchElementException(JOVALSystem.getMessage("ERROR_REF_OBJECT", id));
	}
	return objectType.getVariableValue();
    }

    /**
     * Get an object.
     */
    public ObjectType getObject(String id) throws NoSuchElementException {
	ObjectType objectType = objectTable.get(id);
	if (objectType == null) {
	    throw new NoSuchElementException(JOVALSystem.getMessage("ERROR_REF_OBJECT", id));
	}
	return objectType;
    }

    /**
     * Fetch all the ItemTypes associated with the ObjectType with the given ID.
     */
    public List<ItemType> getItemsByObjectId(String id) throws NoSuchElementException {
	ObjectType objectType = objectTable.get(id);
	if (objectType == null) {
	    throw new NoSuchElementException(JOVALSystem.getMessage("ERROR_REF_OBJECT", id));
	}
	List <ItemType>items = new Vector<ItemType>();
	if (objectType.isSetReference()) {
	    for (ReferenceType referenceType : objectType.getReference()) {
		items.add(itemTable.get(referenceType.getItemRef()).getValue());
	    }
	}
	return items;
    }

    /**
     * Serialize.
     */
    public void write(File f, boolean noNamespaces) {
	OutputStream out = null;
	try {
	    JAXBContext ctx = JAXBContext.newInstance(JOVALSystem.getOvalProperty(JOVALSystem.OVAL_PROP_SYSTEMCHARACTERISTICS));
	    Marshaller marshaller = ctx.createMarshaller();
	    out = new FileOutputStream(f);

	    if (noNamespaces) {
		XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(out);
		IndentingXMLStreamWriter indentingWriter = new IndentingXMLStreamWriter(writer);
		indentingWriter.setIndentStep("  ");
		indentingWriter.setNamespaceContext(new EmptyNamespaceContext());
		marshaller.marshal(getOvalSystemCharacteristics(), indentingWriter);
	    } else {
		marshaller.setProperty("jaxb.formatted.output", new Boolean(true));
		marshaller.marshal(getOvalSystemCharacteristics(), out);
	    }
	} catch (JAXBException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_FILE_GENERATE", f.toString()), e);
	} catch (FactoryConfigurationError e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_FILE_GENERATE", f.toString()), e);
	} catch (XMLStreamException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_FILE_GENERATE", f.toString()), e);
	} catch (FileNotFoundException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_FILE_GENERATE", f.toString()), e);
	} finally {
	    if (out != null) {
		try {
		    out.close();
		} catch (IOException e) {
		    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_FILE_CLOSE", f.toString()), e);
		}
	    }
	}
    }

    // Private

    private OvalSystemCharacteristics createOvalSystemCharacteristics() {
	OvalSystemCharacteristics sc = coreFactory.createOvalSystemCharacteristics();
	sc.setGenerator(Engine.getGenerator());
	sc.setSystemInfo(plugin.getSystemInfo());

	CollectedObjectsType collectedObjectsType = coreFactory.createCollectedObjectsType();
	List <ObjectType>objects = collectedObjectsType.getObject();
	for (ObjectType objectType : objectTable.values()) {
	    objects.add(objectType);
	}
	sc.setCollectedObjects(collectedObjectsType);

	SystemDataType systemDataType = coreFactory.createSystemDataType();
	List <JAXBElement<? extends ItemType>>items = systemDataType.getItem();
	for (JAXBElement<? extends ItemType> itemType : itemTable.values()) {
	    items.add(itemType);
	}
	sc.setSystemData(systemDataType);

	return sc;
    }
}
