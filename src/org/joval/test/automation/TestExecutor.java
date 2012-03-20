// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.test.automation;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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

import org.joval.discovery.Local;
import org.joval.discovery.SessionFactory;
import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.oval.IEngine;
import org.joval.intf.oval.IResults;
import org.joval.intf.oval.IVariables;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.ISession;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.util.IObserver;
import org.joval.intf.util.IProducer;
import org.joval.intf.util.IProperty;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.LogFormatter;
import org.joval.util.StringTools;

/**
 * Executes an OVAL test suite and stores the results.
 *
 * @author David A. Solin
 */
public class TestExecutor implements Runnable {
    private static final String LOCAL = "Local";

    private static final Logger sysLogger = Logger.getLogger(JOVALMsg.getLogger().getName());
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

    private String name;
    private IProperty props;
    private Main.ReportContext ctx;
    private TestSuite suite;
    private SessionFactory sf;
    private IFilesystem fs;

    TestExecutor(Main.ReportContext ctx, IProperty props, SessionFactory sf) {
	this.ctx = ctx;
	name = ctx.getName();
	this.props = props;
	this.sf = sf;
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
        String hostname = props.getProperty("hostname");
	try {
            IBaseSession session = null;
            if (LOCAL.equals(name)) {
		hostname = "localhost";
                session = Local.createSession();
            } else {
                session = sf.createSession(props.getProperty("hostname"));
            }

	    session.setLogger(JOVALMsg.getLogger(name));
	    handler = new FileHandler("logs/target-" + hostname + ".log", false);
	    handler.setFormatter(new LogFormatter(LogFormatter.Type.FILE));
	    Level level = LogFormatter.toLevel(props.getProperty("logging.level"));
	    handler.setLevel(level);
	    Logger logger = Logger.getLogger(session.getLogger().getName());
	    logger.addHandler(handler);
	    logger.setLevel(level);

	    sysLogger.info(name + " - Started test suite");
	    tm = System.currentTimeMillis();

	    if (session.connect()) {
		File testDir = new File(contentDir, session.getType().value());
		if (session.getType() == IBaseSession.Type.UNIX) {
		    testDir = new File(testDir, ((IUnixSession)session).getFlavor().value());
		}
		if (testDir.exists()) {
		    SystemInfoType info = session.getSystemInfo();
		    suite.setOS(info.getOsName());

		    sysLogger.info(name + " - Base directory for tests: " + testDir.getCanonicalPath());
		    if (session instanceof ISession) {
			installSupportFiles((ISession)session, testDir);
		    }

		    IEngine engine = JOVALSystem.createEngine(session);
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
			    IVariables variables = JOVALSystem.createVariables(new File(testDir, "_external-variables.xml"));
			    engine.setExternalVariables(variables);
			}

			long elapsed = System.currentTimeMillis();
			engine.run();
			elapsed = System.currentTimeMillis() - elapsed;
			doc.setRuntime(datatype.newDuration(elapsed));
			switch(engine.getResult()) {
			  case OK:
			    TestResults testResults = factory.createTestResults();
			    IResults res = engine.getResults();
			    String key = name + "-" + xml;
			    ctx.addResult(key, res);
			    testResults.setFileName(key);
			    OvalResults or = res.getOvalResults();
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
				testResults.getTestResult().add(result);
			    }
			    doc.setTestResults(testResults);
			    break;

			  case ERR:
			    doc.setError(LogFormatter.toString(engine.getError()));
			    break;
			}
			suite.getTestDocument().add(doc);
		    }
		} else {
		    sysLogger.info(name + " - No test content found for " + hostname + " at " + testDir.getPath());
		}
	    } else {
		sysLogger.info(name + " - Unable to connect to " + hostname + " - abandoning test suite");
	    }
	} catch (IOException e) {
	    sysLogger.warning(name + " - Failed to install validation support files for " + hostname);
	    sysLogger.warning(name + " - " + LogFormatter.toString(e));
	} catch (OvalException e) {
	    sysLogger.warning(name + " - Failed to create OVAL engine for " + hostname);
	    String err = LogFormatter.toString(e);
	    sysLogger.warning(name + " - " + err);
	    suite.setError(err);
	} finally {
	    if (tm > 0) {
		tm = System.currentTimeMillis() - tm;
		suite.setRuntime(datatype.newDuration(tm));
	    }
	    Report report = ctx.getReport();
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
     * Install the validation support files from the specified testDir to the target host machine.
     */
    private void installSupportFiles(ISession session, File testDir) throws IOException {
	File f = new File((testDir), "ValidationSupportFiles.zip");

	if (!f.exists()) {
	    session.getLogger().warn("Warning: no available validation support files to install");
	}

	fs = ((ISession)session).getFilesystem();
	ZipFile zip = new ZipFile(f, ZipFile.OPEN_READ);

	IFile root = null;
	switch(session.getType()) {
	  case WINDOWS:
	    root = fs.getFile("C:\\ValidationSupportFiles\\", IFile.READWRITE);
	    break;

	  case UNIX:
	    root = fs.getFile("/tmp/ValidationSupportFiles", IFile.READWRITE);
	    break;

	  default:
	    throw new IOException("Unsupported type: " + session.getType());
	}

	Enumeration<? extends ZipEntry> entries = zip.entries();
	while (entries.hasMoreElements()) {
	    ZipEntry entry = entries.nextElement();
	    StringBuffer sb = new StringBuffer(root.getPath());
	    for (String s : StringTools.toList(StringTools.tokenize(entry.getName(), "/"))) {
		if (sb.length() > 0) {
		    sb.append(fs.getDelimiter());
		}
		sb.append(s);
	    }
	    String name = sb.toString();
	    mkdir(root, entry);
	    if (!entry.isDirectory()) {
		IFile newFile = fs.getFile(name, IFile.READWRITE);
		session.getLogger().info("Installing file " + newFile.getPath());
		byte[] buff = new byte[2048];
		InputStream in = zip.getInputStream(entry);
		OutputStream out = newFile.getOutputStream(false);
		try {
		    int len = 0;
		    while((len = in.read(buff)) > 0) {
			out.write(buff, 0, len);
		    }
		    in.close();
		} finally {
		    if (out != null) {
			try {
			    out.close();
			} catch (IOException e) {
			    e.printStackTrace();
			}
		    }
		}
	    }
	}
    }

    private void mkdir(IFile dir, ZipEntry entry) throws IOException {
	if (!dir.exists() && !dir.mkdir()) {
	    throw new IOException("Failed to create " + dir.getPath());
	}

	String subdir = null;
	if (entry.isDirectory()) {
	    subdir = entry.getName();
	} else {
	    String s = entry.getName();
	    subdir = s.substring(0, s.lastIndexOf("/"));
	}

	for (String subdirName : StringTools.toList(StringTools.tokenize(subdir, "/"))) {
	    dir = fs.getFile(dir.getPath() + fs.getDelimiter() + subdirName, IFile.READWRITE);
	    if (!dir.exists()) {
		dir.mkdir();
	    }
	}
    }

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
