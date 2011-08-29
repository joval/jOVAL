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
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
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
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.util.IObserver;
import org.joval.intf.util.IProducer;
import org.joval.os.windows.Timestamp;
import org.joval.oval.OvalException;
import org.joval.oval.ResolveException;
import org.joval.oval.TestException;
import org.joval.oval.util.CheckData;
import org.joval.oval.util.ExistenceData;
import org.joval.oval.util.ItemSet;
import org.joval.oval.util.OperatorData;
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

    private Hashtable <String, Collection<VariableValueType>>variableMap; // A cache of nested VariableValueTypes
    private Definitions definitions;
    private Variables externalVariables = null;
    private SystemCharacteristics sc = null;
    private File scOutputFile = null;
    private IPlugin plugin;
    private OvalException error;
    private Results results;
    private State state;
    private DefinitionFilter filter;
    private Hashtable<Class, AdapterManager> adapters;
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
	adapters = new Hashtable<Class, AdapterManager>();
	for (IAdapter adapter : plugin.getAdapters()) {
	    adapters.put(adapter.getObjectClass(), new AdapterManager(adapter));
	}
	variableMap = new Hashtable<String, Collection<VariableValueType>>();
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
    public void setExternalVariablesFile(File f) throws IllegalThreadStateException, OvalException {
	switch(state) {
	  case CONFIGURE:
	    externalVariables = new Variables(Variables.getOvalVariables(f));
	    break;

	  default:
	    throw new IllegalThreadStateException(JOVALSystem.getMessage("ERROR_ENGINE_STATE", state));
	}
    }

    /**
     * Set the file to which to save collected SystemCharacteristics data.
     */
    public void setSystemCharacteristicsOutputFile(File f) throws IllegalThreadStateException {
	switch(state) {
	  case CONFIGURE:
	    scOutputFile = f;
	    break;

	  default:
	    throw new IllegalThreadStateException(JOVALSystem.getMessage("ERROR_ENGINE_STATE", state));
	}
    }

    /**
     * Set a list of definition IDs to evaluate during the run phase.
     */
    public void setDefinitionFilter(DefinitionFilter filter) throws IllegalThreadStateException {
	switch(state) {
	  case CONFIGURE:
	    this.filter = filter;
	    break;

	  default:
	    throw new IllegalThreadStateException(JOVALSystem.getMessage("ERROR_ENGINE_STATE", state));
	}
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
     * Return the scan IResults (valid if getResult returned COMPLETE_OK).  Only valid after the run() method has finished.
     */
    public IResults getResults() throws IllegalThreadStateException {
	getResult();
	return results;
    }

    /**
     * Return the error (valid if getResult returned COMPLETE_ERR).  Only valid after the run() method has finished.
     */
    public OvalException getError() throws IllegalThreadStateException {
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
	    JOVALSystem.getLogger().log(Level.FINER, JOVALSystem.getMessage("STATUS_VARIABLE_CREATE", varId));
	    Collection<String> result = resolveInternal(var, vars);
	    variableMap.put(varId, vars);
	    return result;
	} else {
	    JOVALSystem.getLogger().log(Level.FINER, JOVALSystem.getMessage("STATUS_VARIABLE_RECYCLE", varId));
	    List<String> result = new Vector<String>();
	    vars.addAll(cachedList);
	    for (VariableValueType variableValueType : cachedList) {
		if (variableValueType.getVariableId().equals(variableId)) {
		    result.add((String)variableValueType.getValue());
		}
	    }
	    if (result.size() > 0) {
		return result;
	    } else {
		throw new OvalException(JOVALSystem.getMessage("ERROR_MISSING_VARIABLE", variableId));
	    }
	}
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
	// Second, scan all those types (for which there are adapters)
	//
	for (Class clazz : varTypes) {
	    AdapterManager manager = adapters.get(clazz);
	    if (manager != null) {
		scanAdapter(manager);
	    }
	}

	//
	// Third, scan all remaining types for which there are adapters
	//
	List<Class> allTypes = getObjectClasses(definitions.iterateObjects());
	for (Class clazz : allTypes) {
	    if (!varTypes.contains(clazz)) {
		AdapterManager manager = adapters.get(clazz);
		if (manager != null) {
		    scanAdapter(manager);
		}
	    }
	}

	//
	// Finally, add all objects for which there are no adapters and flag them NOT_COLLECTED
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
     * Fetch all items associated with objects serviced by the given adapter and store them in the SystemCharacteristics.
     */
    private void scanAdapter(AdapterManager manager) throws OvalException {
	IAdapter adapter = manager.getAdapter();
	if (adapter.connect()) {
	    manager.setActive(true);
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
			    scanObject(new RequestContext(this, obj));
			}
		    }
		}
	    } finally {
		manager.setActive(false);
		adapter.disconnect();
	    }
	}
    }

    /**
     * Scan an object live using an adapter, including crawling down any encountered Sets.
     */
    private Collection<ItemType> scanObject(RequestContext rc) throws OvalException {
	ObjectType obj = rc.getObject();
	String objectId = obj.getId();
	producer.sendNotify(this, MESSAGE_OBJECT, objectId);

	Set s = getObjectSet(obj);
	if (s == null) {
	    AdapterManager manager = adapters.get(obj.getClass());
	    if (manager == null) {
		throw new OvalException(JOVALSystem.getMessage("ERROR_ADAPTER_MISSING", obj.getClass().getName()));
	    } else if (manager.isActive()) {
		Collection<JAXBElement<? extends ItemType>> items = manager.getAdapter().getItems(rc);
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
		for (VariableValueType var : rc.getVars()) {
		    sc.storeVariable(var);
		    sc.relateVariable(objectId, var.getVariableId());
		}
		Collection<ItemType> unwrapped = new Vector<ItemType>();
		for (JAXBElement<? extends ItemType> item : items) {
		    unwrapped.add(item.getValue());
		}
		return unwrapped;
	    } else {
		throw new OvalException(JOVALSystem.getMessage("ERROR_ADAPTER_UNAVAILABLE", obj.getClass().getName()));
	    }
	} else {
	    try {
		Collection<ItemType> items = getSetItems(s);
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
		//
		// Apply filters, if any
		// DAS: needs better error-handling
		//
		Collection<ItemType> filteredItems = new Vector<ItemType>();
		Collection<Filter> filters = s.getFilter();
		if (filters.size() > 0) {
		    for (Filter filter : s.getFilter()) {
			StateType state = definitions.getState(filter.getValue());
			for (ItemType item : items) {
			    try {
				ResultEnumeration result = compare(state, item);
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
		throw new OvalException(JOVALSystem.getMessage("ERROR_SET_COMPLEMENT", new Integer(lists.size())));
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
	oval.schemas.definitions.core.TestType testDefinition = definitions.getTest(testId);
	String objectId = getObjectRef(testDefinition);
	String stateId = getStateRef(testDefinition);
	StateType state = null;
	if (stateId != null) {
	    state = definitions.getState(stateId);
	}

	for (VariableValueType var : sc.getVariablesByObjectId(objectId)) {
	    TestedVariableType testedVariable = JOVALSystem.factories.results.createTestedVariableType();
	    testedVariable.setVariableId(var.getVariableId());
	    testedVariable.setValue(var.getValue());
	    testResult.getTestedVariable().add(testedVariable);
	}

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
			    checkResult = compare(state, item);
			} catch (TestException e) {
			    String s = JOVALSystem.getMessage("ERROR_TESTEXCEPTION", testId, e.getMessage());
			    JOVALSystem.getLogger().log(Level.WARNING, s, e);
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

    private ResultEnumeration compare(StateType state, ItemType item) throws OvalException, TestException {
	try {
	    String stateClassname = state.getClass().getName();
	    String stateBaseClassname = stateClassname.substring(stateClassname.lastIndexOf(".")+1);
	    for (String methodName : getMethodNames(state.getClass())) {
		if (methodName.startsWith("get") && !stateMethodNames.contains(methodName)) {
		    Object stateEntityObj = state.getClass().getMethod(methodName).invoke(state);
		    if (stateEntityObj != null && stateEntityObj instanceof EntityStateSimpleBaseType) {
			EntityStateSimpleBaseType stateEntity = (EntityStateSimpleBaseType)stateEntityObj;
			Object itemEntityObj = item.getClass().getMethod(methodName).invoke(item);
			ResultEnumeration result = ResultEnumeration.UNKNOWN;
			if (itemEntityObj instanceof EntityItemSimpleBaseType || itemEntityObj == null) {
			    result = compare(stateEntity, (EntityItemSimpleBaseType)itemEntityObj);
			} else if (itemEntityObj instanceof JAXBElement) {
			    JAXBElement element = (JAXBElement)itemEntityObj;
			    EntityItemSimpleBaseType itemEntity = (EntityItemSimpleBaseType)element.getValue();
			    result = compare(stateEntity, itemEntity);
			} else if (itemEntityObj instanceof Collection) {
			    CheckData cd = new CheckData();
			    for (Object entityObj : (Collection)itemEntityObj) {
			        EntityItemSimpleBaseType itemEntity = (EntityItemSimpleBaseType)entityObj;
				cd.addResult(compare(stateEntity, itemEntity));
			    }
			    result = cd.getResult(stateEntity.getEntityCheck());
			} else {
			    String message = JOVALSystem.getMessage("ERROR_UNSUPPORTED_ENTITY",
								    itemEntityObj.getClass().getName(), item.getId());
	    		    throw new OvalException(message);
			}
			if (result != ResultEnumeration.TRUE) {
			    return result;
			}
		    }
		}
	    }
	    return ResultEnumeration.TRUE;
	} catch (NoSuchMethodException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_REFLECTION", e.getMessage()), e);
	    throw new OvalException(e);
	} catch (IllegalAccessException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_REFLECTION", e.getMessage()), e);
	    throw new OvalException(e);
	} catch (InvocationTargetException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_REFLECTION", e.getMessage()), e);
	    throw new OvalException(e);
	}
    }

    private static List<String> stateMethodNames = getMethodNames(StateType.class);
    private static List<String> itemMethodNames = getMethodNames(ItemType.class);

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
    private ResultEnumeration compare(EntityStateSimpleBaseType state, EntityItemSimpleBaseType item)
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
		throw new OvalException(JOVALSystem.getMessage("ERROR_DATATYPE_MISMATCH", stateDT, itemDT));
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
		for (String value : resolve(state.getVarRef(), new RequestContext(this, null))) {
		    base.setValue(value);
		    cd.addResult(testImpl(base, item));
		}
	    } catch (NoSuchElementException e) {
//DAS
		throw new TestException(JOVALSystem.getMessage("ERROR_RESOLVE_VAR", state.getVarRef(), e.getMessage()));
	    } catch (ResolveException e) {
		throw new TestException(JOVALSystem.getMessage("ERROR_RESOLVE_VAR", state.getVarRef(), e.getMessage()));
	    }
	    return cd.getResult(state.getVarCheck());
	} else {
	    return testImpl(state, item);
	}
    }

    /**
     * @see http://oval.mitre.org/language/version5.9/ovaldefinition/documentation/oval-common-schema.html#OperationEnumeration
     */
    ResultEnumeration testImpl(EntitySimpleBaseType state, EntityItemSimpleBaseType item) throws TestException, OvalException {
	switch (state.getOperation()) {
	  case CASE_INSENSITIVE_EQUALS:
	    if (equalsIgnoreCase(state, item)) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }

	  case EQUALS:
	    if (equals(state, item)) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }

	  case PATTERN_MATCH: // Always treat as Strings
	    if (item.getValue() == null) {
		return ResultEnumeration.FALSE;
	    } else if (Pattern.compile((String)state.getValue()).matcher((String)item.getValue()).find()) {
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

	  case NOT_EQUAL:
	    if (equals(state, item)) {
		return ResultEnumeration.FALSE;
	    } else {
		return ResultEnumeration.TRUE;
	    }

	  case GREATER_THAN_OR_EQUAL:
	    if (greaterThanOrEqual(state, item)) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }

	  case GREATER_THAN:
	    if (greaterThan(state, item)) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }

	  case LESS_THAN_OR_EQUAL:
	    if (greaterThan(state, item)) {
		return ResultEnumeration.FALSE;
	    } else {
		return ResultEnumeration.TRUE;
	    }

	  case LESS_THAN:
	    if (greaterThanOrEqual(state, item)) {
		return ResultEnumeration.FALSE;
	    } else {
		return ResultEnumeration.TRUE;
	    }

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

	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", state.getOperation()));
	}
    }

    boolean equals(EntitySimpleBaseType state, EntityItemSimpleBaseType item) throws TestException, OvalException {
	switch(getDatatype(state.getDatatype())) {
	  case INT:
	    try {
		return new BigInteger((String)item.getValue()).equals(new BigInteger((String)state.getValue()));
	    } catch (NumberFormatException e) {
		throw new TestException(e);
	    }

	  case FLOAT:
	    try {
		return new Float((String)item.getValue()).equals(new Float((String)state.getValue()));
	    } catch (NumberFormatException e) {
		throw new TestException(e);
	    }

	  case BOOLEAN:
	    return getBoolean((String)state.getValue()) == getBoolean((String)item.getValue());

	  case VERSION:
	    if (Version.isVersion((String)item.getValue()) && Version.isVersion((String)state.getValue())) {
		try {
		    return new Version(item.getValue()).equals(new Version(state.getValue()));
		} catch (NumberFormatException e) {
		    throw new TestException(e);
		}
	    } else {
		return ((String)item.getValue()).compareTo((String)state.getValue()) == 0;
	    }

	  case EVR_STRING:
	    try {
		return new Evr((String)item.getValue()).equals(new Evr((String)state.getValue()));
	    } catch (NumberFormatException e) {
		throw new TestException(e);
	    }

	  case BINARY:
	  case STRING:
	    return ((String)state.getValue()).equals((String)item.getValue());

	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_OPERATION_DATATYPE",
							   state.getDatatype(), OperationEnumeration.EQUALS));
	}
    }

    boolean greaterThanOrEqual(EntitySimpleBaseType state, EntityItemSimpleBaseType item) throws TestException, OvalException {
	switch(getDatatype(state.getDatatype())) {
	  case INT:
	    try {
		return new BigInteger((String)item.getValue()).compareTo(new BigInteger((String)state.getValue())) >= 0;
	    } catch (NumberFormatException e) {
		throw new TestException(e);
	    }

	  case FLOAT:
	    try {
		return new Float((String)item.getValue()).compareTo(new Float((String)state.getValue())) >= 0;
	    } catch (NumberFormatException e) {
		throw new TestException(e);
	    }

	  case VERSION:
	    if (Version.isVersion((String)item.getValue()) && Version.isVersion((String)state.getValue())) {
		try {
		    return new Version(item.getValue()).greaterThanOrEquals(new Version(state.getValue()));
		} catch (NumberFormatException e) {
		    throw new TestException(e);
		}
	    } else {
		return ((String)item.getValue()).compareTo((String)state.getValue()) >= 0;
	    }

	  case EVR_STRING:
	    try {
		return new Evr((String)item.getValue()).greaterThanOrEquals(new Evr((String)state.getValue()));
	    } catch (NumberFormatException e) {
		throw new TestException(e);
	    }

	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_OPERATION_DATATYPE",
							   state.getDatatype(), OperationEnumeration.GREATER_THAN_OR_EQUAL));
	}
    }

    boolean greaterThan(EntitySimpleBaseType state, EntityItemSimpleBaseType item) throws TestException, OvalException {
	switch(getDatatype(state.getDatatype())) {
	  case INT:
	    try {
		return new BigInteger((String)item.getValue()).compareTo(new BigInteger((String)state.getValue())) > 0;
	    } catch (NumberFormatException e) {
		throw new TestException(e);
	    }

	  case FLOAT:
	    try {
		return new Float((String)item.getValue()).compareTo(new Float((String)state.getValue())) > 0;
	    } catch (NumberFormatException e) {
		throw new TestException(e);
	    }

	  case VERSION:
	    if (Version.isVersion((String)item.getValue()) && Version.isVersion((String)state.getValue())) {
		try {
		    return new Version(item.getValue()).greaterThan(new Version(state.getValue()));
		} catch (NumberFormatException e) {
		    throw new TestException(e);
		}
	    } else {
		return ((String)item.getValue()).compareTo((String)state.getValue()) > 0;
	    }

	  case EVR_STRING:
	    try {
		return new Evr((String)item.getValue()).greaterThan(new Evr((String)state.getValue()));
	    } catch (NumberFormatException e) {
		throw new TestException(e);
	    }

	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_OPERATION_DATATYPE",
							   state.getDatatype(), OperationEnumeration.GREATER_THAN_OR_EQUAL));
	}
    }

    boolean equalsIgnoreCase(EntitySimpleBaseType state, EntityItemSimpleBaseType item) throws TestException, OvalException {
	switch(getDatatype(state.getDatatype())) {
	  case STRING:
	    return ((String)state.getValue()).equalsIgnoreCase((String)item.getValue());

	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_OPERATION_DATATYPE",
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
	    throw new OvalException(JOVALSystem.getMessage("ERROR_OPERATION_DATATYPE",
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
	    throw new OvalException(JOVALSystem.getMessage("ERROR_OPERATION_DATATYPE",
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
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_DATATYPE", s));
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
		throw new OvalException(JOVALSystem.getMessage("ERROR_EXTERNAL_VARIABLE_SOURCE", id));
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
		items = scanObject(new RequestContext(this, ot, list));
	    }
	    Collection<String> values = extractItemData(objectId, oc, items);
	    if (values == null || values.size() == 0) {
		throw new NoSuchElementException(JOVALSystem.getMessage("ERROR_COMPONENT_EMPTY"));
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
	    Collection<String> values = new Vector<String>();
	    ConcatFunctionType concat = (ConcatFunctionType)object;
	    for (Object child : concat.getObjectComponentOrVariableComponentOrLiteralComponent()) {
		Collection<String> next = resolveInternal(child, list);
		if (values.size() == 0) {
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
		values.add(escapeRegex(value));
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
		    throw new ResolveException(JOVALSystem.getMessage("ERROR_SUBSTRING", value, new Integer(start)));
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
	    Stack<Collection<String>> rows = new Stack<Collection<String>>();
	    ArithmeticEnumeration op = at.getArithmeticOperation();
	    for (Object child : at.getObjectComponentOrVariableComponentOrLiteralComponent()) {
		Collection<String> row = new Vector<String>();
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
    private List<String> extractItemData(String objectId, ObjectComponentType oc, Collection list) throws OvalException {
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
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_REFLECTION", e.getMessage()), e);
	} catch (IllegalAccessException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_REFLECTION", e.getMessage()), e);
	} catch (InvocationTargetException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_REFLECTION", e.getMessage()), e);
	}

	throw new OvalException(JOVALSystem.getMessage("ERROR_TEST_NOOBJREF", test.getId()));
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

    class AdapterManager {
	IAdapter adapter;
	boolean active = false;

	AdapterManager(IAdapter adapter) {
	    this.adapter = adapter;
	}

	void setActive(boolean active) {
	    this.active = active;
	}

	boolean isActive() {
	    return active;
	}

	IAdapter getAdapter() {
	    return adapter;
	}
    }

    class Evr {
	String epoch, version, release;

	Evr(String evr) {
	    int end = evr.indexOf(":");
	    epoch = evr.substring(0, end);
	    int begin = end+1;
	    end = evr.indexOf("-", begin);
	    version = evr.substring(begin, end);
	    release = evr.substring(end+1);
	}

	boolean greaterThanOrEquals(Evr evr) {
	    if (equals(evr)) {
		return true;
	    } else if (greaterThan(evr)) {
		return true;
	    } else {
		return false;
	    }
	}

	boolean greaterThan(Evr evr) {
	    if (Version.isVersion(epoch) && Version.isVersion(evr.epoch)) {
		if (new Version(epoch).greaterThan(new Version(evr.epoch))) {
		    return true;
		}
	    } else if (epoch.compareTo(evr.epoch) > 0) {
		return true;
	    }
	    if (Version.isVersion(version) && Version.isVersion(evr.version)) {
		if (new Version(version).greaterThan(new Version(evr.version))) {
		    return true;
		}
	    } else if (version.compareTo(evr.version) > 0) {
		return true;
	    }
	    if (Version.isVersion(release) && Version.isVersion(evr.release)) {
		if (new Version(release).greaterThan(new Version(evr.release))) {
		    return true;
		}
	    } else if (release.compareTo(evr.release) > 0) {
		return true;
	    }
	    return false;
	}

	public boolean equals(Object obj) {
	    if (obj instanceof Evr) {
		Evr evr = (Evr)obj;
		return epoch.equals(evr.epoch) && version.equals(evr.version) && release.equals(evr.release);
	    } else {
		return false;
	    }
	}
    }
}
