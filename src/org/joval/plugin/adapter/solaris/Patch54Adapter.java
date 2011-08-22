// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.solaris;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
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
import oval.schemas.systemcharacteristics.solaris.PatchItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.oval.OvalException;
import org.joval.util.JOVALSystem;

/**
 * Evaluates the legacy Solaris Patch OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Patch54Adapter extends PatchAdapter {
    public Patch54Adapter(ISession session) {
	super(session);
    }

    // Implement IAdapter

    public Class getObjectClass() {
	return Patch54Object.class;
    }

    public List<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws OvalException {
	Patch54Object pObj = (Patch54Object)rc.getObject();
	int iBase = 0;
	try {
	    iBase = Integer.parseInt((String)pObj.getBase().getValue());
	} catch (NumberFormatException e) {
	    throw new OvalException(e);
	}

	List<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
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
		try {
		    if (Integer.parseInt(base) < iBase) {
			items.addAll(getItems(pObj, base));
		    }
		} catch (NumberFormatException e) {
		    throw new OvalException(e);
		}
	    }
	    break;

	  case LESS_THAN_OR_EQUAL:
	    for (String base : revisions.keySet()) {
		try {
		    if (Integer.parseInt(base) <= iBase) {
			items.addAll(getItems(pObj, base));
		    }
		} catch (NumberFormatException e) {
		    throw new OvalException(e);
		}
	    }
	    break;

	  case GREATER_THAN:
	    for (String base : revisions.keySet()) {
		try {
		    if (Integer.parseInt(base) > iBase) {
			items.addAll(getItems(pObj, base));
		    }
		} catch (NumberFormatException e) {
		    throw new OvalException(e);
		}
	    }
	    break;

	  case GREATER_THAN_OR_EQUAL:
	    for (String base : revisions.keySet()) {
		try {
		    if (Integer.parseInt(base) >= iBase) {
			items.addAll(getItems(pObj, base));
		    }
		} catch (NumberFormatException e) {
		    throw new OvalException(e);
		}
	    }
	    break;

	  case PATTERN_MATCH: {
	    try {
		Pattern p = Pattern.compile((String)pObj.getBase().getValue());
		for (String base : revisions.keySet()) {
		    if (p.matcher(base).find()) {
			items.addAll(getItems(pObj, base));
		    }
		}
	    } catch (PatternSyntaxException e) {
		MessageType msg = JOVALSystem.factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALSystem.getMessage("ERROR_PATTERN", e.getMessage()));
		rc.addMessage(msg);
		JOVALSystem.getLogger().log(Level.WARNING, e.getMessage(), e);
	    }
	    break;
	  }

	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", pObj.getBase().getOperation()));
	}

	if (error != null) {
	    MessageType msg = JOVALSystem.factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(error);
	    rc.addMessage(msg);
	}
	return items;
    }

    // Internal

    private List<JAXBElement<PatchItem>> getItems(Patch54Object pObj, String base) throws OvalException {
	PatchBehaviors behaviors = pObj.getBehaviors();
	boolean isSupercedence = false;
	if (behaviors != null) {
	    isSupercedence = behaviors.isSupersedence();
	}
	int version = Integer.parseInt((String)pObj.getPatchVersion().getValue());

	List<RevisionEntry> matches = new Vector<RevisionEntry>();
	List<RevisionEntry> entries = revisions.get(base);
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
		    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION",
								   pObj.getPatchVersion().getOperation()));
		}
	    }
	}

	List<PatchEntry> patches = new Vector<PatchEntry>();
	if (isSupercedence) {
	    List<SupercedenceEntry> candidates = supercedence.get(base);
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
		List<SupercedenceEntry> candidates = supercedence.get(Integer.toString(match.patch.base));
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
	    items.add(JOVALSystem.factories.sc.solaris.createPatchItem(makeItem(patch)));
	}

	return items;
    }

    private PatchItem makeItem(PatchEntry patch) {
	PatchItem item = JOVALSystem.factories.sc.solaris.createPatchItem();
	EntityItemIntType baseType = JOVALSystem.factories.sc.core.createEntityItemIntType();
	baseType.setValue(patch.getBaseString());
	baseType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	item.setBase(baseType);
	EntityItemIntType versionType = JOVALSystem.factories.sc.core.createEntityItemIntType();
	versionType.setValue(patch.getVersionString());
	versionType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	item.setVersion(versionType);
	return item;
    }
}
