// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.linux;

import java.util.ArrayList;
import java.util.Collection;

import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;

import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.linux.SelinuxbooleanObject;
import scap.oval.definitions.linux.SelinuxsecuritycontextObject;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.util.JOVALMsg;

/**
 * Manages retrieval of items for Selinuxboolean and Selinuxsecuritycontext objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SelinuxAdapter implements IAdapter {
    private IAdapter securitycontextAdapter, booleanAdapter;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IUnixSession && ((IUnixSession)session).getFlavor() == IUnixSession.Flavor.LINUX) {
	    try {
		String sestatus = SafeCLI.exec("/usr/sbin/getenforce", (IUnixSession)session, IUnixSession.Timeout.S);
		if ("Enforcing".equalsIgnoreCase(sestatus) || "Permissive".equalsIgnoreCase(sestatus)) {
		    booleanAdapter = new SelinuxbooleanAdapter();
		    classes.addAll(booleanAdapter.init(session, notapplicable));
		    securitycontextAdapter = new SelinuxsecuritycontextAdapter();
		    classes.addAll(securitycontextAdapter.init(session, notapplicable));
		} else {
		    notapplicable.add(SelinuxbooleanObject.class);
		    notapplicable.add(SelinuxsecuritycontextObject.class);
		}
	    } catch (Exception e) {
		// SELinux objects will not be checked
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	} else {
	    notapplicable.add(SelinuxbooleanObject.class);
	    notapplicable.add(SelinuxsecuritycontextObject.class);
	}
	return classes;
    }

    // Implement IProvider

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	if (obj instanceof SelinuxbooleanObject) {
	    return booleanAdapter.getItems(obj, rc);
	} else if (obj instanceof SelinuxsecuritycontextObject) {
	    return securitycontextAdapter.getItems(obj, rc);
	} else {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OBJECT, obj.getClass().getName(), obj.getId());
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}
    }
}
