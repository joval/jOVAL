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
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;

import oval.schemas.common.CheckEnumeration;
import oval.schemas.common.ExistenceEnumeration;
import oval.schemas.common.GeneratorType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.OperatorEnumeration;
import oval.schemas.definitions.core.ArithmeticEnumeration;
import oval.schemas.definitions.core.ArithmeticFunctionType;
import oval.schemas.definitions.core.BeginFunctionType;
import oval.schemas.definitions.core.ConcatFunctionType;
import oval.schemas.definitions.core.ConstantVariable;
import oval.schemas.definitions.core.CriteriaType;
import oval.schemas.definitions.core.CriterionType;
import oval.schemas.definitions.core.DateTimeFormatEnumeration;
import oval.schemas.definitions.core.DefinitionType;
import oval.schemas.definitions.core.DefinitionsType;
import oval.schemas.definitions.core.EndFunctionType;
import oval.schemas.definitions.core.EntityObjectStringType;
import oval.schemas.definitions.core.EntitySimpleBaseType;
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

import org.joval.intf.oval.IResults;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IPlugin;
import org.joval.intf.util.IObserver;
import org.joval.intf.util.IProducer;
import org.joval.oval.OvalException;
import org.joval.oval.TestException;
import org.joval.oval.util.CheckData;
import org.joval.oval.util.ExistenceData;
import org.joval.oval.util.ItemSet;
import org.joval.oval.util.OperatorData;
import org.joval.util.JOVALSystem;
import org.joval.util.Producer;
import org.joval.util.StringTools;
import org.joval.windows.Timestamp;

/**
 * Engine that evaluates OVAL tests on remote hosts.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Engine implements IProducer {
    public static final BigDecimal SCHEMA_VERSION = new BigDecimal("5.9");

    public static final int MESSAGE_MIN				= 0;
    public static final int MESSAGE_OBJECT_PHASE_START		= 0;
    public static final int MESSAGE_OBJECT			= 1;
    public static final int MESSAGE_OBJECT_PHASE_END		= 2;
    public static final int MESSAGE_SYSTEMCHARACTERISTICS	= 3;
    public static final int MESSAGE_DEFINITION_PHASE_START	= 4;
    public static final int MESSAGE_DEFINITION			= 5;
    public static final int MESSAGE_DEFINITION_PHASE_END	= 6;
    public static final int MESSAGE_MAX				= 6;

    public enum Result {
	OK,
	ERR;
    }

    private enum State {
	CONFIGURE,
	RUNNING,
	COMPLETE_OK,
	COMPLETE_ERR;
    }

    /**
     * Unmarshal an XML file and return the OvalDefinitions root object.
     */
    public static final OvalDefinitions getOvalDefinitions(File f) throws OvalException {
	return Definitions.getOvalDefinitions(f);
    }

    public static final boolean schematronValidate(OvalDefinitions defs, File transform) {
	return Definitions.schematronValidate(defs, transform);
    }
    public static final List<String> getSchematronValidationErrors() {
	return Definitions.getSchematronValidationErrors();
    }

    private Hashtable <String, List<VariableValueType>>variableMap; // A cache of nested VariableValueTypes
    private Definitions definitions;
    private Variables externalVariables = null;
    private SystemCharacteristics sc = null;
    private File scOutputFile = null;
    private IPlugin plugin;
    private OvalException error;
    private Results results;
    private State state;
    private DefinitionFilter filter;
    private List<AdapterContext> adapterContextList;
    Producer producer;

    /**
     * Create an engine for evaluating OVAL definitions using a plugin.
     */
    public Engine(OvalDefinitions defs, IPlugin plugin) {
	definitions = new Definitions(defs);
	if (plugin == null) {
	    throw new RuntimeException(JOVALSystem.getMessage("ERROR_NO_SYSTEMCHARACTERISTICS"));
	}
	this.plugin = plugin;
	adapterContextList = new Vector<AdapterContext>();
	for (IAdapter adapter : plugin.getAdapters()) {
	    adapterContextList.add(new AdapterContext(adapter, this));
	}
	variableMap = new Hashtable<String, List<VariableValueType>>();
	producer = new Producer();
	filter = new DefinitionFilter();
	state = State.CONFIGURE;
    }

    /**
     * Create an engine for evaluating OVAL definitions against a SystemCharacteristics (for example, one that has been
     * loaded from a file).
     */
    public Engine(OvalDefinitions defs, IPlugin plugin, SystemCharacteristics sc) {
	this(defs, plugin);
	this.sc = sc;
    }

    /**
     * Set the file from which to read external variable definitions.
     */
    public void setExternalVariablesFile(File f) throws OvalException {
	externalVariables = new Variables(Variables.getOvalVariables(f));
    }

    /**
     * Set the file to which to save collected SystemCharacteristics data.
     */
    public void setSystemCharacteristicsOutputFile(File f) {
	scOutputFile = f;
    }

    /**
     * Set a list of definition IDs to evaluate during the run phase.
     */
    public void setDefinitionFilter(DefinitionFilter filter) {
	this.filter = filter;
    }

    /**
     * Returns Result.OK or Result.ERR, or throws an exception if the engine hasn't run, or is running.
     */
    public Result getResult() throws IllegalThreadStateException {
	switch(state) {
	  case COMPLETE_OK:
	    return Result.OK;

	  case COMPLETE_ERR:
	    return Result.ERR;

	  case CONFIGURE:
	  case RUNNING:
	  default:
	    throw new IllegalThreadStateException(JOVALSystem.getMessage("ERROR_ENGINE_STATE", state));
	}
    }

    /**
     * Return the results.  Only valid after the run() method is called.
     */
    public IResults getResults() throws IllegalThreadStateException {
	getResult();
	return results;
    }

    public OvalException getError() {
	getResult();
	return error;
    }

    // Implement IProducer

    public void addObserver(IObserver observer, int min, int max) {
	producer.addObserver(observer, min, max);
    }

    public void removeObserver(IObserver observer) {
	producer.removeObserver(observer);
    }

    // Implement Runnable

    /**
     * Do what one might do with an Engine if one were to do it in its own Thread.
     */
    public void run() {
	state = State.RUNNING;
	try {
	    if (sc == null) {
		scan();
		if (scOutputFile != null) {
		    producer.sendNotify(this, MESSAGE_SYSTEMCHARACTERISTICS, scOutputFile.toString());
		    sc.write(scOutputFile);
		}
	    }
	    results = new Results(definitions, sc);
	    producer.sendNotify(this, MESSAGE_DEFINITION_PHASE_START, null);

	    //
	    // Use the filter to separate the definitions into allowed and disallowed lists.  First evaluate all the allowed
	    // definitions, then go through the disallowed definitions.  This makes it possible to cache both test and
	    // definition results without having to double-check if they were previously intentionally skipped.
	    //
	    List<DefinitionType>allowed = new Vector<DefinitionType>();
	    List<DefinitionType>disallowed = new Vector<DefinitionType>();
	    definitions.filterDefinitions(filter, allowed, disallowed);

	    filter.setEvaluationAllowed(true);
	    for (DefinitionType definition : allowed) {
		evaluateDefinition(definition);
	    }

	    filter.setEvaluationAllowed(false);
	    for (DefinitionType definition : disallowed) {
		evaluateDefinition(definition);
	    }

	    producer.sendNotify(this, MESSAGE_DEFINITION_PHASE_END, null);
	    state = State.COMPLETE_OK;
	} catch (OvalException e) {
	    error = e;
	    state = State.COMPLETE_ERR;
	}
    }

    // Internal

    static final GeneratorType getGenerator() {
	GeneratorType generator = JOVALSystem.factories.common.createGeneratorType();
	generator.setProductName(JOVALSystem.getProperty(JOVALSystem.PROP_PRODUCT));
	generator.setProductVersion(JOVALSystem.getProperty(JOVALSystem.PROP_VERSION));
	generator.setSchemaVersion(SCHEMA_VERSION);
	try {
	    generator.setTimestamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
	} catch (DatatypeConfigurationException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_TIMESTAMP"), e);
	}
	return generator;
    }

    IAdapter getAdapterForObject(Class clazz) {
	for (AdapterContext ctx : adapterContextList) {
	    if (clazz.equals(ctx.getAdapter().getObjectClass())) {
		return ctx.getAdapter();
	    }
	}
	return null;
    }

    IAdapter getAdapterForTest(oval.schemas.definitions.core.TestType testDefinition) throws OvalException {
	String objectId = getObjectRef(testDefinition);
	if (objectId == null) {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_OBJECT_MISSING", testDefinition.getId()));
	} else {
	    return getAdapterForObject(definitions.getObject(objectId).getClass());
	}
    }

    IAdapter getAdapterForState(Class clazz) {
	for (AdapterContext ctx : adapterContextList) {
	    if (clazz.equals(ctx.getAdapter().getStateClass())) {
		return ctx.getAdapter();
	    }
	}
	return null;
    }

    AdapterContext getAdapterContext(IAdapter adapter) throws NoSuchElementException {
	for (AdapterContext ac : adapterContextList) {
	    if (ac.getAdapter().equals(adapter)) {
		return ac;
	    }
	}
	throw new NoSuchElementException(adapter.toString());
    }

    /**
     * Return the value of the Variable with the specified ID, and also add any chained variables to the provided list.
     */
    List<String> resolve(String id, List<VariableValueType> vars) throws NoSuchElementException, OvalException {
	VariableType var = definitions.getVariable(id);
	String varId = var.getId();
	List<VariableValueType> cachedList = variableMap.get(varId);
	if (cachedList == null) {
	    JOVALSystem.getLogger().log(Level.FINER, JOVALSystem.getMessage("STATUS_VARIABLE_CREATE", varId));
	    List<String> result = resolveInternal(var, vars);
	    variableMap.put(varId, vars);
	    return result;
	} else {
	    JOVALSystem.getLogger().log(Level.FINER, JOVALSystem.getMessage("STATUS_VARIABLE_RECYCLE", varId));
	    List<String> result = new Vector<String>();
	    vars.addAll(cachedList);
	    for (VariableValueType variableValueType : cachedList) {
		if (variableValueType.getVariableId().equals(id)) {
		    result.add((String)variableValueType.getValue());
		}
	    }
	    if (result.size() > 0) {
		return result;
	    } else {
		throw new OvalException(JOVALSystem.getMessage("ERROR_MISSING_VARIABLE", id));
	    }
	}
    }

    void addObjectMessage(String objectId, MessageType msg) {
	sc.setObject(objectId, null, null, null, msg);
    }

    // Private

    /**
     * Scan SystemCharactericts using the plugin.
     */
    private void scan() throws OvalException {
	producer.sendNotify(this, MESSAGE_OBJECT_PHASE_START, null);
	sc = new SystemCharacteristics(plugin);


	//
	// First, find all the object types that are referenced by variables
	//
	List<Class> varTypes = new Vector<Class>();
	Iterator<VariableType> varIter = definitions.iterateVariables();
	while(varIter.hasNext()) {
	    VariableType vt = varIter.next();
	    getObjectClasses(vt, varTypes);
	}

	//
	// Next, scan all those types (for which there are adapters)
	//
	for (Class clazz : varTypes) {
	    IAdapter adapter = getAdapterForObject(clazz);
	    if (adapter != null) {
		scanAdapter(adapter);
	    }
	}

	//
	// Then, scan all remaining types for which there are adapters
	//
	List<Class> allTypes = getObjectClasses(definitions.iterateObjects());
	for (Class clazz : allTypes) {
	    if (!varTypes.contains(clazz)) {
		IAdapter adapter = getAdapterForObject(clazz);
		if (adapter != null) {
		    scanAdapter(adapter);
		}
	    }
	}

	//
	// Finally, add all objects for which there are no adapters, and flag them NOT_COLLECTED
	//
	Iterator<ObjectType> allObjects = definitions.iterateObjects();
	while(allObjects.hasNext()) {
	    ObjectType obj = allObjects.next();
	    String objectId = obj.getId();
	    if (!sc.containsObject(objectId)) {
		MessageType message = JOVALSystem.factories.common.createMessageType();
		message.setLevel(MessageLevelEnumeration.WARNING);
		message.setValue(JOVALSystem.getMessage("ERROR_ADAPTER_MISSING", obj.getClass().getName()));
		sc.setObject(objectId, obj.getComment(), obj.getVersion(), FlagEnumeration.NOT_COLLECTED, message);
	    }
	}
	producer.sendNotify(this, MESSAGE_OBJECT_PHASE_END, null);
    }

    /**
     * Fetch all items associated with objects serviced by the given adapter, and store them in the SystemCharacteristics.
     */
    private void scanAdapter(IAdapter adapter) throws OvalException {
	if (adapter.connect()) {
	    AdapterContext adapterContext = getAdapterContext(adapter);
	    adapterContext.setActive(true);
	    try {
		Iterator<ObjectType> iter = definitions.iterateObjects(adapter.getObjectClass());
		while (iter.hasNext()) {
		    ObjectType obj = iter.next();
		    String objectId = obj.getId();

		    Boolean isSetSet = (Boolean)safeInvokeMethod(obj, "isSetSet");
		    if (isSetSet != null && isSetSet.booleanValue()) {
			//
			// Is a Set
			//
			Set s = getObjectSet(obj);
			if (s != null) {
			    try {
				sc.setObject(objectId, obj.getComment(), obj.getVersion(), FlagEnumeration.COMPLETE, null);
				for (ItemType itemType : getSetItems(s)) {
				    sc.relateItem(objectId, itemType.getId());
				}
			    } catch (NoSuchElementException e) {
				throw new OvalException(e);
			    }
			}
		    } else {
			//
			// If items have been retrieved in the course of resolving a Set or a Variable, then skip
			// its collection.
			//
			try {
			    sc.getObject(objectId);
			} catch (NoSuchElementException e) {
			    scanObject(obj, new Vector<VariableValueType>());
			}
		    }
		}
	    } finally {
		adapterContext.setActive(false);
		adapter.disconnect();
	    }
	}
    }

    /**
     * Scan an object live using an adapter, including crawling down any encountered Sets.
     */
    private List<ItemType> scanObject(ObjectType obj, List<VariableValueType> vars) throws OvalException {
	String objectId = obj.getId();
	producer.sendNotify(this, MESSAGE_OBJECT, objectId);

	Set s = getObjectSet(obj);
	if (s == null) {
	    IAdapter adapter = getAdapterForObject(obj.getClass());
	    if (adapter == null) {
		throw new OvalException(JOVALSystem.getMessage("ERROR_ADAPTER_MISSING", obj.getClass().getName()));
	    } else if (getAdapterContext(adapter).isActive()) {
		List<JAXBElement<? extends ItemType>> items = adapter.getItems(obj, vars);
		if (items.size() == 0) {
		    MessageType msg = JOVALSystem.factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.INFO);
		    msg.setValue(JOVALSystem.getMessage("STATUS_EMPTY_OBJECT"));
		    sc.setObject(objectId, obj.getComment(), obj.getVersion(), FlagEnumeration.DOES_NOT_EXIST, msg);
		} else {
		    sc.setObject(objectId, obj.getComment(), obj.getVersion(), FlagEnumeration.COMPLETE, null);
		    for (JAXBElement<? extends ItemType> item : items) {
			BigInteger itemId = sc.storeItem(item);
			sc.relateItem(objectId, itemId);
		    }
		}
		for (VariableValueType var : vars) {
		    sc.storeVariable(var);
		    sc.relateVariable(objectId, var.getVariableId());
		}
		List<ItemType> unwrapped = new Vector<ItemType>();
		for (JAXBElement<? extends ItemType> item : items) {
		    unwrapped.add(item.getValue());
		}
		return unwrapped;
	    } else {
		throw new OvalException(JOVALSystem.getMessage("ERROR_ADAPTER_UNAVAILABLE", obj.getClass().getName()));
	    }
	} else {
	    try {
		List<ItemType> items = getSetItems(s);
		sc.setObject(objectId, obj.getComment(), obj.getVersion(), FlagEnumeration.COMPLETE, null);
		for (ItemType item : items) {
		    sc.relateItem(objectId, item.getId());
		}
		return items;
	    } catch (NoSuchElementException e) {
		throw new OvalException(e);
	    }
	}
    }

    /**
     * Get a list of items belonging to a Set.
     */
    private List<ItemType> getSetItems(Set s) throws NoSuchElementException, OvalException {
	//
	// First, retrieve the filtered list of items in the Set, recursively.
	//
	List<List<ItemType>> lists = new Vector<List<ItemType>>();
	if (s.isSetSet()) {
	    for (Set set : s.getSet()) {
		lists.add(getSetItems(set));
	    }
	} else {
	    for (String objectId : s.getObjectReference()) {
		List<ItemType> items = null;
		try {
		    items = sc.getItemsByObjectId(objectId);
		} catch (NoSuchElementException e) {
		    items = scanObject(definitions.getObject(objectId), new Vector<VariableValueType>());
		}
		//
		// Apply filters, if any
		// DAS: needs better error-handling
		//
		List<ItemType> filteredItems = new Vector<ItemType>();
		List<Filter> filters = s.getFilter();
		if (filters.size() > 0) {
		    for (Filter filter : s.getFilter()) {
			StateType state = definitions.getState(filter.getValue());
			for (ItemType item : items) {
			    try {
				ResultEnumeration result = getAdapterForState(state.getClass()).compare(state, item);
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
				JOVALSystem.getLogger().log(Level.WARNING, e.getMessage(), e);
			    }
			}
		    }
		    lists.add(filteredItems);
		} else {
		    lists.add(items);
		}
	    }
	}

	switch(s.getSetOperator()) {
	  case INTERSECTION: {
	    ItemSet intersection = null;
	    for (List<ItemType> items : lists) {
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
		return new ItemSet(lists.get(0)).complement(new ItemSet(lists.get(1))).toList();
	    } else {
		throw new OvalException(JOVALSystem.getMessage("ERROR_SET_COMPLEMENT", new Integer(lists.size())));
	    }
	  }

	  case UNION:
	  default: {
	    ItemSet union = new ItemSet();
	    for (List<ItemType> items : lists) {
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
	    throw new OvalException(JOVALSystem.getMessage("ERROR_DEFINITION_NOID"));
	}
	producer.sendNotify(this, MESSAGE_DEFINITION, defId);
	oval.schemas.results.core.DefinitionType defResult = results.getDefinition(defId);

	if (defResult == null) {
	    defResult = JOVALSystem.factories.results.createDefinitionType();
	    defResult.setDefinitionId(defId);
	    defResult.setVersion(defDefinition.getVersion());
	    defResult.setClazz(defDefinition.getClazz());
	    oval.schemas.results.core.CriteriaType criteriaResult = evaluateCriteria(defDefinition.getCriteria());
	    defResult.setResult(criteriaResult.getResult());
	    defResult.setCriteria(evaluateCriteria(defDefinition.getCriteria()));
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

	    if (filter.getEvaluationAllowed()) {
		if (testDefinition instanceof UnknownTest) {
		    testResult.setResult(ResultEnumeration.UNKNOWN);
		} else {
		    IAdapter adapter = getAdapterForTest(testDefinition);
		    if (adapter == null) {
			MessageType message = JOVALSystem.factories.common.createMessageType();
			message.setLevel(MessageLevelEnumeration.WARNING);
			message.setValue(JOVALSystem.getMessage("ERROR_ADAPTER_MISSING", testDefinition.getClass().getName()));
			testResult.getMessage().add(message);
			testResult.setResult(ResultEnumeration.NOT_EVALUATED);
		    } else if (getObjectRef(testDefinition) == null) {
			throw new OvalException(JOVALSystem.getMessage("ERROR_TEST_NOOBJREF", testId));
		    } else {
			evaluateTest(testResult, adapter);
		    }
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

    private void evaluateTest(TestType testResult, IAdapter adapter) throws OvalException {
	String testId = testResult.getTestId();
	oval.schemas.definitions.core.TestType testDefinition = definitions.getTest(testId);
	String objectId = getObjectRef(testDefinition);
	String stateId = getStateRef(testDefinition);
	StateType state = null;
	if (stateId != null) {
	    state = definitions.getState(stateId, adapter.getStateClass());
	}

	for (VariableValueType var : sc.getVariablesByObjectId(objectId)) {
	    TestedVariableType testedVariable = JOVALSystem.factories.results.createTestedVariableType();
	    testedVariable.setVariableId(var.getVariableId());
	    testedVariable.setValue(var.getValue());
	    testResult.getTestedVariable().add(testedVariable);
	}

	ExistenceData existence = new ExistenceData();
	CheckData check = new CheckData();

	List<ItemType> items = sc.getItemsByObjectId(objectId);
	for (ItemType item : items) {
	    if (item.getClass().getName().equals(adapter.getItemClass().getName())) {
		TestedItemType testedItem = JOVALSystem.factories.results.createTestedItemType();
		testedItem.setItemId(item.getId());
		testedItem.setResult(ResultEnumeration.NOT_EVALUATED);

		StatusEnumeration status = item.getStatus();
		switch(status) {
		  case EXISTS:
		    if (state != null) {
			ResultEnumeration checkResult = ResultEnumeration.UNKNOWN;

			switch(sc.getObject(objectId).getFlag()) {
			  case COMPLETE:
			  case INCOMPLETE:
			  case DOES_NOT_EXIST:
			    try {
				checkResult = adapter.compare(state, item);
			    } catch (TestException e) {
				String s = JOVALSystem.getMessage("ERROR_TESTEXCEPTION", testId, e.getMessage());
				JOVALSystem.getLogger().log(Level.WARNING, s, e);
				MessageType message = JOVALSystem.factories.common.createMessageType();
				message.setLevel(MessageLevelEnumeration.ERROR);
				message.setValue(e.getMessage());
				testedItem.getMessage().add(message);
				checkResult = ResultEnumeration.ERROR;
			    }
			    break;

			  case ERROR:
			    checkResult = ResultEnumeration.ERROR;
			    break;
			  case NOT_APPLICABLE:
			    checkResult = ResultEnumeration.NOT_APPLICABLE;
			    break;
			  case NOT_COLLECTED:
			  default:
			    checkResult = ResultEnumeration.UNKNOWN;
			    break;
			}

			testedItem.setResult(checkResult);
			check.addResult(checkResult);
		    }
		    // fall-thru

		  default:
		    existence.addStatus(status);
		    break;
		}

		testResult.getTestedItem().add(testedItem);
	    } else {
		throw new OvalException(JOVALSystem.getMessage("ERROR_INSTANCE", adapter.getItemClass().getName(),
							       item.getClass().getName()));
	    }
	}

	ResultEnumeration existenceResult = existence.getResult(testDefinition.getCheckExistence());
	if (state == null) {
	    testResult.setResult(existenceResult);
	} else {
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
		oval.schemas.results.core.ExtendDefinitionType edtResult = JOVALSystem.factories.results.createExtendDefinitionType();
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
		throw new OvalException(JOVALSystem.getMessage("ERROR_BAD_COMPONENT", child.getClass().getName()));
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

    /**
     * Get a List of all the distinct ObjectType classes that appear in the Iterator.
     */
    private List<Class> getObjectClasses(Iterator<ObjectType> iter) {
	List<Class> classes = new Vector<Class>();
	while(iter.hasNext()) {
	    Class clazz = iter.next().getClass();
	    if (!classes.contains(clazz)) {
		classes.add(clazz);
	    }
	}
	return classes;
    }

    /**
     * Crawl down the rabbit hole and add any new object type class names to the list.
     */
    private void getObjectClasses(Object object, List<Class> list) throws OvalException {
	if (object == null) {
	    return; // Terminated in a constant type
	} else if (object instanceof VariableComponentType) {
	    return; // The variable this references will add any classes.
	} else if (object instanceof ExternalVariable) {
	    return; // Just a String.
	} else if (object instanceof ConstantVariable) {
	    return; // Just a String.
	} else if (object instanceof LiteralComponentType) {
	    return; // Just a String.
	} else if (object instanceof ObjectComponentType) {
	    ObjectComponentType oct = (ObjectComponentType)object;
	    String objectId = oct.getObjectRef();
	    Class clazz = definitions.getObject(objectId).getClass();
	    if (!list.contains(clazz)) {
		list.add(clazz);
	    }
	} else {
	    Object obj = getComponent(object);
	    if (obj instanceof List) {
		for (Object child : (List)obj) {
		    getObjectClasses(child, list);
		}
	    } else {
		getObjectClasses(getComponent(object), list);
	    }
	}
    }

    /**
     * Resolve the component.  If the component is a variable, resolve it and append it to the list, appending its value to
     * the value that is ultimately returned.
     *
     * @see http://oval.mitre.org/language/version5.9/ovaldefinition/documentation/oval-definitions-schema.html#FunctionGroup
     *
     * DAS: TBD for 5.10: implementations for Count and Unique functions
     */
    private List<String> resolveInternal(Object object, List <VariableValueType>list)
		throws NoSuchElementException, OvalException {
	//
	// Why do variables point to variables?  Because sometimes they are nested.
	//
	if (object instanceof LocalVariable) {
	    LocalVariable localVariable = (LocalVariable)object;
	    List<String> values = resolveInternal(getComponent(localVariable), list);
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
		throw new OvalException(JOVALSystem.getMessage("ERROR_EXTERNAL_VARIABLE_SOURCE", id));
	    } else {
		List<String> values = externalVariables.getValue(id);
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
	    List<String> values = new Vector<String>();
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
	    List<String> values = new Vector<String>();
	    values.add((String)((LiteralComponentType)object).getValue());
	    return values;

	//
	// Retrieve from an ItemType (which possibly has to be fetched from an adapter)
	//
	} else if (object instanceof ObjectComponentType) {
	    ObjectComponentType oc = (ObjectComponentType)object;
	    String objectId = oc.getObjectRef();
	    List<ItemType> items = null;
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
		items = scanObject(ot, list);
	    }
	    List<String> values = extractItemData(objectId, oc, items);
	    if (values == null || values.size() == 0) {
		throw new NoSuchElementException(oc.getObjectRef());
	    } else {
		return values;
	    }

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
	    List<String> values = new Vector<String>();
	    ConcatFunctionType concat = (ConcatFunctionType)object;
	    for (Object child : concat.getObjectComponentOrVariableComponentOrLiteralComponent()) {
		List<String> next = resolveInternal(child, list);
		if (values.size() == 0) {
		    values.addAll(next);
		} else {
		    List<String> newValues = new Vector<String>();
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
	    List<String> values = new Vector<String>();
	    for (String value : resolveInternal(getComponent((EscapeRegexFunctionType)object), list)) {
		values.add(escapeRegex(value));
	    }
	    return values;

	//
	// Process a Split, which contains a component and a delimiter with which to split it up.
	//
	} else if (object instanceof SplitFunctionType) {
	    SplitFunctionType split = (SplitFunctionType)object;
	    List<String> values = new Vector<String>();
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
	    List<String> values = new Vector<String>();
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
	    List<String> values = new Vector<String>();
	    for (String value : resolveInternal(getComponent(st), list)) {
		if (start > value.length()) {
		    throw new OvalException(JOVALSystem.getMessage("ERROR_SUBSTRING", value, new Integer(start)));
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
	    List<String> values = new Vector<String>();
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
	    List<String> values = new Vector<String>();
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
	    List<String> values = new Vector<String>();
	    List<Object> children = tt.getObjectComponentOrVariableComponentOrLiteralComponent();
	    List<String> timestamp1;
	    List<String> timestamp2;
	    if (children.size() == 1) {
		timestamp1 = new Vector<String>();
		timestamp1.add(new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(new Date()));
		timestamp2 = resolveInternal(children.get(0), list);
	    } else if (children.size() == 2) {
		timestamp1 = resolveInternal(children.get(0), list);
		timestamp2 = resolveInternal(children.get(1), list);
	    } else {
		throw new OvalException(JOVALSystem.getMessage("ERROR_BAD_TIMEDIFFERENCE", new Integer(children.size())));
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
	    Stack<List<String>> rows = new Stack<List<String>>();
	    ArithmeticEnumeration op = at.getArithmeticOperation();
	    for (Object child : at.getObjectComponentOrVariableComponentOrLiteralComponent()) {
		List<String> row = new Vector<String>();
		for (String cell : resolveInternal(child, list)) {
		    row.add(cell);
		}
		rows.add(row);
	    }
	    if (rows.empty()) {
		throw new NoSuchElementException(JOVALSystem.getMessage("ERROR_COMPONENT_EMPTY"));
	    } else {
		return computeProduct(op, rows);
	    }

	} else {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_COMPONENT", object.getClass().getName()));
	}
    }

    private static final String ESCAPE = "\\";
    private static final String[] REGEX_CHARS = {ESCAPE, "^", ".", "$", "|", "(", ")", "[", "]", "{", "}", "*", "+", "?"};

    private String escapeRegex(String s) {
	Stack<String> delims = new Stack<String>();
	for (int i=0; i < REGEX_CHARS.length; i++) {
	    delims.add(REGEX_CHARS[i]);
	}
	return safeEscape(delims, s);
    }

    private String safeEscape(Stack<String> delims, String s) {
	if (delims.empty()) {
	    return s;
	} else {
	    String delim = delims.pop();
	    Stack<String> copy = new Stack<String>();
	    copy.addAll(delims);
	    List<String> list = StringTools.toList(StringTools.tokenize(s, delim, false));
	    int len = list.size();
	    StringBuffer result = new StringBuffer();
	    for (int i=0; i < len; i++) {
		    if (i > 0) {
			result.append(ESCAPE);
			result.append(delim);
		    }
		    result.append(safeEscape(copy, list.get(i)));
	    }
	    return result.toString();
	}
    }

    /**
     * Perform the Arithmetic operation on permutations of the Stack, and return the resulting permutations.
     */
    private List<String> computeProduct(ArithmeticEnumeration op, Stack<List<String>> rows) {
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
		Stack<List<String>> copy = new Stack<List<String>>();
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
    private List<String> extractItemData(String objectId, ObjectComponentType oc, List list) throws OvalException {
	if (list.size() == 0) {
	    return null;
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
		    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_REFLECTION", e.getMessage()), e);
		    return null;
		} catch (IllegalAccessException e) {
		    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_REFLECTION", e.getMessage()), e);
		    return null;
		} catch (InvocationTargetException e) {
		    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_REFLECTION", e.getMessage()), e);
		    return null;
		}
	    }

	    if (o instanceof JAXBElement) {
		o = ((JAXBElement)o).getValue();
	    }
	    if (o instanceof EntityItemSimpleBaseType) {
		EntityItemSimpleBaseType entity = (EntityItemSimpleBaseType)o;
		values.add((String)entity.getValue());
	    } else if (o instanceof List) {
		return extractItemData(objectId, null, (List)o);
	    } else if (o instanceof EntityItemRecordType) {
		EntityItemRecordType record = (EntityItemRecordType)o;
		String fieldName = oc.getRecordField();
		for (EntityItemFieldType field : record.getField()) {
		    if (field.getName().equals(fieldName)) {
			values.add((String)field.getValue());
		    }
		}
	    } else {
		throw new OvalException(JOVALSystem.getMessage("ERROR_REFLECTION", o.getClass().getName()));
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
	obj = safeInvokeMethod(unknown, "getVariableComponent");
	if (obj != null) {
	    return obj;
	}
	obj = safeInvokeMethod(unknown, "getObjectComponentOrVariableComponentOrLiteralComponent");
	if (obj != null) {
	    return obj;
	}

	throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_COMPONENT", unknown.getClass().getName()));
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
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_REFLECTION", e.getMessage()), e);
	} catch (InvocationTargetException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_REFLECTION", e.getMessage()), e);
	}
	return result;
    }

    private String getObjectRef(oval.schemas.definitions.core.TestType test) {
	try {
	    Method getObject = test.getClass().getMethod("getObject");
	    ObjectRefType objectRef = (ObjectRefType)getObject.invoke(test);
	    if (objectRef != null) {
		return objectRef.getObjectRef();
	    }
	} catch (NoSuchMethodException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_REFLECTION", e.getMessage()), e);
	} catch (IllegalAccessException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_REFLECTION", e.getMessage()), e);
	} catch (InvocationTargetException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_REFLECTION", e.getMessage()), e);
	}
	return null;
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
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_REFLECTION", e.getMessage()), e);
	} catch (IllegalAccessException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_REFLECTION", e.getMessage()), e);
	} catch (InvocationTargetException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_REFLECTION", e.getMessage()), e);
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
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_REFLECTION", e.getMessage()), e);
	} catch (InvocationTargetException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_REFLECTION", e.getMessage()), e);
	}
	return objectSet;
    }

    /**
     * @see http://oval.mitre.org/language/version5.9/ovaldefinition/documentation/oval-definitions-schema.html#DateTimeFormatEnumeration
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
		return df.parse(s).getTime();
	    }
	} catch (Exception e) {
	    JOVALSystem.getLogger().log(Level.WARNING, e.getMessage(), e);
	    throw new OvalException(JOVALSystem.getMessage("ERROR_TIME_PARSE", s, format, e.getMessage()));
	}

	throw new OvalException(JOVALSystem.getMessage("ERROR_ILLEGAL_TIME", format, s));
    }
}
