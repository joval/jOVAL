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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
public class Main extends ThreadPoolExecutor {
    private static final String LOCAL = "Local";

    private static String PACKAGES = "org.joval.test.automation.schema:" +
				     JOVALSystem.getOvalProperty(JOVALSystem.OVAL_PROP_RESULTS);

    private static final ObjectFactory factory = new ObjectFactory();

    private static final File reportDir		= new File("reports");
    private static final File logDir		= new File("logs");

    private static DatatypeFactory datatype;
    private static Logger sysLogger;

    public static void main(String[] argv) {
	if (argv.length != 1) {
	    System.out.println("Usage:");
	    System.out.println("  java " + Main.class.getName() + " [test.ini]");
	    System.exit(1);
	}

	try {
	    datatype = DatatypeFactory.newInstance();

	    if (logDir.exists()) {
		File[] logs = logDir.listFiles();
		for (int i=0; i < logs.length; i++) {
		    logs[i].delete();
		}
	    } else {
		logDir.mkdir();
	    }
	    Handler sysHandler = new FileHandler("logs/main.log", false);
	    sysHandler.setFormatter(new LogFormatter());
	    sysHandler.setLevel(Level.FINER);
	    sysLogger = Logger.getLogger(JOVALSystem.getLogger().getName());
	    sysLogger.setLevel(Level.FINER);
	    sysLogger.addHandler(sysHandler);

	    if (!reportDir.exists()) {
		reportDir.mkdir();
	    }

	    BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
	    IniFile config = new IniFile(new File(argv[0]));
	    for (String name : config.listSections()) {
		System.out.println("Queuing test suite " + name);
		Properties props = config.getSection(name);
		PolymorphicPlugin plugin;
		if (LOCAL.equals(name)) {
		    plugin = new PolymorphicPlugin();
		} else {
		    plugin = new PolymorphicPlugin(props);
		}
		queue.add(new TestExecutor(name, props, plugin));
	    }

	    Report report = factory.createReport();
	    long runtime = System.currentTimeMillis();

	    ThreadPoolExecutor executor = new Main(report, 2, 3, 1L, TimeUnit.SECONDS, queue);
	    executor.shutdown();
	    executor.awaitTermination(5, TimeUnit.HOURS);

	    runtime = System.currentTimeMillis() - runtime;
	    report.setRuntime(datatype.newDuration(runtime));
	    report.setDate(datatype.newXMLGregorianCalendar(new GregorianCalendar()));
	    writeReport(report);

	    sysHandler.close();
	    System.exit(0);
	} catch (Exception e) {
	    e.printStackTrace();
	}

	System.exit(1);
    }

    /**
     * @override
     */
    protected void afterExecute(Runnable r, Throwable t) {
	if (r instanceof TestExecutor) {
	    TestExecutor te = (TestExecutor)r;
	    TestSuite suite = te.getTestSuite();
	    if (t != null) {
		suite.setError(LogFormatter.toString(t));
	    }
	    synchronized(report) {
		report.getTestSuite().add(suite);
	    }
	}
    }

    // Private

    private Report report;

    private Main(Report report, int minThreads, int maxThreads, long timeout, TimeUnit unit, BlockingQueue<Runnable> q) {
	super(minThreads, maxThreads, timeout, unit, q);
	this.report = report;
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

        writeTransform(report, new File("report_to_html.xsl"), new File(reportDir, fbase + ".html"));
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
}
