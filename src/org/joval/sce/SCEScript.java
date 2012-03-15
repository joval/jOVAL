// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.sce;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import xccdf.schemas.core.ResultEnumType;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.system.ISession;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.SafeCLI;

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

    private URL source;
    private ISession session;
    private Properties environment;
    private Date runtime;
    private List<String> stdout;
    private int exitCode = -1;
    private ResultEnumType result;

    /**
     * Create a new SCE script specifying the URL of its source.
     */
    public SCEScript(URL source, ISession session) {
	this.source = source;
	this.session = session;
	runtime = null;
	environment = new Properties();
	environment.setProperty("XCCDF_RESULT_PASS", Integer.toString(XCCDF_RESULT_PASS));
	environment.setProperty("XCCDF_RESULT_FAIL", Integer.toString(XCCDF_RESULT_FAIL));
	environment.setProperty("XCCDF_RESULT_ERROR", Integer.toString(XCCDF_RESULT_ERROR));
	environment.setProperty("XCCDF_RESULT_UNKNOWN", Integer.toString(XCCDF_RESULT_UNKNOWN));
	environment.setProperty("XCCDF_RESULT_NOT_APPLICABLE", Integer.toString(XCCDF_RESULT_NOT_APPLICABLE));
	environment.setProperty("XCCDF_RESULT_NOT_CHECKED", Integer.toString(XCCDF_RESULT_NOT_CHECKED));
	environment.setProperty("XCCDF_RESULT_NOT_SELECTED", Integer.toString(XCCDF_RESULT_NOT_SELECTED));
	environment.setProperty("XCCDF_RESULT_INFORMATIONAL", Integer.toString(XCCDF_RESULT_INFORMATIONAL));
	environment.setProperty("XCCDF_RESULT_FIXED", Integer.toString(XCCDF_RESULT_FIXED));
    }

    /**
     * Set a variable for SCE script execution. Use a null value to unset a variable.
     */
    public void setenv(String name, String value) {
	if (name.startsWith(ENV_VALUE_PREFIX)) {
	    name = ENV_VALUE_PREFIX + name;
	}
	if (value == null) {
	    environment.remove(name);
	} else {
	    environment.setProperty(name, value);
	}
    }

    /**
     * Execute the script.
     *
     * @returns true if data was successfully collected using the script -- which is different from passing the test!
     *
     * @throws IllegalStateException if the script was already executed.
     */
    public boolean exec() throws IllegalStateException {
	if (runtime != null) {
	    String msg = JOVALSystem.getMessage(JOVALMsg.ERROR_SCE_RAN, source.getFile(), session.getHostname(), runtime);
	    throw new IllegalStateException(msg);
	}

	InputStream in = null;
	OutputStream out = null;
	IFile script = null;
	try {
	    IFilesystem fs = session.getFilesystem();
	    HashSet<String> existing = new HashSet<String>(Arrays.asList(fs.getFile(session.getTempDir()).list()));
	    for (int i=0; script == null; i++) {
		String fname = "sce_script_" + i + ".sh";
		if (!existing.contains(fname)) {
		    script = fs.getFile(session.getTempDir() + fs.getDelimiter() + fname, IFile.READWRITE);
		}
	    }
	    byte[] buff = new byte[1024];
	    int len = 0;
	    in = source.openStream();
	    out = script.getOutputStream(false);
	    while((len = in.read(buff)) > 0) {
		out.write(buff, 0, len);
	    }
	    out.close();
	    out = null;
	    String[] env = new String[environment.size()];
	    int i=0;
	    for (String var : environment.stringPropertyNames()) {
		env[i++] = var + "=" + environment.getProperty(var);
	    }
	    runtime = new Date();
	    SafeCLI.ExecData data = SafeCLI.execData(script.getPath(), env, session, session.getTimeout(ISession.Timeout.M));
	    if (data != null) {
		exitCode = data.getExitCode();
		switch(exitCode) {
		  case XCCDF_RESULT_PASS:
		    result = ResultEnumType.PASS;
		    break;
		  case XCCDF_RESULT_FAIL:
		    result = ResultEnumType.FAIL;
		    break;
		  case XCCDF_RESULT_ERROR:
		    result = ResultEnumType.ERROR;
		    break;
		  case XCCDF_RESULT_NOT_APPLICABLE:
		    result = ResultEnumType.NOTAPPLICABLE;
		    break;
		  case XCCDF_RESULT_NOT_CHECKED:
		    result = ResultEnumType.NOTCHECKED;
		    break;
		  case XCCDF_RESULT_NOT_SELECTED:
		    result = ResultEnumType.NOTSELECTED;
		    break;
		  case XCCDF_RESULT_INFORMATIONAL:
		    result = ResultEnumType.INFORMATIONAL;
		    break;
		  case XCCDF_RESULT_FIXED:
		    result = ResultEnumType.FIXED;
		    break;
		  case XCCDF_RESULT_UNKNOWN:
		  default:
		    result = ResultEnumType.UNKNOWN;
		    break;
		}
		stdout = data.getStdout();
		return true;
	    }
	} catch (IOException e) {
	    session.getLogger().warn(JOVALMsg.ERROR_IO, script, e.getMessage());
	} catch (Exception e) {
	    session.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} finally {
	    if (in != null) {
		try {
		    in.close();
		} catch (IOException e) {
		}
	    }
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
	return false;
    }

    /**
     * Obtain a list of lines of output from the script execution.
     *
     * @throws IllegalStateException if the script has not been executed.
     */
    public List<String> getStdout() throws IllegalStateException {
	if (runtime == null) {
	    String msg = JOVALSystem.getMessage(JOVALMsg.ERROR_SCE_NOTRUN, source.getFile(), session.getHostname());
	    throw new IllegalStateException(msg);
	} else {
	    return stdout;
	}
    }

    public ResultEnumType getResult() throws IllegalStateException {
	if (runtime == null) {
	    String msg = JOVALSystem.getMessage(JOVALMsg.ERROR_SCE_NOTRUN, source.getFile(), session.getHostname());
	    throw new IllegalStateException(msg);
	} else {
	    return result;
	}
    }
}
