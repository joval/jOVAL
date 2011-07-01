// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.windows.remote.wmi.process;

import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jinterop.dcom.common.IJIAuthInfo;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JICallBuilder;
import org.jinterop.dcom.core.JIClsid;
import org.jinterop.dcom.core.JIComServer;
import org.jinterop.dcom.core.JIFlags;
import org.jinterop.dcom.core.JISession;
import org.jinterop.dcom.core.JIString;
import org.jinterop.dcom.core.JIVariant;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.jinterop.dcom.impls.automation.IJIDispatch;
import org.jinterop.dcom.impls.automation.JIExcepInfo;

import com.h9labs.jwbem.SWbemServices;
import com.h9labs.jwbem.SWbemObjectSet;

import org.joval.util.JOVALSystem;

/**
 * Sparse implementation of a Win32_Process WMI class.
 * @see http://msdn.microsoft.com/en-us/library/aa394372%28v=vs.85%29.aspx
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Win32Process {
    public static final int SUCCESSFUL_COMPLETION	= 0;
    public static final int ACCESS_DENIED		= 2;
    public static final int INSUFFICIENT_PRIVILEGE	= 3;
    public static final int UNKNOWN_FAILURE		= 8;
    public static final int PATH_NOT_FOUND		= 9;
    public static final int INVALID_PARAMETER		= 21;

    private static final String NAME = "Win32_Process";

    private IJIDispatch dispatch;
    private int pid = 0;

    public Win32Process(SWbemServices services) throws JIException {
	IJIDispatch servicesDispatch = services.getObjectDispatcher();
	Object[] inParams = new Object[] {new JIString(NAME), JIVariant.OPTIONAL_PARAM(), JIVariant.OPTIONAL_PARAM()};
	JIVariant[] results = servicesDispatch.callMethodA("Get", inParams);
	JIVariant variant = results[0];
	dispatch = (IJIDispatch)JIObjectFactory.narrowObject(variant.getObjectAsComObject());
    }

    public int getProcessId() {
	return pid;
    }

    public int create(String command, String cwd, Win32ProcessStartup startupInfo) throws JIException {
	Object[] params = new Object[]	{
					    new JIString(command),
					    new JIString(cwd),
					    startupInfo.getVariant(), //DAS: works if set to JIVariant.OPTIONAL_PARAM()
					    new JIVariant(0, true)		// out param
					};
	JIVariant[] results = dispatch.callMethodA("Create", params);
	int status = results[0].getObjectAsInt();
	switch(status) {
	  case SUCCESSFUL_COMPLETION:
	    pid = results[1].getObjectAsInt();
	    break;
	}
	return status;
    }

    public int terminate(int exitCode) throws JIException {
	Object[] params = new Object[] {new JIVariant(exitCode)};
	return dispatch.callMethodA("Terminate", params)[0].getObjectAsInt();
    }

    public JIExcepInfo getError() {
	return dispatch.getLastExcepInfo();
    }
}
