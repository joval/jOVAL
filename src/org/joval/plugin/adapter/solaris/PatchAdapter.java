// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.solaris;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.ExistenceEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.definitions.core.EntityObjectIntType;
import oval.schemas.definitions.core.EntityStateIntType;
import oval.schemas.definitions.core.ObjectComponentType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.StateType;
import oval.schemas.definitions.solaris.PatchObject;
import oval.schemas.definitions.solaris.PatchState;
import oval.schemas.definitions.solaris.PatchTest;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.solaris.PatchItem;
import oval.schemas.systemcharacteristics.solaris.ObjectFactory;
import oval.schemas.results.core.ResultEnumeration;
import oval.schemas.results.core.TestedItemType;
import oval.schemas.results.core.TestedVariableType;
import oval.schemas.results.core.TestType;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IAdapterContext;
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.intf.oval.IDefinitions;
import org.joval.intf.oval.ISystemCharacteristics;
import org.joval.oval.OvalException;
import org.joval.util.JOVALSystem;

/**
 * Evaluates the legacy Solaris Patch OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class PatchAdapter implements IAdapter {
    private IAdapterContext ctx;
    private IDefinitions definitions;
    private ISession session;
    private oval.schemas.systemcharacteristics.core.ObjectFactory coreFactory;
    private ObjectFactory solarisFactory;
    private Hashtable<String, List<RevisionEntry>> revisions;

    public PatchAdapter(ISession session) {
	this.session = session;
	coreFactory = new oval.schemas.systemcharacteristics.core.ObjectFactory();
	solarisFactory = new ObjectFactory();
	revisions = new Hashtable<String, List<RevisionEntry>>();
    }

    // Implement IAdapter

    public void init(IAdapterContext ctx) {
	this.ctx = ctx;
	definitions = ctx.getDefinitions();
    }

    public void scan(ISystemCharacteristics sc) throws OvalException {
	ctx.status("Retrieving revision list");
	scanRevisions();

	Iterator<ObjectType> iter = definitions.iterateObjects(PatchObject.class);
	while (iter.hasNext()) {
	    PatchObject pObj = (PatchObject)iter.next();
	    ctx.status(pObj.getId());
	    String base = (String)pObj.getBase().getValue();
	    List<RevisionEntry> entries = revisions.get(base);
	    if (entries == null) {
		sc.setObject(pObj.getId(), pObj.getComment(), pObj.getVersion(), FlagEnumeration.DOES_NOT_EXIST, null);
	    } else {
		sc.setObject(pObj.getId(), pObj.getComment(), pObj.getVersion(), FlagEnumeration.COMPLETE, null);
		for (RevisionEntry entry : entries) {
		    PatchItem item = solarisFactory.createPatchItem();
		    EntityItemIntType baseType = coreFactory.createEntityItemIntType();
		    baseType.setValue(entry.patch.getBaseString());
		    item.setBase(baseType);
		    EntityItemIntType versionType = coreFactory.createEntityItemIntType();
		    versionType.setValue(entry.patch.getVersionString());
		    item.setVersion(versionType);
		    BigInteger itemId = sc.storeItem(solarisFactory.createPatchItem(item));
		    sc.relateItem(pObj.getId(), itemId);
		}
	    }
	}
    }

    public Class getObjectClass() {
	return PatchObject.class;
    }

    public Class getTestClass() {
	return PatchTest.class;
    }

    public Class getStateClass() {
	return PatchState.class;
    }

    public Class getItemClass() {
	return PatchItem.class;
    }

    public String getItemData(ObjectComponentType object, ISystemCharacteristics sc) throws OvalException {
	return null; // What foolish variable would point to a PatchObject?
    }

    public ResultEnumeration compare(StateType st, ItemType it) throws OvalException {
	if (compare((PatchState)st, (PatchItem)it)) {
	    return ResultEnumeration.TRUE;
	} else {
	    return ResultEnumeration.FALSE;
	}
    }

    // Internal

    private boolean compare(PatchState state, PatchItem item) throws OvalException {
	int stateVersion = Integer.parseInt((String)state.getPatchVersion().getValue());
	int itemVersion = Integer.parseInt((String)item.getVersion().getValue());
	switch (state.getPatchVersion().getOperation()) {
	  case EQUALS:
	    return stateVersion == itemVersion;
	  case NOT_EQUAL:
	    return stateVersion != itemVersion;
	  case GREATER_THAN:
	    return stateVersion > itemVersion;
	  case GREATER_THAN_OR_EQUAL:
	    return stateVersion >= itemVersion;
	  case LESS_THAN:
	    return stateVersion < itemVersion;
	  case LESS_THAN_OR_EQUAL:
	    return stateVersion <= itemVersion;
	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION",
							   state.getPatchVersion().getOperation()));
	}
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
	try {
	    IProcess p = session.createProcess("/usr/bin/showrev -p");
	    p.start();
	    BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    String line = br.readLine(); // skip over the header row.
	    while((line = br.readLine()) != null) {
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
		    obsoletes.add(new PatchEntry(tok.nextToken().trim()));
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
		    List<RevisionEntry> list = new Vector<RevisionEntry>();
		    list.add(entry);
		    revisions.put(patch.getBaseString(), list);
		}
	    }
	    br.close();
	} catch (Exception e) {
	    JOVALSystem.getLogger().log(Level.SEVERE, e.getMessage(), e);
	}
    }

    class RevisionEntry {
	PatchEntry patch;
	List<PatchEntry> obsoletes, requires, incompatibles;
	List<String> packages;

	RevisionEntry(PatchEntry patch,
		      List<PatchEntry> obsoletes,
		      List<PatchEntry> requires,
		      List<PatchEntry> incompatibles,
		      List<String> packages) {
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
}
