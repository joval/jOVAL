// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import org.slf4j.cal10n.LocLogger;

import oval.schemas.common.CheckEnumeration;
import oval.schemas.common.ExistenceEnumeration;
import oval.schemas.common.GeneratorType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.OperatorEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ArithmeticEnumeration;
import oval.schemas.definitions.core.ArithmeticFunctionType;
import oval.schemas.definitions.core.BeginFunctionType;
import oval.schemas.definitions.core.ConcatFunctionType;
import oval.schemas.definitions.core.ConstantVariable;
import oval.schemas.definitions.core.CountFunctionType;
import oval.schemas.definitions.core.CriteriaType;
import oval.schemas.definitions.core.CriterionType;
import oval.schemas.definitions.core.DefinitionType;
import oval.schemas.definitions.core.DefinitionsType;
import oval.schemas.definitions.core.EndFunctionType;
import oval.schemas.definitions.core.EntityComplexBaseType;
import oval.schemas.definitions.core.EntityObjectStringType;
import oval.schemas.definitions.core.EntitySimpleBaseType;
import oval.schemas.definitions.core.EntityStateFieldType;
import oval.schemas.definitions.core.EntityStateRecordType;
import oval.schemas.definitions.core.EntityStateSimpleBaseType;
import oval.schemas.definitions.core.EscapeRegexFunctionType;
import oval.schemas.definitions.core.ExtendDefinitionType;
import oval.schemas.definitions.core.ExternalVariable;
import oval.schemas.definitions.core.Filter;
import oval.schemas.definitions.core.LiteralComponentType;
import oval.schemas.definitions.core.LocalVariable;
import oval.schemas.definitions.core.ObjectComponentType;
import oval.schemas.definitions.core.ObjectRefType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.ObjectsType;
import oval.schemas.definitions.core.OvalDefinitions;
import oval.schemas.definitions.core.RegexCaptureFunctionType;
import oval.schemas.definitions.core.Set;
import oval.schemas.definitions.core.SetOperatorEnumeration;
import oval.schemas.definitions.core.SplitFunctionType;
import oval.schemas.definitions.core.StateRefType;
import oval.schemas.definitions.core.StateType;
import oval.schemas.definitions.core.StatesType;
import oval.schemas.definitions.core.SubstringFunctionType;
import oval.schemas.definitions.core.TestsType;
import oval.schemas.definitions.core.TimeDifferenceFunctionType;
import oval.schemas.definitions.core.UniqueFunctionType;
import oval.schemas.definitions.core.ValueType;
import oval.schemas.definitions.core.VariableComponentType;
import oval.schemas.definitions.core.VariableType;
import oval.schemas.definitions.core.VariablesType;
import oval.schemas.definitions.independent.EntityObjectVariableRefType;
import oval.schemas.definitions.independent.UnknownTest;
import oval.schemas.definitions.independent.VariableObject;
import oval.schemas.results.core.ResultEnumeration;
import oval.schemas.results.core.TestedItemType;
import oval.schemas.results.core.TestedVariableType;
import oval.schemas.results.core.TestType;
import oval.schemas.systemcharacteristics.core.EntityItemAnySimpleType;
import oval.schemas.systemcharacteristics.core.EntityItemFieldType;
import oval.schemas.systemcharacteristics.core.EntityItemRecordType;
import oval.schemas.systemcharacteristics.core.EntityItemSimpleBaseType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.core.SystemDataType;
import oval.schemas.systemcharacteristics.core.SystemInfoType;
import oval.schemas.systemcharacteristics.core.VariableValueType;
import oval.schemas.systemcharacteristics.independent.EntityItemVariableRefType;
import oval.schemas.systemcharacteristics.independent.VariableItem;
import oval.schemas.variables.core.OvalVariables;

import org.joval.intf.oval.IDefinitionFilter;
import org.joval.intf.oval.IDefinitions;
import org.joval.intf.oval.IEngine;
import org.joval.intf.oval.IResults;
import org.joval.intf.oval.ISystemCharacteristics;
import org.joval.intf.oval.IVariables;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.util.IObserver;
import org.joval.intf.util.IProducer;
import org.joval.oval.CollectException;
import org.joval.oval.DefinitionFilter;
import org.joval.oval.Definitions;
import org.joval.oval.Directives;
import org.joval.oval.Factories;
import org.joval.oval.ItemSet;
import org.joval.oval.OvalException;
import org.joval.oval.OvalFactory;
import org.joval.oval.Results;
import org.joval.oval.SystemCharacteristics;
import org.joval.oval.sysinfo.SysinfoFactory;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.Producer;
import org.joval.util.StringTools;
import org.joval.util.Version;

/**
 * Engine that evaluates OVAL tests using an IBaseSession.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Engine implements IEngine, IAdapter {
    private static final String ADAPTERS_RESOURCE = "adapters.txt";

    /**
     * Get a list of all the IAdapters that are bundled with the engine.
     */
    private static Collection<IAdapter> getAdapters() {
	Collection<IAdapter> adapters = new HashSet<IAdapter>();
	try {
	    ClassLoader cl = Thread.currentThread().getContextClassLoader();
	    InputStream rsc = cl.getResourceAsStream(ADAPTERS_RESOURCE);
	    if (rsc == null) {
		JOVALMsg.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_MISSING_RESOURCE, ADAPTERS_RESOURCE));
	    } else {
		BufferedReader reader = new BufferedReader(new InputStreamReader(rsc));
		String line = null;
		while ((line = reader.readLine()) != null) {
		    if (!line.startsWith("#")) {
			try {
			    Object obj = Class.forName(line).newInstance();
			    if (obj instanceof IAdapter) {
				adapters.add((IAdapter)obj);
			    }
			} catch (InstantiationException e) {
			} catch (IllegalAccessException e) {
			} catch (ClassNotFoundException e) {
			}
		    }
		}
	    }
	} catch (IOException e) {
	    JOVALMsg.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return adapters;
    }

    private enum State {
	CONFIGURE,
	RUNNING,
	COMPLETE_OK,
	COMPLETE_ERR;
    }

    private Hashtable <String, Collection<TypedData>>variableMap;
    private IVariables externalVariables = null;
    private IDefinitions definitions = null;
    private IBaseSession session = null;
    private ISystemCharacteristics sc = null;
    private IDefinitionFilter filter = null;
    private IEngine.Mode mode;
    private Hashtable<Class, IAdapter> adapters = null;
    private Exception error;
    private Results results;
    private State state;
    private boolean evalEnabled = true, abort = false;
    private Producer producer;
    private LocLogger logger;

    /**
     * Create an engine for evaluating OVAL definitions using a session.
     */
    protected Engine(IEngine.Mode mode, IBaseSession session) {
	if (session == null) {
	    logger = JOVALMsg.getLogger();
	} else {
	    logger = session.getLogger();
	    this.session = session;
	}
	this.mode = mode;
	producer = new Producer();
	filter = new DefinitionFilter();
	reset();
    }

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	classes.add(VariableObject.class);
	return classes;
    }

    public Collection<VariableItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException, OvalException {
	VariableObject vObj = (VariableObject)obj;
	Collection<VariableItem> items = new Vector<VariableItem>();
	try {
	    Collection<TypedData> values = resolve((String)vObj.getVarRef().getValue(), (RequestContext)rc);
	    if (values.size() > 0) {
		VariableItem item = Factories.sc.independent.createVariableItem();
		EntityItemVariableRefType ref = Factories.sc.independent.createEntityItemVariableRefType();
		ref.setValue(vObj.getVarRef().getValue());
		item.setVarRef(ref);
		for (TypedData value : values) {
		    EntityItemAnySimpleType valueType = Factories.sc.core.createEntityItemAnySimpleType();
		    valueType.setValue(value.getString());
		    valueType.setDatatype(value.getSimpleType().value());
		    item.getValue().add(valueType);
		}
		items.add(item);
	    }
	} catch (ResolveException e) {
	    throw new CollectException(e.getMessage(), FlagEnumeration.ERROR);
	}
	return items;
    }

    // Implement IEngine

    public void setDefinitions(IDefinitions definitions) throws IllegalThreadStateException {
	switch(state) {
	  case RUNNING:
	    throw new IllegalThreadStateException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_STATE, state));

	  case COMPLETE_OK:
	  case COMPLETE_ERR:
	    reset();
	    // fall-through

	  default:
	    this.definitions = definitions;
	    break;
	}
    }

    public void setDefinitionFilter(IDefinitionFilter filter) throws IllegalThreadStateException {
	switch(state) {
	  case CONFIGURE:
	    this.filter = filter;
	    break;

	  default:
	    throw new IllegalThreadStateException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_STATE, state));
	}
    }

    public void setSystemCharacteristics(ISystemCharacteristics sc) throws IllegalThreadStateException, OvalException {
	switch(state) {
	  case CONFIGURE:
	    mode = Mode.EXHAUSTIVE;
	    this.sc = sc;
	    break;

	  default:
	    throw new IllegalThreadStateException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_STATE, state));
	}
    }

    public void setExternalVariables(IVariables variables) throws IllegalThreadStateException {
	switch(state) {
	  case CONFIGURE:
	    externalVariables = variables;
	    break;

	  default:
	    throw new IllegalThreadStateException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_STATE, state));
	}
    }

    public IProducer getNotificationProducer() {
	return producer;
    }

    public Result getResult() throws IllegalThreadStateException {
	switch(state) {
	  case COMPLETE_OK:
	    return Result.OK;

	  case COMPLETE_ERR:
	    return Result.ERR;

	  case CONFIGURE:
	  case RUNNING:
	  default:
	    throw new IllegalThreadStateException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_STATE, state));
	}
    }

    public IResults getResults() throws IllegalThreadStateException {
	getResult();
	return results;
    }

    public Exception getError() throws IllegalThreadStateException {
	getResult();
	return error;
    }

    public void destroy() {
	if (state == State.RUNNING) {
	    abort = true;
	}
    }

    public ResultEnumeration evaluateDefinition(String id) throws IllegalStateException, NoSuchElementException, OvalException {
	try {
	    if (definitions == null) {
		throw new IllegalStateException(JOVALMsg.getMessage(JOVALMsg.ERROR_DEFINITIONS_NONE));
	    }
	    state = State.RUNNING;
	    if (adapters == null) {
		loadAdapters();
	    }
	    if (sc == null) {
		SystemCharacteristics sc = new SystemCharacteristics(SysinfoFactory.createSystemInfo(session));
		sc.setLogger(logger);
		this.sc = sc;
	    }
	    if (results == null) {
		results = new Results(definitions, sc);
		results.setLogger(logger);
	    }
	    return evaluateDefinition(definitions.getDefinition(id)).getResult();
	} finally {
	    state = State.COMPLETE_OK;
	}
    }

    // Implement Runnable

    /**
     * The engine runs differently depending on the mode that was used to initialize it:
     *
     * DIRECTED & LAZY:
     *   The Engine will iterate through the [filtered] definitions and probe objects as they are encountered.
     *
     * EXHAUSTIVE:
     *   First the Engine probes all the objects in the OVAL definitions, or it uses the supplied ISystemCharacteristics.
     *   Then, all the definitions are evaluated.  This mirrors the way that ovaldi processes OVAL definitions.
     *
     * Note: if the session is connected before running, it will remain connected after the run has completed.  If it
     * was not connected before running, the engine will temporarily connect, then disconnect when finished.
     */
    public void run() {
	state = State.RUNNING;

	boolean doDisconnect = false;
	try {
	    //
	    // Connect and initialize adapters if necessary
	    //
	    boolean scanRequired = sc == null;
	    if (scanRequired) {
		if (!session.isConnected()) {
	    	    if (session == null) {
			throw new RuntimeException(JOVALMsg.getMessage(JOVALMsg.ERROR_SESSION_NONE));
		    } else {
			if (session.connect()) {
			    doDisconnect = true;
			} else {
			    throw new RuntimeException(JOVALMsg.getMessage(JOVALMsg.ERROR_SESSION_CONNECT));
			}
		    }
		}
		if (adapters == null) {
		    loadAdapters();
		}
		SystemCharacteristics sc = new SystemCharacteristics(SysinfoFactory.createSystemInfo(session));
		sc.setLogger(logger);
		this.sc = sc;
	    }

	    switch(mode) {
	      case EXHAUSTIVE:
		//
		// Perform an exhaustive scan of all objects, and disconnect if permitted
		//
		if (scanRequired) {
		    producer.sendNotify(MESSAGE_OBJECT_PHASE_START, null);
		    for (ObjectType obj : definitions.getObjects()) {
			if (!sc.containsObject(obj.getId())) {
			    scanObject(new RequestContext(this, definitions.getObject(obj.getId())));
			}
		    }
		    producer.sendNotify(MESSAGE_OBJECT_PHASE_END, null);
		    if (doDisconnect) {
			session.disconnect();
			doDisconnect = false;
		    }
		    producer.sendNotify(MESSAGE_SYSTEMCHARACTERISTICS, sc);
		}
		break;

	      default:
		producer.sendNotify(MESSAGE_OBJECT_PHASE_START, null);
		break;
	    }

	    results = new Results(definitions, sc);
	    results.setLogger(logger);
	    producer.sendNotify(MESSAGE_DEFINITION_PHASE_START, null);

	    //
	    // Use the filter to separate the definitions into allowed and disallowed lists.  First evaluate all the allowed
	    // definitions, then go through the disallowed definitions.  This makes it possible to cache both test and
	    // definition results without having to double-check if they were previously intentionally skipped.
	    //
	    Collection<DefinitionType>allowed = new Vector<DefinitionType>();
	    Collection<DefinitionType>disallowed = new Vector<DefinitionType>();
	    definitions.filterDefinitions(filter, allowed, disallowed);

	    evalEnabled = true;
	    for (DefinitionType definition : allowed) {
		evaluateDefinition(definition);
	    }

	    evalEnabled = false;
	    for (DefinitionType definition : disallowed) {
		evaluateDefinition(definition);
	    }

	    switch(mode) {
	      case DIRECTED:
		producer.sendNotify(MESSAGE_OBJECT_PHASE_END, null);
		producer.sendNotify(MESSAGE_SYSTEMCHARACTERISTICS, sc);
		break;
	    }

	    producer.sendNotify(MESSAGE_DEFINITION_PHASE_END, null);
	    state = State.COMPLETE_OK;
	} catch (Exception e) {
	    error = e;
	    state = State.COMPLETE_ERR;
	} finally {
	    if (doDisconnect) {
		session.disconnect();
	    }
	}
    }

    // Internal

    ISystemCharacteristics getSystemCharacteristics() {
	return sc;
    }

    /**
     * Return the value of the Variable with the specified ID, and also add any chained variables to the provided list.
     */
    Collection<TypedData> resolve(String variableId, RequestContext rc)
		throws NoSuchElementException, ResolveException, OvalException {

	VariableType var = definitions.getVariable(variableId);
	String varId = var.getId();
	Collection<TypedData> result = variableMap.get(varId);
	if (result == null) {
	    logger.trace(JOVALMsg.STATUS_VARIABLE_CREATE, varId);
	    result = resolveInternal(var, rc);
	    variableMap.put(varId, result);
	} else {
	    logger.trace(JOVALMsg.STATUS_VARIABLE_RECYCLE, varId);
	}
	return result;
    }

    // Private

    private void loadAdapters() {
	adapters = new Hashtable<Class, IAdapter>();
	Collection<IAdapter> coll = getAdapters();
	if (coll != null) {
	    adapters = new Hashtable<Class, IAdapter>();
	    for (IAdapter adapter : coll) {
		for (Class clazz : adapter.init(session)) {
		    adapters.put(clazz, adapter);
		}
	    }
	}
	adapters.put(VariableObject.class, this);
    }

    private void reset() {
	sc = null;
	state = State.CONFIGURE;
	variableMap = new Hashtable<String, Collection<TypedData>>();
	error = null;
    }

    /**
     * Scan an object live using an adapter, including crawling down any encountered Sets.  Items are stored in the
     * system-characteristics as they are collected.
     *
     * If for some reason (like an error) no items can be obtained, this method just returns an empty list so processing
     * can continue.
     *
     * @throws OvalException if processing should cease for some good reason
     */
    private Collection<ItemType> scanObject(RequestContext rc) throws OvalException {
	ObjectType masterObj = rc.getObject();
	String objectId = masterObj.getId();
	logger.debug(JOVALMsg.STATUS_OBJECT, objectId);
	producer.sendNotify(MESSAGE_OBJECT, objectId);
	Collection<ItemType> items = new Vector<ItemType>();
	if (adapters.containsKey(masterObj.getClass())) {
	    Set s = getObjectSet(masterObj);
	    if (s == null) {
		List<MessageType> messages = new Vector<MessageType>();
		FlagData flag = new FlagData();
		try {
		    for (ObjectType obj : resolveObjects(rc)) {
			//
			// As the lowest level scan operation, this is a good place to check if the engine is being destroyed.
			//
			if (abort) {
			    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_ABORT));
			}

			try {
			    IAdapter adapter = adapters.get(obj.getClass());
			    @SuppressWarnings("unchecked")
			    Collection<ItemType> retrieved = (Collection<ItemType>)adapter.getItems(obj, rc);
			    flag.add(FlagEnumeration.COMPLETE);
			    items.addAll(filterItems(getFilters(masterObj), retrieved, rc));
			} catch (CollectException e) {
			    MessageType msg = Factories.common.createMessageType();
			    msg.setLevel(MessageLevelEnumeration.WARNING);
			    String err = JOVALMsg.getMessage(JOVALMsg.ERROR_ADAPTER_COLLECTION, e.getMessage());
			    msg.setValue(err);
			    messages.add(msg);
			    flag.add(e.getFlag());
			} catch (Exception e) {
			    //
			    // Handle an uncaught, unexpected exception emanating from the adapter.
			    //
			    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
			    MessageType msg = Factories.common.createMessageType();
			    msg.setLevel(MessageLevelEnumeration.ERROR);
			    msg.setValue(e.getMessage());
			    messages.add(msg);
			    flag.add(FlagEnumeration.ERROR);
			}
		    }

		    messages.addAll(rc.getMessages());
		    sc.setObject(objectId, null, null, null, null);
		    for (VariableValueType var : rc.getVars()) {
			sc.storeVariable(var);
			sc.relateVariable(objectId, var.getVariableId());
		    }
		    if (items.size() == 0) {
			MessageType msg = Factories.common.createMessageType();
			msg.setLevel(MessageLevelEnumeration.INFO);
			msg.setValue(JOVALMsg.getMessage(JOVALMsg.STATUS_EMPTY_OBJECT));
			messages.add(msg);
		    }
		} catch (ResolveException e) {
		    MessageType msg = Factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.ERROR);
		    msg.setValue(e.getMessage());
		    messages.add(msg);
		    flag.add(FlagEnumeration.ERROR);
		}
		for (MessageType msg : messages) {
		    sc.setObject(objectId, null, null, null, msg);
		    switch(msg.getLevel()) {
		      case FATAL:
		      case ERROR:
			flag.add(FlagEnumeration.INCOMPLETE);
			break;
		    }
		}
		sc.setObject(objectId, masterObj.getComment(), masterObj.getVersion(), flag.getFlag(), null);
	    } else {
		items = getSetItems(s, rc);
		MessageType msg = null;
		if (items.size() == 0) {
		    msg = Factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.INFO);
		    msg.setValue(JOVALMsg.getMessage(JOVALMsg.STATUS_EMPTY_SET));
		}
		sc.setObject(objectId, masterObj.getComment(), masterObj.getVersion(), FlagEnumeration.COMPLETE, msg);
	    }
	} else {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.WARNING);
	    String err = JOVALMsg.getMessage(JOVALMsg.ERROR_ADAPTER_MISSING, masterObj.getClass().getName());
	    msg.setValue(err);
	    sc.setObject(objectId, masterObj.getComment(), masterObj.getVersion(), FlagEnumeration.NOT_COLLECTED, msg);
	}
	for (ItemType item : items) {
	    sc.relateItem(objectId, sc.storeItem(item));
	}
	return items;
    }

    /**
     * Convert an ObectType whose EntityObjectSimpleBaseType members may contain var_refs into a list of ObjectTypes
     * containing only resolved entities (i.e., isSetVarRef() == false).
     */
    private Collection<ObjectType> resolveObjects(RequestContext rc) throws OvalException, ResolveException {
	ObjectType obj = rc.getObject();
	List<ObjectType>objects = new Vector<ObjectType>();
	try {
	    //
	    // First, create lists of entities within the object
	    //
	    Hashtable<String, List<Object>> lists = new Hashtable<String, List<Object>>();
	    int numPermutations = 1;
	    for (String methodName : getMethodNames(obj.getClass())) {
		if (methodName.startsWith("get") && !objectBaseMethodNames.contains(methodName)) {
		    Object entity = obj.getClass().getMethod(methodName).invoke(obj);
		    if (entity == null) {
			// continue
		    } else {
			List<Object> list = resolveUnknownEntity(methodName, entity, rc);
			if (list.size() > 0) {
			    numPermutations = numPermutations * list.size();
			    lists.put(methodName, list);
			}
		    }
		}
	    }
	    //
	    // Create a permutation list of objects using the entity lists
	    //
	    Class<?> objClass = obj.getClass();
	    String pkgName = objClass.getPackage().getName();
	    Class<?> factoryClass = Class.forName(pkgName + ".ObjectFactory");
	    Object factory = factoryClass.newInstance();
	    String unqualClassName = objClass.getName().substring(pkgName.length() + 1);
	    Method createObj = factoryClass.getMethod("create" + unqualClassName);
	    for (int i=0; i < numPermutations; i++) {
		ObjectType ot = (ObjectType)createObj.invoke(factory);
		ot.setId(obj.getId());
		Object behaviors = safeInvokeMethod(obj, "getBehaviors");
		if (behaviors != null) {
		    Method setBehaviors = objClass.getMethod("setBehaviors", behaviors.getClass());
		    setBehaviors.invoke(ot, behaviors);
		}
		objects.add(ot);
	    }
	    for (String getter : lists.keySet()) {
		List<Object> list = lists.get(getter);
		int divisor = list.size();
		int groupSize = objects.size() / divisor;
		int index = 0;
		String setter = new StringBuffer("s").append(getter.substring(1)).toString();
		Class entityClass = list.get(0).getClass();
		@SuppressWarnings("unchecked")
		Method setObj = objClass.getMethod(setter, entityClass);
		for (Object entity : list) {
		    for (int i=0; i < groupSize; i++) {
			setObj.invoke(objects.get(index++), entity);
		    }
		}
	    }
	    return objects;
	} catch (ClassNotFoundException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), obj.getId()));
	} catch (InstantiationException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), obj.getId()));
	} catch (NoSuchMethodException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), obj.getId()));
	} catch (IllegalAccessException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), obj.getId()));
	} catch (InvocationTargetException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), obj.getId()));
	}
    }

    /**
     * Take an entity that may be a var_ref, and return a list of all resulting concrete entities (i.e., isSetValue == true).
     * The result may be a JAXBElement list, or EntitySimpleBaseType list or an EntityComplexBaseType list.
     */
    private List<Object> resolveUnknownEntity(String methodName, Object entity, RequestContext rc)
		throws OvalException, ResolveException, InstantiationException, ClassNotFoundException,
		NoSuchMethodException, IllegalAccessException, InvocationTargetException {

	List<Object> result = new Vector<Object>();
	if (entity instanceof JAXBElement) {
	    String pkgName = rc.getObject().getClass().getPackage().getName();
	    Class<?> factoryClass = Class.forName(pkgName + ".ObjectFactory");
	    Object factory = factoryClass.newInstance();
	    String unqualClassName = rc.getObject().getClass().getName().substring(pkgName.length() + 1);
	    String entityName = methodName.substring(3);
	    if (((JAXBElement)entity).getValue() == null) {
		result.add(entity);
	    } else {
		Class targetClass = ((JAXBElement)entity).getValue().getClass();
		Method method = factoryClass.getMethod("create" + unqualClassName + entityName, targetClass);
		for (Object resolved : resolveUnknownEntity(methodName, ((JAXBElement)entity).getValue(), rc)) {
		    result.add(method.invoke(factory, resolved));
		}
	    }
	} else if (entity instanceof EntitySimpleBaseType) {
	    EntitySimpleBaseType simple = (EntitySimpleBaseType)entity;
	    if (simple.isSetVarRef()) {
		Class objClass = entity.getClass();
		String pkgName = objClass.getPackage().getName();
		Class<?> factoryClass = Class.forName(pkgName + ".ObjectFactory");
		Object factory = factoryClass.newInstance();
		String unqualClassName = objClass.getName().substring(pkgName.length() + 1);
		Method method = factoryClass.getMethod("create" + unqualClassName);
		for (TypedData type : resolve(simple.getVarRef(), rc)) {
		    if (type.getType() == TypedData.Type.SIMPLE) {
			EntitySimpleBaseType instance = (EntitySimpleBaseType)method.invoke(factory);
			instance.setDatatype(type.getSimpleType().value());
			instance.setValue(type.getString());
			instance.setOperation(simple.getOperation());
			result.add(instance);
		    }
		}
	    } else {
		result.add(entity);
	    }
/*
// NEED TO FINISH IMPLEMENTING
	} else if (entity instanceof EntityComplexBaseType) {
	    //
	    // DAS: this is a bit of a cluster-fuck, as each field can have its own var_ref...
	    //
	    EntityComplexBaseType complex = (EntityComplexBaseType)entity;
	    if (complex.isSetVarRef()) {
		Class objClass = entity.getClass();
		String pkgName = objClass.getPackage().getName();
		String unqualClassName = objClass.getName().substring(pkgName + 1);
		Method method = factory.getMethod("create" + unqualClassName, objClass);
		for (TypedData type : resolve(simple.getVarRef(), rc)) {
		    if (type.getType() == TypedData.Type.COMPLEX) {
			EntityComplexBaseType instance = (EntityComplexBaseType)method.invoke(factory);
			instance.setDatatype(type.getComplexDatatype().value());
			if (instance instanceof EntityObjectRecordType && type.getValue() instanceof EntityItemRecordType) {
			    for (EntityItemFieldType field : ((EntityItemRecordType)type.getValue()).getField()) {
				EntityObjectFieldType recordField = new EntityObjectFieldType();
				recordField.setName(field.getName());
				recordField.setDatatype(field.getDatatype());
				recordField.setValue(field.getValue());
				recordField.setOperation(field.getValue());


				((EntityObjectRecordType)instance).getField().add(field.getValue());
			    }
			}
			result.add(instance);
		    }
		}
	    } else {
		result.add(entity);
	    }
*/
	} else {
	    String id = rc.getObject().getId();
	    String message = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_ENTITY, entity.getClass().getName(), id);
	    throw new OvalException(message);
	}
	return result;
    }

    /**
     * If getFilter() were a method of ObjectType (instead of only some of its subclasses), this is what it would return.
     */
    private List<Filter> getFilters(ObjectType obj) {
	List<Filter> filters = new Vector<Filter>();
	Object oFilters = safeInvokeMethod(obj, "getFilter");
	if (oFilters != null && oFilters instanceof List) {
	    for (Object oFilter : (List)oFilters) {
		if (oFilter instanceof Filter) {
		    filters.add((Filter)oFilter);
		}
	    }
	}
	return filters;
    }

    /**
     * Given Collections of items and filters, returns the appropriately filtered collection.
     */
    private Collection<ItemType> filterItems(List<Filter> filters, Collection<ItemType> items, RequestContext rc)
		throws NoSuchElementException, OvalException {

	if (filters.size() == 0) {
	    return items;
	}
	Collection<ItemType> filteredItems = new HashSet<ItemType>();
	for (Filter filter : filters) {
	    StateType state = definitions.getState(filter.getValue());
	    for (ItemType item : items) {
		try {
		    ResultEnumeration result = compare(state, item, rc);
		    switch(filter.getAction()) {
		      case INCLUDE:
			if (result == ResultEnumeration.TRUE) {
			    filteredItems.add(item);
			}
			break;

		      case EXCLUDE:
			if (result != ResultEnumeration.TRUE) {
			    filteredItems.add(item);
			}
			break;
		    }
		} catch (TestException e) {
		    logger.debug(JOVALMsg.getMessage(JOVALMsg.ERROR_COMPONENT_FILTER), e.getMessage());
		    logger.trace(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
	    }
	}
	return filteredItems;
    }

    /**
     * Get a list of items belonging to a Set.
     */
    private Collection<ItemType> getSetItems(Set s, RequestContext rc) throws NoSuchElementException, OvalException {
	//
	// First, retrieve the filtered list of items in the Set, recursively.
	//
	Collection<Collection<ItemType>> lists = new Vector<Collection<ItemType>>();
	if (s.isSetSet()) {
	    for (Set set : s.getSet()) {
		lists.add(getSetItems(set, rc));
	    }
	} else {
	    for (String objectId : s.getObjectReference()) {
		Collection<ItemType> items = null;
		try {
		    items = sc.getItemsByObjectId(objectId);
		} catch (NoSuchElementException e) {
		    rc.pushObject(definitions.getObject(objectId));
		    items = scanObject(rc);
		    rc.popObject();
		}
		lists.add(filterItems(s.getFilter(), items, rc));
	    }
	}

	switch(s.getSetOperator()) {
	  case INTERSECTION: {
	    ItemSet<ItemType> intersection = null;
	    for (Collection<ItemType> items : lists) {
		if (intersection == null) {
		    intersection = new ItemSet<ItemType>(items);
		} else {
		    intersection = intersection.intersection(new ItemSet<ItemType>(items));
		}
	    }
	    return intersection.toList();
	  }

	  case COMPLEMENT: {
	    if (lists.size() == 2) {
		Iterator<Collection<ItemType>> iter = lists.iterator();
		Collection<ItemType> set1 = iter.next();
		Collection<ItemType> set2 = iter.next();
		return new ItemSet<ItemType>(set1).complement(new ItemSet<ItemType>(set2)).toList();
	    } else {
		throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_SET_COMPLEMENT, new Integer(lists.size())));
	    }
	  }

	  case UNION:
	  default: {
	    ItemSet<ItemType> union = new ItemSet<ItemType>();
	    for (Collection<ItemType> items : lists) {
		union = union.union(new ItemSet<ItemType>(items));
	    }
	    return union.toList();
	  }
	}
    }

    /**
     * Evaluate the DefinitionType.
     */
    private oval.schemas.results.core.DefinitionType evaluateDefinition(DefinitionType definition) throws OvalException {
	String id = definition.getId();
	if (id == null) {
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_DEFINITION_NOID));
	}
	oval.schemas.results.core.DefinitionType result = results.getDefinition(id);
	if (result == null) {
	    result = Factories.results.createDefinitionType();
	    logger.debug(JOVALMsg.STATUS_DEFINITION, id);
	    producer.sendNotify(MESSAGE_DEFINITION, id);
	    result.setDefinitionId(id);
	    result.setVersion(definition.getVersion());
	    result.setClazz(definition.getClazz());
	    try {
		oval.schemas.results.core.CriteriaType criteriaResult = evaluateCriteria(definition.getCriteria());
		result.setResult(criteriaResult.getResult());
		result.setCriteria(criteriaResult);
	    } catch (NoSuchElementException e) {
		result.setResult(ResultEnumeration.ERROR);
		MessageType message = Factories.common.createMessageType();
		message.setLevel(MessageLevelEnumeration.ERROR);
		message.setValue(e.getMessage());
		result.getMessage().add(message);
	    }
	    results.storeDefinitionResult(result);
	}
	return result;
    }

    private oval.schemas.results.core.CriterionType evaluateCriterion(CriterionType criterionDefinition)
		throws NoSuchElementException, OvalException {

	String testId = criterionDefinition.getTestRef();
	TestType testResult = results.getTest(testId);
	if (testResult == null) {
	    oval.schemas.definitions.core.TestType testDefinition = definitions.getTest(testId);
	    testResult = Factories.results.createTestType();
	    testResult.setTestId(testDefinition.getId());
	    testResult.setCheck(testDefinition.getCheck());
	    testResult.setCheckExistence(testDefinition.getCheckExistence());
	    testResult.setStateOperator(testDefinition.getStateOperator());

	    if (evalEnabled) {
		if (testDefinition instanceof UnknownTest) {
		    testResult.setResult(ResultEnumeration.UNKNOWN);
		} else {
		    evaluateTest(testResult);
		}
	    } else {
		testResult.setResult(ResultEnumeration.NOT_EVALUATED);
	    }

	    results.storeTestResult(testResult);
	}

	oval.schemas.results.core.CriterionType criterionResult = Factories.results.createCriterionType();
	criterionResult.setTestRef(testId);
	if (criterionDefinition.isSetNegate() && criterionDefinition.isNegate()) {
	    criterionResult.setNegate(true);
	    switch (testResult.getResult()) {
	      case TRUE:
		criterionResult.setResult(ResultEnumeration.FALSE);
		break;
	      case FALSE:
		criterionResult.setResult(ResultEnumeration.TRUE);
		break;
	      default:
		criterionResult.setResult(testResult.getResult());
		break;
	    }
	} else {
	    criterionResult.setResult(testResult.getResult());
	}
	return criterionResult;
    }

    private void evaluateTest(TestType testResult) throws NoSuchElementException, OvalException {
	String testId = testResult.getTestId();
	logger.debug(JOVALMsg.STATUS_TEST, testId);
	oval.schemas.definitions.core.TestType testDefinition = definitions.getTest(testId);
	String objectId = getObjectRef(testDefinition);

	if (!sc.containsObject(objectId)) {
	    switch(mode) {
	      //
	      // In EXHAUSTIVE mode all the objects have already been scanned, so, if the object is not found in the
	      // SystemCharacteristics, the test cannot be evaluated.
	      //
	      case EXHAUSTIVE:
		testResult.setResult(ResultEnumeration.NOT_EVALUATED);
		return;

	      default:
		scanObject(new RequestContext(this, definitions.getObject(objectId)));
		break;
	    }
	}

	String stateId = getStateRef(testDefinition);
	StateType state = null;
	if (stateId != null) {
	    state = definitions.getState(stateId);
	}

	//
	// Create all the structures we'll need to store information about the evaluation of the test.
	//
	RequestContext rc = new RequestContext(this, definitions.getObject(objectId));
	ExistenceData existence = new ExistenceData();
	CheckData check = new CheckData();
	switch(sc.getObject(objectId).getFlag()) {
	  case COMPLETE:
	    //
	    // If object flag == COMPLETE but there are no items, existenceResult will default to DOES_NOT_EXIST
	    // (which is, of course, exactly what we want to happen).
	    //
	  case INCOMPLETE:
	    for (ItemType item : sc.getItemsByObjectId(objectId)) {
		existence.addStatus(item.getStatus());

		TestedItemType testedItem = Factories.results.createTestedItemType();
		testedItem.setItemId(item.getId());
		testedItem.setResult(ResultEnumeration.NOT_EVALUATED);

		switch(item.getStatus()) {
		  case EXISTS:
		    if (state != null) {
			ResultEnumeration checkResult = ResultEnumeration.UNKNOWN;
			try {
			    checkResult = compare(state, item, rc);
			} catch (TestException e) {
			    logger.warn(JOVALMsg.ERROR_TESTEXCEPTION, testId, e.getMessage());
			    logger.debug(JOVALMsg.ERROR_EXCEPTION, e);

			    MessageType message = Factories.common.createMessageType();
			    message.setLevel(MessageLevelEnumeration.ERROR);
			    message.setValue(e.getMessage());
			    testedItem.getMessage().add(message);
			    checkResult = ResultEnumeration.ERROR;
			}
			testedItem.setResult(checkResult);
			check.addResult(checkResult);
		    }
		    break;

		  case DOES_NOT_EXIST:
		    check.addResult(ResultEnumeration.NOT_APPLICABLE);
		    break;
		  case ERROR:
		    check.addResult(ResultEnumeration.ERROR);
		    break;
		  case NOT_COLLECTED:
		    check.addResult(ResultEnumeration.NOT_EVALUATED);
		    break;
		}

		testResult.getTestedItem().add(testedItem);
	    }
	    break;

	  case DOES_NOT_EXIST:
	    existence.addStatus(StatusEnumeration.DOES_NOT_EXIST);
	    break;
	  case ERROR:
	    existence.addStatus(StatusEnumeration.ERROR);
	    break;
	  case NOT_APPLICABLE:
	    // No impact on existence check
	    break;
	  case NOT_COLLECTED:
	    existence.addStatus(StatusEnumeration.NOT_COLLECTED);
	    break;
	}

	//
	// Add all the tested variables that were resolved for the object and state (stored in the RequestContext).
	//
	for (VariableValueType var : rc.getVars()) {
	    TestedVariableType testedVariable = Factories.results.createTestedVariableType();
	    testedVariable.setVariableId(var.getVariableId());
	    testedVariable.setValue(var.getValue());
	    testResult.getTestedVariable().add(testedVariable);
	}

	//
	// DAS: Note that the NONE_EXIST check is deprecated as of 5.3, and will be eliminated in 6.0.
	// Per D. Haynes, in this case, any state and/or check should be ignored.
	//
	if (testDefinition.getCheck() == CheckEnumeration.NONE_EXIST) {
	    logger.warn(JOVALMsg.STATUS_CHECK_NONE_EXIST, testDefinition.getCheckExistence(), testId);
	    testResult.setResult(existence.getResult(ExistenceEnumeration.NONE_EXIST));

	//
	// If there are no items matching the object, or if there is no state for the test, then the result of the test is
	// simply the result of the existence check.
	//
	} else if (sc.getItemsByObjectId(objectId).size() == 0 || stateId == null) {
	    testResult.setResult(existence.getResult(testDefinition.getCheckExistence()));

	//
	// If there are items matching the object, then check the existence check, then (if successful) the check.
	//
	} else {
	    ResultEnumeration existenceResult = existence.getResult(testDefinition.getCheckExistence());
	    switch(existenceResult) {
	      case TRUE:
		testResult.setResult(check.getResult(testDefinition.getCheck()));
		break;

	      default:
		testResult.setResult(existenceResult);
		break;
	    }
	}
    }

    private oval.schemas.results.core.CriteriaType evaluateCriteria(CriteriaType criteriaDefinition)
		throws NoSuchElementException, OvalException {

	oval.schemas.results.core.CriteriaType criteriaResult = Factories.results.createCriteriaType();
	criteriaResult.setOperator(criteriaDefinition.getOperator());
	OperatorData operator = new OperatorData();
	for (Object child : criteriaDefinition.getCriteriaOrCriterionOrExtendDefinition()) {
	    Object resultObject = null;
	    if (child instanceof CriteriaType) {
		CriteriaType ctDefinition = (CriteriaType)child;
		oval.schemas.results.core.CriteriaType ctResult = evaluateCriteria(ctDefinition);
		operator.addResult(ctResult.getResult());
		resultObject = ctResult;
	    } else if (child instanceof CriterionType) {
		CriterionType ctDefinition = (CriterionType)child;
		oval.schemas.results.core.CriterionType ctResult = evaluateCriterion(ctDefinition);
		operator.addResult(ctResult.getResult());
		resultObject = ctResult;
	    } else if (child instanceof ExtendDefinitionType) {
		ExtendDefinitionType edtDefinition = (ExtendDefinitionType)child;
		String defId = edtDefinition.getDefinitionRef();
		DefinitionType defDefinition = definitions.getDefinition(defId);
		oval.schemas.results.core.DefinitionType defResult = evaluateDefinition(defDefinition);
		oval.schemas.results.core.ExtendDefinitionType edtResult;
		edtResult = Factories.results.createExtendDefinitionType();
		edtResult.setDefinitionRef(defId);
		edtResult.setVersion(defDefinition.getVersion());
		if (edtDefinition.isSetNegate() && edtDefinition.isNegate()) {
		    edtResult.setNegate(true);
		    edtResult.setResult(defResult.getResult()); // Overridden for true and false, below
		    switch(defResult.getResult()) {
		      case TRUE:
			edtResult.setResult(ResultEnumeration.FALSE);
			break;
		      case FALSE:
			edtResult.setResult(ResultEnumeration.TRUE);
			break;
		    }
		} else {
		    edtResult.setResult(defResult.getResult());
		}
		operator.addResult(edtResult.getResult());
		resultObject = edtResult;
	    } else {
		throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_BAD_COMPONENT, child.getClass().getName()));
	    }
	    criteriaResult.getCriteriaOrCriterionOrExtendDefinition().add(resultObject);
	}

	ResultEnumeration result = operator.getResult(criteriaDefinition.getOperator());
	if (criteriaDefinition.isSetNegate() && criteriaDefinition.isNegate()) {
	    criteriaResult.setNegate(true);
	    if (result == ResultEnumeration.TRUE) {
		result = ResultEnumeration.FALSE;
	    } else if (result == ResultEnumeration.FALSE) {
		result = ResultEnumeration.TRUE;
	    }
	}
	criteriaResult.setResult(result);
	return criteriaResult;
    }

    private ResultEnumeration compare(StateType state, ItemType item, RequestContext rc) throws OvalException, TestException {
	//
	// As the lowest level analysis operation, this is a good place to check if the engine is being destroyed.
	//
	if (abort) {
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_ABORT));
	}

	try {
	    OperatorData result = new OperatorData();
	    for (String methodName : getMethodNames(state.getClass())) {
		if (methodName.startsWith("get") && !stateBaseMethodNames.contains(methodName)) {
		    Object stateEntityObj = state.getClass().getMethod(methodName).invoke(state);
		    if (stateEntityObj == null) {
			// continue
		    } else if (stateEntityObj instanceof EntityStateSimpleBaseType) {
			EntityStateSimpleBaseType stateEntity = (EntityStateSimpleBaseType)stateEntityObj;
			Object itemEntityObj = item.getClass().getMethod(methodName).invoke(item);
			if (itemEntityObj instanceof EntityItemSimpleBaseType || itemEntityObj == null) {
			    result.addResult(compare(stateEntity, (EntityItemSimpleBaseType)itemEntityObj, rc));
			} else if (itemEntityObj instanceof JAXBElement) {
			    JAXBElement element = (JAXBElement)itemEntityObj;
			    EntityItemSimpleBaseType itemEntity = (EntityItemSimpleBaseType)element.getValue();
			    result.addResult(compare(stateEntity, itemEntity, rc));
			} else if (itemEntityObj instanceof Collection) {
			    CheckData cd = new CheckData();
			    for (Object entityObj : (Collection)itemEntityObj) {
				EntityItemSimpleBaseType itemEntity = (EntityItemSimpleBaseType)entityObj;
				cd.addResult(compare(stateEntity, itemEntity, rc));
			    }
			    result.addResult(cd.getResult(stateEntity.getEntityCheck()));
			} else {
			    String message = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_ENTITY,
								 itemEntityObj.getClass().getName(), item.getId());
	    		    throw new OvalException(message);
			}
		    } else if (stateEntityObj instanceof EntityStateRecordType) {
			EntityStateRecordType stateEntity = (EntityStateRecordType)stateEntityObj;
			Object itemEntityObj = item.getClass().getMethod(methodName).invoke(item);
			if (itemEntityObj instanceof EntityItemRecordType) {
			    result.addResult(compare(stateEntity, (EntityItemRecordType)itemEntityObj, rc));
			} else if (itemEntityObj instanceof Collection) {
			    CheckData cd = new CheckData();
			    for (Object entityObj : (Collection)itemEntityObj) {
				if (entityObj instanceof EntityItemRecordType) {
				    cd.addResult(compare(stateEntity, (EntityItemRecordType)entityObj, rc));
				} else {
				    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_ENTITY,
								     entityObj.getClass().getName(), item.getId());
				    throw new OvalException(msg);
				}
			    }
			    result.addResult(cd.getResult(stateEntity.getEntityCheck()));
			} else {
			    String message = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_ENTITY,
								 itemEntityObj.getClass().getName(), item.getId());
	    		    throw new OvalException(message);
			}
		    } else {
			String message = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_ENTITY,
							     item.getClass().getName(), item.getId());
	    		throw new OvalException(message);
		    }
		}
	    }
	    return result.getResult(state.getOperator());
	} catch (NoSuchMethodException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), state.getId()));
	} catch (IllegalAccessException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), state.getId()));
	} catch (InvocationTargetException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), state.getId()));
	}
    }

    /**
     * Compare a state and item record.  All fields must match for a TRUE result.
     *
     * See:
     * http://oval.mitre.org/language/version5.10/ovaldefinition/documentation/oval-definitions-schema.html#EntityStateRecordType
     */
    private ResultEnumeration compare(EntityStateRecordType stateRecord, EntityItemRecordType itemRecord, RequestContext rc)
	    throws OvalException, TestException {

	ResultEnumeration result = ResultEnumeration.UNKNOWN;

	for (EntityStateFieldType stateField : stateRecord.getField()) {
	    EntityStateSimpleBaseType state = null;
	    EntityItemSimpleBaseType item = null;

	    for (EntityItemFieldType itemField : itemRecord.getField()) {
		if (itemField.getName().equals(stateField.getName())) {
		    state = new StateFieldBridge(stateField);
		    item = new ItemFieldBridge(itemField);
		    break;
		}
	    }
	    if (item == null) {
		return ResultEnumeration.FALSE;
	    } else {
		result = compare(state, item, rc);
		switch(result) {
		  case TRUE:
		    break;

		  default:
		    return result;
		}
	    }
	}

	return result;
    }

    private static List<String> objectBaseMethodNames = getMethodNames(ObjectType.class);
    static {
	objectBaseMethodNames.add("getBehaviors");
	objectBaseMethodNames.add("getFilter");
	objectBaseMethodNames.add("getSet");
    }
    private static List<String> stateBaseMethodNames = getMethodNames(StateType.class);
    private static List<String> itemBaseMethodNames = getMethodNames(ItemType.class);

    private static List<String> getMethodNames(Class clazz) {
	List<String> names = new Vector<String>();
	Method[] methods = clazz.getMethods();
	for (int i=0; i < methods.length; i++) {
	    names.add(methods[i].getName());
	}
	return names;
    }

    /**
     * Compare a state SimpleBaseType to an item SimpleBaseType.  If the item is null, this method returns false.  That
     * allows callers to simply check if the state is set before invoking the comparison.
     */
    private ResultEnumeration compare(EntityStateSimpleBaseType state, EntityItemSimpleBaseType item, RequestContext rc)
		throws TestException, OvalException {
	if (item == null) {
	    return ResultEnumeration.NOT_APPLICABLE;
	} else {
	    switch(item.getStatus()) {
	      case NOT_COLLECTED:
		return ResultEnumeration.NOT_EVALUATED;

	      case ERROR:
		return ResultEnumeration.ERROR;

	      case DOES_NOT_EXIST:
		return ResultEnumeration.FALSE;
	    }
	}

	//
	// Check datatype compatibility; anything can be compared with a string.
	//
	SimpleDatatypeEnumeration stateDT = getDatatype(state.getDatatype());
	SimpleDatatypeEnumeration itemDT =  getDatatype(item.getDatatype());
	if (itemDT != stateDT) {
	    if (itemDT != SimpleDatatypeEnumeration.STRING && stateDT != SimpleDatatypeEnumeration.STRING) {
		throw new TestException(JOVALMsg.getMessage(JOVALMsg.ERROR_DATATYPE_MISMATCH, stateDT, itemDT));
	    }
	}

	//
	// Handle the variable_ref case
	//
	if (state.isSetVarRef()) {
	    CheckData cd = new CheckData();
	    EntitySimpleBaseType base = Factories.definitions.core.createEntityObjectAnySimpleType();
	    base.setDatatype(state.getDatatype());
	    base.setOperation(state.getOperation());
	    base.setMask(state.isMask());
	    try {
		Collection<TypedData> values = resolve(state.getVarRef(), rc);
		if (values.size() == 0) {
		    String reason = JOVALMsg.getMessage(JOVALMsg.ERROR_VARIABLE_NO_VALUES);
		    throw new TestException(JOVALMsg.getMessage(JOVALMsg.ERROR_RESOLVE_VAR, state.getVarRef(), reason));
		} else {
		    //
		    // DAS: Expand type-casting logic
		    //
		    for (TypedData value : values) {
			switch(TypedData.getSimpleDatatype(state.getDatatype())) {
			  case STRING:
			  default:
			    base.setValue(value.getString());
			    cd.addResult(testImpl(base, item));
			    break;
			}
		    }
		}
	    } catch (NoSuchElementException e) {
		String reason = JOVALMsg.getMessage(JOVALMsg.ERROR_VARIABLE_MISSING);
		throw new TestException(JOVALMsg.getMessage(JOVALMsg.ERROR_RESOLVE_VAR, state.getVarRef(), reason));
	    } catch (ResolveException e) {
		throw new TestException(JOVALMsg.getMessage(JOVALMsg.ERROR_RESOLVE_VAR, state.getVarRef(), e.getMessage()));
	    }
	    return cd.getResult(state.getVarCheck());
	} else {
	    return testImpl(state, item);
	}
    }

    /**
     * Perform the the OVAL test by comparing the state and item.
     *
     * @see http://oval.mitre.org/language/version5.10/ovaldefinition/documentation/oval-common-schema.html#OperationEnumeration
     */
    ResultEnumeration testImpl(EntitySimpleBaseType state, EntityItemSimpleBaseType item) throws TestException, OvalException {
	if (!item.isSetValue() || !state.isSetValue()) {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_TEST_INCOMPARABLE, item.getValue(), state.getValue());
	    throw new TestException(msg);
	}

	switch (state.getOperation()) {
	  case BITWISE_AND:
	    if (bitwiseAnd(state, item)) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }

	  case BITWISE_OR:
	    if (bitwiseOr(state, item)) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }

	  case CASE_INSENSITIVE_EQUALS:
	    if (equalsIgnoreCase(state, item)) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }

	  case CASE_INSENSITIVE_NOT_EQUAL:
	    if (equalsIgnoreCase(state, item)) {
		return ResultEnumeration.FALSE;
	    } else {
		return ResultEnumeration.TRUE;
	    }

	  case PATTERN_MATCH: // Always treat as Strings
	    try {
		if (item.getValue() == null) {
		    return ResultEnumeration.FALSE;
		} else if (Pattern.compile((String)state.getValue()).matcher((String)item.getValue()).find()) {
		    return ResultEnumeration.TRUE;
		} else {
		    return ResultEnumeration.FALSE;
		}
	    } catch (PatternSyntaxException e) {
		throw new TestException(e);
	    }

	  case EQUALS:
	    if (compareValues(item, state) == 0) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }

	  case NOT_EQUAL:
	    if (compareValues(item, state) != 0) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }

	  case GREATER_THAN_OR_EQUAL:
	    if (compareValues(item, state) >= 0) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }

	  case GREATER_THAN:
	    if (compareValues(item, state) > 0) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }

	  case LESS_THAN_OR_EQUAL:
	    if (compareValues(item, state) <= 0) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }

	  case LESS_THAN:
	    if (compareValues(item, state) < 0) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }

	  case SUBSET_OF:
	    if (subsetOf(item, state)) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }

	  case SUPERSET_OF:
	    if (subsetOf(item, state)) {
		return ResultEnumeration.FALSE;
	    } else {
		return ResultEnumeration.TRUE;
	    }

	  default:
	    throw new TestException(JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, state.getOperation()));
	}
    }

    /**
     * Perform the equivalent of item.getValue().compareTo(state.getValue()).  Both item and state should have already been
     * checked to insure they have (non-null) values.
     */
    int compareValues(EntityItemSimpleBaseType item, EntitySimpleBaseType state) throws TestException, OvalException {
	String itemStr = (String)item.getValue();
	String stateStr = (String)state.getValue();

	switch(getDatatype(state.getDatatype())) {
	  case INT:
	    try {
		return new BigInteger(itemStr.trim()).compareTo(new BigInteger((String)state.getValue()));
	    } catch (NumberFormatException e) {
		throw new TestException(e);
	    }

	  case FLOAT:
	    try {
		return getFloat(itemStr).compareTo(getFloat(stateStr));
	    } catch (NumberFormatException e) {
		throw new TestException(e);
	    }

	  case BOOLEAN:
	    if (getBoolean(itemStr.trim()) == getBoolean(stateStr.trim())) {
		return 0;
	    } else {
		return 1;
	    }

	  case VERSION:
	    try {
		return new Version(itemStr.trim()).compareTo(new Version(stateStr.trim()));
	    } catch (IllegalArgumentException e) {
		throw new TestException(e);
	    }

	  case FILESET_REVISION:
	  case IOS_VERSION:
	  case EVR_STRING:
	    try {
		return new EvrString(itemStr.trim()).compareTo(new EvrString(stateStr.trim()));
	    } catch (IllegalArgumentException e) {
		throw new TestException(e);
	    }

	  case BINARY:
	  case STRING:
	    return (itemStr).compareTo(stateStr);

	  case IPV_4_ADDRESS:
	  case IPV_6_ADDRESS:
	    try {
		return new IpAddress(itemStr.trim()).compareTo(new IpAddress(stateStr.trim()));
	    } catch (IllegalArgumentException e) {
		throw new TestException(e);
	    }

	  default:
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_OPERATION_DATATYPE,
							state.getDatatype(), OperationEnumeration.EQUALS));
	}
    }

    boolean equalsIgnoreCase(EntitySimpleBaseType state, EntityItemSimpleBaseType item) throws TestException, OvalException {
	switch(getDatatype(state.getDatatype())) {
	  case STRING:
	    return ((String)state.getValue()).equalsIgnoreCase((String)item.getValue());

	  default:
	    throw new TestException(JOVALMsg.getMessage(JOVALMsg.ERROR_OPERATION_DATATYPE,
							state.getDatatype(), OperationEnumeration.CASE_INSENSITIVE_EQUALS));
	}
    }

    boolean bitwiseAnd(EntitySimpleBaseType state, EntityItemSimpleBaseType item) throws TestException, OvalException {
	switch(getDatatype(state.getDatatype())) {
	  case INT:
	    try {
		int sInt = Integer.parseInt((String)state.getValue());
		int iInt = Integer.parseInt((String)item.getValue());
		return sInt == (sInt & iInt);
	    } catch (NumberFormatException e) {
		throw new TestException(e);
	    }

	  default:
	    throw new TestException(JOVALMsg.getMessage(JOVALMsg.ERROR_OPERATION_DATATYPE,
							state.getDatatype(), OperationEnumeration.BITWISE_AND));
	}
    }

    boolean bitwiseOr(EntitySimpleBaseType state, EntityItemSimpleBaseType item) throws TestException, OvalException {
	switch(getDatatype(state.getDatatype())) {
	  case INT:
	    try {
		int sInt = Integer.parseInt((String)state.getValue());
		int iInt = Integer.parseInt((String)item.getValue());
		return sInt == (sInt | iInt);
	    } catch (NumberFormatException e) {
		throw new TestException(e);
	    }

	  default:
	    throw new TestException(JOVALMsg.getMessage(JOVALMsg.ERROR_OPERATION_DATATYPE,
							state.getDatatype(), OperationEnumeration.BITWISE_OR));
	}
    }

    boolean subsetOf(EntityItemSimpleBaseType item, EntitySimpleBaseType state) throws TestException, OvalException {
	switch(getDatatype(state.getDatatype())) {
	  case IPV_4_ADDRESS:
	  case IPV_6_ADDRESS:
	    try {
		IpAddress sIp = new IpAddress((String)state.getValue());
		IpAddress iIp = new IpAddress((String)item.getValue());
		return iIp.contains(sIp);
	    } catch (IllegalArgumentException e) {
		throw new TestException(e);
	    }

	  default:
	    throw new TestException(JOVALMsg.getMessage(JOVALMsg.ERROR_OPERATION_DATATYPE,
							state.getDatatype(), OperationEnumeration.SUBSET_OF));
	}
    }

    /**
     * Convert the datatype String into a SimpleDatatypeEnumeration for use in switches.
     */
    SimpleDatatypeEnumeration getDatatype(String s) throws OvalException {
	if ("binary".equals(s)) {
	    return SimpleDatatypeEnumeration.BINARY;
	} else if ("boolean".equals(s)) {
	    return SimpleDatatypeEnumeration.BOOLEAN;
	} else if ("evr_string".equals(s)) {
	    return SimpleDatatypeEnumeration.EVR_STRING;
	} else if ("fileset_revision".equals(s)) {
	    return SimpleDatatypeEnumeration.FILESET_REVISION;
	} else if ("float".equals(s)) {
	    return SimpleDatatypeEnumeration.FLOAT;
	} else if ("int".equals(s)) {
	    return SimpleDatatypeEnumeration.INT;
	} else if ("ios_version".equals(s)) {
	    return SimpleDatatypeEnumeration.IOS_VERSION;
	} else if ("ipv4_address".equals(s)) {
	    return SimpleDatatypeEnumeration.IPV_4_ADDRESS;
	} else if ("ipv6_address".equals(s)) {
	    return SimpleDatatypeEnumeration.IPV_6_ADDRESS;
	} else if ("string".equals(s)) {
	    return SimpleDatatypeEnumeration.STRING;
	} else if ("version".equals(s)) {
	    return SimpleDatatypeEnumeration.VERSION;
	} else {
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_DATATYPE, s));
	}
    }

    Float getFloat(String s) throws NumberFormatException {
	s = s.trim();
	if (s.equals("NaN")) {
	    return new Float(Float.NaN);
	} else if (s.equals("INF")) {
	    return new Float(Float.POSITIVE_INFINITY);
	} else if (s.equals("-INF")) {
	    return new Float(Float.NEGATIVE_INFINITY);
	} else {
	    return new Float(s);
	}
    }

    boolean getBoolean(String s) {
	if (s == null) {
	    return false;
	} else if (s.equalsIgnoreCase("true")) {
	    return true;
	} else if (s.equals("0")) {
	    return false;
	} else if (s.equalsIgnoreCase("false")) {
	    return false;
	} else if (s.length() == 0) {
	    return false;
	} else {
	    return true;
	}
    }

    /**
     * Resolve the component.  If the component is a variable, resolve it and append it to the list, appending its value to
     * the value that is ultimately returned.
     *
     * @see http://oval.mitre.org/language/version5.10/ovaldefinition/documentation/oval-definitions-schema.html#FunctionGroup
     */
    private Collection<TypedData> resolveInternal(Object object, RequestContext rc)
		throws NoSuchElementException, ResolveException, OvalException {
	//
	// Why do variables point to variables?  Because sometimes they are nested.
	//
	if (object instanceof LocalVariable) {
	    LocalVariable localVariable = (LocalVariable)object;
	    Collection<TypedData> values = resolveInternal(getComponent(localVariable), rc);
	    if (values.size() == 0) {
		VariableValueType variableValueType = Factories.sc.core.createVariableValueType();
		variableValueType.setVariableId(localVariable.getId());
		rc.addVar(variableValueType);
	    } else {
		for (TypedData value : values) {
		    if (TypedData.Type.SIMPLE == value.getType()) {
			VariableValueType variableValueType = Factories.sc.core.createVariableValueType();
			variableValueType.setVariableId(localVariable.getId());
			variableValueType.setValue(value.getString());
			rc.addVar(variableValueType);
		    }
		}
	    }
	    return values;

	//
	// Add an externally-defined variable.
	//
	} else if (object instanceof ExternalVariable) {
	    ExternalVariable externalVariable = (ExternalVariable)object;
	    String id = externalVariable.getId();
	    if (externalVariables == null) {
		throw new ResolveException(JOVALMsg.getMessage(JOVALMsg.ERROR_EXTERNAL_VARIABLE_SOURCE, id));
	    } else {
		Collection<TypedData> values = new Vector<TypedData>();
		for (IVariables.Typed t : externalVariables.getValue(id)) {
		    values.add(new TypedData(t.getDatatype(), t.getValue()));
		}
		if (values.size() == 0) {
		    VariableValueType variableValueType = Factories.sc.core.createVariableValueType();
		    variableValueType.setVariableId(externalVariable.getId());
		    rc.addVar(variableValueType);
		} else {
		    for (TypedData value : values) {
			if (TypedData.Type.SIMPLE == value.getType()) {
			    VariableValueType variableValueType = Factories.sc.core.createVariableValueType();
			    variableValueType.setVariableId(externalVariable.getId());
			    variableValueType.setValue(value);
			    rc.addVar(variableValueType);
			}
		    }
		}
		return values;
	    }

	//
	// Add a constant variable.
	//
	} else if (object instanceof ConstantVariable) {
	    ConstantVariable constantVariable = (ConstantVariable)object;
	    String id = constantVariable.getId();
	    Collection<TypedData> values = new Vector<TypedData>();
	    List<ValueType> valueTypes = constantVariable.getValue();
	    if (valueTypes.size() == 0) {
		VariableValueType variableValueType = Factories.sc.core.createVariableValueType();
		variableValueType.setVariableId(constantVariable.getId());
		rc.addVar(variableValueType);
	    } else {
		for (ValueType value : valueTypes) {
		    VariableValueType variableValueType = Factories.sc.core.createVariableValueType();
		    variableValueType.setVariableId(id);
		    String s = (String)value.getValue();
		    variableValueType.setValue(s);
		    rc.addVar(variableValueType);
		    values.add(new TypedData(s));
		}
	    }
	    return values;

	//
	// Add a static (literal) value.
	//
	} else if (object instanceof LiteralComponentType) {
	    LiteralComponentType literal = (LiteralComponentType)object;
	    Collection<TypedData> values = new Vector<TypedData>();
	    values.add(new TypedData(literal));
	    return values;

	//
	// Retrieve from an ItemType (which possibly has to be fetched from an adapter)
	//
	} else if (object instanceof ObjectComponentType) {
	    ObjectComponentType oc = (ObjectComponentType)object;
	    String objectId = oc.getObjectRef();
	    Collection<ItemType> items = null;
	    try {
		//
		// First, we scan the SystemCharacteristics for items related to the object.
		//
		items = sc.getItemsByObjectId(objectId);
	    } catch (NoSuchElementException e) {
		//
		// If the object has not yet been scanned, then it must be retrieved live from the adapter.
		//
		rc.pushObject(definitions.getObject(objectId));
		items = scanObject(rc);
		rc.popObject();
	    }
	    return extractItemData(objectId, oc, items);

	//
	// Resolve and return.
	//
	} else if (object instanceof VariableComponentType) {
	    return resolveInternal(definitions.getVariable(((VariableComponentType)object).getVarRef()), rc);

	//
	// Resolve and concatenate child components.
	//
	} else if (object instanceof ConcatFunctionType) {
	    Collection<TypedData> values = new Vector<TypedData>();
	    ConcatFunctionType concat = (ConcatFunctionType)object;
	    for (Object child : concat.getObjectComponentOrVariableComponentOrLiteralComponent()) {
		Collection<TypedData> next = resolveInternal(child, rc);
		if (next.size() == 0) {
		    @SuppressWarnings("unchecked")
		    Collection<TypedData> empty = (Collection<TypedData>)Collections.EMPTY_LIST;
		    return empty;
		} else if (values.size() == 0) {
		    values.addAll(next);
		} else {
		    Collection<TypedData> newValues = new Vector<TypedData>();
		    for (TypedData base : values) {
			for (TypedData val : next) {
			    newValues.add(new TypedData(base.getString() + val.getString()));
			}
		    }
		    values = newValues;
		}
	    }
	    return values;

	//
	// Escape anything that could be pattern-matched.
	//
	} else if (object instanceof EscapeRegexFunctionType) {
	    Collection<TypedData> values = new Vector<TypedData>();
	    for (TypedData value : resolveInternal(getComponent((EscapeRegexFunctionType)object), rc)) {
		values.add(new TypedData(StringTools.escapeRegex(value.getString())));
	    }
	    return values;

	//
	// Process a Split, which contains a component and a delimiter with which to split it up.
	//
	} else if (object instanceof SplitFunctionType) {
	    SplitFunctionType split = (SplitFunctionType)object;
	    Collection<TypedData> values = new Vector<TypedData>();
	    for (TypedData value : resolveInternal(getComponent(split), rc)) {
		for (String s : StringTools.toList(StringTools.tokenize(value.getString(), split.getDelimiter(), false))) {
		    values.add(new TypedData(s));
		}
	    }
	    return values;

	//
	// Process a RegexCapture, which returns the regions of a component resolved as a String that match the first
	// subexpression in the given pattern.
	//
	} else if (object instanceof RegexCaptureFunctionType) {
	    RegexCaptureFunctionType regexCapture = (RegexCaptureFunctionType)object;
	    Pattern p = Pattern.compile(regexCapture.getPattern());
	    Collection<TypedData> values = new Vector<TypedData>();
	    for (TypedData value : resolveInternal(getComponent(regexCapture), rc)) {
		Matcher m = p.matcher(value.getString());
		if (m.groupCount() > 0) {
		    if (m.find()) {
			values.add(new TypedData(m.group(1)));
		    } else {
			values.add(TypedData.EMPTY_STRING);
		    }
		} else {
		    values.add(TypedData.EMPTY_STRING);
		}
	    }
	    return values;

	//
	// Process a Substring
	//
	} else if (object instanceof SubstringFunctionType) {
	    SubstringFunctionType st = (SubstringFunctionType)object;
	    int start = st.getSubstringStart();
	    start = Math.max(1, start); // a start index < 1 means start at 1
	    start--;			// OVAL counter begins at 1 instead of 0
	    int len = st.getSubstringLength();
	    Collection<TypedData> values = new Vector<TypedData>();
	    for (TypedData value : resolveInternal(getComponent(st), rc)) {
		String str = value.getString();

		//
		// If the substring_start attribute has value greater than the length of the original string
		// an error should be reported.
		//
		if (start > str.length()) {
		    throw new ResolveException(JOVALMsg.getMessage(JOVALMsg.ERROR_SUBSTRING, str, new Integer(start)));

		//
		// A substring_length value greater than the actual length of the string, or a negative value,
		// means to include all of the characters after the starting character.
		//
		} else if (len < 0 || str.length() <= (start+len)) {
		    values.add(new TypedData(str.substring(start)));

		} else {
		    values.add(new TypedData(str.substring(start, start+len)));
		}
	    }
	    return values;

	//
	// Process a Begin
	//
	} else if (object instanceof BeginFunctionType) {
	    BeginFunctionType bt = (BeginFunctionType)object;
	    String s = bt.getCharacter();
	    Collection<TypedData> values = new Vector<TypedData>();
	    for (TypedData value : resolveInternal(getComponent(bt), rc)) {
		String str = value.getString();
		if (str.startsWith(s)) {
		    values.add(value);
		} else {
		    values.add(new TypedData(s + str));
		}
	    }
	    return values;

	//
	// Process an End
	//
	} else if (object instanceof EndFunctionType) {
	    EndFunctionType et = (EndFunctionType)object;
	    String s = et.getCharacter();
	    Collection<TypedData> values = new Vector<TypedData>();
	    for (TypedData value : resolveInternal(getComponent(et), rc)) {
		String str = value.getString();
		if (str.endsWith(s)) {
		    values.add(value);
		} else {
		    values.add(new TypedData(str + s));
		}
	    }
	    return values;

	//
	// Process a TimeDifference
	//
	} else if (object instanceof TimeDifferenceFunctionType) {
	    TimeDifferenceFunctionType tt = (TimeDifferenceFunctionType)object;
	    Collection<TypedData> values = new Vector<TypedData>();
	    List<Object> children = tt.getObjectComponentOrVariableComponentOrLiteralComponent();
	    Collection<TypedData> ts1;
	    Collection<TypedData> ts2;
	    if (children.size() == 1) {
		ts1 = new Vector<TypedData>();
		ts1.add(new TypedData(new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(new Date(System.currentTimeMillis()))));
		ts2 = resolveInternal(children.get(0), rc);
	    } else if (children.size() == 2) {
		ts1 = resolveInternal(children.get(0), rc);
		ts2 = resolveInternal(children.get(1), rc);
	    } else {
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_BAD_TIMEDIFFERENCE, Integer.toString(children.size()));
		throw new ResolveException(msg);
	    }
	    for (TypedData time1 : ts1) {
		try {
		    long tm1 = DateTime.getTime(time1.getString(), tt.getFormat1());
		    for (TypedData time2 : ts2) {
			long tm2 = DateTime.getTime(time2.getString(), tt.getFormat2());
			long diff = (tm1 - tm2)/1000L; // convert diff to seconds
			values.add(new TypedData(diff));
		    }
		} catch (IllegalArgumentException e) {
		    throw new ResolveException(e.getMessage());
		} catch (ParseException e) {
		    throw new ResolveException(e.getMessage());
		}
	    }
	    return values;

	//
	// Process Arithmetic
	//
	} else if (object instanceof ArithmeticFunctionType) {
	    ArithmeticFunctionType at = (ArithmeticFunctionType)object;
	    Stack<Collection<TypedData>> rows = new Stack<Collection<TypedData>>();
	    ArithmeticEnumeration op = at.getArithmeticOperation();
	    for (Object child : at.getObjectComponentOrVariableComponentOrLiteralComponent()) {
		Collection<TypedData> row = new Vector<TypedData>();
		for (TypedData cell : resolveInternal(child, rc)) {
		    row.add(cell);
		}
		rows.add(row);
	    }
	    return computeProduct(op, rows);

	//
	// Process Count
	//
	} else if (object instanceof CountFunctionType) {
	    CountFunctionType ct = (CountFunctionType)object;
	    Collection<TypedData> children = new Vector<TypedData>();
	    for (Object child : ct.getObjectComponentOrVariableComponentOrLiteralComponent()) {
		children.addAll(resolveInternal(child, rc));
	    }
	    Collection<TypedData> values = new Vector<TypedData>();
	    values.add(new TypedData(children.size()));
	    return values;

	//
	// Process Unique
	//
	} else if (object instanceof UniqueFunctionType) {
	    UniqueFunctionType ut = (UniqueFunctionType)object;
	    HashSet<TypedData> values = new HashSet<TypedData>();
	    for (Object child : ut.getObjectComponentOrVariableComponentOrLiteralComponent()) {
		values.addAll(resolveInternal(child, rc));
	    }
	    return values;

	} else {
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_COMPONENT, object.getClass().getName()));
	}
    }

    /**
     * Perform the Arithmetic operation on permutations of the Stack, and return the resulting permutations.
     */
    private List<TypedData> computeProduct(ArithmeticEnumeration op, Stack<Collection<TypedData>> rows) {
	List<TypedData> results = new Vector<TypedData>();
	if (rows.empty()) {
	    switch(op) {
		case ADD:
		  results.add(new TypedData(0));
		  break;
		case MULTIPLY:
		  results.add(new TypedData(1));
		  break;
	    }
	} else {
	    for (TypedData type : rows.pop()) {
		String value = type.getString();
		Stack<Collection<TypedData>> copy = new Stack<Collection<TypedData>>();
		copy.addAll(rows);
		for (TypedData otherType : computeProduct(op, copy)) {
		    String otherValue = otherType.getString();
		    switch(op) {
		      case ADD:
			if (value.indexOf(".") == -1 && otherValue.indexOf(".") == -1) {
			    results.add(new TypedData(new BigInteger(value).add(new BigInteger(otherValue))));
			} else {
			    results.add(new TypedData(new BigDecimal(value).add(new BigDecimal(otherValue))));
			}
			break;

		      case MULTIPLY:
			if (value.indexOf(".") == -1 && otherValue.indexOf(".") == -1) {
			    results.add(new TypedData(new BigInteger(value).multiply(new BigInteger(otherValue))));
			} else {
			    results.add(new TypedData(new BigDecimal(value).multiply(new BigDecimal(otherValue))));
			}
			break;
		    }
		}
	    }
	}
	return results;
    }

    /**
     * The final step in resolving an object reference variable's value is extracting the item field or record from the items
     * associated with that ObjectType, which is the function of this method.
     */
    private List<TypedData> extractItemData(String objectId, ObjectComponentType oc, Collection list)
		throws OvalException, ResolveException, NoSuchElementException {

	List<TypedData> values = new Vector<TypedData>();
	for (Object o : list) {
	    if (o instanceof ItemType) {
		String fieldName = oc.getItemField();
		try {
		    ItemType item = (ItemType)o;
		    String methodName = getAccessorMethodName(fieldName);
		    Method method = item.getClass().getMethod(methodName);
		    o = method.invoke(item);
		} catch (NoSuchMethodException e) {
		    //
		    // The specification indicates that an object_component must have an error flag in this case.
		    //
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_RESOLVE_ITEM_FIELD, fieldName, o.getClass().getName());
		    throw new ResolveException(msg);
		} catch (IllegalAccessException e) {
		    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		    return null;
		} catch (InvocationTargetException e) {
		    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		    return null;
		}
	    }

	    if (o instanceof JAXBElement) {
		o = ((JAXBElement)o).getValue();
	    }
	    if (o instanceof EntityItemSimpleBaseType) {
		try {
		    values.add(new TypedData((EntityItemSimpleBaseType)o));
		} catch (IllegalArgumentException e) {
		    throw new ResolveException(e);
		}
	    } else if (o instanceof List) {
		// DAS: does this ever happen??
		return extractItemData(objectId, null, (List)o);
	    } else if (o instanceof EntityItemRecordType) {
		EntityItemRecordType record = (EntityItemRecordType)o;
		if (oc.isSetRecordField()) {
		    String fieldName = oc.getRecordField();
		    for (EntityItemFieldType field : record.getField()) {
			try {
			    values.add(new TypedData(field));
			} catch (IllegalArgumentException e) {
			    throw new ResolveException(e);
			}
		    }
		} else {
		    //
		    // In this case, the variable points directly to the record without specifying a field. This only
		    // makes sense in the context of a CountFunctionType, in which case a null value is safe.
		    //
		    values.add(new TypedData(record));
		}
	    } else {
		throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, o.getClass().getName(), objectId));
	    }
	}
	return values;
    }

    /**
     * Given the name of an XML node, guess the name of the accessor field that JAXB would generate.
     * For example, field_name -> getFieldName.
     */
    private String getAccessorMethodName(String fieldName) {
	StringTokenizer tok = new StringTokenizer(fieldName, "_");
	StringBuffer sb = new StringBuffer("get");
	while(tok.hasMoreTokens()) {
	    try {
		byte[] ba = tok.nextToken().toLowerCase().getBytes("US-ASCII");
		if (97 <= ba[0] && ba[0] <= 122) {
		    ba[0] -= 32; // Capitalize the first letter.
		}
		sb.append(new String(ba, Charset.forName("US-ASCII")));
	    } catch (UnsupportedEncodingException e) {
	    }
	}
	return sb.toString();
    }

    /**
     * Use reflection to get the child component.
     */
    private Object getComponent(Object unknown) throws OvalException {
	Object obj = safeInvokeMethod(unknown, "getArithmetic");
	if (obj != null) {
	    return obj;
	}
	obj = safeInvokeMethod(unknown, "getBegin");
	if (obj != null) {
	    return obj;
	}
	obj = safeInvokeMethod(unknown, "getCount");
	if (obj != null) {
	    return obj;
	}
	obj = safeInvokeMethod(unknown, "getConcat");
	if (obj != null) {
	    return obj;
	}
	obj = safeInvokeMethod(unknown, "getEnd");
	if (obj != null) {
	    return obj;
	}
	obj = safeInvokeMethod(unknown, "getEscapeRegex");
	if (obj != null) {
	    return obj;
	}
	obj = safeInvokeMethod(unknown, "getLiteralComponent");
	if (obj != null) {
	    return obj;
	}
	obj = safeInvokeMethod(unknown, "getObjectComponent");
	if (obj != null) {
	    return obj;
	}
	obj = safeInvokeMethod(unknown, "getRegexCapture");
	if (obj != null) {
	    return obj;
	}
	obj = safeInvokeMethod(unknown, "getSplit");
	if (obj != null) {
	    return obj;
	}
	obj = safeInvokeMethod(unknown, "getSubstring");
	if (obj != null) {
	    return obj;
	}
	obj = safeInvokeMethod(unknown, "getTimeDifference");
	if (obj != null) {
	    return obj;
	}
	obj = safeInvokeMethod(unknown, "getUnique");
	if (obj != null) {
	    return obj;
	}
	obj = safeInvokeMethod(unknown, "getVariableComponent");
	if (obj != null) {
	    return obj;
	}
	obj = safeInvokeMethod(unknown, "getObjectComponentOrVariableComponentOrLiteralComponent");
	if (obj != null) {
	    return obj;
	}

	throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_COMPONENT, unknown.getClass().getName()));
    }

    /**
     * Safely invoke a method that takes no arguments and returns an Object.
     *
     * @returns null if the method is not implemented, if there was an error, or if the method returned null.
     */
    private Object safeInvokeMethod(Object obj, String name) {
	Object result = null;
	try {
	    Method m = obj.getClass().getMethod(name);
	    result = m.invoke(obj);
	} catch (NoSuchMethodException e) {
	    // Object doesn't implement the method; no big deal.
	} catch (IllegalAccessException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (InvocationTargetException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return result;
    }

    /**
     * Get the object ID to which a test refers, or throw an exception if there is none.
     */
    private String getObjectRef(oval.schemas.definitions.core.TestType test) throws OvalException {
	try {
	    Method getObject = test.getClass().getMethod("getObject");
	    ObjectRefType objectRef = (ObjectRefType)getObject.invoke(test);
	    if (objectRef != null) {
		String ref = objectRef.getObjectRef();
		if (ref != null) {
		    return objectRef.getObjectRef();
		}
	    }
	} catch (NoSuchMethodException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (IllegalAccessException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (InvocationTargetException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}

	throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_TEST_NOOBJREF, test.getId()));
    }

    private String getStateRef(oval.schemas.definitions.core.TestType test) {
	try {
	    Method getObject = test.getClass().getMethod("getState");
	    Object o = getObject.invoke(test);
	    if (o instanceof List && ((List)o).size() > 0) {
		return ((StateRefType)((List)o).get(0)).getStateRef();
	    } else if (o instanceof StateRefType) {
		return ((StateRefType)o).getStateRef();
	    }
	} catch (NoSuchMethodException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (IllegalAccessException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (InvocationTargetException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return null;
    }

    private Set getObjectSet(ObjectType obj) {
	Set objectSet = null;
	try {
	    Method isSetSet = obj.getClass().getMethod("isSetSet");
	    if (((Boolean)isSetSet.invoke(obj)).booleanValue()) {
		Method getSet = obj.getClass().getMethod("getSet");
		objectSet = (Set)getSet.invoke(obj);
	    }
	} catch (NoSuchMethodException e) {
	    // Object doesn't support Sets; no big deal.
	} catch (IllegalAccessException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (InvocationTargetException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return objectSet;
    }

    class StateFieldBridge extends EntityStateSimpleBaseType {
	StateFieldBridge(EntityStateFieldType field) {
	    datatype = field.getDatatype();
	    mask = field.isMask();
	    operation = field.getOperation();
	    value = field.getValue();
	    varCheck = field.getVarCheck();
	    varRef = field.getVarRef();
	    entityCheck = field.getEntityCheck();
	}
    }

    class ItemFieldBridge extends EntityItemSimpleBaseType {
	ItemFieldBridge(EntityItemFieldType field) {
	    datatype = field.getDatatype();
	    mask = field.isMask();
	    value = field.getValue();
	}
    }
}
