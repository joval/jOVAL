// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Vector;
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
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.cal10n.LocLogger;

import oval.schemas.common.GeneratorType;
import oval.schemas.common.MessageType;
import oval.schemas.directives.core.OvalDirectives;
import oval.schemas.definitions.core.OvalDefinitions;
import oval.schemas.results.core.ContentEnumeration;
import oval.schemas.results.core.CriteriaType;
import oval.schemas.results.core.CriterionType;
import oval.schemas.results.core.DefinitionType;
import oval.schemas.results.core.DirectiveType;
import oval.schemas.results.core.ExtendDefinitionType;
import oval.schemas.results.core.ObjectFactory;
import oval.schemas.results.core.OvalResults;
import oval.schemas.results.core.DefinitionsType;
import oval.schemas.results.core.ResultEnumeration;
import oval.schemas.results.core.ResultsType;
import oval.schemas.results.core.SystemType;
import oval.schemas.results.core.TestedItemType;
import oval.schemas.results.core.TestedVariableType;
import oval.schemas.results.core.TestsType;
import oval.schemas.results.core.TestType;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.OvalSystemCharacteristics;
import oval.schemas.systemcharacteristics.core.VariableValueType;

import org.joval.intf.oval.IDefinitions;
import org.joval.intf.oval.IResults;
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
public class Results implements IResults {
    private Hashtable<String, DefinitionType> definitionTable;
    private Hashtable<String, TestType> testTable;
    private IDefinitions definitions;
    private SystemCharacteristics sc;
    private Directives directives;
    private OvalResults or;
    private LocLogger logger;
    private JAXBContext ctx;

    public static final OvalResults getOvalResults(File f) throws OvalException {
	try {
	    String packages = JOVALSystem.getSchemaProperty(JOVALSystem.OVAL_PROP_RESULTS);
	    JAXBContext ctx = JAXBContext.newInstance(packages);
	    Unmarshaller unmarshaller = ctx.createUnmarshaller();
	    Object rootObj = unmarshaller.unmarshal(f);
	    if (rootObj instanceof OvalResults) {
		return (OvalResults)rootObj;
	    } else {
	        throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_RESULTS_BAD_SOURCE, f));
	    }
	} catch (JAXBException e) {
	    throw new OvalException(e);
	}
    }

    /**
     * Create a Results based on the specified Definitions and SystemCharacteristics.
     */
    Results(IDefinitions definitions, SystemCharacteristics sc) {
	this.definitions = definitions;
	this.sc = sc;
	logger = sc.getLogger();
	definitionTable = new Hashtable<String, DefinitionType>();
	testTable = new Hashtable<String, TestType>();
	directives = new Directives();
	or = null;
	try {
	    ctx = JAXBContext.newInstance(JOVALSystem.getSchemaProperty(JOVALSystem.OVAL_PROP_RESULTS));
	} catch (JAXBException e) {
	    logger.error(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    // Implement ITransformable

    public Source getSource() {
	Source src = null;
	try {
	    src = new JAXBSource(ctx, getOvalResults());
	} catch (JAXBException e) {
	    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return src;
    }

    // Implement IResults

    public void setDirectives(File f) throws OvalException {
	directives = new Directives(f);
	or = null; // reset results if they have been previously computed.
    }

    /**
     * Serialize to an XML File.
     */
    public void writeXML(File f) {
	OutputStream out = null;
	try {
	    Marshaller marshaller = ctx.createMarshaller();
	    OvalNamespacePrefixMapper.configure(marshaller, OvalNamespacePrefixMapper.URI.RES);
	    out = new FileOutputStream(f);
	    marshaller.marshal(getOvalResults(), out);
	} catch (JAXBException e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
	} catch (FactoryConfigurationError e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
	} catch (FileNotFoundException e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
	} finally {
	    if (out != null) {
		try {
		    out.close();
		} catch (IOException e) {
		    logger.warn(JOVALMsg.ERROR_FILE_CLOSE,  e.toString());
		}
	    }
	}
    }

    /**
     * Transform using the specified template, and serialize to the specified file.
     */
    public void writeTransform(File transform, File output) {
	try {
	    TransformerFactory xf = TransformerFactory.newInstance();
	    Transformer transformer = xf.newTransformer(new StreamSource(new FileInputStream(transform)));
	    transformer.transform(getSource(), new StreamResult(output));
	} catch (FileNotFoundException e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, output);
	} catch (TransformerConfigurationException e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, output);
	} catch (TransformerException e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, output);
	}
    }

    public OvalResults getOvalResults() {
	if (or != null) {
	    return or;
	}
	or = JOVALSystem.factories.results.createOvalResults();
	or.setGenerator(Engine.getGenerator());
	OvalDirectives od = directives.getOvalDirectives();
	or.setDirectives(od.getDirectives());
	or.getClassDirectives().addAll(od.getClassDirectives());
	if (directives.includeSource()) {
	    or.setOvalDefinitions(definitions.getOvalDefinitions());
	}
	SystemType systemType = JOVALSystem.factories.results.createSystemType();

	//
	// Add definitions (using the Directives-filtered method) and simultaneously track reportable tests.
	//
	Hashtable<String, TestType> reportableTests = new Hashtable<String, TestType>();
	DefinitionsType definitionsType = JOVALSystem.factories.results.createDefinitionsType();
	Collection<DefinitionType> defs = new Vector<DefinitionType>();
	for (DefinitionType definition : definitionTable.values()) {
	    DirectiveType directive = directives.getDirective(definition);
	    if (directive.isReported()) {
		switch (directive.getContent()) {
		  case FULL:
		    defs.add(definition);
		    break;
		  case THIN: {
		    DefinitionType thinDefinition = JOVALSystem.factories.results.createDefinitionType();
		    thinDefinition.setDefinitionId(definition.getDefinitionId());
		    thinDefinition.setClazz(definition.getClazz());
		    thinDefinition.setResult(definition.getResult());
		    defs.add(thinDefinition);
		    break;
		  }
		}
	    }
	}
	for (DefinitionType definition : defs) {
	    definitionsType.getDefinition().add(definition);
	    for (String testId : getTestIds(definition)) {
		if (!reportableTests.containsKey(testId)) {
		    reportableTests.put(testId, testTable.get(testId));
		}
	    }
	}
	systemType.setDefinitions(definitionsType);

	//
	// Add only those tests for which there are fully-reportable definitions.
	//
	TestsType testsType = JOVALSystem.factories.results.createTestsType();
	testsType.getTest().addAll(reportableTests.values());
	systemType.setTests(testsType);

	//
	// Add OvalSystemCharacteristics filtered by reportable Variable and Object IDs.
	//
	Collection<String> reportableVariables = getVariableIds(reportableTests);
	Collection<BigInteger> reportableItems = getItemIds(reportableTests);
	systemType.setOvalSystemCharacteristics(sc.getOvalSystemCharacteristics(reportableVariables, reportableItems));

	ResultsType resultsType = JOVALSystem.factories.results.createResultsType();
	resultsType.getSystem().add(systemType);
	or.setResults(resultsType);
	return or;
    }

    // Internal

    void storeTestResult(TestType test) {
	logger.trace(JOVALMsg.STATUS_TEST, test.getTestId());
	testTable.put(test.getTestId(), test);
    }

    TestType getTest(String testId) {
	return testTable.get(testId);
    }

    ResultEnumeration getTestResult(String testId) {
	TestType testType = testTable.get(testId);
	if (testType == null) {
	    return null;
	}
	return testType.getResult();
    }

    void storeDefinitionResult(DefinitionType definition) {
	definitionTable.put(definition.getDefinitionId(), definition);
    }

    DefinitionType getDefinition(String definitionId) {
	return definitionTable.get(definitionId);
    }

    ResultEnumeration getDefinitionResult(String definitionId) {
	DefinitionType definitionType = definitionTable.get(definitionId);
	if (definitionType == null) {
	    return null;
	}
	return definitionType.getResult();
    }

    // Private

    private Collection<String> getVariableIds(Hashtable<String, TestType> tests) {
	Collection<String> variableIds = new Vector<String>();
	for (TestType test : tests.values()) {
	   for (TestedVariableType testedVariableType : test.getTestedVariable()) {
		String variableId = testedVariableType.getVariableId();
		if (!variableIds.contains(variableId)) {
		    variableIds.add(variableId);
		}
	    }
	}
	return variableIds;
    }

    private Collection<BigInteger> getItemIds(Hashtable<String, TestType> tests) {
	Collection<BigInteger> itemIds = new Vector<BigInteger>();
	for (TestType test : tests.values()) {
	    for (TestedItemType testedItemType : test.getTestedItem()) {
		BigInteger itemId = testedItemType.getItemId();
		if (!itemIds.contains(itemId)) {
		    itemIds.add(itemId);
		}
	    }
	}
	return itemIds;
    }

    private Collection<String> getTestIds(DefinitionType definition) {
	Collection<String> testIds = new Vector<String>();
	getTestIds(definition.getCriteria(), testIds);
	return testIds;
    }

    private void getTestIds(CriteriaType criteria, Collection<String> testIds) {
	if (criteria == null) {
	    return; // Criteria have been filtered from the definition.
	}
	for (Object child : criteria.getCriteriaOrCriterionOrExtendDefinition()) {
	    if (child instanceof CriteriaType) {
		getTestIds((CriteriaType)child, testIds);
	    } else if (child instanceof CriterionType) {
		String testId = ((CriterionType)child).getTestRef();
		if (!testIds.contains(testId)) {
		    testIds.add(testId);
		}
	    } else if (child instanceof ExtendDefinitionType) {
		String definitionId = ((ExtendDefinitionType)child).getDefinitionRef();
		for (String testId : getTestIds(definitionTable.get(definitionId))) {
		    if (!testIds.contains(testId)) {
			testIds.add(testId);
		    }
		}
	    }
	}
    }
}
