// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.cisco.ios;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Collection;
import java.util.Vector;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.ios.Version55Object;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.ios.VersionItem;
import oval.schemas.systemcharacteristics.core.EntityItemIOSVersionType;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.ISession;
import org.joval.oval.OvalException;
import org.joval.oval.TestException;
import org.joval.util.JOVALSystem;

/**
 * Provides Cisco IOS Version55Item OVAL items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Version55Adapter implements IAdapter {
    static final String OPEN_PEREN = "(";
    static final String CLOSE_PEREN = ")";

    VersionItem item;
    ISession session;

    public Version55Adapter(ISession session) {
	this.session = session;
    }
    
    // Implement IAdapter

    private static Class[] objectClasses = {Version55Object.class};

    public Class[] getObjectClasses() {
	return objectClasses;
    }

    public boolean connect() {
	item = JOVALSystem.factories.sc.ios.createVersionItem();
	int begin = 0;
	String version = session.getSystemInfo().getOsVersion();
	int end = version.indexOf(".");
	if (end != -1) {
	    EntityItemIntType majorVersion = JOVALSystem.factories.sc.core.createEntityItemIntType();
	    majorVersion.setValue(version.substring(begin, end));
	    majorVersion.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    item.setMajorVersion(majorVersion);
	}

	begin = end + 1;
	end = version.indexOf(OPEN_PEREN);
	if (begin > 0 && end > begin) {
	    EntityItemIntType minorVersion = JOVALSystem.factories.sc.core.createEntityItemIntType();
	    minorVersion.setValue(version.substring(begin, end));
	    minorVersion.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    item.setMinorVersion(minorVersion);
	}

	begin = end + 1;
	end = version.indexOf(CLOSE_PEREN);
	if (begin > 0 && end > begin) {
	    StringBuffer sb = new StringBuffer();
	    for (int i=begin; i < end; i++) {
		char ch = version.charAt(i);
		if (isNum(ch)) {
		    sb.append(ch);
		} else {
		    break;
		}
	    }
	    if (sb.length() > 0) {
		EntityItemIntType release = JOVALSystem.factories.sc.core.createEntityItemIntType();
		release.setValue(sb.toString());
		release.setDatatype(SimpleDatatypeEnumeration.INT.value());
		item.setRelease(release);

		begin += sb.length();
		if (begin < end) {
		    EntityItemStringType mainlineRebuild = JOVALSystem.factories.sc.core.createEntityItemStringType();
		    mainlineRebuild.setValue(version.substring(begin, end));
		    item.setMainlineRebuild(mainlineRebuild);
		}
	    }
	}

	begin = end + 1;
	end = version.length();
	if (begin > 0) {
	    StringBuffer sb = new StringBuffer();
	    for (int i=begin; i < end; i++) {
		char ch = version.charAt(i);
		if (isNum(ch)) {
		    break;
		} else {
		    sb.append(ch);
		}
	    }
	    if (sb.length() > 0) {
		EntityItemStringType trainIdentifier = JOVALSystem.factories.sc.core.createEntityItemStringType();
		trainIdentifier.setValue(sb.toString());
		item.setTrainIdentifier(trainIdentifier);

		begin += sb.length();
		if (begin < end) {
		    sb = new StringBuffer();
		    for (int i=begin; i < end; i++) {
			char ch = version.charAt(i);
			if (isNum(ch)) {
			    sb.append(ch);
			} else {
			    break;
			}
		    }
		    if (sb.length() > 0) {
			EntityItemIntType rebuild = JOVALSystem.factories.sc.core.createEntityItemIntType();
			rebuild.setValue(sb.toString());
			rebuild.setDatatype(SimpleDatatypeEnumeration.INT.value());
			item.setRebuild(rebuild);
		    }

		    begin += sb.length();
		    if (begin < end) {
			EntityItemStringType subrebuild = JOVALSystem.factories.sc.core.createEntityItemStringType();
			subrebuild.setValue(version.substring(begin, end));
			item.setSubrebuild(subrebuild);
		    }
		}
	    }
	}

	EntityItemIOSVersionType versionString = JOVALSystem.factories.sc.core.createEntityItemIOSVersionType();
	versionString.setValue(version);
	versionString.setDatatype(SimpleDatatypeEnumeration.IOS_VERSION.value());
	item.setVersionString(versionString);

	return true;
    }

    public void disconnect() {
    }

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws OvalException {
	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	items.add(JOVALSystem.factories.sc.ios.createVersionItem(item));
	return items;
    }

    // Private

    private boolean isNum(char ch) {
	return ch >= '0' && ch <= '9';
    }
}
