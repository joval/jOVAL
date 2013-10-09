// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.macos;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.macos.NvramObject;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.macos.NvramItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Evaluates NvramTest OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class NvramAdapter implements IAdapter {
    private IUnixSession session;
    private Map<String, NvramItem> values;
    private CollectException error;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IUnixSession && ((IUnixSession)session).getFlavor() == IUnixSession.Flavor.MACOSX) {
	    this.session = (IUnixSession)session;
	    classes.add(NvramObject.class);
	} else {
	    notapplicable.add(NvramObject.class);
	}
	return classes;
    }

    public Collection<NvramItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	init();
	NvramObject nObj = (NvramObject)obj;
	Collection<NvramItem> items = new ArrayList<NvramItem>();
	switch(nObj.getNvramVar().getOperation()) {
	  case EQUALS:
	    if (values.containsKey((String)nObj.getNvramVar().getValue())) {
		items.add(values.get((String)nObj.getNvramVar().getValue()));
	    }
	    break;

	  case CASE_INSENSITIVE_EQUALS:
	    for (Map.Entry<String, NvramItem> entry : values.entrySet()) {
		if (entry.getKey().equalsIgnoreCase((String)nObj.getNvramVar().getValue())) {
		    items.add(entry.getValue());
		}
	    }
	    break;

	  case NOT_EQUAL:
	    for (Map.Entry<String, NvramItem> entry : values.entrySet()) {
		if (!entry.getKey().equals((String)nObj.getNvramVar().getValue())) {
		    items.add(entry.getValue());
		}
	    }
	    break;

	  case CASE_INSENSITIVE_NOT_EQUAL:
	    for (Map.Entry<String, NvramItem> entry : values.entrySet()) {
		if (!entry.getKey().equalsIgnoreCase((String)nObj.getNvramVar().getValue())) {
		    items.add(entry.getValue());
		}
	    }
	    break;

	  case PATTERN_MATCH:
	    try {
		Pattern p = Pattern.compile((String)nObj.getNvramVar().getValue());
		for (Map.Entry<String, NvramItem> entry : values.entrySet()) {
		    if (p.matcher(entry.getKey()).find()) {
			items.add(entry.getValue());
		    }
		}
	    } catch (PatternSyntaxException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
		rc.addMessage(msg);
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    break;

	  default:
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, nObj.getNvramVar().getOperation());
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}
	return items;
    }

    // Private

    private void init() throws CollectException {
	if (error != null) {
	    throw error;
	} else if (values == null) {
	    try {
		List<String> lines = SafeCLI.multiLine("nvram -p", session, IUnixSession.Timeout.S);
		if (lines.size() == 1 &&
		    lines.get(0).equals("nvram: nvram is not supported on this system")) {

		    throw error = new CollectException(lines.get(0), FlagEnumeration.NOT_APPLICABLE);
		} else {
		    values = new HashMap<String, NvramItem>();
		    for (String line : lines) {
			StringTokenizer tok = new StringTokenizer(line);
			String var = tok.nextToken();
			NvramItem item = Factories.sc.macos.createNvramItem();
			values.put(var, item);

			EntityItemStringType nvramVar = Factories.sc.core.createEntityItemStringType();
			nvramVar.setValue(var);
			item.setNvramVar(nvramVar);

			EntityItemStringType nvramValue = Factories.sc.core.createEntityItemStringType();
			nvramValue.setValue(tok.nextToken("\n"));
			item.setNvramValue(nvramValue);
		    }
		}
	    } catch (CollectException e) {
		throw e;
	    } catch (Exception e) {
		error = new CollectException(e, FlagEnumeration.ERROR);
		throw error;
	    }
	}
    }
}
