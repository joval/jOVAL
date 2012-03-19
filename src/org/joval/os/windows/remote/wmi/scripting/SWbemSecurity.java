// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.wmi.scripting;

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
 * @author David A. Solin
 * @version %I% %G%
 */
public class SWbemSecurity {
    public final static int ImpersonationLevel_ANONYMOUS	= 1;
    public final static int ImpersonationLevel_IDENTIFICATION	= 2;
    public final static int ImpersonationLevel_IMPERSONATION	= 3;
    public final static int ImpersonationLevel_DELEGATION	= 4;

    public final static int AuthenticationLevel_DEFAULT		= 0;
    public final static int AuthenticationLevel_NONE		= 1;
    public final static int AuthenticationLevel_CONNECT		= 2;
    public final static int AuthenticationLevel_CALL		= 3;
    public final static int AuthenticationLevel_PKT		= 4;
    public final static int AuthenticationLevel_PKTINTEGRITY	= 5;
    public final static int AuthenticationLevel_PKTPRIVACY	= 6;

    private static final String classid = "b54d66e9-2287-11d2-8b33-00600806d9b6";

    private IJIDispatch dispatch;

    public SWbemSecurity(SWbemServices services) throws JIException {
	IJIDispatch servicesDispatch = services.getObjectDispatcher();
	JIVariant securityVariant = servicesDispatch.get("Security_");
	dispatch = (IJIDispatch)JIObjectFactory.narrowObject(securityVariant.getObjectAsComObject());
    }

    public void setImpersonationLevel(int level) throws JIException {
	dispatch.put("ImpersonationLevel", new JIVariant(level));
    }

    public void setAuthenticationLevel(int level) throws JIException {
	dispatch.put("AuthenticationLevel", new JIVariant(level));
    }

//    SWbemPrivilegeSet getPrivileges() throws JIException;
}
