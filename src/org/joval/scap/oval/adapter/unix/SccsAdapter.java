// Copyright (C) 2014 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.unix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.Message;
import jsaf.intf.io.IFile;
import jsaf.intf.io.IFilesystem.IMount;
import jsaf.intf.system.ISession;
import jsaf.intf.unix.io.IUnixFileInfo;
import jsaf.intf.unix.io.IUnixFilesystem;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;
import jsaf.util.StringTools;

import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.MessageType;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.unix.SccsObject;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.unix.SccsItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.scap.oval.adapter.independent.BaseFileAdapter;
import org.joval.util.JOVALMsg;

/**
 * Resolves UNIX SCCS OVAL objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SccsAdapter extends BaseFileAdapter<SccsItem> {
    public static final String SCCS = "/usr/ccs/bin/sccs";
    public static final String PRS = "/usr/ccs/bin/prs";

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IUnixSession) {
	    IUnixSession us = (IUnixSession)session;
	    try {
		if (us.getFilesystem().getFile(SCCS).exists()) {
		    baseInit(us);
		    classes.add(SccsObject.class);
		} else {
		    notapplicable.add(SccsObject.class);
		}
	    } catch (IOException e) {
		// SCCS objects will not be collected
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	} else {
	    notapplicable.add(SccsObject.class);
	}
	return classes;
    }

    // Protected

    protected Class getItemClass() {
	return SccsItem.class;
    }

    protected Collection<SccsItem> getItems(ObjectType obj, Collection<IFile> files, IRequestContext rc)
		throws CollectException {

	SccsObject sObj = (SccsObject)obj;
	Collection<SccsItem> items = new ArrayList<SccsItem>();
	for (IFile f : files) {
	    if (f != null && f.exists()) {
		try {
		    items.add(makeItem(sObj, f));
		} catch (Exception e) {
		    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		    MessageType msg = Factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.ERROR);
		    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_UNIX_SCCS, f.getPath(), e.getMessage()));
		    rc.addMessage(msg);
		}
	    }
	}
	return items;
    }

    // Private

    private static final String EMPTY = "";

    private SccsItem makeItem(SccsObject sObj, IFile f) throws Exception {
	StringBuffer cmd = new StringBuffer(PRS);
	cmd.append(" -d'Branch=:B:\nLvl=:L:\nSeq=:S:\nRel=:R:\nModName=:M:\nModType=:Y:\nWhat=:W:'");
	SafeCLI.ExecData ed = SafeCLI.execData(cmd.toString(), null, session, session.getTimeout(IUnixSession.Timeout.M));
	if (ed.getExitCode() == 0) {
	    SccsItem item = (SccsItem)getBaseItem(sObj, f);

	    String last = null;
	    Map<String, String> props = new HashMap<String, String>();
	    for (String line : ed.getLines()) {
		int ptr = line.indexOf("=");
		if (ptr == -1) {
		    if (last != null) {
			props.put(last, new StringBuffer(props.get(last)).append("\n").append(line).toString());
		    }
		} else {
		    last = line.substring(0,ptr);
		    props.put(last, line.substring(ptr+1));
		}
	    }

	    EntityItemStringType moduleName = Factories.sc.core.createEntityItemStringType();
	    String temp = props.get("ModName");
	    if (EMPTY.equals(temp)) {
		moduleName.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		moduleName.setValue(temp);
	    }
	    item.setModuleName(moduleName);

	    EntityItemStringType moduleType = Factories.sc.core.createEntityItemStringType();
	    temp = props.get("ModName");
	    if (EMPTY.equals(temp)) {
		moduleType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		moduleType.setValue(temp);
	    }
	    item.setModuleType(moduleType);

	    EntityItemStringType release = Factories.sc.core.createEntityItemStringType();
	    temp = props.get("Rel");
	    if (EMPTY.equals(temp)) {
		release.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		release.setValue(temp);
	    }
	    item.setRelease(release);

	    EntityItemStringType level = Factories.sc.core.createEntityItemStringType();
	    temp = props.get("Lvl");
	    if (EMPTY.equals(temp)) {
		level.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		level.setValue(temp);
	    }
	    item.setLevel(level);

	    EntityItemStringType branch = Factories.sc.core.createEntityItemStringType();
	    temp = props.get("Branch");
	    if (EMPTY.equals(temp)) {
		branch.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		branch.setValue(temp);
	    }
	    item.setBranch(branch);

	    EntityItemStringType sequence = Factories.sc.core.createEntityItemStringType();
	    temp = props.get("Seq");
	    if (EMPTY.equals(temp)) {
		sequence.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		sequence.setValue(temp);
	    }
	    item.setSequence(sequence);

	    EntityItemStringType whatString = Factories.sc.core.createEntityItemStringType();
	    temp = props.get("What");
	    if (EMPTY.equals(temp)) {
		whatString.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		whatString.setValue(temp);
	    }
	    item.setWhatString(whatString);

	    return item;
	} else {
	    throw new Exception(new String(ed.getData(), StringTools.UTF8));
	}
    }
}
