// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.test.automation;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import oval.schemas.results.core.DefinitionType;
import oval.schemas.results.core.OvalResults;
import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.test.automation.schema.ObjectFactory;
import org.joval.test.automation.schema.Report;
import org.joval.test.automation.schema.TestDocument;
import org.joval.test.automation.schema.TestResult;
import org.joval.test.automation.schema.TestResultEnumeration;
import org.joval.test.automation.schema.TestResults;
import org.joval.test.automation.schema.TestSuite;

import org.joval.intf.oval.IEngine;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.util.IObserver;
import org.joval.intf.util.IProducer;
import org.joval.intf.util.IProperty;
import org.joval.oval.OvalException;
import org.joval.util.JOVALSystem;
import org.joval.util.LogFormatter;

/**
 * Executes an OVAL test suite and stores the results.
 *
 * @author David A. Solin
 */
public class TestExecutor implements Runnable {
    private static final Logger sysLogger = Logger.getLogger(JOVALSystem.getLogger().getName());
    private static final ObjectFactory factory = new ObjectFactory();
    private static final File contentDir = new File("content");

    private static DatatypeFactory datatype;
    private static HashSet<String> knownFalses, knownUnknowns;
    static {
	try {
	    datatype = DatatypeFactory.newInstance();
	    knownFalses = new HashSet<String>();
	    knownUnknowns = new HashSet<String>();
	    knownFalses.add("oval:org.mitre.oval.test:def:608");	// Windows
	    knownUnknowns.add("oval:org.mitre.oval.test:def:337");	// Windows
	    knownFalses.add("oval:org.mitre.oval.test:def:997");	// Linux
	    knownUnknowns.add("oval:org.mitre.oval.test:def:423");	// Linux
	    knownFalses.add("oval:org.mitre.oval.test:def:879");	// Solaris
	    knownUnknowns.add("oval:org.mitre.oval.test:def:909");	// Solaris
	    knownFalses.add("oval:org.mitre.oval.test:def:87");		// MacOS X
	    knownUnknowns.add("oval:org.mitre.oval.test:def:581");	// MacOS X
	} catch (DatatypeConfigurationException e) {
	    throw new RuntimeException(e);
	}
    }

    private IProperty props;
    private String name;
    private PolymorphicPlugin plugin;
    private Report report;
    private TestSuite suite;

    TestExecutor(String name, IProperty props, PolymorphicPlugin plugin, Report report) {
	this.name = name;
	this.props = props;
	this.plugin = plugin;
	this.report = report;
	suite = factory.createTestSuite();
	suite.setName(name);
    }

    TestSuite getTestSuite() {
	return suite;
    }

    // Implement Runnable

    public void run() {
	long tm = 0;
	Handler handler = null;
	try {
	    plugin.setLogger(JOVALSystem.getLogger(name));
	    handler = new FileHandler("logs/target-" + plugin.getHostname() + ".log", false);
	    handler.setFormatter(new LogFormatter(LogFormatter.Type.FILE));
	    Level level = LogFormatter.toLevel(props.getProperty("logging.level"));
	    handler.setLevel(level);
	    Logger logger = Logger.getLogger(plugin.getLogger().getName());
	    logger.addHandler(handler);
	    logger.setLevel(level);

	    sysLogger.info(name + " - Started test suite");
	    tm = System.currentTimeMillis();
	    plugin.connect();
	    File testDir = new File(contentDir, plugin.getSessionType().value());
	    if (plugin.getSessionType() == IBaseSession.Type.UNIX) {
		testDir = new File(testDir, plugin.getSessionFlavor().value());
	    }
	    if (testDir.exists()) {
		SystemInfoType info = plugin.getSystemInfo();
		suite.setOS(info.getOsName());

		sysLogger.info(name + " - Base directory for tests: " + testDir.getCanonicalPath());
		plugin.installSupportFiles(testDir);
		plugin.disconnect();
		IEngine engine = JOVALSystem.createEngine(plugin);
		engine.getNotificationProducer().addObserver(new Observer(), IEngine.MESSAGE_MIN, IEngine.MESSAGE_MAX);

		for (String xml : testDir.list(new XMLFilter())) {
		    sysLogger.info(name + " - Processing " + xml);
		    File definitions = new File(testDir, xml);
		    engine.setDefinitionsFile(definitions);
		    TestDocument doc = factory.createTestDocument();
		    doc.setFileName(xml);

		    //
		    // Set the external variables file for the external variables test!
		    //
		    if ("oval-def_external_variable.xml".equals(xml)) {
			engine.setExternalVariables(JOVALSystem.createVariables(new File(testDir, "_external-variables.xml")));
		    }

		    long elapsed = System.currentTimeMillis();
		    engine.run();
		    elapsed = System.currentTimeMillis() - elapsed;
		    doc.setRuntime(datatype.newDuration(elapsed));
		    switch(engine.getResult()) {
		      case OK:
			TestResults results = factory.createTestResults();
			OvalResults or = engine.getResults().getOvalResults();
			results.setOvalResults(or);
			for (DefinitionType def : or.getResults().getSystem().get(0).getDefinitions().getDefinition()) {
			    TestResult result = factory.createTestResult();
			    String id = def.getDefinitionId();
			    result.setDefinitionId(id);
			    switch(def.getResult()) {
			      case TRUE:
				result.setResult(TestResultEnumeration.PASSED);
				break;

			      case FALSE:
				if (knownFalses.contains(id)) {
				    result.setResult(TestResultEnumeration.PASSED);
				} else {
				    result.setResult(TestResultEnumeration.FAILED);
				}
				break;

			      case UNKNOWN:
				if (knownUnknowns.contains(id)) {
				    result.setResult(TestResultEnumeration.PASSED);
				} else {
				    result.setResult(TestResultEnumeration.FAILED);
				}
				break;

			      default:
				result.setResult(TestResultEnumeration.FAILED);
				break;
			    }
			    results.getTestResult().add(result);
			}
			doc.setTestResults(results);
			break;

		      case ERR:
			doc.setError(LogFormatter.toString(engine.getError()));
			break;
		    }
		    suite.getTestDocument().add(doc);
		}
	    } else {
		sysLogger.info(name + " - No test content found for " + plugin.getHostname() + " at " + testDir.getPath());
	    }
	} catch (IOException e) {
	    sysLogger.warning(name + " - Failed to install validation support files for " + plugin.getHostname());
	    sysLogger.warning(name + " - " + LogFormatter.toString(e));
	} catch (OvalException e) {
	    sysLogger.warning(name + " - Failed to create OVAL engine for " + plugin.getHostname());
	    String err = LogFormatter.toString(e);
	    sysLogger.warning(name + " - " + err);
	    suite.setError(err);
	} finally {
	    if (tm > 0) {
		tm = System.currentTimeMillis() - tm;
		suite.setRuntime(datatype.newDuration(tm));
	    }
	    synchronized(report) {
		report.getTestSuite().add(suite);
	    }
	    sysLogger.info(name + " - Completed test suite");
	    if (handler != null) {
		handler.close();
	    }
	}
    }

    // Private

    /**
     * A FilenameFilter that lists the definition XML files for the Test Suite being executed.
     */
    private class XMLFilter implements FilenameFilter {
	XMLFilter() {}

	// Implement FilenameFilter

	public boolean accept(File dir, String fname) {
	    return !fname.startsWith("_") && fname.toLowerCase().endsWith(".xml");
	}
    }

    /**
     * An inner class that logs information about Engine notifications.
     */
    private class Observer implements IObserver {
	public Observer() {}
    
	public void notify(IProducer source, int msg, Object arg) {
	    switch(msg) {
	      case IEngine.MESSAGE_OBJECT_PHASE_START:
		sysLogger.info(name + " - Scanning objects");
		break;

	      case IEngine.MESSAGE_DEFINITION_PHASE_START:
		sysLogger.info(name + " - Evaluating definitions");
		break;

	      case IEngine.MESSAGE_DEFINITION:
		sysLogger.fine(name + " - Evaluating " + (String)arg);
		break;

	      case IEngine.MESSAGE_OBJECT:
		sysLogger.fine(name + " - Collecting " + (String)arg);
		break;

	      case IEngine.MESSAGE_OBJECT_PHASE_END:
		sysLogger.info(name + " - Finished collecting objects");
		break;

	      case IEngine.MESSAGE_DEFINITION_PHASE_END:
		sysLogger.info(name + " - Finished evaluating definitions");
		break;

	      case IEngine.MESSAGE_SYSTEMCHARACTERISTICS:
		break;

	      default:
		sysLogger.warning(name + " - Unexpected message: " + msg);
		break;
	    }
	}
    }
}
