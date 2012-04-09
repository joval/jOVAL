// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashSet;
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
import org.joval.xml.SchemaRegistry;

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
	    String packages = SchemaRegistry.lookup(SchemaRegistry.OVAL_SYSTEMCHARACTERISTICS);
	    JAXBContext ctx = JAXBContext.newInstance(packages);
	    Unmarshaller unmarshaller = ctx.createUnmarshaller();
	    Object rootObj = unmarshaller.unmarshal(src);
	    if (rootObj instanceof OvalSystemCharacteristics) {
		return (OvalSystemCharacteristics)rootObj;
	    } else {
		throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_SC_BAD_SOURCE, src.getSystemId()));
	    }
	} catch (JAXBException e) {
	    throw new OvalException(e);
	}
    }

    private LocLogger logger = JOVALMsg.getLogger();
    private SystemInfoType systemInfo;
    private Hashtable<String, ObjectData> objectTable;
    private Hashtable<BigInteger, ItemType> itemTable;
    private Hashtable<String, List<VariableValueType>> variableTable;
    private Hashtable<String, HashSet<BigInteger>> objectItemTable;
    private Hashtable<String, HashSet<String>> objectVariableTable;
    private Hashtable<String, BigInteger> itemChecksums;
    private JAXBContext ctx;
    private Marshaller csMarshaller = null;

    /**
     * Create an empty SystemCharacteristics.
     */
    SystemCharacteristics() {
	objectTable = new Hashtable<String, ObjectData>();
	itemTable = new Hashtable<BigInteger, ItemType>();
	variableTable = new Hashtable<String, List<VariableValueType>>();
	objectItemTable = new Hashtable<String, HashSet<BigInteger>>();
	objectVariableTable = new Hashtable<String, HashSet<String>>();
	variableTable = new Hashtable<String, List<VariableValueType>>();
    }

    /**
     * Load a SystemCharacteristics from a File.
     */
    public SystemCharacteristics(File f) throws OvalException {
	this(getOvalSystemCharacteristics(f));
    }

    /**
     * Create an empty SystemCharacteristics for scanning with the given SystemInfoType.
     */
    public SystemCharacteristics(SystemInfoType systemInfo) {
	this();
	this.systemInfo = systemInfo;
	itemChecksums = new Hashtable<String, BigInteger>();
	try {
	    String packages = SchemaRegistry.lookup(SchemaRegistry.OVAL_SYSTEMCHARACTERISTICS);
	    ctx = JAXBContext.newInstance(packages);
	    csMarshaller = ctx.createMarshaller();
	    OvalNamespacePrefixMapper.configure(csMarshaller, OvalNamespacePrefixMapper.URI.SC);
	} catch (JAXBException e) {
	    logger.error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (FactoryConfigurationError e) {
	    logger.error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    /**
     * Create a SystemCharacteristics from an OvalSystemCharacteristics (i.e., from a parsed File).
     */
    public SystemCharacteristics(OvalSystemCharacteristics osc) throws OvalException {
	this();
	systemInfo = osc.getSystemInfo();

	for (JAXBElement<? extends ItemType> item : osc.getSystemData().getItem()) {
	    storeItem(item.getValue());
	}

	for (ObjectType obj : osc.getCollectedObjects().getObject()) {
	    String id = obj.getId();
	    for (MessageType message : obj.getMessage()) {
		setObject(id, null, null, null, message);
	    }
	    setObject(id, obj.getComment(), obj.getVersion(), obj.getFlag(), null);

	    for (ReferenceType ref : obj.getReference()) {
		relateItem(id, ref.getItemRef());
	    }

	    for (VariableValueType var : obj.getVariableValue()) {
		storeVariable(var);
		relateVariable(id, var.getVariableId());
	    }
	}
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
	    src = new JAXBSource(ctx, getOvalSystemCharacteristics(false));
	} catch (JAXBException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (OvalException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return src;
    }

    // Implement ISystemCharacteristics

    public SystemInfoType getSystemInfo() {
	return systemInfo;
    }

    //
    // DAS mask support is TBD
    //
    public OvalSystemCharacteristics getOvalSystemCharacteristics(boolean mask) throws OvalException {
	OvalSystemCharacteristics sc = Factories.sc.core.createOvalSystemCharacteristics();
	sc.setGenerator(OvalFactory.getGenerator());
	sc.setSystemInfo(systemInfo);

	CollectedObjectsType objects = Factories.sc.core.createCollectedObjectsType();
	for (String objectId : objectTable.keySet()) {
	    objects.getObject().add(makeObject(objectId));
	}
	sc.setCollectedObjects(objects);

	SystemDataType items = Factories.sc.core.createSystemDataType();
	for (BigInteger itemId : itemTable.keySet()) {
	    items.getItem().add(wrapItem(itemTable.get(itemId)));
	}
	sc.setSystemData(items);

	return sc;
    }

    public synchronized BigInteger storeItem(ItemType item) throws OvalException {
	BigInteger itemId = null;
	if (item.isSetId() && !itemTable.containsKey(item.getId())) {
	    itemTable.put(item.getId(), item);
	} else {
	    String cs = getChecksum(item);
	    itemId = itemChecksums.get(cs);
	    if (itemId == null) {
		itemId = new BigInteger(Integer.toString(itemTable.size()));
		itemTable.put(itemId, item);
		itemChecksums.put(cs, itemId);
	    }
	    item.setId(itemId);
	}
	return itemId;
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

    public void setObject(String objectId, String comment, BigInteger version, FlagEnumeration flag, MessageType message) {
	ObjectData data = objectTable.get(objectId);
	if (data == null) {
	    data = new ObjectData(objectId);
	    objectTable.put(objectId, data);
	}
	if (comment != null) {
	    data.comment = comment;
	}
	if (version != null) {
	    data.version = version;
	}
	if (flag != null) {
	    data.flag = flag;
	}
	if (message != null) {
	    data.messages.add(message);
	}
    }

    public void relateItem(String objectId, BigInteger itemId) throws NoSuchElementException {
	if (!objectTable.containsKey(objectId)) {
	    throw new NoSuchElementException(JOVALMsg.getMessage(JOVALMsg.ERROR_REF_OBJECT, objectId));
	}
	if (!itemTable.containsKey(itemId)) {
	    throw new NoSuchElementException(JOVALMsg.getMessage(JOVALMsg.ERROR_REF_ITEM, itemId.toString()));
	}
	HashSet<BigInteger> items = objectItemTable.get(objectId);
	if (items == null) {
	    items = new HashSet<BigInteger>();
	    objectItemTable.put(objectId, items);
	}
	items.add(itemId);
    }

    public void relateVariable(String objectId, String variableId) throws NoSuchElementException {
	if (!objectTable.containsKey(objectId)) {
	    throw new NoSuchElementException(JOVALMsg.getMessage(JOVALMsg.ERROR_REF_OBJECT, objectId));
	}
	if (!variableTable.containsKey(variableId)) {
	    throw new NoSuchElementException(JOVALMsg.getMessage(JOVALMsg.ERROR_REF_VARIABLE, variableId));
	}
	HashSet<String> variables = objectVariableTable.get(objectId);
	if (variables == null) {
	    variables = new HashSet<String>();
	    objectVariableTable.put(objectId, variables);
	}
	variables.add(variableId);
    }

    public boolean containsObject(String objectId) {
	return objectTable.containsKey(objectId);
    }

    public FlagEnumeration getObjectFlag(String id) throws NoSuchElementException {
	if (!objectTable.containsKey(id)) {
	    throw new NoSuchElementException(JOVALMsg.getMessage(JOVALMsg.ERROR_REF_OBJECT, id));
	}
	return objectTable.get(id).flag;
    }

    public List<ItemType> getItemsByObjectId(String id) throws NoSuchElementException {
	if (!objectTable.containsKey(id)) {
	    throw new NoSuchElementException(JOVALMsg.getMessage(JOVALMsg.ERROR_REF_OBJECT, id));
	}
	List <ItemType>items = new Vector<ItemType>();
	if (objectItemTable.containsKey(id)) {
	    for (BigInteger itemId : objectItemTable.get(id)) {
		if (itemTable.containsKey(itemId)) {
		    items.add(itemTable.get(itemId));
		} else {
		    throw new NoSuchElementException(JOVALMsg.getMessage(JOVALMsg.ERROR_REF_ITEM, itemId));
		}
	    }
	}
	return items;
    }

    public List<VariableValueType> getVariablesByObjectId(String id) throws NoSuchElementException {
	if (!objectTable.containsKey(id)) {
	    throw new NoSuchElementException(JOVALMsg.getMessage(JOVALMsg.ERROR_REF_OBJECT, id));
	}
	List<VariableValueType> variables = new Vector<VariableValueType>();
	if (objectVariableTable.containsKey(id)) {
	    for (String variableId : objectVariableTable.get(id)) {
		if (variableTable.containsKey(variableId)) {
		    variables.addAll(variableTable.get(variableId));
		} else {
		    throw new NoSuchElementException(JOVALMsg.getMessage(JOVALMsg.ERROR_REF_VARIABLE, variableId));
		}
	    }
	}
	return variables;
    }

    public void writeXML(File f) {
	OutputStream out = null;
	try {
	    Marshaller marshaller = ctx.createMarshaller();
	    OvalNamespacePrefixMapper.configure(marshaller, OvalNamespacePrefixMapper.URI.SC);
	    out = new FileOutputStream(f);
	    marshaller.marshal(getOvalSystemCharacteristics(false), out);
	} catch (JAXBException e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (FactoryConfigurationError e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (FileNotFoundException e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (OvalException e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
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

    private ObjectType makeObject(String id) throws NoSuchElementException {
	ObjectData data = objectTable.get(id);
	if (data == null) {
	    throw new NoSuchElementException(JOVALMsg.getMessage(JOVALMsg.ERROR_REF_OBJECT, id));
	}
	ObjectType obj = Factories.sc.core.createObjectType();
	obj.setId(data.id);
	obj.setFlag(data.flag);
	if (data.comment != null) {
	    obj.setComment(data.comment);
	}
	if (data.version != null) {
	    obj.setVersion(data.version);
	}
	for (MessageType message : data.messages) {
	    obj.getMessage().add(message);
	}
	for (ItemType item : getItemsByObjectId(id)) {
	    ReferenceType ref = Factories.sc.core.createReferenceType();
	    ref.setItemRef(item.getId());
	    obj.getReference().add(ref);
	}
	for (VariableValueType variable : getVariablesByObjectId(id)) {
	    obj.getVariableValue().add(variable);
	}
	return obj;
    }

    /**
     * Get the checksum of an ItemType by:
     * 1) Temporarily setting the ID to null
     * 2) Wrapping it in a JAXBElement
     * 3) Marshalling it to a stream in memory
     * 4) Taking the checkum of the stream
     * 5) Setting the ID back to the original value
     * 6) Returning the checksum
     */
    private String getChecksum(ItemType item) throws OvalException {
	String checksum = null;
	synchronized(item) {
	    BigInteger itemId = item.getId();
	    item.setId(null);
	    JAXBElement elt = wrapItem(item);
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
		checksum = sb.toString();
	    } catch (NoSuchAlgorithmException e) {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    } catch (JAXBException e) {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    } finally {
		item.setId(itemId);
	    }
	}
	return checksum;
    }

    private Hashtable<Class, Object> wrapperFactories = new Hashtable<Class, Object>();
    private Hashtable<Class, Method> wrapperMethods = new Hashtable<Class, Method>();

    private JAXBElement<? extends ItemType> wrapItem(ItemType item) throws OvalException {
	try {
	    Class clazz = item.getClass();
	    Method method = wrapperMethods.get(clazz);
	    Object factory = wrapperFactories.get(clazz);
	    if (method == null || factory == null) {
		String packageName = clazz.getPackage().getName();
		String unqualClassName = clazz.getName().substring(packageName.length()+1);
		Class<?> factoryClass = Class.forName(packageName + ".ObjectFactory");
		factory = factoryClass.newInstance();
		wrapperFactories.put(clazz, factory);
		method = factoryClass.getMethod("create" + unqualClassName, item.getClass());
		wrapperMethods.put(clazz, method);
	    }
	    @SuppressWarnings("unchecked")
	    JAXBElement<ItemType> wrapped = (JAXBElement<ItemType>)method.invoke(factory, item);
	    return wrapped;
	} catch (Exception e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), item.getId()));
	}
    }

    class ObjectData {
	String id;
	String comment;
	BigInteger version;
	FlagEnumeration flag;
	List<MessageType> messages;

	ObjectData(String id) {
	    this.id = id;
	    flag = FlagEnumeration.INCOMPLETE;
	    messages = new Vector<MessageType>();
	}
    }
}
