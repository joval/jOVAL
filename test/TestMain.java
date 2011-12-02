// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
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
import org.joval.intf.oval.IResults;
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

    private static File contentDir = new File("content");
    private static File reportDir = new File("report");

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

    public static void main(String[] argv) {
	try {
	    LogManager.getLogManager().readConfiguration(new FileInputStream("logging.properties"));
	    Handler handler = new FileHandler("test.log", false);
	    handler.setFormatter(new LogfileFormatter());
	    handler.setLevel(Level.FINEST);
	    Logger logger = Logger.getLogger(JOVALSystem.class.getName());
	    logger.addHandler(handler);

	    IniFile config = new IniFile(new File("test.ini"));
	    SimpleCredentialStore scs = new SimpleCredentialStore();
	    setCredentialStore(scs);
	    setDataDirectory(new File("."));

	    if (!reportDir.exists()) {
		reportDir.mkdir();
	    }
	    Hashtable<String, ReportEntry> reports = new Hashtable<String, ReportEntry>();
	    for (String name : config.listSections()) {
		System.out.println("Starting test suite run for " + name);
		Properties props = config.getSection(name);
		scs.add(props);
		ReportEntry report = new ReportEntry(props);
		runTests(new TestMain(props.getProperty(SimpleCredentialStore.PROP_HOSTNAME)), report);
		reports.put(name, report);
		System.out.println("Tests completed for " + name);
	    }
	    writeReport(reports);
	    System.exit(0);
	} catch (IOException e) {
	    e.printStackTrace();
	}

	System.exit(1);
    }

    private static void runTests(TestMain plugin, ReportEntry report) {
	try {
	    plugin.connect();
	    File testDir = new File(contentDir, plugin.session.getType().toString());
	    if (plugin.session.getType() == IBaseSession.Type.UNIX) {
		testDir = new File(testDir, ((IUnixSession)plugin.session).getFlavor().getOsName());
	    }
	    if (testDir.exists()) {
		System.out.println("Base directory for tests: " + testDir.getCanonicalPath());
		plugin.installSupportFiles(testDir);
		plugin.disconnect();
		IEngine engine = JOVALSystem.createEngine(plugin);
		engine.getNotificationProducer().addObserver(new Observer(), IEngine.MESSAGE_MIN, IEngine.MESSAGE_MAX);

		for (String xml : testDir.list(new XMLFilter())) {
		    System.out.println("Processing " + xml);
		    try {
			File definitions = new File(testDir, xml);
			engine.setDefinitionsFile(definitions);

			//
			// Set the external variables file for the external variables test!
			//
			if ("oval-def_external_variable.xml".equals(xml)) {
			    engine.setExternalVariablesFile(new File(testDir, "_external-variables.xml"));
			}

			long elapsed = System.currentTimeMillis();
			engine.run();
			elapsed = System.currentTimeMillis() - elapsed;
			switch(engine.getResult()) {
			  case OK:
			    report.add(xml, engine.getResults(), elapsed);
			    break;

			  case ERR:
			    report.add(xml, engine.getError());
			    break;
			}
		    } catch (OvalException e) {
			System.out.println("Problem loading " + xml);
			e.printStackTrace();
		    }
		}
	    } else {
		System.out.println("No test content found for " + plugin.hostname + " at " + testDir.getPath());
	    }
	} catch (IOException e) {
	    System.out.println("Failed to install validation support files for " + plugin.hostname);
	    e.printStackTrace();
	} catch (OvalException e) {
	    System.out.println("Failed to create OVAL engine for " + plugin.hostname);
	    e.printStackTrace();
	}
    }

    private static void writeReport(Hashtable<String, ReportEntry> reports) {
	File reportFile = new File(reportDir, "report.html");

	try {
	    PrintStream out = new PrintStream(reportFile);
	    out.println("<html>");
	    out.println("  <head>");
	    out.println("    <title>jOVAL automated test report</title>");
	    out.println("  </head>");
	    out.println("  <body>");
	    out.println("    <h2>jOVAL automated test report</h2>");
	    out.println("    <p>Generated: " + new Date().toString() + "</p>");
	    out.println("    <table border=0 cellspacing=10>");

	    for (String name : reports.keySet()) {
		out.println("      <tr>");
		out.println("        <td style=\"border-style: solid; border-width: 1px\">");
		out.println("          <table width=800 cellpadding=5 cellspacing=0 border=0>");
		out.println("            <tr><td height=50 colspan=3 bgcolor=#dddddd><b>Test Suite: "+name+"</b></td></tr>");
		for (String row : reports.get(name).toRows()) {
		    out.println("            " + row);
		}
		out.println("          </table>");
		out.println("        </td>");
		out.println("      </tr>");
	    }

	    out.println("    </table>");
	    out.println("  </body>");
	    out.println("</html>");
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    // Private instance

    private IFilesystem fs;

    private TestMain(String hostname) {
	super(hostname);
    }

    private void installSupportFiles(File testDir) throws IOException {
	File f = new File((testDir), "ValidationSupportFiles.zip");

	if (!f.exists()) {
	    System.out.println("Warning: no available validation support files to install");
	} else {
	    fs = session.getFilesystem();
	    ZipFile zip = new ZipFile(f, ZipFile.OPEN_READ);
    
	    IFile root = null;
	    switch(session.getType()) {
	      case WINDOWS:
		root = fs.getFile("C:\\ValidationSupportFiles\\");
		break;
    
	      case UNIX:
		root = fs.getFile("/tmp/ValidationSupportFiles");
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

    private static final String LF = System.getProperty("line.separator");

    private static class LogfileFormatter extends Formatter {
	public String format(LogRecord record) {
	    StringBuffer line = new StringBuffer(currentDateString());
	    line.append(" - ");
	    line.append(record.getLevel().getName());
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

    private static String toString(Throwable t) {
	StringBuffer sb = new StringBuffer(t.getClass().getName());
	sb.append(":").append(t.getMessage()).append("\n");
	StackTraceElement[] ste = t.getStackTrace();
	for (int i=0; i < ste.length; i++) {
	    sb.append("    ").append(ste[i].toString()).append("\n");
	}
	Throwable cause = t.getCause();
	if (cause != null) {
	    sb.append("caused by:\n");
	    sb.append(toString(cause));
	}
	return sb.toString();
    }

    // Private data structures

    private static class XMLFilter implements FilenameFilter {
	XMLFilter() {}

	// Implement FilenameFilter

	public boolean accept(File dir, String name) {
	    return !name.startsWith("_") && name.toLowerCase().endsWith(".xml");
	}
    }

    /**
     * A class for interpreting an ini-style config file.  Each header is treated as a section full of properties.
     */
    private static class IniFile {
	Hashtable<String, Properties> sections;

	IniFile(File f) throws IOException {
	    sections = new Hashtable<String, Properties>();
	    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
	    String line = null;
	    Properties section = null;
	    String name = null;
	    int ptr;
	    while ((line = br.readLine()) != null) {
		if (line.startsWith("[") && line.trim().endsWith("]")) {
		    if (section != null) {
			sections.put(name, section);
		    }
		    section = new Properties();
		    name = line.substring(1, line.length() - 1);
		} else if (line.startsWith("#")) {
		    // skip comment
		} else if ((ptr = line.indexOf("=")) > 0) {
		    if (section != null) {
			String key = line.substring(0,ptr);
			String val = line.substring(ptr+1);
			section.setProperty(key, val);
		    }
		}
	    }
	    if (section != null) {
		sections.put(name, section);
	    }
	}

	Collection<String> listSections() {
	    return sections.keySet();
	}

	Properties getSection(String name) {
	    return sections.get(name);
	}

	String getProperty(String section, String key) {
	    Properties p = getSection(section);
	    String val = null;
	    if (p != null) {
		val = p.getProperty(key);
	    }
	    return val;
	}
    }

    private static class ReportEntry {
	private Properties props;
	private Hashtable<String, TestData> results;
	private Hashtable<String, Exception> errors;

	ReportEntry(Properties props) {
	    this.props = props;
	    results = new Hashtable<String, TestData>();
	    errors = new Hashtable<String, Exception>();
	}

	void add(String xml, IResults res, long elapsed) {
	    TestData data = new TestData(elapsed);
	    results.put(xml, data);

	    boolean saveResults = false;
	    for (DefinitionType def : res.getOvalResults().getResults().getSystem().get(0).getDefinitions().getDefinition()) {
		String id = def.getDefinitionId();
		switch(def.getResult()) {
		  case TRUE:
		    data.tally.put(id, Boolean.TRUE);
		    break;

		  case FALSE:
		    if (knownFalses.contains(id)) {
			data.tally.put(id, Boolean.TRUE);
		    } else {
			data.tally.put(id, Boolean.FALSE);
		    }
		    break;

		  case UNKNOWN:
		    if (knownUnknowns.contains(id)) {
			data.tally.put(id, Boolean.TRUE);
		    } else {
			data.tally.put(id, Boolean.FALSE);
		    }
		    break;

		  default:
		    data.tally.put(id, Boolean.FALSE);
		    break;
		}
	    }

	    //
	    // If there was any kind of imperfect result, the results XML is saved so it can be analyzed.
	    //
	    if (data.tally.containsValue(Boolean.FALSE)) {
		File f = new File(reportDir, "results_" + xml);
		res.writeXML(f);
	    }
	}

	void add(String xml, Exception error) {
	    errors.put(xml, error);
	}

	/**
	 * Convert the ReportEntry to a collection of HTML table rows.
	 */
	List<String> toRows() {
	    List<String> rows = new Vector<String>();
	    if (results.size() > 0) {
		for (String xml : results.keySet()) {
		    TestData data = results.get(xml);
		    rows.add("<tr bgcolor=#eeeeee><td height=25><b>" + xml + "</b></td><td colspan=2>Run time: " +
			 data.elapsed + "ms</td></tr>");
		    for (String id : data.tally.keySet()) {
			if (data.tally.get(id).booleanValue()) {
			    StringBuffer sb = new StringBuffer();
			    sb.append("<tr><td width=600>").append(id).append("</td>");
			    sb.append("<td colspan=2><font color=#00ee00>PASSED</font></td></tr>");
			    rows.add(sb.toString());
			} else {
			    StringBuffer sb = new StringBuffer();
			    sb.append("<tr><td width=600>").append(id).append("</td>");
			    sb.append("<td><font color=#ff0000 weight=bold>FAILED</font></td>");
			    sb.append("<td><a href=\"results_").append(xml).append("\">results.xml</a></td></tr>");
			    rows.add(sb.toString());
			}
		    }
		}
	    }
	    if (errors.size() > 0) {
		for (String xml : errors.keySet()) {
		    rows.add("<tr><td height=25 bgcolor=#eeeeee colspan=3><b>" + xml + "</b></td></tr>");
		    Exception e = errors.get(xml);
		    StringBuffer sb = new StringBuffer("<tr><td colspan=3><p>Encountered exception loading XML:</p>");
		    sb.append("<pre>").append(TestMain.toString(e)).append("</pre></td></tr>");
		    rows.add(sb.toString());
		}
	    }
	    return rows;
	}

	private class TestData {
	    long elapsed;
	    Hashtable<String, Boolean> tally;

	    TestData(long elapsed) {
		this.elapsed = elapsed;
		tally = new Hashtable<String, Boolean>();
	    }
	}
    }
}
