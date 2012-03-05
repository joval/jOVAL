// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.NoSuchElementException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.cal10n.LocLogger;

import oval.schemas.common.GeneratorType;
import oval.schemas.common.MessageType;
import oval.schemas.systemcharacteristics.core.CollectedObjectsType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.ObjectType;
import oval.schemas.systemcharacteristics.core.OvalSystemCharacteristics;
import oval.schemas.systemcharacteristics.core.ReferenceType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.core.SystemDataType;
import oval.schemas.systemcharacteristics.core.SystemInfoType;
import oval.schemas.systemcharacteristics.core.VariableValueType;

import org.joval.intf.oval.ISystemCharacteristics;
import org.joval.intf.util.ILoggable;
import org.joval.oval.OvalException;
import org.joval.oval.xml.OvalNamespacePrefixMapper;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * The purpose of this class is to mirror the apparent relational storage structure used by Ovaldi to generate the system-
 * characteristics file.  That file appears to maintain a table of objects and a separate table of item containing data about
 * those objects.  This class also maintains separate structures for the purpose of serializing them to the proper format,
 * but it also provides direct access to the item data given the object ID, so that it is computationally useful as well.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SystemCharacteristics implements ISystemCharacteristics, ILoggable {
    public static final OvalSystemCharacteristics getOvalSystemCharacteristics(File f) throws OvalException {
	return getOvalSystemCharacteristics(new StreamSource(f));
    }

    public static final OvalSystemCharacteristics getOvalSystemCharacteristics(Source src) throws OvalException {
	try {
	    String packages = JOVALSystem.getSchemaProperty(JOVALSystem.OVAL_PROP_SYSTEMCHARACTERISTICS);
	    JAXBContext ctx = JAXBContext.newInstance(packages);
	    Unmarshaller unmarshaller = ctx.createUnmarshaller();
	    Object rootObj = unmarshaller.unmarshal(src);
	    if (rootObj instanceof OvalSystemCharacteristics) {
		return (OvalSystemCharacteristics)rootObj;
	    } else {
		throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_SC_BAD_SOURCE, src.getSystemId()));
	    }
	} catch (JAXBException e) {
	    throw new OvalException(e);
	}
    }

    private LocLogger logger = JOVALSystem.getLogger();
    private OvalSystemCharacteristics osc;
    private GeneratorType generator;
    private SystemInfoType systemInfo;
    private Hashtable<String, ObjectType> objectTable;
    private Hashtable<String, Hashtable<BigInteger, JAXBElement<? extends ItemType>>> objectItemTable;
    private Hashtable<BigInteger, JAXBElement<? extends ItemType>> itemTable;
    private Hashtable<String, BigInteger> itemChecksums;
    private Hashtable<String, List<VariableValueType>> variableTable;
    private JAXBContext ctx;
    private Marshaller csMarshaller = null;

    /**
     * Load a SystemCharacteristics from a File.
     */
    public SystemCharacteristics(File f) throws OvalException {
	this(getOvalSystemCharacteristics(f));
    }

    /**
     * Create an empty SystemCharacteristics for scanning.
     */
    public SystemCharacteristics(GeneratorType generator, SystemInfoType systemInfo) {
	this.generator = generator;
	this.systemInfo = systemInfo;
	objectTable = new Hashtable<String, ObjectType>();
	objectItemTable = new Hashtable<String, Hashtable<BigInteger, JAXBElement<? extends ItemType>>>();
	itemTable = new Hashtable<BigInteger, JAXBElement<? extends ItemType>>();
	variableTable = new Hashtable<String, List<VariableValueType>>();
	itemChecksums = new Hashtable<String, BigInteger>();
	try {
	    String packages = JOVALSystem.getSchemaProperty(JOVALSystem.OVAL_PROP_SYSTEMCHARACTERISTICS);
	    ctx = JAXBContext.newInstance(packages);
	    csMarshaller = ctx.createMarshaller();
	    OvalNamespacePrefixMapper.configure(csMarshaller, OvalNamespacePrefixMapper.URI.SC);
	} catch (JAXBException e) {
	    logger.error(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (FactoryConfigurationError e) {
	    logger.error(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    /**
     * Create a SystemCharacteristics from an OvalSystemCharacteristics (i.e., from a parsed File).
     */
    public SystemCharacteristics(OvalSystemCharacteristics osc) {
	this.osc = osc;

	itemTable = new Hashtable<BigInteger, JAXBElement<? extends ItemType>>();
	for (JAXBElement<? extends ItemType> item : osc.getSystemData().getItem()) {
	    storeItem(item);
	}

	variableTable = new Hashtable<String, List<VariableValueType>>();
	objectTable = new Hashtable<String, ObjectType>();
	objectItemTable = new Hashtable<String, Hashtable<BigInteger, JAXBElement<? extends ItemType>>>();
	for (ObjectType obj : osc.getCollectedObjects().getObject()) {
	    String id = obj.getId();
	    List<MessageType> messages = obj.getMessage();
	    if (messages.size() == 0) {
		setObject(id, obj.getComment(), obj.getVersion(), obj.getFlag(), null);
	    } else {
		String comment = obj.getComment();
		BigInteger version = obj.getVersion();
		FlagEnumeration flag = obj.getFlag();
		for (MessageType message : messages) {
		    setObject(id, comment, version, flag, message);
		}
	    }

	    for (ReferenceType ref : obj.getReference()) {
		relateItem(id, ref.getItemRef());
	    }

	    for (VariableValueType var : obj.getVariableValue()) {
		storeVariable(var);
		relateVariable(id, var.getVariableId());
	    }
	}
    }

    /**
     * Test whether an ObjectType with the specified ID is present.
     */
    public boolean containsObject(String objectId) {
	return objectTable.containsKey(objectId);
    }

    /**
     * Return a filtered OvalSystemCharacteristics, containing only objects and items pertaining to the specified variables
     * and objects.
     */
    public OvalSystemCharacteristics getOvalSystemCharacteristics(Collection<String> vars, Collection<BigInteger> itemIds) {
	OvalSystemCharacteristics filteredSc = JOVALSystem.factories.sc.core.createOvalSystemCharacteristics();
	if (osc == null) {
	    osc = getOvalSystemCharacteristics();
	}
	filteredSc.setGenerator(osc.getGenerator());
	filteredSc.setSystemInfo(osc.getSystemInfo());

	//
	// Add only objects whose items and variables are all specified.
	//
	CollectedObjectsType collectedObjects = JOVALSystem.factories.sc.core.createCollectedObjectsType();
	for (ObjectType obj : objectTable.values()) {
	    boolean add = true;
	    for (ReferenceType ref : obj.getReference()) {
		if (!itemIds.contains(ref.getItemRef())) {
		    add = false;
		    logger.trace(JOVALMsg.STATUS_SC_FILTER_ITEM, ref.getItemRef(), obj.getId());
		    break;
		}
	    }
	    if (add) {
		for (VariableValueType var : obj.getVariableValue()) {
		    if (!vars.contains(var.getVariableId())) {
			logger.trace(JOVALMsg.STATUS_SC_FILTER_VARIABLE, var.getVariableId(), obj.getId());
			add = false;
			break;
		    }
		}
		if (add) {
		    collectedObjects.getObject().add(obj);
		}
	    }
	}
	filteredSc.setCollectedObjects(collectedObjects);

	//
	// Add only items in the list.
	//
	SystemDataType systemData = JOVALSystem.factories.sc.core.createSystemDataType();
	for (BigInteger itemId : itemIds) {
	    systemData.getItem().add(itemTable.get(itemId));
	}
	filteredSc.setSystemData(systemData);
	return filteredSc;
    }

    /**
     * Store the ItemType in the itemTable and return the ID used to store it.  The plugin should retain this ID in case the
     * item is shared by multiple objects.
     */
    public synchronized BigInteger storeItem(JAXBElement<? extends ItemType> wrappedItem) {
	ItemType item = wrappedItem.getValue();
	BigInteger itemId = null;
	if (item.isSetId() && !itemTable.containsKey(item.getId())) {
	    itemTable.put(item.getId(), wrappedItem);
	} else {
	    String cs = getChecksum(wrappedItem);
	    itemId = itemChecksums.get(cs);
	    if (itemId == null) {
		itemId = new BigInteger(new Integer(itemTable.size()).toString());
		itemTable.put(itemId, wrappedItem);
		itemChecksums.put(cs, itemId);
	    }
	    item.setId(itemId);
	}
	return itemId;
    }

    /**
     * Add some information about an object to the store, without relating it to a variable or an item.
     */
    public void setObject(String objectId, String comment, BigInteger version, FlagEnumeration flag, MessageType message) {
	ObjectType obj = objectTable.get(objectId);
	boolean created = false;
	if (obj == null) {
	    obj = JOVALSystem.factories.sc.core.createObjectType();
	    obj.setId(objectId);
	    objectTable.put(objectId, obj);
	    created = true;
	}
	if (comment != null) {
	    obj.setComment(comment);
	}
	if (version != null) {
	    obj.setVersion(version);
	}
	if (flag == null) {
	    if (created) {
		obj.setFlag(FlagEnumeration.INCOMPLETE);
	    }
	} else {
	    obj.setFlag(flag);
	}
	if (message != null) {
	    obj.getMessage().add(message);
	}
    }

    /**
     * Fetch an existing ObjectType or create a new ObjectType and store it in the objectTable, and create a relation between
     * the ObjectType and ItemType (if such a relation does not already exist).
     */
    public void relateItem(String objectId, BigInteger itemId) throws NoSuchElementException {
	JAXBElement<? extends ItemType> item = itemTable.get(itemId);
	if (item == null) {
	    throw new NoSuchElementException(JOVALSystem.getMessage(JOVALMsg.ERROR_REF_ITEM, itemId.toString()));
	}
	ObjectType obj = objectTable.get(objectId);
	if (obj == null) {
	    throw new NoSuchElementException(JOVALSystem.getMessage(JOVALMsg.ERROR_REF_OBJECT, objectId));
	}

	Hashtable<BigInteger, JAXBElement<? extends ItemType>> objectItems = objectItemTable.get(objectId);
	if (objectItems == null) {
	    objectItems = new Hashtable<BigInteger, JAXBElement<? extends ItemType>>();
	    objectItemTable.put(objectId, objectItems);
	}
	if (!objectItems.containsKey(itemId)) {
	    objectItems.put(itemId, item);
	    ReferenceType ref = JOVALSystem.factories.sc.core.createReferenceType();
	    ref.setItemRef(itemId);
	    obj.getReference().add(ref);
	}
    }

    public void storeVariable(VariableValueType var) {
	List<VariableValueType> vars = variableTable.get(var.getVariableId());

	if (vars == null) {
	    vars = new Vector<VariableValueType>();
	    variableTable.put(var.getVariableId(), vars);
	}
	for (VariableValueType existingType : vars) {
	    if (existingType.isSetValue()) {
		if (((String)existingType.getValue()).equals((String)var.getValue())) {
		    return; //duplicate
		}
	    } else if (!var.isSetValue()) {
		return; // both null -- duplicate
	    }
	}
	vars.add(var);
    }

    /**
     * Add a variable reference to an ObjectType.  Both must already exist.
     */
    public void relateVariable(String objectId, String variableId) throws NoSuchElementException {
	List<VariableValueType> variables = variableTable.get(variableId);
	if (variables == null) {
	    throw new NoSuchElementException(JOVALSystem.getMessage(JOVALMsg.ERROR_REF_VARIABLE, variableId));
	}
	ObjectType obj = objectTable.get(objectId);
	if (obj == null) {
	    throw new NoSuchElementException(JOVALSystem.getMessage(JOVALMsg.ERROR_REF_OBJECT, objectId));
	}

	List<VariableValueType> objectVariableValues = obj.getVariableValue();
	List<VariableValueType> filterList = new Vector<VariableValueType>();
	for (VariableValueType existingVariable : objectVariableValues) {
	    if (variableId.equals((String)existingVariable.getVariableId())) {
		filterList.add(existingVariable);
	    }
	}
	for (VariableValueType variableValue : variables) {
	    boolean add = true;
	    for (VariableValueType existingVariable : filterList) {
		if (existingVariable.isSetValue()) {
		    if (((String)existingVariable.getValue()).equals((String)variableValue.getValue())) {
			add = false;
			break;
		    }
		} else if (!variableValue.isSetValue()) {
		    add = false;
		    break;
		}
	    }
	    if (add) {
		objectVariableValues.add(variableValue);
	    }
	}
    }

    public List<VariableValueType> getVariablesByObjectId(String id) throws NoSuchElementException {
	ObjectType obj = objectTable.get(id);
	if (obj == null) {
	    throw new NoSuchElementException(JOVALSystem.getMessage(JOVALMsg.ERROR_REF_OBJECT, id));
	}
	return obj.getVariableValue();
    }

    /**
     * Get an object.
     */
    public ObjectType getObject(String id) throws NoSuchElementException {
	ObjectType obj = objectTable.get(id);
	if (obj == null) {
	    throw new NoSuchElementException(JOVALSystem.getMessage(JOVALMsg.ERROR_REF_OBJECT, id));
	}
	return obj;
    }

    /**
     * Fetch all the ItemTypes associated with the ObjectType with the given ID.
     */
    public List<ItemType> getItemsByObjectId(String id) throws NoSuchElementException {
	ObjectType obj = objectTable.get(id);
	if (obj == null) {
	    throw new NoSuchElementException(JOVALSystem.getMessage(JOVALMsg.ERROR_REF_OBJECT, id));
	}
	List <ItemType>items = new Vector<ItemType>();
	if (obj.isSetReference()) {
	    for (ReferenceType ref : obj.getReference()) {
		items.add(itemTable.get(ref.getItemRef()).getValue());
	    }
	}
	return items;
    }

    // Implement ILoggable

    public LocLogger getLogger() {
	return logger;
    }

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    // Implement ITransformable

    public Source getSource() {
	Source src = null;
	try {
	    src = new JAXBSource(ctx, getOvalSystemCharacteristics());
	} catch (JAXBException e) {
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return src;
    }

    // Implement ISystemCharacteristics

    public OvalSystemCharacteristics getOvalSystemCharacteristics() {
	if (osc == null) {
	    osc = createOvalSystemCharacteristics();
	}
	return osc;
    }

    public void writeXML(File f) {
	OutputStream out = null;
	try {
	    Marshaller marshaller = ctx.createMarshaller();
	    OvalNamespacePrefixMapper.configure(marshaller, OvalNamespacePrefixMapper.URI.SC);
	    out = new FileOutputStream(f);
	    marshaller.marshal(getOvalSystemCharacteristics(), out);
	} catch (JAXBException e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (FactoryConfigurationError e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (FileNotFoundException e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} finally {
	    if (out != null) {
		try {
		    out.close();
		} catch (IOException e) {
		    logger.warn(JOVALMsg.ERROR_FILE_CLOSE, f.toString());
		}
	    }
	}
    }

    // Private

    private OvalSystemCharacteristics createOvalSystemCharacteristics() {
	OvalSystemCharacteristics sc = JOVALSystem.factories.sc.core.createOvalSystemCharacteristics();
	sc.setGenerator(generator);
	sc.setSystemInfo(systemInfo);

	CollectedObjectsType collectedObjects = JOVALSystem.factories.sc.core.createCollectedObjectsType();
	List <ObjectType>objects = collectedObjects.getObject();
	for (ObjectType obj : objectTable.values()) {
	    objects.add(obj);
	}
	sc.setCollectedObjects(collectedObjects);

	SystemDataType systemData = JOVALSystem.factories.sc.core.createSystemDataType();
	List <JAXBElement<? extends ItemType>>items = systemData.getItem();
	for (JAXBElement<? extends ItemType> itemType : itemTable.values()) {
	    items.add(itemType);
	}
	sc.setSystemData(systemData);

	return sc;
    }

    private String getChecksum(JAXBElement elt) {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
	    csMarshaller.marshal(elt, out);
	    byte[] buff = out.toByteArray();
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(buff, 0, buff.length);
	    byte[] cs = digest.digest();
            StringBuffer sb = new StringBuffer();
            for (int i=0; i < cs.length; i++) {
                sb.append(Integer.toHexString(0xFF & cs[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (JAXBException e) {
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
        }
	return null;
    }
}
