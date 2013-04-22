// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.solaris;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.StringTools;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.solaris.PatchBehaviors;
import scap.oval.definitions.solaris.Patch54Object;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.EntityItemIntType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.solaris.PatchItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Evaluates the legacy Solaris Patch OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Patch54Adapter extends PatchAdapter {
    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IUnixSession && ((IUnixSession)session).getFlavor() == IUnixSession.Flavor.SOLARIS) {
	    super.init(session, new ArrayList<Class>());
	    classes.add(Patch54Object.class);
	} else {
	    notapplicable.add(Patch54Object.class);
	}
	return classes;
    }

    public Collection<PatchItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	if (!initialized) {
	    scanRevisions();
	}
	Patch54Object pObj = (Patch54Object)obj;
	int iBase = 0;
	try {
	    iBase = Integer.parseInt((String)pObj.getBase().getValue());
	} catch (NumberFormatException e) {
	    throw new CollectException(e, FlagEnumeration.ERROR);
	}

	if (error != null) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(error);
	    rc.addMessage(msg);
	}

	Collection<PatchItem> items = new ArrayList<PatchItem>();
	try {
	    switch(pObj.getBase().getOperation()) {
	      case EQUALS:
		items.addAll(getItems(pObj, Integer.toString(iBase)));
		break;

	      case NOT_EQUAL:
		for (String base : revisions.keySet()) {
		    if (!base.equals(pObj.getBase())) {
			items.addAll(getItems(pObj, base));
		    }
		}
		break;

	      case LESS_THAN:
		for (String base : revisions.keySet()) {
		    if (Integer.parseInt(base) < iBase) {
			items.addAll(getItems(pObj, base));
		    }
		}
		break;

	      case LESS_THAN_OR_EQUAL:
		for (String base : revisions.keySet()) {
		    if (Integer.parseInt(base) <= iBase) {
			items.addAll(getItems(pObj, base));
		    }
		}
		break;

	      case GREATER_THAN:
		for (String base : revisions.keySet()) {
		    if (Integer.parseInt(base) > iBase) {
			items.addAll(getItems(pObj, base));
		    }
		}
		break;

	      case GREATER_THAN_OR_EQUAL:
		for (String base : revisions.keySet()) {
		    if (Integer.parseInt(base) >= iBase) {
			items.addAll(getItems(pObj, base));
		    }
		}
		break;

	      case PATTERN_MATCH:
		Pattern p = StringTools.pattern((String)pObj.getBase().getValue());
		for (String base : revisions.keySet()) {
		    if (p.matcher(base).find()) {
			items.addAll(getItems(pObj, base));
		    }
		}
		break;

	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, pObj.getBase().getOperation());
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	} catch (NumberFormatException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(e.getMessage());
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (PatternSyntaxException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}

	return items;
    }

    // Internal

    private Collection<PatchItem> getItems(Patch54Object pObj, String base) throws CollectException {
	PatchBehaviors behaviors = pObj.getBehaviors();
	boolean isSupercedence = false;
	if (behaviors != null) {
	    isSupercedence = behaviors.getSupersedence();
	}
	int version = Integer.parseInt((String)pObj.getPatchVersion().getValue());

	Collection<RevisionEntry> matches = new ArrayList<RevisionEntry>();
	Collection<RevisionEntry> entries = revisions.get(base);
	if (entries != null) {
	    for (RevisionEntry entry : entries) {
		OperationEnumeration op = pObj.getPatchVersion().getOperation();
		switch(op) {
		  case EQUALS:
		    if (entry.patch.version == version) {
			matches.add(entry);
		    }
		    break;
		  case NOT_EQUAL:
		    if (entry.patch.version != version) {
			matches.add(entry);
		    }
		    break;
		  case LESS_THAN:
		    if (entry.patch.version < version) {
			matches.add(entry);
		    }
		    break;
		  case LESS_THAN_OR_EQUAL:
		    if (entry.patch.version <= version) {
			matches.add(entry);
		    }
		    break;
		  case GREATER_THAN:
		    if (entry.patch.version > version) {
			matches.add(entry);
		    }
		    break;
		  case GREATER_THAN_OR_EQUAL:
		    if (entry.patch.version >= version) {
			matches.add(entry);
		    }
		    break;
		  default:
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		}
	    }
	}

	Collection<PatchEntry> patches = new ArrayList<PatchEntry>();
	if (isSupercedence) {
	    Collection<SupercedenceEntry> candidates = supercedence.get(base);
	    if (candidates != null) {
		for (SupercedenceEntry candidate : candidates) {
		    if (candidate.superceded.version >= version) {
			patches.add(candidate.by);
		    }
		}
	    }
	}
	for (RevisionEntry match : matches) {
	    patches.add(match.patch);

	    if (isSupercedence) {
		Collection<SupercedenceEntry> candidates = supercedence.get(Integer.toString(match.patch.base));
		if (candidates != null) {
		    for (SupercedenceEntry candidate : candidates) {
			if (candidate.superceded.version >= match.patch.version) {
			    patches.add(candidate.by);
			}
		    }
		}
	    }
	}

	ArrayList<PatchItem> items = new ArrayList<PatchItem>();
	for (PatchEntry entry : patches) {
	    items.add(makeItem(entry));
	}
	return items;
    }

    private PatchItem makeItem(PatchEntry patch) {
	PatchItem item = Factories.sc.solaris.createPatchItem();
	EntityItemIntType baseType = Factories.sc.core.createEntityItemIntType();
	baseType.setValue(patch.getBaseString());
	baseType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	item.setBase(baseType);
	EntityItemIntType versionType = Factories.sc.core.createEntityItemIntType();
	versionType.setValue(patch.getVersionString());
	versionType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	item.setPatchVersion(versionType);
	return item;
    }
}
