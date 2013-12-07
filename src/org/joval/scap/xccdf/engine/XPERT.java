// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.xccdf.engine;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xml.sax.SAXException;

import jsaf.util.StringTools;

import scap.xccdf.CheckContentRefType;
import scap.xccdf.CheckType;
import scap.xccdf.ComplexCheckType;
import scap.xccdf.MetadataType;
import scap.xccdf.TestResultType;
import scap.xccdf.RuleResultType;
import scap.xccdf.RuleType;

import org.joval.intf.plugin.IPlugin;
import org.joval.intf.scap.IScapContext;
import org.joval.intf.scap.arf.IReport;
import org.joval.intf.scap.datastream.IDatastream;
import org.joval.intf.scap.ocil.IChecklist;
import org.joval.intf.scap.ocil.IExport;
import org.joval.intf.scap.oval.IOvalEngine;
import org.joval.intf.scap.xccdf.SystemEnumeration;
import org.joval.intf.scap.xccdf.ITailoring;
import org.joval.intf.scap.xccdf.IXccdfEngine;
import org.joval.intf.util.IObserver;
import org.joval.intf.util.IProducer;
import org.joval.plugin.PluginFactory;
import org.joval.plugin.PluginConfigurationException;
import org.joval.scap.ScapException;
import org.joval.scap.ScapFactory;
import org.joval.scap.cpe.CpeException;
import org.joval.scap.diagnostics.RuleDiagnostics;
import org.joval.scap.datastream.DatastreamCollection;
import org.joval.scap.ocil.Checklist;
import org.joval.scap.ocil.OcilException;
import org.joval.scap.oval.OvalException;
import org.joval.scap.xccdf.Benchmark;
import org.joval.scap.xccdf.Bundle;
import org.joval.scap.xccdf.Tailoring;
import org.joval.scap.xccdf.XccdfException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.LogFormatter;
import org.joval.xml.XSLTools;
import org.joval.xml.SchemaValidator;
import org.joval.xml.SignatureValidator;

/**
 * XCCDF Processing Engine and Reporting Tool (XPERT) main class.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class XPERT {
    private static File CWD = new File(".");
    private static File BASE_DIR = CWD;
    private static PropertyResourceBundle resources;
    static {
	String s = System.getProperty("xpert.baseDir");
	if (s != null) {
	    BASE_DIR = new File(s);
	}
	try {
	    ClassLoader cl = Thread.currentThread().getContextClassLoader();
	    Locale locale = Locale.getDefault();
	    URL url = cl.getResource("xpert.resources_" + locale.toString() + ".properties");
	    if (url == null) {
		url = cl.getResource("xpert.resources_" + locale.getLanguage() + ".properties");
	    }
	    if (url == null) {
		url = cl.getResource("xpert.resources.properties");
	    }
	    resources = new PropertyResourceBundle(url.openStream());
	    JOVALSystem.setSystemProperty(JOVALSystem.SYSTEM_PROP_PRODUCT, getMessage("product.name"));
	} catch (IOException e) {
	    e.printStackTrace();
	    System.exit(-1);
	}
    }

    static Logger logger;

    static void printHeader(IPlugin plugin) {
	PrintStream console = System.out;
	console.println(getMessage("divider"));
	console.println(getMessage("product.name"));
	console.println(getMessage("description"));
	console.println(getMessage("message.version", JOVALSystem.getSystemProperty(JOVALSystem.SYSTEM_PROP_VERSION)));
	console.println(getMessage("message.buildDate", JOVALSystem.getSystemProperty(JOVALSystem.SYSTEM_PROP_BUILD_DATE)));
	console.println(getMessage("copyright"));
	if (plugin != null) {
	    console.println("");
	    console.println(getMessage("message.plugin.name", plugin.getProperty(IPlugin.PROP_DESCRIPTION)));
	    console.println(getMessage("message.plugin.version", plugin.getProperty(IPlugin.PROP_VERSION)));
	    console.println(getMessage("message.plugin.copyright", plugin.getProperty(IPlugin.PROP_COPYRIGHT)));
	}
	console.println(getMessage("divider"));
	console.println("");
    }

    static void printHelp(IPlugin plugin) {
	System.out.println(getMessage("helpText"));
	if (plugin != null) {
	    System.out.println(plugin.getProperty(IPlugin.PROP_HELPTEXT));
	}
    }

    /**
     * Retrieve a message using its key.
     */
    static String getMessage(String key, Object... arguments) {
	return MessageFormat.format(resources.getString(key), arguments);
    }

    /**
     * Run from the command-line.
     */
    public static void main(String[] argv) {
	boolean help = false;
	File source = new File(CWD, "scap-datastream.xml");
	String streamId = null;
	String benchmarkId = null;
	String profileId = null;
	Map<String, IChecklist> checklists = new HashMap<String, IChecklist>();
	File tailoringFile = null;
	File xmlDir = new File(BASE_DIR, "xml");
	File reportFile = new File(CWD, "xpert-arf.xml");
	File transformFile = new File(xmlDir, "xccdf_results_to_html.xsl");
	File reportHTML = new File("xpert-report.html");
	File ocilDir = new File("ocil-export");
	SystemEnumeration[] systems = {SystemEnumeration.ANY};

	Level level = Level.INFO;
	boolean query = false, verify = true, verbose = false;
	File logFile = new File(CWD, "xpert.log");
	String pluginName = "default";
	String configSource = null;
	File ksFile = new File(new File(BASE_DIR, "security"), "cacerts.jks");
	String ksPass = "jOVAL s3cure";

	for (int i=0; i < argv.length; i++) {
	    if (argv[i].equals("-h")) {
		help = true;
	    } else if (argv[i].equals("-q")) {
		query = true;
	    } else if (argv[i].equals("-s")) {
		verify = false;
	    } else if (argv[i].equals("-v")) {
		verbose = true;
	    } else if (argv[i].equals("-n")) {
		systems = new SystemEnumeration[] {SystemEnumeration.OVAL, SystemEnumeration.SCE};
	    } else if ((i + 1) < argv.length) {
		if (argv[i].equals("-d")) {
		    source = new File(argv[++i]);
		} else if (argv[i].equals("-i")) {
		    streamId = argv[++i];
		} else if (argv[i].equals("-b")) {
		    benchmarkId = argv[++i];
		} else if (argv[i].equals("-p")) {
		    profileId = argv[++i];
		} else if (argv[i].equals("-o")) {
		    String pair = argv[++i];
		    int ptr = pair.indexOf("=");
		    String key, val;
		    if (ptr == -1) {
			key = "";
			val = pair;
		    } else {
			key = pair.substring(0,ptr);
			val = pair.substring(ptr+1);
		    }
		    if (checklists.containsKey(key)) {
			System.out.println(getMessage("warning.ocil.href", key));
		    }
		    try {
			File f = new File(val);
			if (f.exists() && f.isFile()) {
			    checklists.put(key, new Checklist(new File(val)));
			} else {
			    System.out.println(getMessage("warning.ocil.source", val));
			}
		    } catch (OcilException e) {
			System.out.println(getMessage("error.ocil.source", val));
			e.printStackTrace();
			System.exit(1);
		    }
		} else if (argv[i].equals("-r")) {
		    reportFile = new File(argv[++i]);
		} else if (argv[i].equals("-l")) {
		    try {
			switch(Integer.parseInt(argv[++i])) {
			  case 4:
			    level = Level.SEVERE;
			    break;
			  case 3:
			    level = Level.WARNING;
			    break;
			  case 2:
			    level = Level.INFO;
			    break;
			  case 1:
			    level = Level.FINEST;
			    break;
			  default:
			    System.out.println(getMessage("warning.log.level.range", argv[i]));
			    break;
			}
		    } catch (NumberFormatException e) {
			System.out.println(getMessage("warning.log.level.value", argv[i]));
		    }
		} else if (argv[i].equals("-y")) {
		    logFile = new File(argv[++i]);
		} else if (argv[i].equals("-e")) {
		    ocilDir = new File(argv[++i]);
		} else if (argv[i].equals("-t")) {
		    transformFile = new File(argv[++i]);
		} else if (argv[i].equals("-a")) {
		    tailoringFile = new File(argv[++i]);
		} else if (argv[i].equals("-x")) {
		    reportHTML = new File(argv[++i]);
		} else if (argv[i].equals("-k")) {
		    ksFile = new File(argv[++i]);
		} else if (argv[i].equals("-w")) {
		    ksPass = argv[++i];
		} else if (argv[i].equals("-plugin")) {
		    pluginName = argv[++i];
		} else if (argv[i].equals("-config")) {
		    configSource = argv[++i];
		} else {
		    System.out.println(getMessage("warning.cli.arg", argv[i]));
		}
	    } else {
		System.out.println(getMessage("warning.cli.arg", argv[i]));
	    }
	}

	int exitCode = 1;
	IPlugin plugin = null;
	try {
	    plugin = PluginFactory.newInstance(new File(BASE_DIR, "plugin")).createPlugin(pluginName);
	    exitCode = 0;
	} catch (IllegalArgumentException e) {
	    System.out.println("Not a directory: " + e.getMessage());
	} catch (NoSuchElementException e) {
	    System.out.println("Plugin not found: " + e.getMessage());
	} catch (PluginConfigurationException e) {
	    System.out.println(LogFormatter.toString(e));
	}

	printHeader(plugin);
	if (help || plugin == null) {
	    printHelp(plugin);
	} else {
	    exitCode = 1;
	    try {
		logger = LogFormatter.createDuplex(logFile, level);
		logger.info(getMessage("message.start", new Date()));

		//
		// Configure the jOVAL plugin
		//
		if (!query) {
		    try {
			Properties config = new Properties();
			InputStream in = null;
			try {
			    if (configSource == null) {
				File f = new File(CWD, IPlugin.DEFAULT_FILE);
				if (f.exists()) {
				    in = new FileInputStream(f);
				}
			    } else {
				if ("-".equals(configSource)) {
				    System.out.println(getMessage("message.plugin.config"));
				    ByteArrayOutputStream buff = new ByteArrayOutputStream();
				    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
				    String line = null;
				    while((line = reader.readLine()) != null) {
					if ("#EOF".equals(line)) {
					    break;
					} else {
					    buff.write(line.getBytes(StringTools.UTF8));
					    buff.write(StringTools.LOCAL_CR.getBytes(StringTools.UTF8));
					}
				    }
				    buff.close();
				    in = new ByteArrayInputStream(buff.toByteArray());
				} else {
				    in = new FileInputStream(new File(configSource));
				}
			    }
			    if (in != null) {
				config.load(in);
			    }
			} finally {
			    if (in != null) {
				try {
				    in.close();
				} catch (IOException e) {
				}
			    }
			}
			plugin.configure(config);
		    } catch (Exception e) {
			throw new XPERTException(getMessage("error.plugin", e.getMessage(), logFile));
		    }
		}

		//
		// Read the input document
		//
		IDatastream ds = null;
		String sourceName = source.getName();
		if (source.isFile() && source.getName().toLowerCase().endsWith(".xml")) {
		    //
		    // Process a datastream
		    //
		    logger.info(getMessage("message.xmlvalidation", source.toString()));
		    validateDatastream(source);
		    DatastreamCollection dsc = null;
		    if (verify) {
			logger.info(getMessage("message.signature.validating", source.toString()));
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(new FileInputStream(ksFile), ksPass.toCharArray());
			SignatureValidator validator = new SignatureValidator(source, keyStore);
			if (!validator.containsSignature()) {
			    throw new XPERTException(getMessage("error.signature.failed"));
			} else if (validator.validate()) {
			    logger.info(getMessage("message.signature.valid"));
			    logger.info(getMessage("message.stream.loading"));
			    dsc = new DatastreamCollection(validator.getSource());
			} else {
			    throw new XPERTException(getMessage("error.signature.missing"));
			}
		    } else {
			logger.info(getMessage("message.stream.loading"));
			dsc = new DatastreamCollection(source);
		    }
		    if (query) {
			logger.info(getMessage("message.stream.query", source.toString()));
			for (String sId : dsc.getStreamIds()) {
			    logger.info("Stream ID=\"" + sId + "\"");
			    ds = dsc.getDatastream(sId);
			    for (String bId : ds.getBenchmarkIds()) {
				logger.info("  Benchmark ID=\"" + bId + "\"");
				for (String pId : ds.getProfileIds(bId)) {
				    logger.info("    Profile ID=\"" + pId + "\"");
				}
			    }
			}
		    } else {
			//
			// Determine the singleton Stream ID, if none was specified
			//
			if (streamId == null) {
			    if (dsc.getStreamIds().size() == 1) {
				streamId = dsc.getStreamIds().iterator().next();
				logger.info(getMessage("message.stream.autoselect", streamId));
			    } else {
				throw new XPERTException(getMessage("error.stream"));
			    }
			}
			sourceName = new StringBuffer(sourceName).append(", stream ").append(streamId).toString();
			try {
			    ds = dsc.getDatastream(streamId);
			} catch (NoSuchElementException e) {
			    throw new XPERTException(getMessage("error.stream.id", streamId));
			}
		    }
		} else if (source.isDirectory() || source.getName().toLowerCase().endsWith(".zip")) {
		    //
		    // Process a bundle
		    //
		    ds = new Bundle(source);
		    if (query) {
			logger.info(getMessage("message.bundle.query", source.toString()));
			for (String bId : ds.getBenchmarkIds()) {
			    logger.info("  Benchmark ID=\"" + bId + "\"");
			    for (String pId : ds.getProfileIds(bId)) {
				logger.info("    Profile ID=\"" + pId + "\"");
			    }
			}
		    }
		} else {
		    throw new XPERTException(getMessage("error.source", source.toString()));
		}

		//
		// Perform the scan
		//
		if (!query) {
		    //
		    // Create the SCAP context
		    //
		    if (benchmarkId == null) {
			//
			// Determine whether there's a singleton benchmark in the datastream
			//
			if (ds.getBenchmarkIds().size() == 1) {
			    benchmarkId = ds.getBenchmarkIds().iterator().next();
			    logger.info(getMessage("message.benchmark.autoselect", benchmarkId));
			} else {
			    throw new XPERTException(getMessage("error.benchmark", sourceName));
			}
		    }
		    IScapContext ctx;
		    if (tailoringFile == null) {
			if (profileId == null) {
			    Collection<String> profiles = ds.getProfileIds(benchmarkId);
			    if (profiles.size() > 1 && ds.getContext(benchmarkId, null).getSelectedRules().size() == 0) {
				//
				// If no rules are selected by default, then require that the user select a profile
				//
				StringBuffer sb = new StringBuffer();
				for (String id : profiles) {
				    sb.append(LogFormatter.LF).append("  ").append(id);
				}
				throw new XPERTException(getMessage("error.profile", sb.toString()));

			    //
			    // If there's a singleton profile in the benchmark and no default selections, use the singleton
			    //
			    } else if (profiles.size() == 1) {
				profileId = profiles.iterator().next();
				logger.info(getMessage("message.profile.autoselect", profileId));
			    }
			}
			ctx = ds.getContext(benchmarkId, profileId);
		    } else {
			ITailoring tailoring = ScapFactory.createTailoring(tailoringFile);
			if (profileId == null) {
			    Collection<String> profiles = tailoring.getProfileIds();
			    if (profiles.size() == 1) {
				profileId = profiles.iterator().next();
				logger.info(getMessage("message.profile.autoselect", profileId));
			    } else {
				StringBuffer sb = new StringBuffer();
				for (String id : profiles) {
				    sb.append(LogFormatter.LF).append("  ").append(id);
				}
				throw new XPERTException(getMessage("error.profile", sb.toString()));
			    }
			}
			ctx = ds.getContext(tailoring, profileId);
		    }

		    //
		    // Export OCIL and quit, if required
		    //
		    if (systems[0] == SystemEnumeration.ANY && checklists.size() == 0) {
			Collection<IExport> exports = ScapFactory.getOcilExports(ctx);
			if (exports.size() > 0) {
			    for (IExport export : exports) {
				String base = export.getHref();
				if (base.endsWith(".xml")) {
				    base = base.substring(0, base.length() - 4);
				}
				try {
				    if (!ocilDir.exists()) {
					ocilDir.mkdirs();
				    }
				    File checklist = new File(ocilDir, base + ".xml");
				    logger.warning("Exporting OCIL checklist to file: " + checklist.toString());
				    export.getChecklist().writeXML(checklist);
				    File variables = new File(ocilDir, base + "-variables.xml");
				    logger.warning("Exporting OCIL variables to file: " + variables.toString());
				    export.getVariables().writeXML(variables);
				} catch (IOException e) {
				    e.printStackTrace();
				}
			    }
			    throw new OcilException(JOVALMsg.getMessage(JOVALMsg.ERROR_OCIL_REQUIRED));
			}
		    }

		    try {
			Engine engine = new Engine(plugin, systems);
			engine.setContext(ctx);
			if (checklists.size() > 0) {
			    for (Map.Entry<String, IChecklist> entry : checklists.entrySet()) {
				engine.addChecklist(entry.getKey(), entry.getValue());
			    }
			}
			XccdfObserver observer = new XccdfObserver(ocilDir);
			engine.getNotificationProducer().addObserver(observer);
			engine.run();
			plugin.dispose();
			engine.getNotificationProducer().removeObserver(observer);
			switch(engine.getResult()) {
			  case OK:
			    IReport report = null;
			    if (verbose) {
				report = engine.getReport(SystemEnumeration.ANY);
				String assetId = report.getAssetIds().iterator().next();
				Map<String, RuleDiagnostics> diagnostics = new HashMap<String, RuleDiagnostics>();
				logger.info(getMessage("message.diagnostics.generate"));
				for (RuleDiagnostics rd : report.getDiagnostics(assetId, benchmarkId, profileId)) {
				    String ruleId = rd.getRuleId();
				    logger.info(getMessage("message.diagnostics.rule", ruleId));
				    rd.setRuleResult(null);
				    diagnostics.put(ruleId, rd);
				}
				TestResultType trt = ctx.getBenchmark().getRootObject().getTestResult().get(0);
				for (RuleResultType rrt : trt.getRuleResult()) {
				    MetadataType md = ScapFactory.XCCDF.createMetadataType();
				    md.getAny().add(diagnostics.get(rrt.getIdref()));
				    rrt.getMetadata().add(md);
				}
			    } else {
				report = engine.getReport(SystemEnumeration.XCCDF);
			    }
			    if (report == null) {
				logger.info(getMessage("message.report.none"));
			    } else if (report.getRootObject().isSetReports()) {
				logger.info(getMessage("message.report.save", reportFile));
				report.writeXML(reportFile);
				logger.info(getMessage("message.transform", reportHTML));
				try {
				    FileInputStream fin = new FileInputStream(transformFile);
				    ctx.getBenchmark().writeTransform(XSLTools.getTransformer(fin), reportHTML);
				} catch (Exception e) {
				    logger.severe(getMessage("error.transform", e.getMessage()));
				}
			    }
			    logger.info(getMessage("message.benchmark.processed"));
			    exitCode = 0;
			    break;

			  case ERR:
			    throw engine.getError();
			}
		    } catch (UnknownHostException e) {
			logger.severe(getMessage("error.host.unknown", e.getMessage()));
		    } catch (ConnectException e) {
			logger.severe(getMessage("error.host.connect", e.getMessage()));
		    }
		}
	    } catch (IOException e) {
		logger.severe(LogFormatter.toString(e));
	    } catch (OcilException e) {
		logger.severe(getMessage("error.ocil", e.getMessage()));
		logger.info(getMessage("message.ocil.export", ocilDir));
	    } catch (ScapException e) {
		logger.warning(LogFormatter.toString(e));
		exitCode = 2;
	    } catch (XPERTException e) {
		logger.severe(e.getMessage());
		exitCode = 1;
	    } catch (Exception e) {
		logger.severe(getMessage("error.fatal"));
		logger.severe(LogFormatter.toString(e));
		exitCode = 3;
	    }
	}
	System.exit(exitCode);
    }

    // Internal

    /**
     * Given a file path and password, load a JKS file.
     */
    static KeyStore loadKeyStore(String fname, String password) throws Exception {
	KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
	File cacerts = new File(fname);
	if (cacerts.exists()) {
	    ks.load(new FileInputStream(cacerts), password.toCharArray());
	    return ks;
	} else {
	    throw new FileNotFoundException(fname);
	}
    }

    static class XPERTException extends Exception {
	XPERTException(String message) {
	    super(message);
	}
    }

    static class XccdfObserver implements IObserver<IXccdfEngine.Message> {
	File ocilDir;

	XccdfObserver(File ocilDir) {
	    this.ocilDir = ocilDir;
	}

	public void notify(IProducer<IXccdfEngine.Message> sender, IXccdfEngine.Message msg, Object arg) {
	    switch(msg) {
	      case PLATFORM_PHASE_START:
		logger.info(JOVALMsg.getMessage(JOVALMsg.STATUS_XCCDF_APPLICABILITY));
		break;

	      case PLATFORM_CPE:
		logger.info(JOVALMsg.getMessage(JOVALMsg.STATUS_XCCDF_PLATFORM_CHECK, (String)arg));
		break;

	      case PLATFORM_PHASE_END:
		if (((Boolean)arg).booleanValue()) {
		    logger.info(JOVALMsg.getMessage(JOVALMsg.STATUS_XCCDF_APPLICABLE));
		} else {
		    logger.info(JOVALMsg.getMessage(JOVALMsg.STATUS_XCCDF_NOTAPPLICABLE));
		}
		break;

	      case RULES_PHASE_START:
		logger.info(JOVALMsg.getMessage(JOVALMsg.STATUS_XCCDF_RULES));
		break;

	      case RULES_PHASE_END:
		logger.info(JOVALMsg.getMessage(JOVALMsg.STATUS_XCCDF_RULES_DONE));
		break;

	      case OVAL_ENGINE:
		logger.info(JOVALMsg.getMessage(JOVALMsg.STATUS_XCCDF_OVAL));
		IOvalEngine oe = (IOvalEngine)arg;
		oe.getNotificationProducer().addObserver(new OvalObserver(oe.getNotificationProducer()));
		break;

	      case SCE_SCRIPT:
		logger.info(JOVALMsg.getMessage(JOVALMsg.STATUS_SCE_RUN, (String)arg));
		break;
	    }
	}
    }

    static class OvalObserver implements IObserver<IOvalEngine.Message> {
	private IProducer<IOvalEngine.Message> producer;

	OvalObserver(IProducer<IOvalEngine.Message> producer) {
	    this.producer = producer;
	}

	public void notify(IProducer<IOvalEngine.Message> sender, IOvalEngine.Message msg, Object arg) {
	    switch(msg) {
	      case OBJECT_PHASE_START:
		logger.info(JOVALMsg.getMessage(JOVALMsg.STATUS_OVAL_SCANNING));
		break;

	      case OBJECT:
		logger.info(JOVALMsg.getMessage(JOVALMsg.STATUS_OVAL_OBJECT, (String)arg));
		break;

	      case OBJECTS:
		for (String id : (String[])arg) {
		    logger.info(JOVALMsg.getMessage(JOVALMsg.STATUS_OVAL_OBJECT, id));
		}
		break;

	      case OBJECT_PHASE_END:
		logger.info(JOVALMsg.getMessage(JOVALMsg.STATUS_OVAL_SCAN_COMPLETE));
		break;

	      case DEFINITION_PHASE_START:
		logger.info(JOVALMsg.getMessage(JOVALMsg.STATUS_OVAL_EVAL));
		break;

	      case DEFINITION:
		logger.info(JOVALMsg.getMessage(JOVALMsg.STATUS_OVAL_DEFINITION, (String)arg));
		break;

	      case DEFINITION_PHASE_END:
		logger.info(JOVALMsg.getMessage(JOVALMsg.STATUS_OVAL_EVAL_COMPLETE));
		producer.removeObserver(this);
		break;

	      case SYSTEMCHARACTERISTICS:
		// no-op
		break;
	    }
	}
    }

    // Private

    private static Collection<CheckType> getChecks(ComplexCheckType complex) {
	Collection<CheckType> checks = new ArrayList<CheckType>();
	for (Object obj : complex.getCheckOrComplexCheck()) {
	    if (obj instanceof CheckType) {
		checks.add((CheckType)obj);
	    } else if (obj instanceof ComplexCheckType) {
		checks.addAll(getChecks((ComplexCheckType)obj));
	    }
	}
	return checks;
    }

    private static void validateDatastream(File f) throws SAXException, IOException, XPERTException {
	File xmlDir = new File(BASE_DIR, "xml");
	ArrayList<File> schemas = new ArrayList<File>();
	schemas.addAll(Arrays.asList(new File(xmlDir, "ds-" + IDatastream.SCHEMA_VERSION.toString()).listFiles()));
	schemas.addAll(Arrays.asList(new File(xmlDir, "xccdf-" + IXccdfEngine.SCHEMA_VERSION.toString()).listFiles()));
	schemas.addAll(Arrays.asList(new File(xmlDir, "ocil-" + IChecklist.SCHEMA_VERSION.toString()).listFiles()));
	schemas.addAll(Arrays.asList(new DefinitionsSchemaFilter(xmlDir).list()));
	SchemaValidator validator = new SchemaValidator(schemas.toArray(new File[schemas.size()]));
	try {
	    validator.validate(f);
	} catch (Exception e) {
	    throw new XPERTException(getMessage("error.validation", e.getMessage()));
	}
    }

    static final String SIGNATURE_SCHEMA = "xmldsig-core-schema.xsd";
    static final String DEFINITION_SCHEMA = "-definitions-schema.xsd";

    private static class DefinitionsSchemaFilter implements FilenameFilter {
	private File xmlDir;

	DefinitionsSchemaFilter(File xmlDir) {
	    this.xmlDir = xmlDir;
	}

	File[] list() {
	    File ovalDir = new File(xmlDir, "oval-" + IOvalEngine.SCHEMA_VERSION.toString());
	    return ovalDir.listFiles(this);
	}

	public boolean accept(File dir, String fname) {
	    return fname.endsWith(DEFINITION_SCHEMA);
	}
    }
}
