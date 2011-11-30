// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.GregorianCalendar;
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

import oval.schemas.results.core.DefinitionType;
import oval.schemas.results.core.ResultsType;
import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.identity.SimpleCredentialStore;
import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.oval.IEngine;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.util.IObserver;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.util.IProducer;
import org.joval.oval.OvalException;
import org.joval.plugin.RemotePlugin;
import org.joval.util.JOVALSystem;
import org.joval.util.StringTools;

/**
 * A test class that runs jOVAL through all the relevant OVAL test content and generates a report.
 *
 * @author David A. Solin
 */
public class TestMain extends RemotePlugin {
    public static void main(String[] argv) {
	try {
	    LogManager.getLogManager().readConfiguration(new FileInputStream("logging.properties"));
	    Handler handler = new FileHandler("test.log", false);
	    handler.setFormatter(new LogfileFormatter());
	    handler.setLevel(Level.INFO);
	    Logger logger = Logger.getLogger(JOVALSystem.class.getName());
	    logger.addHandler(handler);

	    Properties props = new Properties();
	    props.load(new FileInputStream(new File(argv[0])));

	    IEngine engine = null;
	    setCredentialStore(new SimpleCredentialStore(props));
	    setDataDirectory(new File("."));
	    TestMain plugin = new TestMain(props.getProperty("hostname"));
	    File testDir = null;
	    try {
		plugin.connect();
		File content = new File("content");
		testDir = new File(content, plugin.session.getType().toString());
		if (plugin.session.getType() == IBaseSession.Type.UNIX) {
		    testDir = new File(testDir, ((IUnixSession)plugin.session).getFlavor().getOsName());
		}
		System.out.println("Base directory for tests: " + testDir.getCanonicalPath());
		plugin.installSupportFiles(testDir);
		plugin.disconnect();
		engine = JOVALSystem.createEngine(plugin);
	    } catch (IOException e) {
		System.out.println("Failed to install validation support files");
		e.printStackTrace();
		System.exit(1);
	    } catch (OvalException e) {
		System.out.println("Failed to create OVAL engine");
		e.printStackTrace();
		System.exit(1);
	    }

	    if (testDir.exists()) {
		engine.getNotificationProducer().addObserver(new Observer(), IEngine.MESSAGE_MIN, IEngine.MESSAGE_MAX);
		for (String xml : testDir.list(new XMLFilter())) {
		    System.out.println("Processing " + xml);
		    try {
			File definitions = new File(testDir, xml);
			engine.setDefinitionsFile(definitions);
			if ("oval-def_external_variable.xml".equals(xml)) {
			    engine.setExternalVariablesFile(new File(testDir, "_external-variables.xml"));
			}

			engine.run();
			switch(engine.getResult()) {
			  case OK:
			    ResultsType results = engine.getResults().getOvalResults().getResults();
			    for (DefinitionType definition : results.getSystem().get(0).getDefinitions().getDefinition()) {
				switch(definition.getResult()) {
				  case TRUE:
				    break;

				  case FALSE:
				    if (!"oval:org.mitre.oval.test:def:608".equals(definition.getDefinitionId())) {
					error(definitions, definition);
				    }
				    break;

				  case UNKNOWN:
				    if (!"oval:org.mitre.oval.test:def:423".equals(definition.getDefinitionId())) {
					error(definitions, definition);
				    }
				    break;

				  default:
				    error(definitions, definition);
				    break;
				}
			    }
			    break;
    
			  case ERR:
			    System.out.println("Problem running engine:");
			    engine.getError().printStackTrace();
			    break;
			}

		    } catch (OvalException e) {
			System.out.println("Problem loading " + xml);
			e.printStackTrace();
		    }
		}
		System.exit(0);
	    } else {
		System.out.println("No test content found: " + testDir.getPath());
		System.exit(1);
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	}

	System.exit(1);
    }

    // Private instance

    private IFilesystem fs;

    private TestMain(String hostname) {
	super(hostname);
    }

    private void installSupportFiles(File testDir) throws IOException {
	fs = session.getFilesystem();

	File f = new File((testDir), "ValidationSupportFiles.zip");
	ZipFile zip = new ZipFile(f, ZipFile.OPEN_READ);

	IFile root = null;
	switch(session.getType()) {
	  case WINDOWS:
	    root = fs.getFile("C:\\ValidationSupportFiles\\");
	    break;

	  case UNIX:
	    root = fs.getFile("/tmp/ValidationSupportFiles2");
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
	    if (!entry.isDirectory()) {
		mkdir(root, entry);
		IFile newFile = fs.getFile(name);
		System.out.println("Installing file " + newFile.getLocalName());
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
	    throw new IOException("Failed to create " + dir.getLocalName());
	}

	String subdir = null;
	if (entry.isDirectory()) {
	    subdir = entry.getName();
	} else {
	    String s = entry.getName();
	    subdir = s.substring(0, s.lastIndexOf("/"));
	}

	for (String subdirName : StringTools.toList(StringTools.tokenize(subdir, "/"))) {
	    dir = fs.getFile(dir.getPath() + fs.getDelimiter() + subdirName);
	    if (!dir.exists()) {
		dir.mkdir();
	    }
	}
    }


    // Private static

    /**
     * TBD
     */
    private static void error(File f, DefinitionType def) {
	System.out.println("Problem result (" + def.getResult() + ") for definition " + def.getDefinitionId());
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
		System.out.println("  Scanning objects...");
		System.out.print("    ");
		break;

	      case IEngine.MESSAGE_DEFINITION_PHASE_START:
		System.out.println("  Evaluating definitions...");
		System.out.print("    ");
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

    private static final String LF = System.getProperty("line.separator");

    private static class LogfileFormatter extends Formatter {
	public String format(LogRecord record) {
	    StringBuffer line = new StringBuffer(currentDateString());
	    line.append(" - ");
	    line.append(record.getMessage());
	    line.append(LF);
	    Throwable thrown = record.getThrown();
	    if (thrown != null) {
		line.append(thrown.toString());
		line.append(LF);
		StackTraceElement[] ste = thrown.getStackTrace();
		for (int i=0; i < ste.length; i++) {
		    line.append("    at ");
		    line.append(ste[i].getClassName());
		    line.append(".");
		    line.append(ste[i].getMethodName());
		    line.append(", ");
		    line.append(ste[i].getFileName());
		    line.append(" line: ");
		    line.append(Integer.toString(ste[i].getLineNumber()));
		    line.append(LF);
		}
	    }
	    return line.toString();
	}
    }

    private static String currentDateString() {
	StringBuffer sb = new StringBuffer();
	Calendar date = new GregorianCalendar();
	sb.append(date.get(Calendar.YEAR)).append(".");
	int month = 1 + date.get(Calendar.MONTH);
	sb.append(pad(month)).append(".");
	sb.append(pad(date.get(Calendar.DAY_OF_MONTH))).append(" ");
	sb.append(pad(date.get(Calendar.HOUR_OF_DAY))).append(":");
	sb.append(pad(date.get(Calendar.MINUTE))).append(":");
	sb.append(pad(date.get(Calendar.SECOND))).append(".");
	sb.append(pad(date.get(Calendar.MILLISECOND), 3));
	return sb.toString();
    }

    private static String pad(int val) {
	return pad(val, 2);
    }

    private static String pad(int val, int width) {
	StringBuffer sb = new StringBuffer();
	for (int i=(width-1); i > 0; i--) {
	    if (val < Math.pow(10, i)) {
		sb.append("0");
	    } else {
		break;
	    }
	}
	sb.append(Integer.toString(val));
	return sb.toString();
    }

    private static class XMLFilter implements FilenameFilter {
	private XMLFilter() {}

	public boolean accept(File dir, String name) {
	    return !name.startsWith("_") && name.toLowerCase().endsWith(".xml");
	}
    }
}
