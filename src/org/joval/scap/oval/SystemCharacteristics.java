// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.zip.Adler32;
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

import jsaf.intf.util.ILoggable;
import org.slf4j.cal10n.LocLogger;

import scap.oval.common.GeneratorType;
import scap.oval.common.MessageType;
import scap.oval.systemcharacteristics.core.CollectedObjectsType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.ObjectType;
import scap.oval.systemcharacteristics.core.OvalSystemCharacteristics;
import scap.oval.systemcharacteristics.core.ReferenceType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.core.SystemDataType;
import scap.oval.systemcharacteristics.core.SystemInfoType;
import scap.oval.systemcharacteristics.core.VariableValueType;

import org.joval.intf.scap.oval.ISystemCharacteristics;
import org.joval.scap.oval.xml.OvalNamespacePrefixMapper;
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
    public static final Object parse(InputStream in) throws OvalException {
	return parse(new StreamSource(in));
    }

    public static final Object parse(Source src) throws OvalException {
	try {
	    Unmarshaller unmarshaller = SchemaRegistry.OVAL_SYSTEMCHARACTERISTICS.getJAXBContext().createUnmarshaller();
	    return unmarshaller.unmarshal(src);
	} catch (JAXBException e) {
	    throw new OvalException(e);
	}
    }

    public static final OvalSystemCharacteristics getOvalSystemCharacteristics(File f) throws OvalException {
	return getOvalSystemCharacteristics(new StreamSource(f));
    }

    public static final OvalSystemCharacteristics getOvalSystemCharacteristics(Source src) throws OvalException {
	Object rootObj = parse(src);
	if (rootObj instanceof OvalSystemCharacteristics) {
	    return (OvalSystemCharacteristics)rootObj;
	} else {
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_SC_BAD_SOURCE, src.getSystemId()));
	}
    }

    private LocLogger logger = JOVALMsg.getLogger();
    private GeneratorType generator;
    private SystemInfoType systemInfo;
    private Map<String, ObjectData> objectTable;
    private Map<BigInteger, ItemType> itemTable;
    private Map<String, Collection<VariableValueType>> variableTable;
    private Map<String, Collection<BigInteger>> objectItemTable;
    private Map<String, Collection<String>> objectVariableTable;
    private Map<String, Collection<BigInteger>> itemChecksums;
    private int itemCounter = 0;
    private Marshaller marshaller = null;

    /**
     * Create an empty SystemCharacteristics.
     */
    SystemCharacteristics() {
	objectTable = new HashMap<String, ObjectData>();
	itemTable = new HashMap<BigInteger, ItemType>();
	itemChecksums = new HashMap<String, Collection<BigInteger>>();
	variableTable = new HashMap<String, Collection<VariableValueType>>();
	objectItemTable = new HashMap<String, Collection<BigInteger>>();
	objectVariableTable = new HashMap<String, Collection<String>>();
	variableTable = new HashMap<String, Collection<VariableValueType>>();
	try {
	    marshaller = SchemaRegistry.OVAL_SYSTEMCHARACTERISTICS.getJAXBContext().createMarshaller();
	    OvalNamespacePrefixMapper.configure(marshaller, OvalNamespacePrefixMapper.URI.SC);
	} catch (JAXBException e) {
	    logger.error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (FactoryConfigurationError e) {
	    logger.error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	generator = OvalFactory.getGenerator();
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
    }

    /**
     * Create a SystemCharacteristics from an OvalSystemCharacteristics (i.e., from a parsed File).
     */
    public SystemCharacteristics(OvalSystemCharacteristics osc) throws OvalException {
	this();
	generator = osc.getGenerator();
	systemInfo = osc.getSystemInfo();

	for (JAXBElement<? extends ItemType> item : osc.getSystemData().getItem()) {
	    itemTable.put(item.getValue().getId(), item.getValue());
	    itemCounter++;
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

    public Source getSource() throws JAXBException, OvalException {
	return new JAXBSource(SchemaRegistry.OVAL_SYSTEMCHARACTERISTICS.getJAXBContext(), getOvalSystemCharacteristics(false));
    }

    public Object getRootObject() {
	return getOvalSystemCharacteristics(false);
    }

    public JAXBContext getJAXBContext() throws JAXBException {
	return SchemaRegistry.OVAL_SYSTEMCHARACTERISTICS.getJAXBContext();
    }

    // Implement ISystemCharacteristics

    public SystemInfoType getSystemInfo() {
	return systemInfo;
    }

    //
    // DAS mask support is TBD
    //
    public OvalSystemCharacteristics getOvalSystemCharacteristics(boolean mask) {
	OvalSystemCharacteristics sc = Factories.sc.core.createOvalSystemCharacteristics();
	sc.setGenerator(generator);
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
	if (item.isSetId()) {
	    // The item has been stored previously someplace else, so store a copy of it.
	    Object obj = parse(new ByteArrayInputStream(toCanonicalBytes(item)));
	    if (obj instanceof JAXBElement) {
		obj = ((JAXBElement)obj).getValue();
	    }
	    item = (ItemType)obj;
	}
	byte[] data = toCanonicalBytes(item);
	Adler32 adler = new Adler32();
	adler.update(data);
	String cs = Long.toString(adler.getValue());
	if (itemChecksums.containsKey(cs)) {
	    //
	    // If another item with the same Adler32 checksum has been stored previously, that doesn't mean
	    // it contains the same data.  So, we compare it to all the previously-stored items with the
	    // same checksum.
	    //
	    boolean match = true;
	    for (BigInteger id : itemChecksums.get(cs)) {
		byte[] candidate = toCanonicalBytes(itemTable.get(id));
		if (candidate.length == data.length) {
		    match = true;
		    for (int i=0; i < data.length; i++) {
			if (candidate[i] != data[i]) {
			    match = false;
			    break;
			}
		    }
		} else {
		    match = false;
		}
		if (match) {
		    itemId = id;
		    item.setId(itemId);
		    break;
		}
	    }
	    //
	    // Having determined that the item is indeed new, we store it.
	    //
	    if (!match) {
		itemId = new BigInteger(Integer.toString(itemCounter++));
		item.setId(itemId);
		itemTable.put(itemId, item);
		itemChecksums.get(cs).add(itemId);
	    }
	} else {
	    itemId = new BigInteger(Integer.toString(itemCounter++));
	    item.setId(itemId);
	    itemTable.put(itemId, item);
	    Collection<BigInteger> set = new HashSet<BigInteger>();
	    set.add(itemId);
	    itemChecksums.put(cs, set);
	}
	return itemId;
    }

    public void storeVariable(VariableValueType var) {
	Collection<VariableValueType> vars = variableTable.get(var.getVariableId());
	if (vars == null) {
	    vars = new ArrayList<VariableValueType>();
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
	Collection<BigInteger> items = objectItemTable.get(objectId);
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
	Collection<String> variables = objectVariableTable.get(objectId);
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

    public Collection<ItemType> getItemsByObjectId(String id) throws NoSuchElementException {
	if (!objectTable.containsKey(id)) {
	    throw new NoSuchElementException(JOVALMsg.getMessage(JOVALMsg.ERROR_REF_OBJECT, id));
	}
	Collection <ItemType>items = new ArrayList<ItemType>();
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

    public Collection<VariableValueType> getVariablesByObjectId(String id) throws NoSuchElementException {
	if (!objectTable.containsKey(id)) {
	    throw new NoSuchElementException(JOVALMsg.getMessage(JOVALMsg.ERROR_REF_OBJECT, id));
	}
	Collection<VariableValueType> variables = new ArrayList<VariableValueType>();
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
     * Canonicalize the item by stripping out its ID (if any) marshalling it to XML, and returning the bytes.
     */
    private byte[] toCanonicalBytes(ItemType item) throws OvalException {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	synchronized(item) {
	    BigInteger itemId = item.getId();
	    item.setId(null);
	    JAXBElement elt = wrapItem(item);
	    try {
		marshaller.marshal(elt, out);
	    } catch (JAXBException e) {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    } finally {
		item.setId(itemId);
	    }
	}
	return out.toByteArray();
    }

    private Map<Class, Object> wrapperFactories = new HashMap<Class, Object>();
    private Map<Class, Method> wrapperMethods = new HashMap<Class, Method>();

    private JAXBElement<? extends ItemType> wrapItem(ItemType item) {
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
	    throw new RuntimeException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), item.getId()));
	}
    }

    class ObjectData {
	String id;
	String comment;
	BigInteger version;
	FlagEnumeration flag;
	Collection<MessageType> messages;

	ObjectData(String id) {
	    this.id = id;
	    flag = FlagEnumeration.INCOMPLETE;
	    messages = new ArrayList<MessageType>();
	}
    }
}
