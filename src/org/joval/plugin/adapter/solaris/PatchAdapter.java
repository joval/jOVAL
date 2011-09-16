// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.solaris;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Hashtable;
import java.util.Collection;
import java.util.Vector;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.solaris.PatchObject;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.solaris.PatchItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Evaluates the legacy Solaris Patch OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class PatchAdapter implements IAdapter {
    ISession session;
    String error = null;
    Hashtable<String, Collection<RevisionEntry>> revisions;
    Hashtable<String, Collection<SupercedenceEntry>> supercedence;

    public PatchAdapter(ISession session) {
	this.session = session;
	revisions = new Hashtable<String, Collection<RevisionEntry>>();
	supercedence = new Hashtable<String, Collection<SupercedenceEntry>>();
    }

    // Implement IAdapter

    public Class getObjectClass() {
	return PatchObject.class;
    }

    public boolean connect() {
	if (session != null) {
	    scanRevisions();
	    return true;
	}
	return false;
    }

    public void disconnect() {
    }

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws OvalException {
	PatchObject pObj = (PatchObject)rc.getObject();
	int iBase = 0;
	try {
	    iBase = Integer.parseInt((String)pObj.getBase().getValue());
	} catch (NumberFormatException e) {
	    throw new OvalException(e);
	}

	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	switch(pObj.getBase().getOperation()) {
	  case EQUALS:
	    items.addAll(getItems((String)pObj.getBase().getValue()));
	    break;

	  case NOT_EQUAL:
	    for (String base : revisions.keySet()) {
		if (!base.equals((String)pObj.getBase().getValue())) {
		    items.addAll(getItems(base));
		}
	    }
	    break;

	  case LESS_THAN:
	    for (String base : revisions.keySet()) {
		try {
		    if (Integer.parseInt(base) < iBase) {
			items.addAll(getItems(base));
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
			items.addAll(getItems(base));
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
			items.addAll(getItems(base));
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
			items.addAll(getItems(base));
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
			items.addAll(getItems(base));
		    }
		}
	    } catch (PatternSyntaxException e) {
		MessageType msg = JOVALSystem.factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALSystem.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
		rc.addMessage(msg);
		JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    break;
	  }

	  default: {
	    String s = JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, pObj.getBase().getOperation());
	    throw new OvalException(s);
	  }
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

    private Collection<JAXBElement<PatchItem>> getItems(String base) {
	Collection<JAXBElement<PatchItem>> items = new Vector<JAXBElement<PatchItem>>();
	Collection<RevisionEntry> entries = revisions.get(base);
	if (entries != null) {
	    for (RevisionEntry entry : entries) {
		PatchItem item = JOVALSystem.factories.sc.solaris.createPatchItem();
		EntityItemIntType baseType = JOVALSystem.factories.sc.core.createEntityItemIntType();
		baseType.setValue(entry.patch.getBaseString());
		baseType.setDatatype(SimpleDatatypeEnumeration.INT.value());
		item.setBase(baseType);
		EntityItemIntType versionType = JOVALSystem.factories.sc.core.createEntityItemIntType();
		versionType.setValue(entry.patch.getVersionString());
		versionType.setDatatype(SimpleDatatypeEnumeration.INT.value());
		item.setPatchVersion(versionType);
		items.add(JOVALSystem.factories.sc.solaris.createPatchItem(item));
	    }
	}
	return items;
    }

    private static final String PATCH		= "Patch:";
    private static final String OBSOLETES	= "Obsoletes:";
    private static final String REQUIRES	= "Requires:";
    private static final String INCOMPATIBLES	= "Incompatibles:";
    private static final String PACKAGES	= "Packages:";

    /**
     * REMIND: Stops if it encounters any exceptions at all; make this more robust?
     */
    private void scanRevisions() {
	BufferedReader br = null;
	try {
	    IProcess p = session.createProcess("/usr/bin/showrev -p");
	    p.start();
	    br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    String line;
	    while((line = br.readLine()) != null) {
		if (!line.startsWith(PATCH)) {
		    continue;
		}
		StringTokenizer tok = null;
		String buff = null;
		int begin, end;

		begin = line.indexOf(PATCH) + PATCH.length();
		end = line.indexOf(OBSOLETES);
		buff = line.substring(begin, end).trim();
		PatchEntry patch = new PatchEntry(buff);

		begin = line.indexOf(OBSOLETES) + OBSOLETES.length();
		end = line.indexOf(REQUIRES);
		buff = line.substring(begin, end).trim();
		Vector<PatchEntry> obsoletes = new Vector<PatchEntry>();
		tok = new StringTokenizer(buff, ",");
		while(tok.hasMoreTokens()) {
		    PatchEntry superceded = new PatchEntry(tok.nextToken().trim());
		    obsoletes.add(superceded);
		    String obsoleteBase = superceded.getBaseString();
		    Collection<SupercedenceEntry> list = supercedence.get(obsoleteBase);
		    if (list == null) {
			list = new Vector<SupercedenceEntry>();
			supercedence.put(obsoleteBase, list);
		    }
		    SupercedenceEntry entry = new SupercedenceEntry(superceded, patch);
		    if (!list.contains(entry)) {
			list.add(entry);
		    }
		}

		begin = line.indexOf(REQUIRES) + REQUIRES.length();
		end = line.indexOf(INCOMPATIBLES);
		buff = line.substring(begin, end).trim();
		Vector<PatchEntry> requires = new Vector<PatchEntry>();
		tok = new StringTokenizer(buff, ",");
		while(tok.hasMoreTokens()) {
		    requires.add(new PatchEntry(tok.nextToken().trim()));
		}

		begin = line.indexOf(INCOMPATIBLES) + INCOMPATIBLES.length();
		end = line.indexOf(PACKAGES);
		buff = line.substring(begin, end).trim();
		Vector<PatchEntry> incompatibles = new Vector<PatchEntry>();
		tok = new StringTokenizer(buff, ",");
		while(tok.hasMoreTokens()) {
		    incompatibles.add(new PatchEntry(tok.nextToken().trim()));
		}

		begin = line.indexOf(PACKAGES) + PACKAGES.length();
		buff = line.substring(begin).trim();
		Vector<String> packages = new Vector<String>();
		tok = new StringTokenizer(buff, ",");
		while(tok.hasMoreTokens()) {
		    packages.add(tok.nextToken().trim());
		}

		RevisionEntry entry = new RevisionEntry(patch, obsoletes, requires, incompatibles, packages);
		if (revisions.containsKey(patch.getBaseString())) {
		    revisions.get(patch.getBaseString()).add(entry);
		} else {
		    Collection<RevisionEntry> list = new Vector<RevisionEntry>();
		    list.add(entry);
		    revisions.put(patch.getBaseString(), list);
		}
	    }
	} catch (Exception e) {
	    error = e.getMessage();
	    JOVALSystem.getLogger().error(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} finally {
	    if (br != null) {
		try {
		    br.close();
		} catch (IOException e) {
		}
	    }
	}
    }

    class RevisionEntry {
	PatchEntry patch;
	Collection<PatchEntry> obsoletes, requires, incompatibles;
	Collection<String> packages;

	RevisionEntry(PatchEntry patch,
		      Collection<PatchEntry> obsoletes,
		      Collection<PatchEntry> requires,
		      Collection<PatchEntry> incompatibles,
		      Collection<String> packages) {
	    this.patch = patch;
	    this.obsoletes = obsoletes;
	    this.requires = requires;
	    this.incompatibles = incompatibles;
	    this.packages = packages;
	}
    }

    class PatchEntry {
	int base, version;

	PatchEntry(String id) throws Exception {
	    int ptr = id.indexOf("-");
	    base = Integer.parseInt(id.substring(0, ptr));
	    version = Integer.parseInt(id.substring(ptr+1));
	}

	String getBaseString() {
	    return Integer.toString(base);
	}

	String getVersionString() {
	    return Integer.toString(version);
	}
    }

    class SupercedenceEntry {
	PatchEntry superceded;
	PatchEntry by;

	SupercedenceEntry (PatchEntry superceded, PatchEntry by) {
	    this.superceded = superceded;
	    this.by = by;
	}

	public boolean equals(Object other) {
	    if (other instanceof SupercedenceEntry) {
		return superceded == ((SupercedenceEntry)other).superceded && by == ((SupercedenceEntry)other).by;
	    }
	    return false;
	}
    }
}
