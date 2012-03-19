// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.cisco.ios;

import java.util.Collection;
import java.util.Vector;

import oval.schemas.definitions.ios.VersionObject;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;

import org.joval.intf.cisco.system.IIosSession;
import org.joval.intf.system.IBaseSession;
import org.joval.util.JOVALSystem;

/**
 * Provides Cisco IOS VersionItem OVAL items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class VersionAdapter extends Version55Adapter {
    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IIosSession) {
	    this.session = (IIosSession)session;
	    initialized = false;
	    classes.add(VersionObject.class);
	}
	return classes;
    }

    @Override
    protected void init() {
	super.init();

	StringBuffer sb = new StringBuffer();
	sb.append((String)item.getMajorVersion().getValue());
	sb.append(".");
	sb.append((String)item.getMinorVersion().getValue());
	EntityItemStringType trainNumber = JOVALSystem.factories.sc.core.createEntityItemStringType();
	trainNumber.setValue(sb.toString());
	item.setTrainNumber(trainNumber);

	sb.append("(");
	sb.append((String)item.getTrainIdentifier().getValue());
	sb.append(")");
	sb.append((String)item.getRebuild().getValue());
	EntityItemStringType majorRelease = JOVALSystem.factories.sc.core.createEntityItemStringType();
	majorRelease.setValue(sb.toString());
	item.setMajorRelease(majorRelease);
    }
}
