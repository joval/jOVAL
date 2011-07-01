// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.windows.remote.wmi.process;

import java.util.Map;
import java.util.Iterator;

import org.jinterop.dcom.common.IJIAuthInfo;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIArray;
import org.jinterop.dcom.core.JIClsid;
import org.jinterop.dcom.core.JIComServer;
import org.jinterop.dcom.core.JILocalCoClass;
import org.jinterop.dcom.core.JILocalInterfaceDefinition;
import org.jinterop.dcom.core.JISession;
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
     * Set up an environment conforming to the specified Properties.
     */
    public void setEnvironment(Map<String, String> env) throws JIException {
	JIString[] array = new JIString[env.size() * 2];
	Iterator<String> keys = env.keySet().iterator();
	for(int i=0; keys.hasNext();) {
	    String key = keys.next();
	    array[i++] = new JIString(key);
	    array[i++] = new JIString(env.get(key));
	}
	dispatch.put("EnvironmentVariables", new JIVariant(new JIArray(array)));
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
return JIVariant.OPTIONAL_PARAM();
//	return new JIVariant(comObject);
    }
}
