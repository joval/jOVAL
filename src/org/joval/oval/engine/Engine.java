// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.engine;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;

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
import oval.schemas.results.core.ResultEnumeration;
import oval.schemas.results.core.TestedItemType;
import oval.schemas.results.core.TestedVariableType;
import oval.schemas.results.core.TestType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.SystemDataType;
import oval.schemas.systemcharacteristics.core.VariableValueType;
import oval.schemas.variables.core.OvalVariables;

import org.joval.intf.oval.IDefinitions;
import org.joval.intf.oval.IResults;
import org.joval.intf.oval.ISystemCharacteristics;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IPlugin;
import org.joval.intf.util.IObserver;
import org.joval.intf.util.IProducer;
import org.joval.oval.OvalException;
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

    IDefinitions getDefinitions() {
	return definitions;
    }

    IAdapter getAdapterForObject(Class clazz) {
	for (AdapterContext ctx : adapterContextList) {
	    if (clazz.equals(ctx.getAdapter().getObjectClass())) {
		return ctx.getAdapter();
	    }
	}
	return null;
    }

    IAdapter getAdapterForTest(Class clazz) {
	for (AdapterContext ctx : adapterContextList) {
	    if (clazz.equals(ctx.getAdapter().getTestClass())) {
		return ctx.getAdapter();
	    }
	}
	return null;
    }

    /**
     * Return the value of the Variable with the specified ID, and also add any chained variables to the provided list.
     */
    String resolve(String id, List<VariableValueType> list) throws NoSuchElementException, OvalException {
	VariableType var = definitions.getVariable(id);
	String varId = var.getId();
	List<VariableValueType> cachedList = variableMap.get(varId);
	if (cachedList == null) {
	    JOVALSystem.getLogger().log(Level.FINER, JOVALSystem.getMessage("STATUS_VARIABLE_CREATE", varId));
	    String result = resolveInternal(var, list);
	    variableMap.put(varId, list);
	    return result;
	} else {
	    JOVALSystem.getLogger().log(Level.FINER, JOVALSystem.getMessage("STATUS_VARIABLE_RECYCLE", varId));
	    list.addAll(cachedList);
	    for (VariableValueType variableValueType : cachedList) {
		if (variableValueType.getVariableId().equals(id)) {
		    return (String)variableValueType.getValue();
		}
	    }
	    throw new OvalException(JOVALSystem.getMessage("ERROR_MISSING_VARIABLE", id));
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
		    ctx.getAdapter().scan(sc);
		    break;
		}
	    }
	}

	//
	// Then, pre-fetch any remaining types for which there are plug-ins
	//
	for (AdapterContext ctx : adapterContextList) {
	    if (!varTypes.contains(ctx.getAdapter().getObjectClass())) {
		ctx.getAdapter().scan(sc);
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
		IAdapter adapter = getAdapterForTest(testDefinition.getClass());
		if (adapter == null) {
		    MessageType message = new MessageType();
		    message.setLevel(MessageLevelEnumeration.WARNING);
		    message.setValue(JOVALSystem.getMessage("ERROR_MISSING_ADAPTER", testDefinition.getClass().getName()));
		    testResult.getMessage().add(message);
		    testResult.setResult(ResultEnumeration.NOT_EVALUATED);
		} else if (getObjectRef(testDefinition) == null) {
		    throw new OvalException(JOVALSystem.getMessage("ERROR_TEST_NOOBJREF", testId));
		} else {
		    evaluateTest(testResult, sc, adapter);
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

    private void evaluateTest(TestType testResult, ISystemCharacteristics sc, IAdapter adapter) throws OvalException {
	String testId = testResult.getTestId();
	oval.schemas.definitions.core.TestType testDefinition = definitions.getTest(testId, adapter.getTestClass());
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

	boolean result = false;
	int trueCount=0, falseCount=0, errorCount=0;
	if (sc.getObject(objectId).getFlag() == FlagEnumeration.ERROR) {
	    errorCount++;
	}

	Iterator<ItemType> items = sc.getItemsByObjectId(objectId).iterator();
	switch(testDefinition.getCheckExistence()) {
	  case NONE_EXIST: {
	    while(items.hasNext()) {
		ItemType item = items.next();
		if (item.getClass().getName().equals(adapter.getItemClass().getName())) {
		    TestedItemType testedItem = JOVALSystem.resultsFactory.createTestedItemType();
		    testedItem.setItemId(item.getId());
		    switch(item.getStatus()) {
		      case EXISTS:
			testedItem.setResult(ResultEnumeration.NOT_EVALUATED); // just an existence check
			trueCount++;
			break;
		      case DOES_NOT_EXIST:
			testedItem.setResult(ResultEnumeration.NOT_EVALUATED); // just an existence check
			falseCount++;
			break;
		      case ERROR:
			testedItem.setResult(ResultEnumeration.ERROR);
			errorCount++;
			break;
		      default:
			testedItem.setResult(ResultEnumeration.NOT_EVALUATED);
			break;
		    }
		    testResult.getTestedItem().add(testedItem);
		} else {
		    throw new OvalException(JOVALSystem.getMessage("ERROR_INSTANCE", adapter.getItemClass().getName(),
								   item.getClass().getName()));
		}
	    }
	    result = trueCount == 0;
	    break;
	  }

	  case AT_LEAST_ONE_EXISTS: {
	    while(items.hasNext()) {
		ItemType item = items.next();
		if (item.getClass().getName().equals(adapter.getItemClass().getName())) {
		    TestedItemType testedItem = JOVALSystem.resultsFactory.createTestedItemType();
		    testedItem.setItemId(item.getId());
		    switch(item.getStatus()) {
		      case EXISTS:
			if (state == null) { // In the absence of a state, check only for the existence of the item
			    trueCount++;
			    testedItem.setResult(ResultEnumeration.NOT_EVALUATED);
			} else {
			    ResultEnumeration matchResult = adapter.compare(state, item);
			    switch(matchResult) {
			      case TRUE:
				trueCount++;
				break;
			      case FALSE:
				falseCount++;
				break;
			      case ERROR:
				errorCount++;
				break;
			    }
			    testedItem.setResult(matchResult);
			}
			break;
		      case DOES_NOT_EXIST:
			testedItem.setResult(ResultEnumeration.FALSE);
			falseCount++;
			break;
		      case ERROR:
			testedItem.setResult(ResultEnumeration.ERROR);
			errorCount++;
			break;
		      default:
			testedItem.setResult(ResultEnumeration.NOT_EVALUATED);
			break;
		    }
		    testResult.getTestedItem().add(testedItem);
		} else {
		    throw new OvalException(JOVALSystem.getMessage("ERROR_INSTANCE", adapter.getItemClass().getName(),
								   item.getClass().getName()));
		}
	    }
	    switch(testDefinition.getCheck()) {
	      case ALL:
		result = falseCount == 0 && trueCount > 0;
		break;
	      case AT_LEAST_ONE:
		result = trueCount > 0;
		break;
	      case ONLY_ONE:
		result = trueCount == 1 && falseCount == 0 && errorCount == 0;
		break;
	      default:
		throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_CHECK", testDefinition.getCheck()));
	    }
	    break;
	  }

	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_EXISTENCE", testDefinition.getCheckExistence()));
	}
	if (errorCount > 0) {
	    testResult.setResult(ResultEnumeration.ERROR);
	} else if (result) {
	    testResult.setResult(ResultEnumeration.TRUE);
	} else {
	    testResult.setResult(ResultEnumeration.FALSE);
	}
    }

    private oval.schemas.results.core.CriteriaType evaluateCriteria(CriteriaType criteriaDefinition) throws OvalException {
	oval.schemas.results.core.CriteriaType criteriaResult = resultsFactory.createCriteriaType();
	criteriaResult.setOperator(criteriaDefinition.getOperator());

	int trueCount=0, falseCount=0, errorCount=0, unknownCount=0, skippedCount=0;
	OperatorEnumeration operator = criteriaDefinition.getOperator();
	for (Object child : criteriaDefinition.getCriteriaOrCriterionOrExtendDefinition()) {
	    Object resultObject = null;
	    if (child instanceof CriteriaType) {
		CriteriaType ctDefinition = (CriteriaType)child;
		oval.schemas.results.core.CriteriaType ctResult = evaluateCriteria(ctDefinition);
		switch(ctResult.getResult()) {
		  case TRUE:
		    trueCount++;
		    break;
		  case FALSE:
		    falseCount++;
		    break;
		  case UNKNOWN:
		    unknownCount++;
		    break;
		  case NOT_EVALUATED:
		    skippedCount++;
		    break;
		  case ERROR:
		    errorCount++;
		    break;
		}
		resultObject = ctResult;
	    } else if (child instanceof CriterionType) {
		CriterionType ctDefinition = (CriterionType)child;
		oval.schemas.results.core.CriterionType ctResult = evaluateCriterion(ctDefinition);
		switch(ctResult.getResult()) {
		  case TRUE:
		    trueCount++;
		    break;
		  case FALSE:
		    falseCount++;
		    break;
		  case NOT_EVALUATED:
		    skippedCount++;
		    break;
		  case ERROR:
		    errorCount++;
		    break;
		}
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
			falseCount++;
			edtResult.setResult(ResultEnumeration.FALSE);
			break;
		      case FALSE:
			trueCount++;
			edtResult.setResult(ResultEnumeration.TRUE);
			break;
		      case UNKNOWN:
			unknownCount++;
			break;
		      case NOT_EVALUATED:
			skippedCount++;
			break;
		      case ERROR:
			errorCount++;
			break;
		    }
		} else {
		    switch(defResult.getResult()) {
		      case TRUE:
			trueCount++;
			break;
		      case FALSE:
			falseCount++;
			break;
		      case UNKNOWN:
			unknownCount++;
			break;
		      case NOT_EVALUATED:
			skippedCount++;
			break;
		      case ERROR:
			errorCount++;
			break;
		    }
		    edtResult.setResult(defResult.getResult());
		}
		resultObject = edtResult;
	    } else {
		throw new OvalException(JOVALSystem.getMessage("ERROR_BAD_COMPONENT", child.getClass().getName()));
	    }
	    criteriaResult.getCriteriaOrCriterionOrExtendDefinition().add(resultObject);
	}

	ResultEnumeration result = ResultEnumeration.UNKNOWN;
	switch(criteriaDefinition.getOperator()) {
	  case AND:
	    if (falseCount > 0) {
		result = ResultEnumeration.FALSE;
	    } else if (trueCount > 0 && errorCount == 0 && skippedCount == 0 && unknownCount == 0) {
		result = ResultEnumeration.TRUE;
	    } else if (errorCount > 0) {
		result = ResultEnumeration.ERROR;
	    }
	    break;
	  case OR:
	    if (trueCount > 0) {
		result = ResultEnumeration.TRUE;
	    } else if (errorCount == 0 && skippedCount == 0 && unknownCount == 0) {
		result = ResultEnumeration.FALSE;
	    } else if (errorCount > 0) {
		result = ResultEnumeration.ERROR;
	    }
	    break;
	  case ONE:
	    if (errorCount == 0 && skippedCount == 0 && unknownCount == 0) {
		if (trueCount == 1) {
		    result = ResultEnumeration.TRUE;
		} else {
		    result = ResultEnumeration.FALSE;
		}
	    } else if (trueCount > 1) {
		result = ResultEnumeration.FALSE;
	    } else if (errorCount > 0) {
		result = ResultEnumeration.ERROR;
	    }
	    break;
	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATOR", criteriaDefinition.getOperator()));
	}
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
     * DAS: Implement the remaining five component types: begin, end, arithmetic, substring and timedifference.
     */
    private String resolveInternal(Object object, List <VariableValueType>list) throws NoSuchElementException, OvalException {
	//
	// Why do variables point to variables?  Because sometimes they are nested.
	//
	if (object instanceof LocalVariable) {
	    LocalVariable localVariable = (LocalVariable)object;
	    VariableValueType variableValueType = new VariableValueType();
	    variableValueType.setVariableId(localVariable.getId());
	    String value = resolveInternal(getComponent(localVariable), list);
	    variableValueType.setValue(value);
	    list.add(variableValueType);
	    return value;

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
		int size = values.size();
		if (size == 1) {
		    VariableValueType variableValueType = new VariableValueType();
		    variableValueType.setVariableId(id);
		    String value = values.get(0);
		    variableValueType.setValue(value);
		    list.add(variableValueType);
		    return value;
		} else {
		    throw new OvalException(JOVALSystem.getMessage("ERROR_EXTERNAL_VARIABLE_CHOICE", new Integer(size), id));
		}
	    }

	//
	// Add a constant variable.
	//
	} else if (object instanceof ConstantVariable) {
	    ConstantVariable constantVariable = (ConstantVariable)object;
	    String id = constantVariable.getId();
	    List<ValueType> values = constantVariable.getValue();
	    int size = values.size();
	    if (size == 1) {
		VariableValueType variableValueType = new VariableValueType();
		variableValueType.setVariableId(id);
		Object value = values.get(0).getValue();
		variableValueType.setValue(value);
		list.add(variableValueType);
		return (String)value;
	    } else {
		throw new OvalException(JOVALSystem.getMessage("ERROR_CONSTANT_VARIABLE_CHOICE", new Integer(size), id));
	    }

	//
	// Add a static (literal) value.
	//
	} else if (object instanceof LiteralComponentType) {
	    return (String)((LiteralComponentType)object).getValue();

	//
	// Use the registered IAdapter to get data from an ObjectType
	//
	} else if (object instanceof ObjectComponentType) {
	    ObjectComponentType oc = (ObjectComponentType)object;
	    ObjectType ot = definitions.getObject(oc.getObjectRef());
	    IAdapter adapter = getAdapterForObject(ot.getClass());
	    if (adapter != null) {
		String data = adapter.getItemData(oc, sc);
		if (data == null) {
		    throw new NoSuchElementException(oc.getObjectRef());
		} else {
		    return data;
		}
	    } else {
		throw new RuntimeException(JOVALSystem.getMessage("ERROR_MISSING_ADAPTER", ot.getClass().getName()));
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
	    StringBuffer sb = new StringBuffer();
	    ConcatFunctionType concat = (ConcatFunctionType)object;
	    for (Object child : concat.getObjectComponentOrVariableComponentOrLiteralComponent()) {
		sb.append(resolveInternal(child, list));
	    }
	    return sb.toString();

	//
	// Escape anything that could be pattern-matched.
	//
	} else if (object instanceof EscapeRegexFunctionType) {
	    return Matcher.quoteReplacement(resolveInternal(getComponent((EscapeRegexFunctionType)object), list));

	//
	// Process a Split, which contains a component and a delimiter with which to split it up.
	//
	} else if (object instanceof SplitFunctionType) {
	    StringBuffer sb = new StringBuffer();
	    SplitFunctionType split = (SplitFunctionType)object;
	    String[] sa = resolveInternal(getComponent(split), list).split(split.getDelimiter());
	    for (int i=0; i < sa.length; i++) {
		sb.append(sa[i]);
	    }
	    return sb.toString();

	//
	// Process a RegexCapture, which returns the region of a component resolved as a String that matches a given pattern.
	//
	} else if (object instanceof RegexCaptureFunctionType) {
	    RegexCaptureFunctionType rc = (RegexCaptureFunctionType)object;
	    Pattern p = Pattern.compile(rc.getPattern());
	    String s = resolveInternal(getComponent(rc), list);
	    Matcher m = p.matcher(s);

	    if (m.matches()) {
		MatchResult mr = m.toMatchResult();
		return s.substring(mr.start(), mr.end());
	    } else {
		throw new NoSuchElementException(JOVALSystem.getMessage("ERROR_REGEX_NOMATCH", rc.getPattern(), s));
	    }
	} else {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_COMPONENT", object.getClass().getName()));
	}
    }

    private Object getComponent(Object unknown) throws OvalException {
	if (unknown instanceof RegexCaptureFunctionType) {
	    return getComponent((RegexCaptureFunctionType)unknown);
	} else if (unknown instanceof SplitFunctionType) {
	    return getComponent((SplitFunctionType)unknown);
	} else if (unknown instanceof EscapeRegexFunctionType) {
	    return getComponent((EscapeRegexFunctionType)unknown);
	} else if (unknown instanceof LocalVariable) {
	    return getComponent((LocalVariable)unknown);
	} else if (unknown instanceof ExternalVariable) {
	    return null; // Constant
	} else if (unknown instanceof ConstantVariable) {
	    return null; // Constant
	} else {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_COMPONENT", unknown.getClass().getName()));
	}
    }

    private Object getComponent(RegexCaptureFunctionType rc) throws OvalException {
	Object obj = null;
	if ((obj = rc.getObjectComponent()) != null) {
	    return obj;
	} else if ((obj = rc.getLiteralComponent()) != null) {
	    return obj;
	} else if ((obj = rc.getVariableComponent() ) != null) {
	    return obj;
	} else if ((obj = rc.getConcat()) != null) {
	    return obj;
	} else if ((obj = rc.getEscapeRegex()) != null) {
	    return obj;
	} else if ((obj = rc.getRegexCapture()) != null) {
	    return obj;
	} else if ((obj = rc.getSplit()) != null) {
	    return obj;
	} else if ((obj = rc.getArithmetic()) != null) {
	    return obj;
	} else if ((obj = rc.getBegin()) != null) {
	    return obj;
	} else if ((obj = rc.getEnd()) != null) {
	    return obj;
	} else if ((obj = rc.getSubstring()) != null) {
	    return obj;
	} else if ((obj = rc.getTimeDifference()) != null) {
	    return obj;
	} else {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_MISSING_COMPONENT"));
	}
    }

    private Object getComponent(SplitFunctionType split) throws OvalException {
	Object obj = null;
	if ((obj = split.getObjectComponent()) != null) {
	    return obj;
	} else if ((obj = split.getLiteralComponent()) != null) {
	    return obj;
	} else if ((obj = split.getVariableComponent() ) != null) {
	    return obj;
	} else if ((obj = split.getConcat()) != null) {
	    return obj;
	} else if ((obj = split.getEscapeRegex()) != null) {
	    return obj;
	} else if ((obj = split.getRegexCapture()) != null) {
	    return obj;
	} else if ((obj = split.getSplit()) != null) {
	    return obj;
	} else if ((obj = split.getArithmetic()) != null) {
	    return obj;
	} else if ((obj = split.getBegin()) != null) {
	    return obj;
	} else if ((obj = split.getEnd()) != null) {
	    return obj;
	} else if ((obj = split.getSubstring()) != null) {
	    return obj;
	} else if ((obj = split.getTimeDifference()) != null) {
	    return obj;
	} else {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_MISSING_COMPONENT"));
	}
    }

    private Object getComponent(EscapeRegexFunctionType er) throws OvalException {
	Object obj = null;
	if ((obj = er.getObjectComponent()) != null) {
	    return obj;
	} else if ((obj = er.getLiteralComponent()) != null) {
	    return obj;
	} else if ((obj = er.getVariableComponent() ) != null) {
	    return obj;
	} else if ((obj = er.getConcat()) != null) {
	    return obj;
	} else if ((obj = er.getEscapeRegex()) != null) {
	    return obj;
	} else if ((obj = er.getRegexCapture()) != null) {
	    return obj;
	} else if ((obj = er.getSplit()) != null) {
	    return obj;
	} else if ((obj = er.getArithmetic()) != null) {
	    return obj;
	} else if ((obj = er.getBegin()) != null) {
	    return obj;
	} else if ((obj = er.getEnd()) != null) {
	    return obj;
	} else if ((obj = er.getSubstring()) != null) {
	    return obj;
	} else if ((obj = er.getTimeDifference()) != null) {
	    return obj;
	} else {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_MISSING_COMPONENT"));
	}
    }

    private Object getComponent(LocalVariable lVar) throws OvalException {
	Object obj = null;
	if ((obj = lVar.getObjectComponent()) != null) {
	    return obj;
	} else if ((obj = lVar.getLiteralComponent()) != null) {
	    return obj;
	} else if ((obj = lVar.getVariableComponent() ) != null) {
	    return obj;
	} else if ((obj = lVar.getConcat()) != null) {
	    return obj;
	} else if ((obj = lVar.getEscapeRegex()) != null) {
	    return obj;
	} else if ((obj = lVar.getRegexCapture()) != null) {
	    return obj;
	} else if ((obj = lVar.getSplit()) != null) {
	    return obj;
	} else if ((obj = lVar.getArithmetic()) != null) {
	    return obj;
	} else if ((obj = lVar.getBegin()) != null) {
	    return obj;
	} else if ((obj = lVar.getEnd()) != null) {
	    return obj;
	} else if ((obj = lVar.getSubstring()) != null) {
	    return obj;
	} else if ((obj = lVar.getTimeDifference()) != null) {
	    return obj;
	} else {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_MISSING_VARIABLE_COMPONENT", lVar.getId()));
	}
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
}
