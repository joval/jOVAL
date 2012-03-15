// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.wmi.win32;

import java.util.Collection;
import java.util.Vector;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIArray;
import org.jinterop.dcom.core.JIString;
import org.jinterop.dcom.core.JIVariant;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.jinterop.dcom.impls.automation.IJIDispatch;

import com.h9labs.jwbem.SWbemServices;

/**
 * Sparse implementation of a Win32_ProcessStartup WMI class.
 * @see http://msdn.microsoft.com/en-us/library/aa394375%28v=VS.85%29.aspx
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Win32ProcessStartup {
    public static final int SUCCESSFUL_COMPLETION	= 0;
    public static final int ACCESS_DENIED		= 2;
    public static final int INSUFFICIENT_PRIVILEGE	= 3;
    public static final int UNKNOWN_FAILURE		= 8;
    public static final int PATH_NOT_FOUND		= 9;
    public static final int INVALID_PARAMETER		= 21;

    private static final String NAME = "Win32_ProcessStartup";

    private IJIComObject comObject;
    private IJIDispatch dispatch;

    public Win32ProcessStartup(SWbemServices services) throws JIException {
	IJIDispatch servicesDispatch = services.getObjectDispatcher();
	Object[] params = new Object[]	{
					    new JIString(NAME),
					    JIVariant.OPTIONAL_PARAM(),
					    JIVariant.OPTIONAL_PARAM(),
					};
	JIVariant[] results = servicesDispatch.callMethodA("Get", params);
	IJIDispatch prototypeDispatch = (IJIDispatch)JIObjectFactory.narrowObject(results[0].getObjectAsComObject());
	JIVariant instance = prototypeDispatch.callMethodA("SpawnInstance_");
	comObject = instance.getObjectAsComObject();
	dispatch = (IJIDispatch)JIObjectFactory.narrowObject(comObject);
    }

    /**
     * Use a constant from ICreateFlags.
     */
    public void setCreateFlags(int flags) throws JIException {
	dispatch.put("CreateFlags", new JIVariant(flags));
    }

    /**
     * Set up an environment.
     */
    public void setEnvironmentVariables(String[] env) throws JIException {
	if (env != null) {
	    Collection<JIString> strings = new Vector<JIString>();
	    for (String s : env) {
		int ptr = s.indexOf("=");
		if (ptr > 0) {
		    strings.add(new JIString(s.substring(0,ptr)));
		    strings.add(new JIString(s.substring(ptr+1)));
		}
	    }
	    JIString[] array = strings.toArray(new JIString[strings.size()]);
	    dispatch.put("EnvironmentVariables", new JIVariant(new JIArray(array)));
	}
    }

    /**
     * Use a constant from IShowWindow.
     */
    public void setShowWindow(short sw) throws JIException {
 	dispatch.put("ShowWindow", new JIVariant(sw));
    }

    public void setX(int x) throws JIException {
	dispatch.put("X", new JIVariant(x));
    }

    public void setY(int y) throws JIException {
	dispatch.put("Y", new JIVariant(y));
    }

    public JIVariant getVariant() {
//	return new JIVariant(comObject);
return JIVariant.OPTIONAL_PARAM();
    }
}
