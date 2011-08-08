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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

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

import org.joval.intf.oval.IResults;
import org.joval.oval.OvalException;
import org.joval.oval.xml.OvalNamespacePrefixMapper;
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
class Results implements IResults {
    private Hashtable<String, DefinitionType> definitionTable;
    private Hashtable<String, TestType> testTable;
    private Definitions definitions;
    private SystemCharacteristics sc;
    private Directives directives;
    private OvalResults or;

    /**
     * Create an empty SystemCharacteristics.
     */
    Results(Definitions definitions, SystemCharacteristics sc) {
	this.definitions = definitions;
	this.sc = sc;
	definitionTable = new Hashtable<String, DefinitionType>();
	testTable = new Hashtable<String, TestType>();
	directives = new Directives();
	or = null;
    }

    // Implement IResults

    public void setDirectives(File f) throws OvalException {
	directives = new Directives(f);
	or = null; // reset results if they have been previously computed.
    }

    public Iterator<DefinitionType> iterateDefinitions() {
	Vector<DefinitionType> definitions = new Vector<DefinitionType>();
	for (DefinitionType definition : definitionTable.values()) {
	    DirectiveType directive = directives.getDirective(definition);
	    if (directive.isReported()) {
		switch (directive.getContent()) {
		  case FULL:
		    definitions.add(definition);
		    break;
		  case THIN: {
		    DefinitionType thinDefinition = JOVALSystem.resultsFactory.createDefinitionType();
		    thinDefinition.setDefinitionId(definition.getDefinitionId());
		    thinDefinition.setClazz(definition.getClazz());
		    thinDefinition.setResult(definition.getResult());
		    definitions.add(thinDefinition);
		    break;
		  }
		}
	    }
	}
	return definitions.iterator();
    }

    /**
     * Serialize to an XML File.
     */
    public void writeXML(File f) {
	OutputStream out = null;
	try {
	    JAXBContext ctx = JAXBContext.newInstance(JOVALSystem.getOvalProperty(JOVALSystem.OVAL_PROP_RESULTS));
	    Marshaller marshaller = ctx.createMarshaller();
	    OvalNamespacePrefixMapper.configure(marshaller, OvalNamespacePrefixMapper.URI.RES);
	    out = new FileOutputStream(f);
	    marshaller.marshal(getOvalResults(), out);
	} catch (JAXBException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_FILE_GENERATE", f.toString()), e);
	} catch (FactoryConfigurationError e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_FILE_GENERATE", f.toString()), e);
	} catch (FileNotFoundException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_FILE_GENERATE", f.toString()), e);
	} finally {
	    if (out != null) {
		try {
		    out.close();
		} catch (IOException e) {
		    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_FILE_CLOSE",  e.toString()));
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
	    JAXBContext ctx = JAXBContext.newInstance(JOVALSystem.getOvalProperty(JOVALSystem.OVAL_PROP_RESULTS));
	    transformer.transform(new JAXBSource(ctx, getOvalResults()), new StreamResult(output));
	} catch (FileNotFoundException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_FILE_GENERATE", output), e.toString());
	} catch (JAXBException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_FILE_GENERATE", output), e.toString());
	} catch (TransformerConfigurationException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_FILE_GENERATE", output), e.toString());
	} catch (TransformerException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_FILE_GENERATE", output), e.toString());
	}
    }

    // Internal

    void storeTestResult(TestType test) {
	JOVALSystem.getLogger().log(Level.FINER, JOVALSystem.getMessage("STATUS_TEST", test.getTestId()));
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

    private OvalResults getOvalResults() {
	if (or != null) {
	    return or;
	}
	or = JOVALSystem.resultsFactory.createOvalResults();
	or.setGenerator(Engine.getGenerator());
	OvalDirectives od = directives.getOvalDirectives();
	or.setDirectives(od.getDirectives());
	or.getClassDirectives().addAll(od.getClassDirectives());
	if (directives.includeSource()) {
	    or.setOvalDefinitions(definitions.getOvalDefinitions());
	}
	SystemType systemType = JOVALSystem.resultsFactory.createSystemType();

	//
	// Add definitions (using the Directives-filtered method) and simultaneously track reportable tests.
	//
	Hashtable<String, TestType> reportableTests = new Hashtable<String, TestType>();
	DefinitionsType definitionsType = JOVALSystem.resultsFactory.createDefinitionsType();
	Iterator<DefinitionType> iter = iterateDefinitions();
	while(iter.hasNext()) {
	    DefinitionType definition = iter.next();
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
	TestsType testsType = JOVALSystem.resultsFactory.createTestsType();
	testsType.getTest().addAll(reportableTests.values());
	systemType.setTests(testsType);

	//
	// Add OvalSystemCharacteristics filtered by reportable Variable and Object IDs.
	//
	List<String> reportableVariables = getVariableIds(reportableTests);
	List<BigInteger> reportableItems = getItemIds(reportableTests);
	systemType.setOvalSystemCharacteristics(sc.getOvalSystemCharacteristics(reportableVariables, reportableItems));

	ResultsType resultsType = JOVALSystem.resultsFactory.createResultsType();
	resultsType.getSystem().add(systemType);
	or.setResults(resultsType);
	return or;
    }

    private List<String> getVariableIds(Hashtable<String, TestType> tests) {
	List<String> variableIds = new Vector<String>();
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

    private List<BigInteger> getItemIds(Hashtable<String, TestType> tests) {
	List<BigInteger> itemIds = new Vector<BigInteger>();
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

    private List<String> getTestIds(DefinitionType definition) {
	List<String> testIds = new Vector<String>();
	getTestIds(definition.getCriteria(), testIds);
	return testIds;
    }

    private void getTestIds(CriteriaType criteria, List<String> testIds) {
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
