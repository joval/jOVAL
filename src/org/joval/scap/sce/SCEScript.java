// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.sce;

import java.io.ByteArrayInputStream;
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
import jsaf.intf.system.ISession;
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

import org.joval.util.JOVALMsg;

/**
 * A representation of a script.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SCEScript {
    public static final int XCCDF_RESULT_PASS		= 101;
    public static final int XCCDF_RESULT_FAIL		= 102;
    public static final int XCCDF_RESULT_ERROR		= 103;
    public static final int XCCDF_RESULT_UNKNOWN	= 104;
    public static final int XCCDF_RESULT_NOT_APPLICABLE	= 105;
    public static final int XCCDF_RESULT_NOT_CHECKED	= 106;
    public static final int XCCDF_RESULT_NOT_SELECTED	= 107;
    public static final int XCCDF_RESULT_INFORMATIONAL	= 108;
    public static final int XCCDF_RESULT_FIXED		= 109;

    private static final String ENV_VALUE_PREFIX	= "XCCDF_VALUE_";
    private static final String ENV_TYPE_PREFIX		= "XCCDF_TYPE_";
    private static final String ENV_OPERATOR_PREFIX	= "XCCDF_OPERATOR_";

    private static final ObjectFactory FACTORY = new ObjectFactory();

    private ScriptDataType source;
    private ISession session;
    private String commandPrefix;
    private String extension;
    private Properties environment;

    /**
     * Create a new SCE script specifying the URL of its source.
     */
    public SCEScript(Map<String, String> exports, ScriptDataType source, ISession session) throws IllegalArgumentException {
	this.source = source;
	this.session = session;

	//
	// Determine the appropriate command prefix to run the script, based on the ISession type and script
	// filename extension.
	//
	commandPrefix = "";
	extension = ".dat";
	switch(session.getType()) {
	  case UNIX:
	    IUnixSession us = (IUnixSession)session;
	    switch(source.getLang()) {
	      case APPLE_SCRIPT:
		if (us.getFlavor() == IUnixSession.Flavor.MACOSX) {
		    extension = "APPLESCRIPT";
		    commandPrefix = "/usr/bin/osascript ";
		} else {
		    String s = JOVALMsg.getMessage(JOVALMsg.ERROR_SCE_PLATFORMLANG, us.getFlavor(), source.getLang());
		    throw new IllegalArgumentException(s);
		}
		break;
	      case JYTHON:
		extension = "jy";
		commandPrefix = "/usr/bin/env jython ";
		break;
	      case PERL:
		extension = "pl";
		commandPrefix = "/usr/bin/env perl ";
		break;
	      case PYTHON:
		extension = "py";
		commandPrefix = "/usr/bin/env python ";
		break;
	      case RUBY:
		extension = "rb";
		commandPrefix = "/usr/bin/env ruby ";
		break;
	      case SHELL:
		extension = "sh";
		commandPrefix = "/bin/sh ";
		break;
	      case TCL:
		extension = "tcl";
		commandPrefix = "/usr/bin/env tclsh ";
		break;
	      default:
		String s = JOVALMsg.getMessage(JOVALMsg.ERROR_SCE_PLATFORMLANG, session.getType(), source.getLang());
		throw new IllegalArgumentException(s);
	    }
	    break;

	  case WINDOWS:
	    IWindowsSession ws = (IWindowsSession)session;
	    switch(source.getLang()) {
	      case PERL:
		extension = "pl";
		commandPrefix = "perl.exe ";
		break;
	      case POWERSHELL:
		extension = "ps1";
		commandPrefix = "powershell.exe ";
		break;
	      case PYTHON:
		extension = "py";
		commandPrefix = "python.exe ";
		break;
	      case VISUAL_BASIC:
		extension = "vbs";
		commandPrefix = "cscript.exe ";
		break;
	      case WINDOWS_BATCH:
		extension = "bat";
		break;
	      default:
		String s = JOVALMsg.getMessage(JOVALMsg.ERROR_SCE_PLATFORMLANG, session.getType(), source.getLang());
		throw new IllegalArgumentException(s);
	    }
	    break;

	  default:
	    String s = JOVALMsg.getMessage(JOVALMsg.ERROR_SCE_PLATFORM, session.getType());
	    throw new IllegalArgumentException(s);
	}

	environment = new Properties();
	setenv( "XCCDF_RESULT_PASS",		Integer.toString(XCCDF_RESULT_PASS));
	setenv( "XCCDF_RESULT_FAIL",		Integer.toString(XCCDF_RESULT_FAIL));
	setenv( "XCCDF_RESULT_ERROR",		Integer.toString(XCCDF_RESULT_ERROR));
	setenv( "XCCDF_RESULT_UNKNOWN",		Integer.toString(XCCDF_RESULT_UNKNOWN));
	setenv( "XCCDF_RESULT_NOT_APPLICABLE",	Integer.toString(XCCDF_RESULT_NOT_APPLICABLE));
	setenv( "XCCDF_RESULT_NOT_CHECKED",	Integer.toString(XCCDF_RESULT_NOT_CHECKED));
	setenv( "XCCDF_RESULT_NOT_SELECTED",	Integer.toString(XCCDF_RESULT_NOT_SELECTED));
	setenv( "XCCDF_RESULT_INFORMATIONAL",	Integer.toString(XCCDF_RESULT_INFORMATIONAL));
	setenv( "XCCDF_RESULT_FIXED",		Integer.toString(XCCDF_RESULT_FIXED));

	if (exports != null) {
	    for (Map.Entry<String, String> entry : exports.entrySet()) {
		setenv(new StringBuffer("XCCDF_VALUE_").append(entry.getKey()).toString(), entry.getValue());
	    }
	}
    }

    /**
     * Execute the script.
     */
    public SceResultsType exec() throws Exception {
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
		    InputStream in = new ByteArrayInputStream(source.getValue().getBytes());
		    out = script.getOutputStream(false);
		    while((len = in.read(buff)) > 0) {
			out.write(buff, 0, len);
		    }
		    in.close();
		    out.close();
		    out = null;
		    result.setScriptPath(script.getPath());
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
	    long to = session.getTimeout(ISession.Timeout.M);
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
	    return result;
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

    private void setenv(String name, String value) {
	if (value == null) {
	    environment.remove(name);
	} else {
	    environment.setProperty(name, value);
	}
    }
}
