// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval;

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
import java.util.HashSet;
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

import jsaf.intf.util.ILoggable;
import org.slf4j.cal10n.LocLogger;

import scap.oval.common.MessageType;
import scap.oval.directives.OvalDirectives;
import scap.oval.definitions.core.OvalDefinitions;
import scap.oval.results.ContentEnumeration;
import scap.oval.results.CriteriaType;
import scap.oval.results.CriterionType;
import scap.oval.results.DefinitionType;
import scap.oval.results.DirectiveType;
import scap.oval.results.ExtendDefinitionType;
import scap.oval.results.ObjectFactory;
import scap.oval.results.OvalResults;
import scap.oval.results.DefinitionsType;
import scap.oval.results.ResultEnumeration;
import scap.oval.results.ResultsType;
import scap.oval.results.SystemType;
import scap.oval.results.TestedItemType;
import scap.oval.results.TestedVariableType;
import scap.oval.results.TestsType;
import scap.oval.results.TestType;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.OvalSystemCharacteristics;
import scap.oval.systemcharacteristics.core.VariableValueType;

import org.joval.intf.scap.oval.IDefinitions;
import org.joval.intf.scap.oval.ISystemCharacteristics;
import org.joval.intf.scap.oval.IResults;
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
public class Results implements IResults, ILoggable {
    private Hashtable<String, DefinitionType> definitionTable;
    private Hashtable<String, TestType> testTable;
    private IDefinitions definitions;
    private ISystemCharacteristics sc;
    private Directives directives;
    private LocLogger logger;

    public static final OvalResults getOvalResults(File f) throws OvalException {
	try {
	    Unmarshaller unmarshaller = SchemaRegistry.OVAL_RESULTS.getJAXBContext().createUnmarshaller();
	    Object rootObj = unmarshaller.unmarshal(f);
	    if (rootObj instanceof OvalResults) {
		return (OvalResults)rootObj;
	    } else {
	        throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_RESULTS_BAD_SOURCE, f));
	    }
	} catch (JAXBException e) {
	    throw new OvalException(e);
	}
    }

    /**
     * Create a Results based on the specified Definitions and SystemCharacteristics.
     */
    public Results(IDefinitions definitions, ISystemCharacteristics sc) {
	this.definitions = definitions;
	this.sc = sc;
	logger = JOVALMsg.getLogger();
	definitionTable = new Hashtable<String, DefinitionType>();
	testTable = new Hashtable<String, TestType>();
	directives = new Directives();
    }

    public void storeTestResult(TestType test) {
	testTable.put(test.getTestId(), test);
    }

    public TestType getTest(String testId) {
	return testTable.get(testId);
    }

    public ResultEnumeration getTestResult(String testId) {
	TestType testType = testTable.get(testId);
	if (testType == null) {
	    return null;
	}
	return testType.getResult();
    }

    public void storeDefinitionResult(DefinitionType definition) {
	definitionTable.put(definition.getDefinitionId(), definition);
    }

    // Implement ILoggable

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    public LocLogger getLogger() {
	return logger;
    }

    // Implement ITransformable

    public Source getSource() throws JAXBException, OvalException {
	return new JAXBSource(SchemaRegistry.OVAL_RESULTS.getJAXBContext(), getOvalResults());
    }

    public Object getRootObject() {
	return getOvalResults();
    }

    public JAXBContext getJAXBContext() throws JAXBException {
	return SchemaRegistry.OVAL_RESULTS.getJAXBContext();
    }

    // Implement IResults

    public ResultEnumeration getDefinitionResult(String definitionId) throws NoSuchElementException {
	DefinitionType definitionType = definitionTable.get(definitionId);
	if (definitionType == null) {
	    throw new NoSuchElementException(definitionId);
	}
	return definitionType.getResult();
    }

    public void setDirectives(File f) throws OvalException {
	directives = new Directives(f);
    }

    public Collection<DefinitionType> getDefinitionResults() throws OvalException {
	return getOvalResults().getResults().getSystem().get(0).getDefinitions().getDefinition();
    }

    public DefinitionType getDefinition(String definitionId) {
	return definitionTable.get(definitionId);
    }

    /**
     * Serialize to an XML File.
     */
    public void writeXML(File f) {
	OutputStream out = null;
	try {
	    Marshaller marshaller = SchemaRegistry.OVAL_RESULTS.getJAXBContext().createMarshaller();
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
	} catch (JAXBException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (OvalException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (FileNotFoundException e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, output);
	} catch (TransformerConfigurationException e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, output);
	} catch (TransformerException e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, output);
	}
    }

    public ISystemCharacteristics getSystemCharacteristics() {
	return sc;
    }

    public OvalResults getOvalResults() {
	OvalResults or = Factories.results.createOvalResults();
	or.setGenerator(OvalFactory.getGenerator());
	OvalDirectives od = directives.getOvalDirectives();
	or.setDirectives(od.getDirectives());
	or.getClassDirectives().addAll(od.getClassDirectives());
	if (directives.includeSource()) {
	    or.setOvalDefinitions(definitions.getOvalDefinitions());
	}
	SystemType systemType = Factories.results.createSystemType();

	//
	// Add definitions (using the Directives-filtered method) and simultaneously track reportable tests.
	//
	Hashtable<String, TestType> reportableTests = new Hashtable<String, TestType>();
	DefinitionsType definitionsType = Factories.results.createDefinitionsType();
	Collection<DefinitionType> defs = new Vector<DefinitionType>();
	for (DefinitionType definition : definitionTable.values()) {
	    DirectiveType directive = directives.getDirective(definition);
	    if (directive.isReported()) {
		switch (directive.getContent()) {
		  case FULL:
		    defs.add(definition);
		    break;
		  case THIN: {
		    DefinitionType thinDefinition = Factories.results.createDefinitionType();
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
	TestsType testsType = Factories.results.createTestsType();
	testsType.getTest().addAll(reportableTests.values());
	systemType.setTests(testsType);

	//
	// Add OvalSystemCharacteristics (applying the mask attributes)
	//
	systemType.setOvalSystemCharacteristics(sc.getOvalSystemCharacteristics(true));

	ResultsType resultsType = Factories.results.createResultsType();
	resultsType.getSystem().add(systemType);
	or.setResults(resultsType);
	return or;
    }

    // Private

    private Collection<String> getTestIds(DefinitionType definition) {
	Collection<String> testIds = new HashSet<String>();
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
