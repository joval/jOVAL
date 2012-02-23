// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.engine;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.SimpleTimeZone;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;

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
import oval.schemas.definitions.core.DateTimeFormatEnumeration;
import oval.schemas.definitions.core.DefinitionType;
import oval.schemas.definitions.core.DefinitionsType;
import oval.schemas.definitions.core.EndFunctionType;
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
import oval.schemas.results.core.ResultEnumeration;
import oval.schemas.results.core.TestedItemType;
import oval.schemas.results.core.TestedVariableType;
import oval.schemas.results.core.TestType;
import oval.schemas.systemcharacteristics.core.EntityItemFieldType;
import oval.schemas.systemcharacteristics.core.EntityItemRecordType;
import oval.schemas.systemcharacteristics.core.EntityItemSimpleBaseType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.core.SystemDataType;
import oval.schemas.systemcharacteristics.core.VariableValueType;
import oval.schemas.variables.core.OvalVariables;

import org.joval.intf.oval.IDefinitionFilter;
import org.joval.intf.oval.IDefinitions;
import org.joval.intf.oval.IEngine;
import org.joval.intf.oval.IResults;
import org.joval.intf.oval.IVariables;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IPlugin;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.util.IObserver;
import org.joval.intf.util.IProducer;
import org.joval.os.windows.Timestamp;
import org.joval.oval.DefinitionFilter;
import org.joval.oval.Definitions;
import org.joval.oval.Directives;
import org.joval.oval.NotCollectableException;
import org.joval.oval.OvalException;
import org.joval.oval.ResolveException;
import org.joval.oval.Results;
import org.joval.oval.SystemCharacteristics;
import org.joval.oval.TestException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.Producer;
import org.joval.util.StringTools;
import org.joval.util.Version;

/**
 * Engine that evaluates OVAL tests on remote hosts.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Engine implements IEngine {
    private enum State {
	CONFIGURE,
	RUNNING,
	COMPLETE_OK,
	COMPLETE_ERR;
    }

    private Hashtable <String, Collection<VariableValueType>>variableMap; // A cache of nested VariableValueTypes
    private IVariables externalVariables = null;
    private IDefinitions definitions = null;
    private IPlugin plugin = null;
    private SystemCharacteristics sc = null;
    private IDefinitionFilter filter = null;
    private Hashtable<Class, IAdapter> adapters = null;
    private Exception error;
    private Results results;
    private State state;
    private boolean evalEnabled = true, abort = false;
    private Producer producer;
    private LocLogger logger;

    /**
     * Create an engine for evaluating OVAL definitions using a plugin.
     */
    public Engine(IPlugin plugin) {
	if (plugin == null) {
	    logger = JOVALSystem.getLogger();
	} else {
	    logger = plugin.getLogger();
	    this.plugin = plugin;
	}
	producer = new Producer();
	filter = new DefinitionFilter();
	reset();
    }

    // Implement IEngine

    public void setDefinitionsFile(File f) throws IllegalThreadStateException, OvalException {
	setDefinitions(new Definitions(f));
    }

    public void setDefinitions(IDefinitions definitions) throws IllegalThreadStateException {
	switch(state) {
	  case RUNNING:
	    throw new IllegalThreadStateException(JOVALSystem.getMessage(JOVALMsg.ERROR_ENGINE_STATE, state));

	  case COMPLETE_OK:
	  case COMPLETE_ERR:
	    reset();
	    // fall-through

	  default:
	    this.definitions = definitions;
	    break;
	}
    }

    public void setDefinitionFilterFile(File f) throws IllegalThreadStateException, OvalException {
	setDefinitionFilter(new DefinitionFilter(f));
    }

    public void setDefinitionFilter(IDefinitionFilter filter) throws IllegalThreadStateException {
	switch(state) {
	  case CONFIGURE:
	    this.filter = filter;
	    break;

	  default:
	    throw new IllegalThreadStateException(JOVALSystem.getMessage(JOVALMsg.ERROR_ENGINE_STATE, state));
	}
    }

    public void setSystemCharacteristicsFile(File f) throws IllegalThreadStateException, OvalException {
	switch(state) {
	  case CONFIGURE:
	    sc = new SystemCharacteristics(f);
	    break;

	  default:
	    throw new IllegalThreadStateException(JOVALSystem.getMessage(JOVALMsg.ERROR_ENGINE_STATE, state));
	}
    }

    public void setExternalVariables(IVariables variables) throws IllegalThreadStateException {
	switch(state) {
	  case CONFIGURE:
	    externalVariables = variables;
	    break;

	  default:
	    throw new IllegalThreadStateException(JOVALSystem.getMessage(JOVALMsg.ERROR_ENGINE_STATE, state));
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
	    throw new IllegalThreadStateException(JOVALSystem.getMessage(JOVALMsg.ERROR_ENGINE_STATE, state));
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

    // Implement Runnable

    /**
     * Do what one might do with an Engine if one were to do it in its own Thread.
     */
    public void run() {
	state = State.RUNNING;
	try {
	    if (sc == null) {
		if (plugin == null) {
		    throw new RuntimeException(JOVALSystem.getMessage(JOVALMsg.ERROR_NULL_PLUGIN));
		}
		try {
		    plugin.connect();
		    if (adapters == null) {
			loadAdapters();
		    }
		    scan();
		} finally {
		    if (plugin != null) {
			plugin.disconnect();
		    }
		}
		producer.sendNotify(MESSAGE_SYSTEMCHARACTERISTICS, sc);
	    }

	    results = new Results(getGenerator(), definitions, sc);
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

	    producer.sendNotify(MESSAGE_DEFINITION_PHASE_END, null);
	    state = State.COMPLETE_OK;
	} catch (Exception e) {
	    error = e;
	    state = State.COMPLETE_ERR;
	}
    }

    // Internal

    static final GeneratorType getGenerator() {
	GeneratorType generator = JOVALSystem.factories.common.createGeneratorType();
	generator.setProductName(JOVALSystem.getSystemProperty(JOVALSystem.SYSTEM_PROP_PRODUCT));
	generator.setProductVersion(JOVALSystem.getSystemProperty(JOVALSystem.SYSTEM_PROP_VERSION));
	generator.setSchemaVersion(SCHEMA_VERSION.toString());
	try {
	    generator.setTimestamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
	} catch (DatatypeConfigurationException e) {
	    JOVALSystem.getLogger().warn(JOVALMsg.ERROR_TIMESTAMP);
	    JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return generator;
    }

    SystemCharacteristics getSystemCharacteristics() {
	return sc;
    }

    /**
     * Return the value of the Variable with the specified ID, and also add any chained variables to the provided list.
     */
    Collection<String> resolve(String variableId, RequestContext rc)
		throws NoSuchElementException, ResolveException, OvalException {

	Collection<VariableValueType> vars = rc.getVars();
	VariableType var = definitions.getVariable(variableId);
	String varId = var.getId();
	Collection<VariableValueType> cachedList = variableMap.get(varId);
	if (cachedList == null) {
	    logger.trace(JOVALMsg.STATUS_VARIABLE_CREATE, varId);
	    Collection<String> result = resolveInternal(var, vars);
	    variableMap.put(varId, vars);
	    return result;
	} else {
	    logger.trace(JOVALMsg.STATUS_VARIABLE_RECYCLE, varId);
	    List<String> result = new Vector<String>();
	    vars.addAll(cachedList);
	    for (VariableValueType variableValueType : cachedList) {
		if (variableValueType.getVariableId().equals(variableId)) {
		    result.add((String)variableValueType.getValue());
		}
	    }
	    return result;
	}
    }

    // Private

    private void loadAdapters() {
	Collection<IAdapter> coll = plugin.getAdapters();
	if (coll == null) {
	    adapters = null;
	} else {
	    adapters = new Hashtable<Class, IAdapter>();
	    for (IAdapter adapter : coll) {
		for (Class clazz : adapter.getObjectClasses()) {
		    adapters.put(clazz, adapter);
		}
	    }
	}
    }

    private void reset() {
	sc = null;
	state = State.CONFIGURE;
	variableMap = new Hashtable<String, Collection<VariableValueType>>();
	error = null;
    }

    /**
     * Scan SystemCharactericts using the plugin.
     */
    private void scan() throws OvalException {
	producer.sendNotify(MESSAGE_OBJECT_PHASE_START, null);
	sc = new SystemCharacteristics(getGenerator(), plugin.getSystemInfo());
	sc.setLogger(logger);

	for (ObjectType obj : definitions.getObjects()) {
	    String objectId = obj.getId();
	    if (!sc.containsObject(objectId)) {
		scanObject(new RequestContext(this, obj));
	    }
	}
	producer.sendNotify(MESSAGE_OBJECT_PHASE_END, null);
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
	//
	// As the lowest level scan operation, this is a good place to check if the engine is being destroyed.
	//
	if (abort) {
	    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_ENGINE_ABORT));
	}

	ObjectType obj = rc.getObject();
	String objectId = obj.getId();
	logger.debug(JOVALMsg.STATUS_OBJECT, objectId);
	producer.sendNotify(MESSAGE_OBJECT, objectId);

	IAdapter adapter = adapters.get(obj.getClass());
	if (adapter == null) {
	    MessageType message = JOVALSystem.factories.common.createMessageType();
	    message.setLevel(MessageLevelEnumeration.WARNING);
	    String err = JOVALSystem.getMessage(JOVALMsg.ERROR_ADAPTER_MISSING, obj.getClass().getName());
	    message.setValue(err);
	    sc.setObject(objectId, obj.getComment(), obj.getVersion(), FlagEnumeration.NOT_COLLECTED, message);
	} else {
	    Set s = getObjectSet(obj);
	    if (s == null) {
		try {
		    Collection<JAXBElement<? extends ItemType>> items = adapter.getItems(rc);
		    if (items.size() == 0) {
			MessageType msg = JOVALSystem.factories.common.createMessageType();
			msg.setLevel(MessageLevelEnumeration.INFO);
			msg.setValue(JOVALSystem.getMessage(JOVALMsg.STATUS_EMPTY_OBJECT));
			sc.setObject(objectId, obj.getComment(), obj.getVersion(), FlagEnumeration.DOES_NOT_EXIST, msg);
		    } else {
			//
			// Apply filters from the object, if there are any
			//
			items = filterWrappedItems(getFilters(obj), items);

			//
			// Add the object to the SystemCharacteristics, and associate all the items with it.
			// DAS: TBD, add a mechanism to flag the item collection as incomplete (i.e., permission issues)
			//
			sc.setObject(objectId, obj.getComment(), obj.getVersion(), FlagEnumeration.COMPLETE, null);
			for (JAXBElement<? extends ItemType> item : items) {
			    BigInteger itemId = sc.storeItem(item);
			    sc.relateItem(objectId, itemId);
			}
		    }
		    for (VariableValueType var : rc.getVars()) {
			sc.storeVariable(var);
			sc.relateVariable(objectId, var.getVariableId());
		    }
		    Collection<ItemType> unwrapped = new Vector<ItemType>();
		    for (JAXBElement<? extends ItemType> item : items) {
			unwrapped.add(item.getValue());
		    }
		    return unwrapped;
		} catch (NotCollectableException e) {
		    MessageType message = JOVALSystem.factories.common.createMessageType();
		    message.setLevel(MessageLevelEnumeration.WARNING);
		    String err = JOVALSystem.getMessage(JOVALMsg.ERROR_ADAPTER_COLLECTION, e.getMessage());
		    message.setValue(err);
		    sc.setObject(objectId, obj.getComment(), obj.getVersion(), FlagEnumeration.NOT_COLLECTED, message);
		} catch (Exception e) {
		    MessageType message = JOVALSystem.factories.common.createMessageType();
		    message.setLevel(MessageLevelEnumeration.ERROR);
		    message.setValue(e.getMessage());
		    sc.setObject(objectId, obj.getComment(), obj.getVersion(), FlagEnumeration.ERROR, message);
		}
	    } else {
		Collection<ItemType> items = getSetItems(s);
		sc.setObject(objectId, obj.getComment(), obj.getVersion(), FlagEnumeration.COMPLETE, null);
		for (ItemType item : items) {
		    sc.relateItem(objectId, item.getId());
		}
		return items;
	    }
	}

	//
	// If this point has been reached, some kind of error has prevented collection and a message and the appropriate
	// flag have been associated with the object.
	//
	@SuppressWarnings("unchecked")
	Collection<ItemType> empty = (Collection<ItemType>)Collections.EMPTY_LIST;
	return empty;
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
     * Given Collections of wrapped items and filters, returns the appropriately filtered collection of wrapped items.
     */
    private Collection<JAXBElement<? extends ItemType>> filterWrappedItems(List<Filter> filters,
		       Collection<JAXBElement<? extends ItemType>> items) throws OvalException {

	if (filters.size() == 0) {
	    return items;
	}
	Collection<JAXBElement<? extends ItemType>> filteredItems = new HashSet<JAXBElement<? extends ItemType>>();
	for (Filter filter : filters) {
	    StateType state = definitions.getState(filter.getValue());
	    for (JAXBElement<? extends ItemType> item : items) {
		try {
		    ResultEnumeration result = compare(state, item.getValue(), new RequestContext(this, null));
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
		    logger.debug(JOVALSystem.getMessage(JOVALMsg.ERROR_COMPONENT_FILTER), e.getMessage());
		    logger.trace(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
	    }
	}
	return filteredItems;
    }

    /**
     * Given Collections of items and filters, returns the appropriately filtered collection.
     */
    private Collection<ItemType> filterItems(List<Filter> filters, Collection<ItemType> items) throws OvalException {
	if (filters.size() == 0) {
	    return items;
	}
	Collection<ItemType> filteredItems = new HashSet<ItemType>();
	for (Filter filter : filters) {
	    StateType state = definitions.getState(filter.getValue());
	    for (ItemType item : items) {
		try {
		    ResultEnumeration result = compare(state, item, new RequestContext(this, null));
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
		    logger.debug(JOVALSystem.getMessage(JOVALMsg.ERROR_COMPONENT_FILTER), e.getMessage());
		    logger.trace(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
	    }
	}
	return filteredItems;
    }

    /**
     * Get a list of items belonging to a Set.
     */
    private Collection<ItemType> getSetItems(Set s) throws NoSuchElementException, OvalException {
	//
	// First, retrieve the filtered list of items in the Set, recursively.
	//
	Collection<Collection<ItemType>> lists = new Vector<Collection<ItemType>>();
	if (s.isSetSet()) {
	    for (Set set : s.getSet()) {
		lists.add(getSetItems(set));
	    }
	} else {
	    for (String objectId : s.getObjectReference()) {
		Collection<ItemType> items = null;
		try {
		    items = sc.getItemsByObjectId(objectId);
		} catch (NoSuchElementException e) {
		    items = scanObject(new RequestContext(this, definitions.getObject(objectId)));
		}
		lists.add(filterItems(s.getFilter(), items));
	    }
	}

	switch(s.getSetOperator()) {
	  case INTERSECTION: {
	    ItemSet intersection = null;
	    for (Collection<ItemType> items : lists) {
		if (intersection == null) {
		    intersection = new ItemSet(items);
		} else {
		    intersection = intersection.intersection(new ItemSet(items));
		}
	    }
	    return intersection.toList();
	  }

	  case COMPLEMENT: {
	    if (lists.size() == 2) {
		Iterator<Collection<ItemType>> iter = lists.iterator();
		Collection<ItemType> set1 = iter.next();
		Collection<ItemType> set2 = iter.next();
		return new ItemSet(set1).complement(new ItemSet(set2)).toList();
	    } else {
		throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_SET_COMPLEMENT, new Integer(lists.size())));
	    }
	  }

	  case UNION:
	  default: {
	    ItemSet union = new ItemSet();
	    for (Collection<ItemType> items : lists) {
		union = union.union(new ItemSet(items));
	    }
	    return union.toList();
	  }
	}
    }

    /**
     * Evaluate the DefinitionType.
     */
    private oval.schemas.results.core.DefinitionType evaluateDefinition(DefinitionType defDefinition) throws OvalException {
	String defId = defDefinition.getId();
	if (defId == null) {
	    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_DEFINITION_NOID));
	}
	oval.schemas.results.core.DefinitionType defResult = results.getDefinition(defId);

	if (defResult == null) {
	    logger.debug(JOVALMsg.STATUS_DEFINITION, defId);
	    producer.sendNotify(MESSAGE_DEFINITION, defId);
	    defResult = JOVALSystem.factories.results.createDefinitionType();
	    defResult.setDefinitionId(defId);
	    defResult.setVersion(defDefinition.getVersion());
	    defResult.setClazz(defDefinition.getClazz());
	    oval.schemas.results.core.CriteriaType criteriaResult = evaluateCriteria(defDefinition.getCriteria());
	    defResult.setResult(criteriaResult.getResult());
	    defResult.setCriteria(criteriaResult);
	    results.storeDefinitionResult(defResult);
	}
	return defResult;
    }

    private oval.schemas.results.core.CriterionType evaluateCriterion(CriterionType criterionDefinition) throws OvalException {
	String testId = criterionDefinition.getTestRef();
	TestType testResult = results.getTest(testId);
	if (testResult == null) {
	    oval.schemas.definitions.core.TestType testDefinition = definitions.getTest(testId);
	    testResult = JOVALSystem.factories.results.createTestType();
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

	oval.schemas.results.core.CriterionType criterionResult = JOVALSystem.factories.results.createCriterionType();
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

    private void evaluateTest(TestType testResult) throws OvalException {
	String testId = testResult.getTestId();
	logger.debug(JOVALMsg.STATUS_TEST, testId);
	oval.schemas.definitions.core.TestType testDefinition = definitions.getTest(testId);
	String objectId = getObjectRef(testDefinition);

	//
	// If the object is not found in the SystemCharacteristics, then the test cannot be evaluated.
	//
	if (!sc.containsObject(objectId)) {
	    testResult.setResult(ResultEnumeration.NOT_EVALUATED);
	    return;
	}

	String stateId = getStateRef(testDefinition);
	StateType state = null;
	if (stateId != null) {
	    state = definitions.getState(stateId);
	}

	//
	// Create all the structures we'll need to store information about the evaluation of the test.
	//
	Collection<VariableValueType> variables = new HashSet<VariableValueType>();
	RequestContext rc = new RequestContext(this, definitions.getObject(objectId), variables);
	ExistenceData existence = new ExistenceData();
	CheckData check = new CheckData();

	switch(sc.getObject(objectId).getFlag()) {
	  case COMPLETE:
	  case INCOMPLETE:
	    for (ItemType item : sc.getItemsByObjectId(objectId)) {
		existence.addStatus(item.getStatus());

		TestedItemType testedItem = JOVALSystem.factories.results.createTestedItemType();
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

			    MessageType message = JOVALSystem.factories.common.createMessageType();
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
	// Add all the tested variables that the TestType has picked up along the way.
	//
	variables.addAll(sc.getVariablesByObjectId(objectId));
	for (VariableValueType var : variables) {
	    TestedVariableType testedVariable = JOVALSystem.factories.results.createTestedVariableType();
	    testedVariable.setVariableId(var.getVariableId());
	    testedVariable.setValue(var.getValue());
	    testResult.getTestedVariable().add(testedVariable);
	}

	//
	// Note that the NONE_EXIST check is deprecated as of 5.3, and will be eliminated in 6.0.
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

    private oval.schemas.results.core.CriteriaType evaluateCriteria(CriteriaType criteriaDefinition) throws OvalException {
	oval.schemas.results.core.CriteriaType criteriaResult = JOVALSystem.factories.results.createCriteriaType();
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
		edtResult = JOVALSystem.factories.results.createExtendDefinitionType();
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
		throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_BAD_COMPONENT, child.getClass().getName()));
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
	    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_ENGINE_ABORT));
	}

	try {
	    for (String methodName : getMethodNames(state.getClass())) {
		if (methodName.startsWith("get") && !stateBaseMethodNames.contains(methodName)) {
		    Object stateEntityObj = state.getClass().getMethod(methodName).invoke(state);
		    if (stateEntityObj == null) {
			// continue
		    } else if (stateEntityObj instanceof EntityStateSimpleBaseType) {
			EntityStateSimpleBaseType stateEntity = (EntityStateSimpleBaseType)stateEntityObj;
			Object itemEntityObj = item.getClass().getMethod(methodName).invoke(item);
			ResultEnumeration result = ResultEnumeration.UNKNOWN;
			if (itemEntityObj instanceof EntityItemSimpleBaseType || itemEntityObj == null) {
			    result = compare(stateEntity, (EntityItemSimpleBaseType)itemEntityObj, rc);
			} else if (itemEntityObj instanceof JAXBElement) {
			    JAXBElement element = (JAXBElement)itemEntityObj;
			    EntityItemSimpleBaseType itemEntity = (EntityItemSimpleBaseType)element.getValue();
			    result = compare(stateEntity, itemEntity, rc);
			} else if (itemEntityObj instanceof Collection) {
			    CheckData cd = new CheckData();
			    for (Object entityObj : (Collection)itemEntityObj) {
			        EntityItemSimpleBaseType itemEntity = (EntityItemSimpleBaseType)entityObj;
				cd.addResult(compare(stateEntity, itemEntity, rc));
			    }
			    result = cd.getResult(stateEntity.getEntityCheck());
			} else {
			    String message = JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_ENTITY,
								    itemEntityObj.getClass().getName(), item.getId());
	    		    throw new OvalException(message);
			}
			if (result != ResultEnumeration.TRUE) {
			    return result;
			}
		    } else if (stateEntityObj instanceof EntityStateRecordType) {
			EntityStateRecordType stateEntity = (EntityStateRecordType)stateEntityObj;
			Object itemEntityObj = item.getClass().getMethod(methodName).invoke(item);
			ResultEnumeration result = ResultEnumeration.UNKNOWN;
			if (itemEntityObj instanceof EntityItemRecordType) {
			    result = compare(stateEntity, (EntityItemRecordType)itemEntityObj, rc);
			} else if (itemEntityObj instanceof Collection) {
			    CheckData cd = new CheckData();
			    for (Object entityObj : (Collection)itemEntityObj) {
				if (entityObj instanceof EntityItemRecordType) {
				    cd.addResult(compare(stateEntity, (EntityItemRecordType)entityObj, rc));
				} else {
				    String msg = JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_ENTITY,
									entityObj.getClass().getName(), item.getId());
				    throw new OvalException(msg);
				}
			    }
			    result = cd.getResult(stateEntity.getEntityCheck());
			} else {
			    String message = JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_ENTITY,
								    itemEntityObj.getClass().getName(), item.getId());
	    		    throw new OvalException(message);
			}
			if (result != ResultEnumeration.TRUE) {
			    return result;
			}
		    } else {
			String message = JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_ENTITY,
								item.getClass().getName(), item.getId());
	    		throw new OvalException(message);
		    }
		}
	    }
	    return ResultEnumeration.TRUE;
	} catch (NoSuchMethodException e) {
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), state.getId()));
	} catch (IllegalAccessException e) {
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), state.getId()));
	} catch (InvocationTargetException e) {
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), state.getId()));
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
		throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_DATATYPE_MISMATCH, stateDT, itemDT));
	    }
	}

	//
	// Handle the variable_ref case
	//
	if (state.isSetVarRef()) {
	    CheckData cd = new CheckData();
	    EntitySimpleBaseType base = JOVALSystem.factories.definitions.core.createEntityObjectAnySimpleType();
	    base.setDatatype(state.getDatatype());
	    base.setOperation(state.getOperation());
	    base.setMask(state.isMask());
	    try {
		for (String value : resolve(state.getVarRef(), rc)) {
		    base.setValue(value);
		    cd.addResult(testImpl(base, item));
		}
	    } catch (NoSuchElementException e) {
		String reason = JOVALSystem.getMessage(JOVALMsg.ERROR_VARIABLE_MISSING);
		throw new TestException(JOVALSystem.getMessage(JOVALMsg.ERROR_RESOLVE_VAR, state.getVarRef(), reason));
	    } catch (ResolveException e) {
		throw new TestException(JOVALSystem.getMessage(JOVALMsg.ERROR_RESOLVE_VAR, state.getVarRef(), e.getMessage()));
	    }
	    return cd.getResult(state.getVarCheck());
	} else {
	    return testImpl(state, item);
	}
    }

    /**
     * Perform the the OVAL test by comparing the state and item.  Relies on the state to carry any datatype information.
     *
     * @see http://oval.mitre.org/language/version5.10/ovaldefinition/documentation/oval-common-schema.html#OperationEnumeration
     */
    ResultEnumeration testImpl(EntitySimpleBaseType state, EntityItemSimpleBaseType item) throws TestException, OvalException {
	if (!item.isSetValue() || !state.isSetValue()) {
	    String msg = JOVALSystem.getMessage(JOVALMsg.ERROR_TEST_INCOMPARABLE, item.getValue(), state.getValue());
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

	  default:
	    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, state.getOperation()));
	}
    }

    /**
     * Perform the equivalent of item.getValue().compareTo(state.getValue()).
     */
    int compareValues(EntityItemSimpleBaseType item, EntitySimpleBaseType state) throws TestException, OvalException {
	switch(getDatatype(state.getDatatype())) {
	  case INT:
	    try {
		return new BigInteger((String)item.getValue()).compareTo(new BigInteger((String)state.getValue()));
	    } catch (NumberFormatException e) {
		throw new TestException(e);
	    }

	  case FLOAT:
	    try {
		return new Float((String)item.getValue()).compareTo(new Float((String)state.getValue()));
	    } catch (NumberFormatException e) {
		throw new TestException(e);
	    }

	  case BOOLEAN:
	    if (getBoolean((String)item.getValue()) == getBoolean((String)state.getValue())) {
		return 0;
	    } else {
		return 1;
	    }

	  case VERSION:
	    try {
		return new Version(item.getValue()).compareTo(new Version(state.getValue()));
	    } catch (IllegalArgumentException e) {
		throw new TestException(e);
	    }

	  case EVR_STRING:
	    try {
		return new Evr((String)item.getValue()).compareTo(new Evr((String)state.getValue()));
	    } catch (IllegalArgumentException e) {
		throw new TestException(e);
	    }

	  case BINARY:
	  case STRING:
	    return ((String)item.getValue()).compareTo((String)state.getValue());

	  default:
	    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_OPERATION_DATATYPE,
							   state.getDatatype(), OperationEnumeration.EQUALS));
	}
    }

    boolean equalsIgnoreCase(EntitySimpleBaseType state, EntityItemSimpleBaseType item) throws TestException, OvalException {
	switch(getDatatype(state.getDatatype())) {
	  case STRING:
	    return ((String)state.getValue()).equalsIgnoreCase((String)item.getValue());

	  default:
	    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_OPERATION_DATATYPE,
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
	    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_OPERATION_DATATYPE,
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
	    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_OPERATION_DATATYPE,
							   state.getDatatype(), OperationEnumeration.BITWISE_OR));
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
	    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_DATATYPE, s));
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
    private Collection<String> resolveInternal(Object object, Collection<VariableValueType>list)
		throws NoSuchElementException, ResolveException, OvalException {
	//
	// Why do variables point to variables?  Because sometimes they are nested.
	//
	if (object instanceof LocalVariable) {
	    LocalVariable localVariable = (LocalVariable)object;
	    Collection<String> values = resolveInternal(getComponent(localVariable), list);
	    for (String value : values) {
		VariableValueType variableValueType = JOVALSystem.factories.sc.core.createVariableValueType();
		variableValueType.setVariableId(localVariable.getId());
		variableValueType.setValue(value);
		list.add(variableValueType);
	    }
	    return values;

	//
	// Add an externally-defined variable.
	//
	} else if (object instanceof ExternalVariable) {
	    ExternalVariable externalVariable = (ExternalVariable)object;
	    String id = externalVariable.getId();
	    if (externalVariables == null) {
		throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_EXTERNAL_VARIABLE_SOURCE, id));
	    } else {
		Collection<String> values = externalVariables.getValue(id);
		for (String value : values) {
		    VariableValueType variableValueType = JOVALSystem.factories.sc.core.createVariableValueType();
		    variableValueType.setVariableId(id);
		    variableValueType.setValue(value);
		    list.add(variableValueType);
		}
		return values;
	    }

	//
	// Add a constant variable.
	//
	} else if (object instanceof ConstantVariable) {
	    ConstantVariable constantVariable = (ConstantVariable)object;
	    String id = constantVariable.getId();
	    Collection<String> values = new Vector<String>();
	    for (ValueType value : constantVariable.getValue()) {
		VariableValueType variableValueType = JOVALSystem.factories.sc.core.createVariableValueType();
		variableValueType.setVariableId(id);
		String s = (String)value.getValue();
		variableValueType.setValue(s);
		list.add(variableValueType);
		values.add(s);
	    }
	    return values;

	//
	// Add a static (literal) value.
	//
	} else if (object instanceof LiteralComponentType) {
	    Collection<String> values = new Vector<String>();
	    values.add((String)((LiteralComponentType)object).getValue());
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
		ObjectType ot = definitions.getObject(objectId);
		try {
		    items = scanObject(new RequestContext(this, ot, list));
		} catch (OvalException oe) {
		    throw new ResolveException(oe);
		}
	    }
	    return extractItemData(objectId, oc, items);

	//
	// Resolve and return.
	//
	} else if (object instanceof VariableComponentType) {
	    VariableComponentType vc = (VariableComponentType)object;
	    return resolveInternal(definitions.getVariable(vc.getVarRef()), list);

	//
	// Resolve and concatenate child components.
	//
	} else if (object instanceof ConcatFunctionType) {
	    Collection<String> values = new Vector<String>();
	    ConcatFunctionType concat = (ConcatFunctionType)object;
	    for (Object child : concat.getObjectComponentOrVariableComponentOrLiteralComponent()) {
		Collection<String> next = resolveInternal(child, list);
		if (next.size() == 0) {
		    @SuppressWarnings("unchecked")
		    Collection<String> empty = (Collection<String>)Collections.EMPTY_LIST;
		    return empty;
		} else if (values.size() == 0) {
		    values.addAll(next);
		} else {
		    Collection<String> newValues = new Vector<String>();
		    for (String base : values) {
			for (String val : next) {
			    newValues.add(base + val);
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
	    Collection<String> values = new Vector<String>();
	    for (String value : resolveInternal(getComponent((EscapeRegexFunctionType)object), list)) {
		values.add(StringTools.escapeRegex(value));
	    }
	    return values;

	//
	// Process a Split, which contains a component and a delimiter with which to split it up.
	//
	} else if (object instanceof SplitFunctionType) {
	    SplitFunctionType split = (SplitFunctionType)object;
	    Collection<String> values = new Vector<String>();
	    for (String value : resolveInternal(getComponent(split), list)) {
		values.addAll(StringTools.toList(StringTools.tokenize(value, split.getDelimiter(), false)));
	    }
	    return values;

	//
	// Process a RegexCapture, which returns the regions of a component resolved as a String that match the first
	// subexpression in the given pattern.
	//
	} else if (object instanceof RegexCaptureFunctionType) {
	    RegexCaptureFunctionType rc = (RegexCaptureFunctionType)object;
	    Pattern p = Pattern.compile(rc.getPattern());
	    Collection<String> values = new Vector<String>();
	    for (String value : resolveInternal(getComponent(rc), list)) {
		Matcher m = p.matcher(value);
		if (m.groupCount() > 0) {
		    if (m.find()) {
			values.add(m.group(1));
		    } else {
			values.add("");
		    }
		} else {
		    values.add("");
		}
	    }
	    return values;

	//
	// Process a Substring
	//
	} else if (object instanceof SubstringFunctionType) {
	    SubstringFunctionType st = (SubstringFunctionType)object;
	    int start = st.getSubstringStart();
	    start = Math.max(1, start);
	    start--; // in OVAL, the index count begins at 1 instead of 0
	    int len = st.getSubstringLength();
	    Collection<String> values = new Vector<String>();
	    for (String value : resolveInternal(getComponent(st), list)) {
		if (start > value.length()) {
		    throw new ResolveException(JOVALSystem.getMessage(JOVALMsg.ERROR_SUBSTRING, value, new Integer(start)));
		} else if (len < 0 || value.length() <= (start+len)) {
		    values.add(value.substring(start));
		} else {
		    values.add(value.substring(start, start+len));
		}
	    }
	    return values;

	//
	// Process a Begin
	//
	} else if (object instanceof BeginFunctionType) {
	    BeginFunctionType bt = (BeginFunctionType)object;
	    String s = bt.getCharacter();
	    Collection<String> values = new Vector<String>();
	    for (String value : resolveInternal(getComponent(bt), list)) {
		if (value.startsWith(s)) {
		    values.add(value);
		} else {
		    values.add(s + value);
		}
	    }
	    return values;

	//
	// Process an End
	//
	} else if (object instanceof EndFunctionType) {
	    EndFunctionType et = (EndFunctionType)object;
	    String s = et.getCharacter();
	    Collection<String> values = new Vector<String>();
	    for (String value : resolveInternal(getComponent(et), list)) {
		if (value.endsWith(s)) {
		    values.add(value);
		} else {
		    values.add(value + s);
		}
	    }
	    return values;

	//
	// Process a TimeDifference
	//
	} else if (object instanceof TimeDifferenceFunctionType) {
	    TimeDifferenceFunctionType tt = (TimeDifferenceFunctionType)object;
	    Collection<String> values = new Vector<String>();
	    List<Object> children = tt.getObjectComponentOrVariableComponentOrLiteralComponent();
	    Collection<String> timestamp1;
	    Collection<String> timestamp2;
	    if (children.size() == 1) {
		timestamp1 = new Vector<String>();
		timestamp1.add(new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(new Date(System.currentTimeMillis())));
		timestamp2 = resolveInternal(children.get(0), list);
	    } else if (children.size() == 2) {
		timestamp1 = resolveInternal(children.get(0), list);
		timestamp2 = resolveInternal(children.get(1), list);
	    } else {
		throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_BAD_TIMEDIFFERENCE,new Integer(children.size())));
	    }
	    for (String time1 : timestamp1) {
		long tm1 = getTime(time1, tt.getFormat1());
		for (String time2 : timestamp2) {
		    long tm2 = getTime(time2, tt.getFormat2());
		    long diff = (tm1 - tm2)/1000L; // convert diff to seconds
		    values.add(Long.toString(diff));
		}
	    }
	    return values;

	//
	// Process Arithmetic
	//
	} else if (object instanceof ArithmeticFunctionType) {
	    ArithmeticFunctionType at = (ArithmeticFunctionType)object;
	    Stack<Collection<String>> rows = new Stack<Collection<String>>();
	    ArithmeticEnumeration op = at.getArithmeticOperation();
	    for (Object child : at.getObjectComponentOrVariableComponentOrLiteralComponent()) {
		Collection<String> row = new Vector<String>();
		for (String cell : resolveInternal(child, list)) {
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
	    Collection<String> children = new Vector<String>();
	    for (Object child : ct.getObjectComponentOrVariableComponentOrLiteralComponent()) {
		children.addAll(resolveInternal(child, list));
	    }
	    Collection<String> values = new Vector<String>();
	    values.add(Integer.toString(children.size()));
	    return values;

	//
	// Process Unique
	//
	} else if (object instanceof UniqueFunctionType) {
	    UniqueFunctionType ut = (UniqueFunctionType)object;
	    HashSet<String> values = new HashSet<String>();
	    for (Object child : ut.getObjectComponentOrVariableComponentOrLiteralComponent()) {
		values.addAll(resolveInternal(child, list));
	    }
	    return values;

	} else {
	    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_COMPONENT, object.getClass().getName()));
	}
    }

    /**
     * Perform the Arithmetic operation on permutations of the Stack, and return the resulting permutations.
     */
    private List<String> computeProduct(ArithmeticEnumeration op, Stack<Collection<String>> rows) {
	List<String> results = new Vector<String>();
	if (rows.empty()) {
	    switch(op) {
		case ADD:
		  results.add("0");
		  break;
		case MULTIPLY:
		  results.add("1");
		  break;
	    }
	} else {
	    for (String value : rows.pop()) {
		Stack<Collection<String>> copy = new Stack<Collection<String>>();
		copy.addAll(rows);
		for (String otherValue : computeProduct(op, copy)) {
		    switch(op) {
		      case ADD:
			if (value.indexOf(".") == -1 && otherValue.indexOf(".") == -1) {
			    results.add(new BigInteger(value).add(new BigInteger(otherValue)).toString());
			} else {
			    results.add(new BigDecimal(value).add(new BigDecimal(otherValue)).toString());
			}
			break;
		      case MULTIPLY:
			if (value.indexOf(".") == -1 && otherValue.indexOf(".") == -1) {
			    results.add(new BigInteger(value).multiply(new BigInteger(otherValue)).toString());
			} else {
			    results.add(new BigDecimal(value).multiply(new BigDecimal(otherValue)).toString());
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
    private List<String> extractItemData(String objectId, ObjectComponentType oc, Collection list)
		throws OvalException, NoSuchElementException {

	//
	// If the ObjectComponentType contains no items, an error is supposed to be reported, hence we throw an exception
	// complaining that the list is empty. See:
	// http://oval.mitre.org/language/version5.10/ovaldefinition/documentation/oval-definitions-schema.html#ObjectComponentType
	//
	if (list.size() == 0) {
	    throw new NoSuchElementException(JOVALSystem.getMessage(JOVALMsg.ERROR_NO_ITEMS, objectId));
	}
	List<String> values = new Vector<String>();
	for (Object o : list) {
	    if (o instanceof ItemType) {
		try {
		    ItemType item = (ItemType)o;
		    String methodName = getAccessorMethodName(oc.getItemField());
		    Method method = item.getClass().getMethod(methodName);
		    o = method.invoke(item);
		} catch (NoSuchMethodException e) {
		    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		    return null;
		} catch (IllegalAccessException e) {
		    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		    return null;
		} catch (InvocationTargetException e) {
		    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		    return null;
		}
	    }

	    if (o instanceof JAXBElement) {
		o = ((JAXBElement)o).getValue();
	    }
	    if (o instanceof EntityItemSimpleBaseType) {
		EntityItemSimpleBaseType entity = (EntityItemSimpleBaseType)o;
		String value = (String)entity.getValue();
		if (value != null) {
		    values.add((String)entity.getValue());
		}
	    } else if (o instanceof List) {
		return extractItemData(objectId, null, (List)o);
	    } else if (o instanceof EntityItemRecordType) {
		EntityItemRecordType record = (EntityItemRecordType)o;
		String fieldName = oc.getRecordField();
		for (EntityItemFieldType field : record.getField()) {
		    if (field.getName().equals(fieldName)) {
			String value = (String)field.getValue();
			if (value != null) {
			    values.add(value);
			}
		    }
		}
	    } else {
		throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_REFLECTION, o.getClass().getName(), objectId));
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

	throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_COMPONENT, unknown.getClass().getName()));
    }

    /**
     * Safely invoke a method that takes no arguments and returns an Object.
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
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (InvocationTargetException e) {
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
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
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (IllegalAccessException e) {
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (InvocationTargetException e) {
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}

	throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_TEST_NOOBJREF, test.getId()));
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
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (IllegalAccessException e) {
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (InvocationTargetException e) {
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
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
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (InvocationTargetException e) {
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return objectSet;
    }

    /**
     * @see http://oval.mitre.org/language/version5.10/ovaldefinition/documentation/oval-definitions-schema.html#DateTimeFormatEnumeration
     */
    long getTime(String s, DateTimeFormatEnumeration format) throws OvalException {
	try {
	    String sdf = null;

	    switch(format) {
	      case SECONDS_SINCE_EPOCH:
		return Long.parseLong(s) * 1000L;
    
	      case WIN_FILETIME:
		return Timestamp.getTime(new BigInteger(s, 16));

	      case DAY_MONTH_YEAR:
		switch(s.length()) {
		  case 8:
		  case 9:
		  case 10:
		    if (s.charAt(1) == '/' || s.charAt(2) == '/') {
			sdf = "dd/MM/yyyy";
		    } else {
			sdf = "dd-MM-yyyy";
		    }
		    break;
    
		  case 18:
		  case 19:
		  case 20:
		    if (s.charAt(1) == '/' || s.charAt(2) == '/') {
			sdf = "dd/MM/yyyy HH:mm:ss";
		    } else {
			sdf = "dd-MM-yyyy HH:mm:ss";
		    }
		    break;

		  default:
		    break;
		}
		break;
    
	      case MONTH_DAY_YEAR:
		switch(s.length()) {
		  case 8:
		  case 9:
		  case 10:
		    if (s.charAt(1) == '/' || s.charAt(2) == '/') {
			sdf = "MM/dd/yyyy";
		    } else {
			sdf = "MM-dd-yyyy";
		    }
		    break;
    
		  case 11:
		  case 12:
		    sdf = "MMM, dd yyyy";
		    break;

		  case 17:
		  case 18:
		  case 19:
		  case 20:
		  case 21:
		    if (s.charAt(1) == '/' || s.charAt(2) == '/') {
			sdf = "MM/dd/yyyy HH:mm:ss";
			break;
		    } else if (s.charAt(1) == '-' || s.charAt(2) == '-') {
			sdf = "MM-dd-yyyy HH:mm:ss";
			break;
		    } else if (s.charAt(3) == ',') {
			sdf = "MMM, dd yyyy HH:mm:ss";
			break;
		    }
		    // fall-through
    
		  default:
		    if (s.indexOf(":") == -1) {
			sdf = "MMMMM, dd yyyy";
		    } else {
			if (s.indexOf(",") == -1) {
			    sdf = "MMMMM dd yyyy HH:mm:ss";
			} else {
			    sdf = "MMMMM, dd yyyy HH:mm:ss";
			}
		    }
		    break;
		}
		break;
    
	      case YEAR_MONTH_DAY: {
		switch(s.length()) {
		  case 8:
		  case 9:
		  case 10:
		    if (s.charAt(4) == '/') {
			sdf = "yyyy/MM/dd";
		    } else if (s.charAt(4) == '-') {
			sdf = "yyyy-MM-dd";
		    } else {
			sdf = "yyyyMMdd";
		    }
		    break;

		  case 15:
		    sdf = "yyyyMMdd'T'HHmmss";
		    break;
    
		  case 19:
		    if (s.charAt(4) == '/') {
			sdf = "yyyy/MM/dd HH:mm:ss";
		    } else {
			sdf = "yyyy-MM-dd HH:mm:ss";
		    }
		    break;
    
		  default:
		    break;
		}
		break;
	      }
	    }

	    if (sdf != null) {
		SimpleDateFormat df = new SimpleDateFormat(sdf);
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date date = df.parse(s);
		df.applyPattern("dd MMM yyyy HH:mm:ss z");
		logger.debug(JOVALMsg.STATUS_DATECONVERSION, s, format, df.format(date), date.getTime());
		return date.getTime();
	    }
	} catch (Exception e) {
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_TIME_PARSE, s, format));
	}

	throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_ILLEGAL_TIME, format, s));
    }

    /**
     * This class is a more or less exact reimplementation of the algorithm used by librpm's rpmvercmp(char* a, char* b)
     * function, as dictated by the OVAL specification. See:
     * http://oval.mitre.org/language/version5.10/ovaldefinition/documentation/oval-definitions-schema.html#EntityStateEVRStringType
     */
    class Evr {
	String evr;

	Evr(String evr) {
	    this.evr = evr;
	}

	/**
	 * Based on rpmvercmp.c
	 */
	int compareTo(Evr other) {
	    //
	    // Easy string comparison to check for equivalence
	    //
	    if (evr.equals(other.evr)) {
		return 0;
	    }

	    byte[] b1 = evr.getBytes();
	    byte[] b2 = other.evr.getBytes();
	    int i1 = 0, i2 = 0;
	    boolean isNum = false;

	    //
	    // Loop through each version segment of the EVRs and compare them
	    //
	    while (i1 < b1.length && i2 < b2.length) {
		while (i1 < b1.length && !isAlphanumeric(b1[i1])) i1++;
		while (i2 < b2.length && !isAlphanumeric(b2[i2])) i2++;

		//
		// If we ran into the end of either, we're done.
		//
		if (i1 == b1.length || i2 == b2.length) break;

		//
		// Grab the first completely alphanumeric segment and compare them
		//
		int start1 = i1, start2 = i2;
		if (isNumeric(b1[i1])) {
		    while (i1 < b1.length && isNumeric(b1[i1])) i1++;
		    while (i2 < b2.length && isNumeric(b2[i2])) i2++;
		    isNum = true;
		} else {
		    while (i1 < b1.length && isAlpha(b1[i1])) i1++;
		    while (i2 < b2.length && isAlpha(b2[i2])) i2++;
		    isNum = false;
		}

		if (i1 == start1) return -1; // arbitrary; shouldn't happen

		if (i2 == start2) return (isNum ? 1 : -1);

		if (isNum) {
		    int int1 = Integer.parseInt(new String(b1).substring(start1, i1));
		    int int2 = Integer.parseInt(new String(b2).substring(start2, i2));

		    if (int1 > int2) return 1;
		    if (int2 > int1) return -1;
		}

		int rc = new String(b1).substring(start1, i1).compareTo(new String(b2).substring(start2, i2));
		if (rc != 0) {
		    return (rc < 1 ? -1 : 1);
		}
	    }

	    // Take care of the case where all segments compare identically, but only the separator chars differed
	    if (i1 == b1.length && i2 == b2.length) return 0;

	    // Whichever version still has characters left over wins
	    if (i1 < b1.length) return 1;
	    return -1;
	}

	boolean isAlphanumeric(byte b) {
	    return isAlpha(b) || isNumeric(b);
	}

	boolean isAlpha(byte b) {
	    return StringTools.isLetter(b);
	}

	boolean isNumeric(byte b) {
	    return '0' <= b && b <= '9';
	}
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
