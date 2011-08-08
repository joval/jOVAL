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
import java.util.Collection;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;
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
import oval.schemas.definitions.core.ConcatFunctionType;
import oval.schemas.definitions.core.ConstantVariable;
import oval.schemas.definitions.core.CriteriaType;
import oval.schemas.definitions.core.CriterionType;
import oval.schemas.definitions.core.DefinitionType;
import oval.schemas.definitions.core.DefinitionsType;
import oval.schemas.definitions.core.EntityObjectStringType;
import oval.schemas.definitions.core.EntitySimpleBaseType;
import oval.schemas.definitions.core.EscapeRegexFunctionType;
import oval.schemas.definitions.core.ExtendDefinitionType;
import oval.schemas.definitions.core.ExternalVariable;
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
import org.joval.util.JOVALSystem;
import org.joval.util.Producer;

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
    private Results results;
    private oval.schemas.results.core.ObjectFactory resultsFactory;
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
	resultsFactory = new oval.schemas.results.core.ObjectFactory();
	variableMap = new Hashtable<String, List<VariableValueType>>();
	producer = new Producer();
	filter = new DefinitionFilter();
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
     * Return the results.  Only valid after the run() method is called.
     */
    public IResults getResults() {
	return results;
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
	} catch (OvalException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, e.getMessage(), e);
	}
    }

    // Internal

    static final GeneratorType getGenerator() {
	GeneratorType generator = JOVALSystem.commonFactory.createGeneratorType();
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

    /**
     * Return the value of the Variable with the specified ID, and also add any chained variables to the provided list.
     */
    List<String> resolve(String id, List<VariableValueType> list) throws NoSuchElementException, OvalException {
	VariableType var = definitions.getVariable(id);
	String varId = var.getId();
	List<VariableValueType> cachedList = variableMap.get(varId);
	if (cachedList == null) {
	    JOVALSystem.getLogger().log(Level.FINER, JOVALSystem.getMessage("STATUS_VARIABLE_CREATE", varId));
	    List<String> result = resolveInternal(var, list);
	    variableMap.put(varId, list);
	    return result;
	} else {
	    JOVALSystem.getLogger().log(Level.FINER, JOVALSystem.getMessage("STATUS_VARIABLE_RECYCLE", varId));
	    List<String> result = new Vector<String>();
	    list.addAll(cachedList);
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
	Vector<Class> varTypes = new Vector<Class>();
	Iterator<VariableType> varIter = definitions.iterateVariables();
	while(varIter.hasNext()) {
	    VariableType vt = varIter.next();
	    getObjectClasses(vt, varTypes);
	}

	//
	// Next, pre-fetch all those types
	//
	for (Class referencedClass : varTypes) {
	    for (AdapterContext ctx : adapterContextList) {
		if (referencedClass.equals(ctx.getAdapter().getObjectClass())) {
		    scanAdapter(ctx.getAdapter());
		    break;
		}
	    }
	}

	//
	// Then, pre-fetch any remaining types for which there are plug-ins
	//
	for (AdapterContext ctx : adapterContextList) {
	    if (!varTypes.contains(ctx.getAdapter().getObjectClass())) {
		scanAdapter(ctx.getAdapter());
	    }
	}

	//
	// Now that we have retrieved all the information we can, handle any Set objects.
	//
	Iterator<ObjectType> objectIter = definitions.iterateSetObjects();
	while(objectIter.hasNext()) {
	    ObjectType obj = objectIter.next();
	    String objectId = obj.getId();
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
	}

	//
	// Finally, add any objects for which there are no plug-ins, flagged NOT_COLLECTED
	//
	Iterator<ObjectType> allObjects = definitions.iterateObjects();
	while(allObjects.hasNext()) {
	    ObjectType obj = allObjects.next();
	    if (!sc.containsObject(obj.getId())) {
		MessageType message = new MessageType();
		message.setLevel(MessageLevelEnumeration.WARNING);
		message.setValue(JOVALSystem.getMessage("ERROR_MISSING_ADAPTER", obj.getClass().getName()));
		sc.setObject(obj.getId(), obj.getComment(), obj.getVersion(), FlagEnumeration.NOT_COLLECTED, message);
	    }
	}
	producer.sendNotify(this, MESSAGE_OBJECT_PHASE_END, null);
    }

    /**
     * Fetch all items associated with objects serviced by the given adapter, and store them in the SystemCharacteristics.
     */
    private void scanAdapter(IAdapter adapter) throws OvalException {
	if (adapter.connect()) {
	    try {
		Iterator<ObjectType> iter = definitions.iterateLeafObjects(adapter.getObjectClass());
		while (iter.hasNext()) {
		    ObjectType obj = iter.next();
		    String objectId = obj.getId();
		    producer.sendNotify(this, MESSAGE_OBJECT, objectId);
		    List<VariableValueType> variableValueTypes = new Vector<VariableValueType>();
		    List<JAXBElement<? extends ItemType>> items = adapter.getItems(obj, variableValueTypes);
		    if (items.size() == 0) {
		        MessageType msg = new MessageType();
		        msg.setLevel(MessageLevelEnumeration.INFO);
		        msg.setValue(JOVALSystem.getMessage("STATUS_EMPTY_OBJECT"));
		        sc.setObject(objectId, obj.getComment(), obj.getVersion(), FlagEnumeration.DOES_NOT_EXIST, msg);
		    } else {
		        sc.setObject(objectId, obj.getComment(), obj.getVersion(), FlagEnumeration.COMPLETE, null);
		        for (JAXBElement<? extends ItemType> item : items) {
		            BigInteger itemId = sc.storeItem(item);
		            sc.relateItem(obj.getId(), itemId);
		        }
		    }
		    for (VariableValueType var : variableValueTypes) {
		        sc.storeVariable(var);
		        sc.relateVariable(objectId, var.getVariableId());
		    }
		}
	    } finally {
		adapter.disconnect();
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
	    defResult = resultsFactory.createDefinitionType();
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
	    testResult = resultsFactory.createTestType();
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
			MessageType message = new MessageType();
			message.setLevel(MessageLevelEnumeration.WARNING);
			message.setValue(JOVALSystem.getMessage("ERROR_MISSING_ADAPTER", testDefinition.getClass().getName()));
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

	oval.schemas.results.core.CriterionType criterionResult = resultsFactory.createCriterionType();
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
	    TestedVariableType testedVariable = JOVALSystem.resultsFactory.createTestedVariableType();
	    testedVariable.setVariableId(var.getVariableId());
	    testedVariable.setValue(var.getValue());
	    testResult.getTestedVariable().add(testedVariable);
	}

	ExistenceData existence = new ExistenceData();
	CheckData check = new CheckData();

	List<ItemType> items = sc.getItemsByObjectId(objectId);
	for (ItemType item : items) {
	    if (item.getClass().getName().equals(adapter.getItemClass().getName())) {
		TestedItemType testedItem = JOVALSystem.resultsFactory.createTestedItemType();
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
				MessageType message = new MessageType();
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
	oval.schemas.results.core.CriteriaType criteriaResult = resultsFactory.createCriteriaType();
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
		oval.schemas.results.core.ExtendDefinitionType edtResult = resultsFactory.createExtendDefinitionType();
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
	} else if (object instanceof ConcatFunctionType) {
	    ConcatFunctionType concat = (ConcatFunctionType)object;
	    for (Object child : concat.getObjectComponentOrVariableComponentOrLiteralComponent()) {
		getObjectClasses(child, list);
	    }
	} else if (object instanceof ObjectComponentType) {
	    ObjectComponentType oct = (ObjectComponentType)object;
	    String objectId = oct.getObjectRef();
	    Class clazz = definitions.getObject(objectId).getClass();
	    if (!list.contains(clazz)) {
		list.add(clazz);
	    }
	} else {
	    getObjectClasses(getComponent(object), list);
	}
    }

    /**
     * Resolve the component.  If the component is a variable, resolve it and append it to the list, appending its value to
     * the value that is ultimately returned.
     *
     * DAS: Implement the remaining component types: begin, end, arithmetic and timedifference.
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
		VariableValueType variableValueType = new VariableValueType();
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
		    VariableValueType variableValueType = new VariableValueType();
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
	    List<ValueType> values = constantVariable.getValue();
	    List<String> stringValues = new Vector<String>(values.size());
	    for (ValueType value : values) {
		VariableValueType variableValueType = new VariableValueType();
		variableValueType.setVariableId(id);
		String s = (String)value.getValue();
		variableValueType.setValue(s);
		list.add(variableValueType);
		stringValues.add(s);
	    }
	    return stringValues;

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
		IAdapter adapter = getAdapterForObject(ot.getClass());
		if (adapter != null) {
		    List<JAXBElement<? extends ItemType>> wrappedItems = adapter.getItems(ot, list);
		    items = new Vector<ItemType>(wrappedItems.size());
		    for (JAXBElement<? extends ItemType> wrappedItem : wrappedItems) {
			items.add(wrappedItem.getValue());
		    }
		} else {
		    throw new RuntimeException(JOVALSystem.getMessage("ERROR_MISSING_ADAPTER", ot.getClass().getName()));
		}
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
	    List<String> values = resolveInternal(getComponent((EscapeRegexFunctionType)object), list);
	    List<String> newValues = new Vector<String>(values.size());
	    for (String value : values) {
		newValues.add(Matcher.quoteReplacement(value));
	    }
	    return newValues;

	//
	// Process a Split, which contains a component and a delimiter with which to split it up.
	//
	} else if (object instanceof SplitFunctionType) {
	    SplitFunctionType split = (SplitFunctionType)object;
	    List<String> values = resolveInternal(getComponent(split), list);
	    List<String> newValues = new Vector<String>();
	    for (String value : values) {
		String[] sa = value.split(split.getDelimiter());
		for (int i=0; i < sa.length; i++) {
		    newValues.add(value + sa[i]);
		}
	    }
	    return newValues;

	//
	// Process a RegexCapture, which returns the regions of a component resolved as a String that match a given pattern.
	//
	} else if (object instanceof RegexCaptureFunctionType) {
	    RegexCaptureFunctionType rc = (RegexCaptureFunctionType)object;
	    Pattern p = Pattern.compile(rc.getPattern());
	    List<String> values = resolveInternal(getComponent(rc), list);
	    List<String> newValues = new Vector<String>();
	    for (String value : values) {
		Matcher m = p.matcher(value);
		while (m.find()) {
		     newValues.add(m.group());
		}
	    }
	    return newValues;

	//
	// Process a Substring
	//
	} else if (object instanceof SubstringFunctionType) {
	    SubstringFunctionType st = (SubstringFunctionType)object;
	    int start = st.getSubstringStart();
	    start = Math.min(1, start);
	    start--; // in OVAL, the start index begins at 1 instead of 0
	    int len = st.getSubstringLength();
	    List<String> values = resolveInternal(getComponent(st), list);
	    List<String> newValues = new Vector<String>();
	    for (String value : values) {
		newValues.add(value.substring(start, start+len));
	    }
	    return newValues;

	} else {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_COMPONENT", object.getClass().getName()));
	}
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

    private List<ItemType> getSetItems(Set s) throws NoSuchElementException {
	List<List<ItemType>> lists = new Vector<List<ItemType>>();
	if (s.isSetSet()) {
	    for (Set set : s.getSet()) {
		lists.add(getSetItems(set));
	    }
	} else {
	    for (String objectId : s.getObjectReference()) {
		lists.add(sc.getItemsByObjectId(objectId));
	    }
	}

	Hashtable<BigInteger, ItemType> results = null;
	for (List<ItemType> list : lists) {
	    Iterator<ItemType> iter = list.iterator();

	    switch(s.getSetOperator()) {
	      case UNION:
		if (results == null) {
		    results = new Hashtable<BigInteger, ItemType>();
		}
		while(iter.hasNext()) {
		    ItemType item = iter.next();
		    if (!results.containsKey(item.getId())) {
			results.put(item.getId(), item);
		    }
		}
		break;

	      case INTERSECTION:
		Hashtable<BigInteger, ItemType> intersection = new Hashtable<BigInteger, ItemType>();
		while(iter.hasNext()) {
		    ItemType item = iter.next();
		    if (results == null) {
			intersection.put(item.getId(), item);
		    } else if (results.containsKey(item.getId())) {
			intersection.put(item.getId(), item);
		    }
		}
		results = intersection;
		break;

	      // This case works only because there can never be more than two child Sets.
	      case COMPLEMENT:
		if (results == null) {
		    results = new Hashtable<BigInteger, ItemType>();
		}
		while(iter.hasNext()) {
		    ItemType item = iter.next();
		    if (results.containsKey(item.getId())) {
			results.remove(item.getId());
		    } else {
			results.put(item.getId(), item);
		    }
		}
		break;
	    }
	}
	return new Vector<ItemType>(results.values());
    }

    /**
     * @see http://oval.mitre.org/language/version5.9/ovaldefinition/documentation/oval-common-schema.html#OperatorEnumeration
     */
    class OperatorData {
	int t, f, e, u, ne, na;

	OperatorData() {
	    t = 0;
	    f = 0;
	    e = 0;
	    u = 0;
	    ne = 0;
	    na = 0;
	}

	void addResult(ResultEnumeration result) {
	    switch(result) {
	      case TRUE:
		t++;
		break;
	      case FALSE:
		f++;
		break;
	      case UNKNOWN:
		u++;
		break;
	      case NOT_APPLICABLE:
		na++;
		break;
	      case NOT_EVALUATED:
		ne++;
		break;
	      case ERROR:
		e++;
		break;
	    }
	}

	ResultEnumeration getResult(OperatorEnumeration op) throws OvalException {
	    ResultEnumeration result = ResultEnumeration.UNKNOWN;
	    switch(op) {
	      case AND:
		if        (t > 0	&& f == 0	&& e == 0	&& u == 0	&& ne == 0	&& na >= 0) {
		    return ResultEnumeration.TRUE;
		} else if (t >= 0	&& f > 0	&& e >= 0	&& u >= 0	&& ne >= 0	&& na >= 0) {
		    return ResultEnumeration.FALSE;
		} else if (t >= 0	&& f == 0	&& e > 0	&& u >= 0	&& ne >= 0	&& na >= 0) {
		    return ResultEnumeration.ERROR;
		} else if (t >= 0	&& f == 0	&& e == 0	&& u > 0	&& ne >= 0	&& na >= 0) {
		    return ResultEnumeration.UNKNOWN;
		} else if (t >= 0	&& f == 0	&& e == 0	&& u == 0	&& ne > 0	&& na >= 0) {
		    return ResultEnumeration.NOT_EVALUATED;
		} else if (t == 0	&& f == 0	&& e == 0	&& u == 0	&& ne == 0	&& na > 0) {
		    return ResultEnumeration.NOT_APPLICABLE;
		}
		break;

	      case ONE:
		if        (t == 1	&& f >= 0	&& e == 0	&& u == 0	&& ne == 0	&& na >= 0) {
		    return ResultEnumeration.TRUE;
		} else if (t > 1	&& f >= 0	&& e >= 0	&& u >= 0	&& ne >= 0	&& na >= 0) {
		    return ResultEnumeration.FALSE;
		} else if (t == 0	&& f > 0	&& e == 0	&& u == 0	&& ne == 0	&& na >= 0) {
		    return ResultEnumeration.FALSE;
		} else if (t < 2	&& f >= 0	&& e > 0	&& u >= 0	&& ne >= 0	&& na >= 0) {
		    return ResultEnumeration.ERROR;
		} else if (t < 2	&& f >= 0	&& e == 0	&& u > 0	&& ne >= 0	&& na >= 0) {
		    return ResultEnumeration.UNKNOWN;
		} else if (t < 2	&& f >= 0	&& e == 0	&& u == 0	&& ne > 0	&& na >= 0) {
		    return ResultEnumeration.NOT_EVALUATED;
		} else if (t == 0	&& f == 0	&& e == 0	&& u == 0	&& ne == 0	&& na > 0) {
		    return ResultEnumeration.NOT_APPLICABLE;
		}
		break;

	      case OR:
		if        (t > 0	&& f >= 0	&& e >= 0	&& u >= 0	&& ne >= 0	&& na >= 0) {
		    return ResultEnumeration.TRUE;
		} else if (t == 0	&& f > 0	&& e == 0	&& u == 0	&& ne == 0	&& na >= 0) {
		    return ResultEnumeration.FALSE;
		} else if (t == 0	&& f >= 0	&& e > 0	&& u >= 0	&& ne >= 0	&& na >= 0) {
		    return ResultEnumeration.ERROR;
		} else if (t == 0	&& f >= 0	&& e == 0	&& u > 0	&& ne >= 0	&& na >= 0) {
		    return ResultEnumeration.UNKNOWN;
		} else if (t == 0	&& f >= 0	&& e == 0	&& u == 0	&& ne > 0	&& na >= 0) {
		    return ResultEnumeration.NOT_EVALUATED;
		} else if (t == 0	&& f == 0	&& e == 0	&& u == 0	&& ne == 0	&& na == 0) {
		    return ResultEnumeration.NOT_APPLICABLE;
		}
		break;

	      case XOR:
		if        (t%2 != 0	&& f >= 0	&& e == 0	&& u == 0	&& ne == 0	&& na >= 0) {
		    return ResultEnumeration.TRUE;
		} else if (t%2 == 0	&& f >= 0	&& e == 0	&& u == 0	&& ne == 0	&& na >= 0) {
		    return ResultEnumeration.FALSE;
		} else if (t >= 0	&& f >= 0	&& e > 0	&& u >= 0	&& ne >= 0	&& na >= 0) {
		    return ResultEnumeration.ERROR;
		} else if (t >= 0	&& f >= 0	&& e == 0	&& u > 0	&& ne >= 0	&& na >= 0) {
		    return ResultEnumeration.UNKNOWN;
		} else if (t >= 0	&& f >= 0	&& e == 0	&& u == 0	&& ne > 0	&& na >= 0) {
		    return ResultEnumeration.NOT_EVALUATED;
		} else if (t == 0	&& f == 0	&& e == 0	&& u == 0	&& ne == 0	&& na == 0) {
		    return ResultEnumeration.NOT_APPLICABLE;
		}
		break;

	      default:
		throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", op));
	    }
	    return result;
	}
    }

    /**
     * @see http://oval.mitre.org/language/version5.9/ovaldefinition/documentation/oval-common-schema.html#CheckEnumeration
     */
    class CheckData extends OperatorData {
	CheckData() {
	    super();
	}

	ResultEnumeration getResult(CheckEnumeration check) throws OvalException {
	    ResultEnumeration result = ResultEnumeration.UNKNOWN;
	    switch(check) {
	      case ALL:
		if        (t > 0	&& f == 0	&& e == 0	&& u == 0	&& ne == 0	&& na >= 0) {
		    return ResultEnumeration.TRUE;
		} else if (t >= 0	&& f > 0	&& e >= 0	&& u >= 0	&& ne >= 0	&& na >= 0) {
		    return ResultEnumeration.FALSE;
		} else if (t >= 0	&& f == 0	&& e > 0	&& u >= 0	&& ne >= 0	&& na >= 0) {
		    return ResultEnumeration.ERROR;
		} else if (t >= 0	&& f == 0	&& e == 0	&& u > 0	&& ne >= 0	&& na >= 0) {
		    return ResultEnumeration.UNKNOWN;
		} else if (t >= 0	&& f == 0	&& e == 0	&& u == 0	&& ne > 0	&& na >= 0) {
		    return ResultEnumeration.NOT_EVALUATED;
		} else if (t == 0	&& f == 0	&& e == 0	&& u == 0	&& ne == 0	&& na > 0) {
		    return ResultEnumeration.NOT_APPLICABLE;
		}
		break;

	      case AT_LEAST_ONE:
		if        (t >= 1	&& f >= 0	&& e >= 0	&& u >= 0	&& ne >= 0	&& na >= 0) {
		    return ResultEnumeration.TRUE;
		} else if (t == 0	&& f > 0	&& e == 0	&& u == 0	&& ne == 0	&& na >= 0) {
		    return ResultEnumeration.FALSE;
		} else if (t == 0	&& f >= 0	&& e > 0	&& u >= 0	&& ne >= 0	&& na >= 0) {
		    return ResultEnumeration.ERROR;
		} else if (t == 0	&& f >= 0	&& e == 0	&& u > 0	&& ne >= 0	&& na >= 0) {
		    return ResultEnumeration.UNKNOWN;
		} else if (t == 0	&& f >= 0	&& e == 0	&& u == 0	&& ne > 0	&& na >= 0) {
		    return ResultEnumeration.NOT_EVALUATED;
		} else if (t == 0	&& f == 0	&& e == 0	&& u == 0	&& ne == 0	&& na > 0) {
		    return ResultEnumeration.NOT_APPLICABLE;
		}
		break;

	      case ONLY_ONE:
		if        (t == 1	&& f >= 0	&& e == 0	&& u == 0	&& ne == 0	&& na >= 0) {
		    return ResultEnumeration.TRUE;
		} else if (t > 1	&& f >= 0	&& e >= 0	&& u >= 0	&& ne >= 0	&& na >= 0) {
		    return ResultEnumeration.FALSE;
		} else if (t == 0	&& f > 0	&& e == 0	&& u == 0	&& ne == 0	&& na >= 0) {
		    return ResultEnumeration.FALSE;
		} else if (t < 2	&& f >= 0	&& e > 0	&& u >= 0	&& ne >= 0	&& na >= 0) {
		    return ResultEnumeration.ERROR;
		} else if (t < 2	&& f >= 0	&& e == 0	&& u > 0	&& ne >= 0	&& na >= 0) {
		    return ResultEnumeration.UNKNOWN;
		} else if (t < 2	&& f >= 0	&& e == 0	&& u == 0	&& ne > 0	&& na >= 0) {
		    return ResultEnumeration.NOT_EVALUATED;
		} else if (t == 0	&& f == 0	&& e == 0	&& u == 0	&& ne == 0	&& na > 0) {
		    return ResultEnumeration.NOT_APPLICABLE;
		}
		break;

	      case NONE_SATISFY:
		if        (t == 0	&& f > 0	&& e == 0	&& u == 0	&& ne == 0	&& na >= 0) {
		    return ResultEnumeration.TRUE;
		} else if (t > 0	&& f >= 0	&& e >= 0	&& u >= 0	&& ne >= 0	&& na >= 0) {
		    return ResultEnumeration.FALSE;
		} else if (t == 0	&& f >= 0	&& e > 0	&& u >= 0	&& ne >= 0	&& na >= 0) {
		    return ResultEnumeration.ERROR;
		} else if (t == 0	&& f >= 0	&& e == 0	&& u > 0	&& ne >= 0	&& na >= 0) {
		    return ResultEnumeration.UNKNOWN;
		} else if (t == 0	&& f >= 0	&& e == 0	&& u == 0	&& ne > 0	&& na >= 0) {
		    return ResultEnumeration.NOT_EVALUATED;
		} else if (t == 0	&& f == 0	&& e == 0	&& u == 0	&& ne == 0	&& na > 0) {
		    return ResultEnumeration.NOT_APPLICABLE;
		}
		break;

	      default:
		throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_CHECK", check));
	    }
	    return result;
	}
    }

    /**
     * @see http://oval.mitre.org/language/version5.9/ovaldefinition/documentation/oval-common-schema.html#ExistenceEnumeration
     */
    class ExistenceData {
	int ex, de, er, nc;

	ExistenceData() {
	    ex = 0;
	    de = 0;
	    er = 0;
	    nc = 0;
	}

	void addStatus(StatusEnumeration status) {
	    switch(status) {
	      case DOES_NOT_EXIST:
		de++;
		break;
	      case ERROR:
		er++;
		break;
	      case EXISTS:
		ex++;
		break;
	      case NOT_COLLECTED:
		nc++;
		break;
	    }
	}

	ResultEnumeration getResult(ExistenceEnumeration existence) throws OvalException {
	    ResultEnumeration result = ResultEnumeration.UNKNOWN;
	    switch(existence) {
	      case ALL_EXIST:
		if        (ex > 0	&& de == 0	&& er == 0	&& nc == 0) {
		    return ResultEnumeration.TRUE;
		} else if (ex == 0	&& de == 0	&& er == 0	&& nc == 0) {
		    return ResultEnumeration.FALSE;
		} else if (ex >= 0	&& de > 0	&& er >= 0	&& nc >= 0) {
		    return ResultEnumeration.FALSE;
		} else if (ex >= 0	&& de == 0	&& er > 0	&& nc >= 0) {
		    return ResultEnumeration.ERROR;
		} else if (ex >= 0	&& de == 0	&& er == 0	&& nc > 0) {
		    return ResultEnumeration.UNKNOWN;
		}
		break;

	      case ANY_EXIST:
		if        (ex >= 0	&& de >= 0	&& er == 0	&& nc >= 0) {
		    return ResultEnumeration.TRUE;
		} else if (ex > 0	&& de >= 0	&& er > 0	&& nc >= 0) {
		    return ResultEnumeration.TRUE;
		} else if (ex == 0	&& de >= 0	&& er > 0	&& nc >= 0) {
		    return ResultEnumeration.ERROR;
		}
		break;

	      case AT_LEAST_ONE_EXISTS:
		if        (ex > 0	&& de >= 0	&& er >= 0	&& nc >= 0) {
		    return ResultEnumeration.TRUE;
		} else if (ex == 0	&& de >= 0	&& er == 0	&& nc == 0) { // Spec says "de > 0"
		    return ResultEnumeration.FALSE;
		} else if (ex == 0	&& de >= 0	&& er > 0	&& nc == 0) {
		    return ResultEnumeration.ERROR;
		} else if (ex == 0	&& de >= 0	&& er == 0	&& nc > 0) {
		    return ResultEnumeration.UNKNOWN;
		}
		break;

	      case NONE_EXIST:
		if        (ex == 0	&& de >= 0	&& er == 0	&& nc == 0) {
		    return ResultEnumeration.TRUE;
		} else if (ex > 0	&& de >= 0	&& er >= 0	&& nc >= 0) {
		    return ResultEnumeration.FALSE;
		} else if (ex == 0	&& de >= 0	&& er > 0	&& nc >= 0) {
		    return ResultEnumeration.ERROR;
		} else if (ex == 0	&& de >= 0	&& er == 0	&& nc > 0) {
		    return ResultEnumeration.UNKNOWN;
		}
		break;

	      case ONLY_ONE_EXISTS:
		if        (ex == 1	&& de >= 0	&& er == 0	&& nc == 0) {
		    return ResultEnumeration.TRUE;
		} else if (ex > 1	&& de >= 0	&& er >= 0	&& nc >= 0) {
		    return ResultEnumeration.FALSE;
		} else if (ex == 0	&& de >= 0	&& er == 0	&& nc == 0) {
		    return ResultEnumeration.FALSE;
		} else if (ex < 2	&& de >= 0	&& er > 0	&& nc >= 0) {
		    return ResultEnumeration.ERROR;
		} else if (ex < 2	&& de >= 0	&& er == 0	&& nc > 0) {
		    return ResultEnumeration.UNKNOWN;
		}
		break;

	      default:
		throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_EXISTENCE", ex));
	    }
	    return result;
	}
    }
}
