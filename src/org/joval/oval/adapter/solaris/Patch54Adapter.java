// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.solaris;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.StateType;
import oval.schemas.definitions.solaris.PatchBehaviors;
import oval.schemas.definitions.solaris.Patch54Object;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.solaris.PatchItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.oval.CollectException;
import org.joval.oval.Factories;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;

/**
 * Evaluates the legacy Solaris Patch OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Patch54Adapter extends PatchAdapter {
    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IUnixSession) {
	    super.init(session);
	    classes.add(Patch54Object.class);
	}
	return classes;
    }

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws OvalException, CollectException {
	if (!initialized) {
	    scanRevisions();
	}
	Patch54Object pObj = (Patch54Object)rc.getObject();
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

	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
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
		Pattern p = Pattern.compile((String)pObj.getBase().getValue());
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
	    throw new OvalException(e);
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

    private Collection<JAXBElement<PatchItem>> getItems(Patch54Object pObj, String base) throws CollectException {
	PatchBehaviors behaviors = pObj.getBehaviors();
	boolean isSupercedence = false;
	if (behaviors != null) {
	    isSupercedence = behaviors.isSupersedence();
	}
	int version = Integer.parseInt((String)pObj.getPatchVersion().getValue());

	Collection<RevisionEntry> matches = new Vector<RevisionEntry>();
	Collection<RevisionEntry> entries = revisions.get(base);
	if (entries != null) {
	    for (RevisionEntry entry : entries) {
		switch(pObj.getPatchVersion().getOperation()) {
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
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION,
							pObj.getPatchVersion().getOperation());
		    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		}
	    }
	}

	Collection<PatchEntry> patches = new Vector<PatchEntry>();
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

	Vector<JAXBElement<PatchItem>> items = new Vector<JAXBElement<PatchItem>>();
	for (PatchEntry patch : patches) {
	    items.add(Factories.sc.solaris.createPatchItem(makeItem(patch)));
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
