// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.cisco.ios;

import oval.schemas.definitions.ios.VersionObject;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;

import org.joval.intf.cisco.system.IIosSession;
import org.joval.util.JOVALSystem;

/**
 * Provides Cisco IOS VersionItem OVAL items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class VersionAdapter extends Version55Adapter {
    public VersionAdapter(IIosSession session) {
	super(session);
    }
    
    // Implement IAdapter

    private static Class[] objectClasses = {VersionObject.class};

    public Class[] getObjectClasses() {
	return objectClasses;
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
