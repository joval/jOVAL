// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.junos;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Vector;
import java.util.regex.Pattern;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.EntityObjectStringType;
import oval.schemas.definitions.junos.VersionObject;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.EntityItemVersionType;
import oval.schemas.systemcharacteristics.junos.VersionItem;

import org.joval.intf.juniper.system.IJunosSession;
import org.joval.intf.juniper.system.ISupportInformation;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.oval.CollectException;
import org.joval.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Provides Junos VersionItem OVAL items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class VersionAdapter implements IAdapter {
    static final String OPEN_BRACKET = "[";
    static final String CLOSE_BRACKET = "]";

    private IJunosSession session;
    private Hashtable<String, VersionItem> componentItems = null;
    private MessageType error = null;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IJunosSession) {
	    this.session = (IJunosSession)session;
	    classes.add(VersionObject.class);
	}
	return classes;
    }

    public Collection<VersionItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	if (componentItems == null) {
	    componentItems = new Hashtable<String, VersionItem>();
	    try {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		for (String line : session.getSupportInformation().getLines("show version detail")) {
		    if (line.trim().length() > 0) {
			if (line.startsWith("Hostname:") || line.startsWith("Model:")) {
			    // no-op
			} else if (line.startsWith("JUNOS")) {
			    VersionItem item = Factories.sc.junos.createVersionItem();

			    int ptr = line.indexOf(OPEN_BRACKET);
			    EntityItemStringType componentType = Factories.sc.core.createEntityItemStringType();
			    String component = line.substring(0, ptr).trim();
			    componentType.setValue(component);
			    componentType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
			    item.setComponent(componentType);

			    EntityItemVersionType releaseType = Factories.sc.core.createEntityItemVersionType();
			    releaseType.setValue(line.substring(ptr+1, line.indexOf(CLOSE_BRACKET)));
			    releaseType.setDatatype(SimpleDatatypeEnumeration.VERSION.value());
			    item.setRelease(releaseType);

			    componentItems.put(component, item);
			} else if (line.indexOf("release") != -1) {
			    VersionItem item = Factories.sc.junos.createVersionItem();

			    int begin = 0;
			    int end = line.indexOf(" release ");
			    String component = line.substring(begin, end).trim();
			    EntityItemStringType componentType = Factories.sc.core.createEntityItemStringType();
			    componentType.setValue(component);
			    componentType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
			    item.setComponent(componentType);

			    begin = end + 9;
			    end = line.indexOf(" built by ");
			    EntityItemVersionType releaseType = Factories.sc.core.createEntityItemVersionType();
			    String release = line.substring(begin, end).trim();
			    releaseType.setValue(release);
			    releaseType.setDatatype(SimpleDatatypeEnumeration.VERSION.value());
			    item.setRelease(releaseType);

			    begin = end + 10;
			    end = line.indexOf(" on ");
			    String builder = line.substring(begin, end).trim();
			    EntityItemStringType builderType = Factories.sc.core.createEntityItemStringType();
			    builderType.setValue(builder);
			    builderType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
			    item.setBuilder(builderType);

			    begin = end + 4;
			    String datestr = line.substring(begin).trim();
			    EntityItemIntType buildDateType = Factories.sc.core.createEntityItemIntType();
			    buildDateType.setValue(Long.toString(dateFormat.parse(datestr).getTime()));
			    buildDateType.setDatatype(SimpleDatatypeEnumeration.INT.value());
			    item.setBuildDate(buildDateType);

			    componentItems.put(component, item);
			}
		    }
		}
	    } catch (Exception e) {
		error = Factories.common.createMessageType();
		error.setLevel(MessageLevelEnumeration.ERROR);
		error.setValue(e.getMessage());
	    }
	}

	Collection<VersionItem> items = new Vector<VersionItem>();
	VersionObject vObj = (VersionObject)obj;
	if (vObj.isSetComponent()) {
	    EntityObjectStringType component = vObj.getComponent();
	    switch(component.getOperation()) {
	      case EQUALS:
		if (componentItems.containsKey((String)component.getValue())) {
		    items.add(componentItems.get((String)component.getValue()));
		}
		break;

	      case NOT_EQUAL:
		for (String name : componentItems.keySet()) {
		    if (!name.equals((String)component.getValue())) {
			items.add(componentItems.get(name));
		    }
		}
		break;

	      case PATTERN_MATCH:
		for (String name : componentItems.keySet()) {
		    if (Pattern.compile((String)component.getValue()).matcher(name).find()) {
			items.add(componentItems.get(name));
		    }
		}
		break;

	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, component.getOperation());
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	} else {
	    items.addAll(componentItems.values());
	}
	if (error != null) {
	    rc.addMessage(error);
	}
	return items;
    }
}
