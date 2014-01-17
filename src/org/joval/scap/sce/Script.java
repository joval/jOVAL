// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.sce;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

import jsaf.intf.io.IFile;
import jsaf.intf.io.IFilesystem;
import jsaf.intf.system.IComputerSystem;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.util.SafeCLI;
import jsaf.util.StringTools;

import scap.xccdf.ResultEnumType;
import org.openscap.sce.results.EnvironmentType;
import org.openscap.sce.results.ObjectFactory;
import org.openscap.sce.results.SceResultsType;
import org.openscap.sce.xccdf.LangEnumeration;
import org.openscap.sce.xccdf.ScriptDataType;

import org.joval.intf.scap.sce.IScript;
import org.joval.intf.scap.sce.IScriptResult;
import org.joval.intf.xml.ITransformable;
import org.joval.scap.ScapException;
import org.joval.util.JOVALMsg;
import org.joval.xml.SchemaRegistry;

/**
 * Implementation of an SCE IScript.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Script implements IScript {
    static final ObjectFactory FACTORY = new ObjectFactory();

    private String href;
    private URL url = null;
    private byte[] data = null;
    private LangEnumeration lang;

    /**
     * Create a new SCE script from the contents of an SCAP datastream extended component.
     */
    public Script(String href, ScriptDataType source) {
	this.href = href;
	lang = source.getLang();
	data = source.getValue().getBytes(StringTools.ASCII);
    }

    /**
     * Create a new SCE script from a URL.
     */
    public Script(String href, URL url) {
	this.href = href;
	lang = getLang(href);
	this.url = url;
    }

    // Implement IScript

    public InputStream getContent() throws IOException {
	if (url == null) {
	    return new ByteArrayInputStream(data);
	} else {
	    return url.openStream();
	}
    }

    public String getHref() {
	return href;
    }

    public LangEnumeration getLanguage() {
	return lang;
    }

    public synchronized IScriptResult exec(Map<String, String> exports, IComputerSystem session) throws Exception {
	String commandPrefix = getCommandPrefix(session, lang);
	String extension = getExtension(lang);
	Properties environment = getEnvironment(exports);

	OutputStream out = null;
	IFile script = null;
	try {
	    SceResultsType result = FACTORY.createSceResultsType();

	    //
	    // Find an appropriate temp filename and copy the script to the target machine
	    //
	    IFilesystem fs = session.getFilesystem();
	    HashSet<String> existing = new HashSet<String>(Arrays.asList(fs.getFile(session.getTempDir()).list()));
	    for (int i=0; script == null; i++) {
		String fname = "sce_script_" + i + "." + extension;
		if (!existing.contains(fname)) {
		    script = fs.getFile(session.getTempDir() + fs.getDelimiter() + fname, IFile.Flags.READWRITE);
		    byte[] buff = new byte[1024];
		    int len = 0;
		    InputStream in = getContent();
		    out = script.getOutputStream(false);
		    while((len = in.read(buff)) > 0) {
			out.write(buff, 0, len);
		    }
		    in.close();
		    out.close();
		    out = null;
		    result.setScriptPath(href);
		}
	    }

	    //
	    // Prepare the environment
	    //
	    EnvironmentType environmentType = FACTORY.createEnvironmentType();
	    String[] env = new String[environment.size()];
	    int i=0;
	    for (String var : environment.stringPropertyNames()) {
		String variable = new StringBuffer(var).append("=").append(environment.getProperty(var)).toString();
		env[i++] = variable;
		environmentType.getEntry().add(variable);
	    }
	    result.setEnvironment(environmentType);

	    //
	    // Run the script and populate the result
	    //
	    long to = session.getTimeout(IComputerSystem.Timeout.M);
	    SafeCLI.ExecData data = SafeCLI.execData(commandPrefix + script.getPath(), env, session, to);
	    int exitCode = data.getExitCode();
	    switch(exitCode) {
	      case XCCDF_RESULT_PASS:
		result.setResult(ResultEnumType.PASS);
		break;
	      case XCCDF_RESULT_FAIL:
		result.setResult(ResultEnumType.FAIL);
		break;
	      case XCCDF_RESULT_ERROR:
		result.setResult(ResultEnumType.ERROR);
		break;
	      case XCCDF_RESULT_NOT_APPLICABLE:
		result.setResult(ResultEnumType.NOTAPPLICABLE);
		break;
	      case XCCDF_RESULT_NOT_CHECKED:
		result.setResult(ResultEnumType.NOTCHECKED);
		break;
	      case XCCDF_RESULT_NOT_SELECTED:
		result.setResult(ResultEnumType.NOTSELECTED);
		break;
	      case XCCDF_RESULT_INFORMATIONAL:
		result.setResult(ResultEnumType.INFORMATIONAL);
		break;
	      case XCCDF_RESULT_FIXED:
		result.setResult(ResultEnumType.FIXED);
		break;
	      case XCCDF_RESULT_UNKNOWN:
	      default:
		result.setResult(ResultEnumType.UNKNOWN);
		break;
	    }
	    result.setExitCode(exitCode);
	    result.setStdout(new String(data.getData(), StringTools.UTF8));
	    return new Result(result);
	} finally {
	    if (out != null) {
		try {
		    out.close();
		} catch (IOException e) {
		}
	    }
	    if (script != null) {
		try {
		    script.delete();
		} catch (IOException e) {
		}
	    }
	}
    }

    // Private

    /**
     * Get the execution environment for the given variable exports.
     */
    private Properties getEnvironment(Map<String, String> exports) {
	Properties env = new Properties();
	setenv(env, "XCCDF_RESULT_PASS", Integer.toString(XCCDF_RESULT_PASS));
	setenv(env, "XCCDF_RESULT_FAIL", Integer.toString(XCCDF_RESULT_FAIL));
	setenv(env, "XCCDF_RESULT_ERROR", Integer.toString(XCCDF_RESULT_ERROR));
	setenv(env, "XCCDF_RESULT_UNKNOWN", Integer.toString(XCCDF_RESULT_UNKNOWN));
	setenv(env, "XCCDF_RESULT_NOT_APPLICABLE", Integer.toString(XCCDF_RESULT_NOT_APPLICABLE));
	setenv(env, "XCCDF_RESULT_NOT_CHECKED", Integer.toString(XCCDF_RESULT_NOT_CHECKED));
	setenv(env, "XCCDF_RESULT_NOT_SELECTED", Integer.toString(XCCDF_RESULT_NOT_SELECTED));
	setenv(env, "XCCDF_RESULT_INFORMATIONAL", Integer.toString(XCCDF_RESULT_INFORMATIONAL));
	setenv(env, "XCCDF_RESULT_FIXED", Integer.toString(XCCDF_RESULT_FIXED));
	if (exports != null) {
	    for (Map.Entry<String, String> entry : exports.entrySet()) {
		setenv(env, new StringBuffer("XCCDF_VALUE_").append(entry.getKey()).toString(), entry.getValue());
	    }
	}
	return env;
    }

    private void setenv(Properties env, String name, String value) {
	if (value == null) {
	    env.remove(name);
	} else {
	    env.setProperty(name, value);
	}
    }

    /**
     * Guess the language of a script based on the extension of its href.
     */
    private LangEnumeration getLang(String href) {
	String extension = href.substring(href.lastIndexOf(".") + 1).toLowerCase();
	if ("applescript".equals(extension)) {
	    return LangEnumeration.APPLE_SCRIPT;
	} else if ("jy".equals(extension)) {
	    return LangEnumeration.JYTHON;
	} else if ("pl".equals(extension)) {
	    return LangEnumeration.PERL;
	} else if ("py".equals(extension)) {
	    return LangEnumeration.PYTHON;
	} else if ("rb".equals(extension)) {
	    return LangEnumeration.RUBY;
	} else if ("sh".equals(extension)) {
	    return LangEnumeration.SHELL;
	} else if ("tcl".equals(extension)) {
	    return LangEnumeration.TCL;
	} else if ("ps1".equals(extension)) {
	    return LangEnumeration.POWERSHELL;
	} else if ("vbs".equals(extension)) {
	    return LangEnumeration.VISUAL_BASIC;
	} else if ("bat".equals(extension)) {
	    return LangEnumeration.WINDOWS_BATCH;
	} else {
	    throw new IllegalArgumentException(extension);
	}
    }

    /**
     * Get the file extension corresponding to the language.
     */
    private String getExtension(LangEnumeration lang) {
	switch(lang) {
	  case APPLE_SCRIPT:
	    return "APPLESCRIPT";
	  case JYTHON:
	    return "jy";
	  case PERL:
	    return "pl";
	  case PYTHON:
	    return "py";
	  case RUBY:
	    return "rb";
	  case SHELL:
	    return "sh";
	  case TCL:
	    return "tcl";
	  case POWERSHELL:
	    return "ps1";
	  case VISUAL_BASIC:
	    return "vbs";
	  case WINDOWS_BATCH:
	    return "bat";
	  default:
	    throw new IllegalArgumentException(lang.toString());
	}
    }

    /**
     * Get the command prefix (i.e., the script engine) corresponding to the session and script type.
     */
    private String getCommandPrefix(IComputerSystem session, LangEnumeration lang) {
	switch(session.getType()) {
	  case UNIX:
	    IUnixSession us = (IUnixSession)session;
	    switch(lang) {
	      case APPLE_SCRIPT:
		if (us.getFlavor() == IUnixSession.Flavor.MACOSX) {
		    return "/usr/bin/osascript ";
		} else {
		    String s = JOVALMsg.getMessage(JOVALMsg.ERROR_SCE_PLATFORMLANG, us.getFlavor(), lang);
		    throw new IllegalArgumentException(s);
		}
	      case JYTHON:
		return "/usr/bin/env jython ";
	      case PERL:
		return "/usr/bin/env perl ";
	      case PYTHON:
		return "/usr/bin/env python ";
	      case RUBY:
		return "/usr/bin/env ruby ";
	      case SHELL:
		return "/bin/sh ";
	      case TCL:
		return "/usr/bin/env tclsh ";
	      default:
		String s = JOVALMsg.getMessage(JOVALMsg.ERROR_SCE_PLATFORMLANG, session.getType(), lang);
		throw new IllegalArgumentException(s);
	    }

	  case WINDOWS:
	    IWindowsSession ws = (IWindowsSession)session;
	    switch(lang) {
	      case PERL:
		return "perl.exe ";
	      case POWERSHELL:
		return "powershell.exe -File ";
	      case PYTHON:
		return "python.exe ";
	      case VISUAL_BASIC:
		return "cscript.exe ";
	      case WINDOWS_BATCH:
		return "";
	      default:
		String s = JOVALMsg.getMessage(JOVALMsg.ERROR_SCE_PLATFORMLANG, session.getType(), lang);
		throw new IllegalArgumentException(s);
	    }

	  default:
	    String s = JOVALMsg.getMessage(JOVALMsg.ERROR_SCE_PLATFORM, session.getType());
	    throw new IllegalArgumentException(s);
	}
    }
}
