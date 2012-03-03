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
import java.util.Hashtable;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
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

import org.joval.test.automation.schema.ObjectFactory;
import org.joval.test.automation.schema.Report;

import org.joval.identity.SimpleCredentialStore;
import org.joval.intf.oval.IEngine;
import org.joval.intf.oval.IResults;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.util.IObserver;
import org.joval.intf.util.IProducer;
import org.joval.intf.util.IProperty;
import org.joval.oval.OvalException;
import org.joval.util.IniFile;
import org.joval.util.JOVALSystem;
import org.joval.util.StringTools;
import org.joval.util.LogFormatter;

/**
 * A test class that runs jOVAL through all the relevant OVAL test content and generates a report.
 *
 * @author David A. Solin
 */
public class Main {
    private static final Logger sysLogger = Logger.getLogger(JOVALSystem.getLogger().getName());
    private static final File reportDir = new File("reports");
    private static final String LOCAL = "Suite: Local";

    public static void main(String[] argv) {
	if (argv.length != 1) {
	    System.out.println("Usage:");
	    System.out.println("  java " + Main.class.getName() + " [test.ini]");
	    System.exit(1);
	}

	try {
	    IniFile config = new IniFile(new File(argv[0]));

	    File logDir = new File("logs");
	    if (logDir.exists()) {
		File[] logs = logDir.listFiles();
		for (int i=0; i < logs.length; i++) {
		    logs[i].delete();
		}
	    } else {
		logDir.mkdir();
	    }
	    Handler sysHandler = new FileHandler("logs/main.log", false);
	    sysHandler.setFormatter(new LogFormatter(LogFormatter.Type.FILE));
	    sysHandler.setLevel(LogFormatter.toLevel(config.getProperty("Config", "logging.level.file")));
	    sysLogger.setLevel(LogFormatter.toLevel(config.getProperty("Config", "logging.level.file")));
	    sysLogger.addHandler(sysHandler);
	    ConsoleHandler consoleHandler = new ConsoleHandler();
	    consoleHandler.setFormatter(new LogFormatter(LogFormatter.Type.CONSOLE));
	    consoleHandler.setLevel(LogFormatter.toLevel(config.getProperty("Config", "logging.level.console")));
	    sysLogger.addHandler(consoleHandler);

	    if (!reportDir.exists()) {
		reportDir.mkdir();
	    }

	    SimpleCredentialStore scs = new SimpleCredentialStore();
	    for (String name : config.listSections()) {
		if (name.startsWith("Credential:")) {
		    String hostname = name.substring(11).trim();
		    IProperty props = config.getSection(name);
		    props.setProperty(SimpleCredentialStore.PROP_HOSTNAME, hostname);
		    scs.add(props);
		}
	    }
	    PolymorphicPlugin.setCredentialStore(scs);
	    PolymorphicPlugin.setDataDirectory(JOVALSystem.getDataDirectory());
	    IProperty routes = config.getSection("Routes");
	    for (String destination : routes) {
		PolymorphicPlugin.addRoute(destination, routes.getProperty(destination));
	    }

	    ExecutorService pool = Executors.newFixedThreadPool(config.getSection("Config").getIntProperty("concurrency"));
	    Report report = new ObjectFactory().createReport();
	    Hashtable<String, IResults> results = new Hashtable<String, IResults>();

	    long runtime = System.currentTimeMillis();

	    for (String name : config.listSections()) {
		if (name.startsWith("Suite: ")) {
		    IProperty props = config.getSection(name);
		    PolymorphicPlugin plugin;
		    if (LOCAL.equals(name)) {
			plugin = new PolymorphicPlugin();
		    } else {
			plugin = new PolymorphicPlugin(props.getProperty("hostname"));
		    }
		    pool.execute(new TestExecutor(name.substring(7), props, plugin, report, results));
		}
	    }

	    pool.shutdown();
	    pool.awaitTermination(5, TimeUnit.HOURS);

	    runtime = System.currentTimeMillis() - runtime;
	    DatatypeFactory datatype = DatatypeFactory.newInstance();
	    report.setRuntime(datatype.newDuration(runtime));
	    report.setDate(datatype.newXMLGregorianCalendar(new GregorianCalendar()));
	    writeReport(report, results);

	    sysHandler.close();
	    System.exit(0);
	} catch (Exception e) {
	    e.printStackTrace();
	}

	System.exit(1);
    }

    // Private

    private static String PACKAGES = "org.joval.test.automation.schema";

    private static void writeReport(Report report, Hashtable<String, IResults> results) throws Exception {
	String fbase = "report." + reportDir.list(new ReportFilter()).length;
	File resultsDir = new File(reportDir, fbase);
	resultsDir.mkdir();
	for (String name : results.keySet()) {
	    File resultsFile = new File(resultsDir, name);
	    sysLogger.info("Writing XML results " + resultsFile.getPath());
	    results.get(name).writeXML(resultsFile);
	}

	File reportFile = new File(resultsDir, "report.xml");
	sysLogger.info("Writing XML report " + reportFile.getPath());

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

        writeTransform(report, new File("report_to_html.xsl"), new File(resultsDir, "report.html"));
    }

    private static void writeTransform(Report report, File transform, File output) throws Exception {
	sysLogger.info("Writing transform " + output);
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
	   return name.startsWith("report.") && new File(dir, name).isDirectory();
	}
    }
}
