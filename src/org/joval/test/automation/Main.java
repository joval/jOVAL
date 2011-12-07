// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.test.automation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Formatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

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
import org.joval.intf.oval.IResults;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.util.IObserver;
import org.joval.intf.util.IProducer;
import org.joval.oval.OvalException;
import org.joval.util.JOVALSystem;
import org.joval.util.StringTools;

/**
 * A test class that runs jOVAL through all the relevant OVAL test content and generates a report.
 *
 * @author David A. Solin
 */
public class Main {
    private static String PACKAGES = "org.joval.test.automation.schema:" +
				     JOVALSystem.getOvalProperty(JOVALSystem.OVAL_PROP_RESULTS);

    private static final ObjectFactory factory = new ObjectFactory();

    private static final File contentDir	= new File("content");
    private static final File reportDir	= new File("reports");

    private static HashSet<String> knownFalses		= new HashSet<String>();
    private static HashSet<String> knownUnknowns	= new HashSet<String>();
    static {
	knownFalses.add("oval:org.mitre.oval.test:def:608");	// Windows
	knownUnknowns.add("oval:org.mitre.oval.test:def:337");	// Windows
	knownFalses.add("oval:org.mitre.oval.test:def:997");	// Linux
	knownUnknowns.add("oval:org.mitre.oval.test:def:423");	// Linux
	knownFalses.add("oval:org.mitre.oval.test:def:879");	// Solaris
	knownUnknowns.add("oval:org.mitre.oval.test:def:909");	// Solaris
	knownFalses.add("oval:org.mitre.oval.test:def:87");	// MacOS X
	knownUnknowns.add("oval:org.mitre.oval.test:def:581");	// MacOS X
    }

    private static DatatypeFactory datatype;

    public static void main(String[] argv) {
	if (argv.length != 1) {
	    System.out.println("Usage:");
	    System.out.println("  java " + Main.class.getName() + " [test.ini]");
	    System.exit(1);
	}

	try {
	    datatype = DatatypeFactory.newInstance();

	    Handler handler = new FileHandler("test.log", false);
	    handler.setFormatter(new LogFormatter());
	    handler.setLevel(Level.FINEST);
	    Logger logger = Logger.getLogger(JOVALSystem.class.getName());
	    logger.addHandler(handler);

	    IniFile config = new IniFile(new File(argv[0]));

	    if (!reportDir.exists()) {
		reportDir.mkdir();
	    }

	    Report report = factory.createReport();
	    long runtime = System.currentTimeMillis();
	    for (String name : config.listSections()) {
		System.out.println("Starting test suite run for " + name);
		TestSuite suite = runTests(config.getSection(name));
		suite.setName(name);
		report.getTestSuite().add(suite);
		System.out.println("Tests completed for " + name);
	    }
	    runtime = System.currentTimeMillis() - runtime;
	    report.setRuntime(datatype.newDuration(runtime));
	    report.setDate(datatype.newXMLGregorianCalendar(new GregorianCalendar()));
	    writeReport(report);

	    System.exit(0);
	} catch (Exception e) {
	    e.printStackTrace();
	}

	System.exit(1);
    }

    // Private

    private static TestSuite runTests(Properties props) {
	TestSuite suite = factory.createTestSuite();
	long tm = System.currentTimeMillis();
	Plugin plugin = new Plugin(props);
	try {
	    plugin.connect();
	    File testDir = new File(contentDir, plugin.getSessionType().toString());
	    if (plugin.getSessionType() == IBaseSession.Type.UNIX) {
		testDir = new File(testDir, plugin.getSessionFlavor().getOsName());
	    }
	    if (testDir.exists()) {
		System.out.println("Base directory for tests: " + testDir.getCanonicalPath());
		plugin.installSupportFiles(testDir);
		plugin.disconnect();
		IEngine engine = JOVALSystem.createEngine(plugin);
		engine.getNotificationProducer().addObserver(new Observer(), IEngine.MESSAGE_MIN, IEngine.MESSAGE_MAX);

		for (String xml : testDir.list(new XMLFilter())) {
		    System.out.println("Processing " + xml);
		    File definitions = new File(testDir, xml);
		    engine.setDefinitionsFile(definitions);
		    TestDocument doc = factory.createTestDocument();
		    doc.setFileName(xml);

		    //
		    // Set the external variables file for the external variables test!
		    //
		    if ("oval-def_external_variable.xml".equals(xml)) {
			engine.setExternalVariablesFile(new File(testDir, "_external-variables.xml"));
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
		System.out.println("No test content found for " + plugin.getHostname() + " at " + testDir.getPath());
	    }
	} catch (IOException e) {
	    System.out.println("Failed to install validation support files for " + plugin.getHostname());
	    e.printStackTrace();
	} catch (OvalException e) {
	    System.out.println("Failed to create OVAL engine for " + plugin.getHostname());
	    e.printStackTrace();
	}
	tm = System.currentTimeMillis() - tm;
	suite.setRuntime(datatype.newDuration(tm));
	return suite;
    }

    private static void writeReport(Report report) throws Exception {
	String fbase = "report." + reportDir.list(new ReportFilter()).length;
	File reportFile = new File(reportDir, fbase + ".xml");
	System.out.println("Writing XML report " + reportFile);

	OutputStream out = null;
	try {
	    JAXBContext ctx = JAXBContext.newInstance(PACKAGES);
	    Marshaller marshaller = ctx.createMarshaller();
	    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
	    out = new FileOutputStream(reportFile);
	    marshaller.marshal(report, out);
	} finally {
	    if (out != null) {
		try {
		    out.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }
	}

        writeTransform(report, new File("transform.xsl"), new File(reportDir, fbase + ".html"));
    }

    private static void writeTransform(Report report, File transform, File output) throws Exception {
	System.out.println("Writing transform " + output);
	TransformerFactory xf = TransformerFactory.newInstance();
	Transformer transformer = xf.newTransformer(new StreamSource(new FileInputStream(transform)));
	JAXBContext ctx = JAXBContext.newInstance(PACKAGES);
	transformer.transform(new JAXBSource(ctx, report), new StreamResult(output));
    }

    private static final Report readReport(File f) throws Exception {
	JAXBContext ctx = JAXBContext.newInstance(PACKAGES);
	Unmarshaller unmarshaller = ctx.createUnmarshaller();
	Object rootObj = unmarshaller.unmarshal(f);
	if (rootObj instanceof Report) {
	    return (Report)rootObj;
	} else {
	    throw new Exception("Not a report file: " + f);
	}
    }

    private static class ReportFilter implements FilenameFilter {
	ReportFilter() {}

	// Implement FilenameFilter

	public boolean accept(File dir, String name) {
	   name = name.toLowerCase();
	   return name.startsWith("report.") && name.endsWith(".xml");
	}
    }

    private static class XMLFilter implements FilenameFilter {
	XMLFilter() {}

	// Implement FilenameFilter

	public boolean accept(File dir, String name) {
	    return !name.startsWith("_") && name.toLowerCase().endsWith(".xml");
	}
    }

    /**
     * An inner class that prints out information about Engine notifications.
     */
    private static class Observer implements IObserver {
	private String lastMessage = null;

	public Observer() {}
    
	public void notify(IProducer source, int msg, Object arg) {
	    switch(msg) {
	      case IEngine.MESSAGE_OBJECT_PHASE_START:
		System.out.print("  Scanning objects... ");
		break;

	      case IEngine.MESSAGE_DEFINITION_PHASE_START:
		System.out.print("  Evaluating definitions... ");
		break;

	      case IEngine.MESSAGE_DEFINITION:
	      case IEngine.MESSAGE_OBJECT: {
		String s = (String)arg;
		int offset=0;
		if (lastMessage != null) {
		    int len = lastMessage.length();
		    int n = Math.min(len, s.length());
		    for (int i=0; i < n; i++) {
			if (s.charAt(i) == lastMessage.charAt(i)) {
			    offset++;
			}
		    }
		    StringBuffer back = new StringBuffer();
		    StringBuffer clean = new StringBuffer();
		    int toClear = len - offset;
		    for (int i=0; i < toClear; i++) {
			back.append('\b');
			clean.append(' ');
		    }
		    System.out.print(back.toString());
		    System.out.print(clean.toString());
		    System.out.print(back.toString());
		}
		System.out.print(s.substring(offset));
		lastMessage = s;
		break;
	      }

	      case IEngine.MESSAGE_OBJECT_PHASE_END:
	      case IEngine.MESSAGE_DEFINITION_PHASE_END:
		notify(source, IEngine.MESSAGE_DEFINITION, "DONE");
		System.out.println("");
		lastMessage = null;
		break;

	      case IEngine.MESSAGE_SYSTEMCHARACTERISTICS:
		break;

	      default:
		System.out.println("  Unexpected message: " + msg);
		break;
	    }
	}
    }
}
