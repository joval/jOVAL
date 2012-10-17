// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.cisco.ios;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Collection;
import java.util.Vector;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.ios.Version55Object;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.ios.VersionItem;
import oval.schemas.systemcharacteristics.core.EntityItemIOSVersionType;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.cisco.system.IIosSession;
import org.joval.intf.cisco.system.ITechSupport;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.scap.oval.Factories;

/**
 * Provides Cisco IOS Version55Item OVAL items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Version55Adapter implements IAdapter {
    static final String OPEN_PEREN = "(";
    static final String CLOSE_PEREN = ")";

    protected IIosSession session;
    protected VersionItem item;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IIosSession) {
	    this.session = (IIosSession)session;
	    classes.add(Version55Object.class);
	}
	return classes;
    }

    public Collection<VersionItem> getItems(ObjectType obj, IRequestContext rc) {
	Collection<VersionItem> items = new Vector<VersionItem>();
	try {
	   items.add(getItem());
	} catch (Exception e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(e.getMessage());
	    rc.addMessage(msg);
	}
	return items;
    }

    // Internal

    protected VersionItem getItem() throws Exception {
	if (item == null) {
	    item = makeItem();
	}
	return item;
    }

    // Private

    private VersionItem makeItem() throws Exception {
	VersionItem item = Factories.sc.ios.createVersionItem();
	String version = null;
	for (String line : session.getTechSupport().getLines("show version")) {
	    if (line.startsWith("Cisco IOS")) {
		int ptr = line.indexOf("Version ");
		if (ptr != -1) {
		    int begin = ptr + 8;
		    int end = line.indexOf(",", begin+1);
		    if (end == -1) {
			version = line.substring(begin);
		    } else {
			version = line.substring(begin, end);
		    }
		}
	    }
	}
	int begin = 0;
	int end = version.indexOf(".");
	if (end != -1) {
	    EntityItemIntType majorVersion = Factories.sc.core.createEntityItemIntType();
	    majorVersion.setValue(version.substring(begin, end));
	    majorVersion.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    item.setMajorVersion(majorVersion);
	}

	begin = end + 1;
	end = version.indexOf(OPEN_PEREN);
	if (begin > 0 && end > begin) {
	    EntityItemIntType minorVersion = Factories.sc.core.createEntityItemIntType();
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
		if (isNum(ch) || ch == '.') {
		    sb.append(ch);
		} else {
		    break;
		}
	    }
	    if (sb.length() > 0) {
		EntityItemIntType release = Factories.sc.core.createEntityItemIntType();
		release.setValue(sb.toString());
		release.setDatatype(SimpleDatatypeEnumeration.INT.value());
		item.setRelease(release);

		begin += sb.length();
		if (begin < end) {
		    EntityItemStringType mainlineRebuild = Factories.sc.core.createEntityItemStringType();
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
		EntityItemStringType trainIdentifier = Factories.sc.core.createEntityItemStringType();
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
			EntityItemIntType rebuild = Factories.sc.core.createEntityItemIntType();
			rebuild.setValue(sb.toString());
			rebuild.setDatatype(SimpleDatatypeEnumeration.INT.value());
			item.setRebuild(rebuild);
		    }

		    begin += sb.length();
		    if (begin < end) {
			EntityItemStringType subrebuild = Factories.sc.core.createEntityItemStringType();
			subrebuild.setValue(version.substring(begin, end));
			item.setSubrebuild(subrebuild);
		    }
		}
	    }
	}

	EntityItemIOSVersionType versionString = Factories.sc.core.createEntityItemIOSVersionType();
	versionString.setValue(version);
	versionString.setDatatype(SimpleDatatypeEnumeration.IOS_VERSION.value());
	item.setVersionString(versionString);
	return item;
    }

    // Private

    private boolean isNum(char ch) {
	return ch >= '0' && ch <= '9';
    }
}
