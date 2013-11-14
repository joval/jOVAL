// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.unix;

import java.io.IOException;
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
import scap.oval.definitions.unix.FileextendedattributeObject;
import scap.oval.systemcharacteristics.core.EntityItemAnySimpleType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.unix.FileextendedattributeItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.scap.oval.adapter.independent.BaseFileAdapter;
import org.joval.util.JOVALMsg;

/**
 * Evaluates UNIX File OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class FileextendedattributeAdapter extends BaseFileAdapter<FileextendedattributeItem> {
    private IUnixSession us;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IUnixSession) {
	    us = (IUnixSession)session;
	    baseInit(us);
	    classes.add(FileextendedattributeObject.class);
	} else {
	    notapplicable.add(FileextendedattributeObject.class);
	}
	return classes;
    }

    // Protected

    protected Class getItemClass() {
	return FileextendedattributeItem.class;
    }

    protected Collection<FileextendedattributeItem> getItems(ObjectType obj, Collection<IFile> files, IRequestContext rc)
		throws CollectException {

	FileextendedattributeObject fObj = (FileextendedattributeObject)obj;
	Collection<FileextendedattributeItem> items = new ArrayList<FileextendedattributeItem>();
	String attributeName = (String)fObj.getAttributeName().getValue();
	OperationEnumeration op = fObj.getAttributeName().getOperation();
	for (IFile f : files) {
	    if (!f.exists()) {
		continue;
	    }

	    // First, collect the attributes for the file
	    Map<String, String> attributes = new HashMap<String, String>();
	    try {
		attributes = getExtendedAttributes(f);
	    } catch (CollectException e) {
		throw e;
	    } catch (Exception e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_UNIX_FILEATTR, f.getPath(), e.getMessage()));
		rc.addMessage(msg);
	    }

	    // Then, add the corresponding items
	    switch(op) {
	      case EQUALS:
		if (attributes.containsKey(attributeName)) {
		    items.add(makeItem(fObj, f, attributeName, attributes.get(attributeName)));
		}
		break;

	      case NOT_EQUAL:
		for (Map.Entry<String, String> entry : attributes.entrySet()) {
		    if (!attributeName.equals(entry.getKey())) {
			items.add(makeItem(fObj, f, entry.getKey(), attributes.get(entry.getKey())));
		    }
		}
		break;

	      case PATTERN_MATCH:
		try {
		    Pattern p = StringTools.pattern(attributeName);
		    for (Map.Entry<String, String> entry : attributes.entrySet()) {
			if (p.matcher(entry.getKey()).find()) {
			    items.add(makeItem(fObj, f, entry.getKey(), attributes.get(entry.getKey())));
			}
		    }
		} catch (PatternSyntaxException e) {
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage());
		    throw new CollectException(msg, FlagEnumeration.ERROR);
		}
		break;

	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	}
	return items;
    }

    // Private

    private FileextendedattributeItem makeItem(FileextendedattributeObject fObj, IFile f, String attribute, String value)
		throws CollectException {

	FileextendedattributeItem item = null;
	try {
	    item = (FileextendedattributeItem)getBaseItem(fObj, f);
	} catch (IOException e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new CollectException(e.getMessage(), FlagEnumeration.ERROR);
	}

	EntityItemStringType attributeName = Factories.sc.core.createEntityItemStringType();
	attributeName.setValue(attribute);
	item.setAttributeName(attributeName);

	EntityItemAnySimpleType valueType = Factories.sc.core.createEntityItemAnySimpleType();
	valueType.setValue(value);
	item.setValue(valueType);

	return item;
    }

    /**
     * Get extended attributes for the specified file.
     */
    private Map<String, String> getExtendedAttributes(IFile f) throws Exception {
	StringBuffer cmd = new StringBuffer();
	switch(us.getFlavor()) {
	  case LINUX:
	    cmd.append("for attr in `getfattr '");
	    cmd.append(f.getPath());
	    cmd.append("' 2>/dev/null | sed -e 's/#.*$//' -e '/^$/d'`; do getfattr -n $attr '");
	    cmd.append(f.getPath());
	    cmd.append("' 2>/dev/null; done | sed -e 's/#.*$//' -e '/^$/d'");
	    break;

	  case MACOSX:
	    cmd.append("for attr in $(xattr '");
	    cmd.append(f.getPath());
	    cmd.append("'); do printf $attr=;xattr -p $attr '");
	    cmd.append(f.getPath());
	    cmd.append("'; done");
	    break;

	  case SOLARIS:
	    cmd.append("for attr in `runat '");
	    cmd.append(f.getPath());
	    cmd.append("' ls`; do printf $attr=;runat '");
	    cmd.append(f.getPath());
	    cmd.append("' cat $attr; done");
	    break;

	  case AIX:
	    return getAIXExtendedAttributes(f);

	  default:
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_UNIX_FLAVOR, us.getFlavor());
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}

	Map<String, String> attrs = new HashMap<String, String>();
	String last = null;
	for (String line : SafeCLI.multiLine(cmd.toString(), session, IUnixSession.Timeout.M)) {
	    int ptr = line.indexOf("=");
	    if (ptr == -1) {
		if (last != null) {
		    attrs.put(last, new StringBuffer(attrs.get(last)).append("\n").append(line).toString());
		}
	    } else {
		last = line.substring(0,ptr);
		attrs.put(last, line.substring(ptr+1));
	    }
	}
	return attrs;
    }

    private Map<String, String> getAIXExtendedAttributes(IFile f) throws Exception {
	IMount mount = getMount(f);
	if ("jfs2".equals(mount.getType())) {
	    String eav = getEAVersion(mount);
	    if ("v2".equals(eav)) {
		//
		// AIX only supports extended attributes on JFS2 v2
		//
		Map<String, String> attrs = new HashMap<String, String>();
		String key = null;
		StringBuffer value = null;
		boolean blank = true;
		for (String line : StringTools.toList(SafeCLI.manyLines("getea '" + f.getPath() + "'", null, us))) {
		    if (line.length() == 0) {
			if (blank && value != null) {
			    value.append("\n");
			}
			blank = true;
		    } else if (blank && line.startsWith("EAName: ")) {
			if (key != null && value != null) {
			    attrs.put(key, value.toString());
			}
			key = line.substring(8);
			value = null;
			blank = false;
		    } else if (key != null && line.startsWith("EAValue")) {
			value = new StringBuffer();
		    } else if (value != null) {
			if (value.length() > 0) {
			    value.append("\n");
			}
			value.append(line);
		    }
		}
		if (key != null && value != null) {
		    attrs.put(key, value.toString());
		}
		return attrs;
	    } else {
		String msg = JOVALMsg.getMessage(JOVALMsg.STATUS_AIX_JFS2EAFORMAT, eav);
		throw new CollectException(msg, FlagEnumeration.NOT_APPLICABLE);
	    }
	} else {
	    String msg = JOVALMsg.getMessage(JOVALMsg.STATUS_AIX_FSTYPE, mount.getType());
	    throw new CollectException(msg, FlagEnumeration.NOT_APPLICABLE);
	}
    }

    /**
     * Get the mount on which the specified file resides.
     */
    private IMount getMount(IFile f) throws IOException {
	String canon = f.getCanonicalPath();
	IMount mount = null;
	for (IMount mnt : us.getFilesystem().getMounts()) {
	    String mntPath = mnt.getPath();
	    if (!mntPath.endsWith("/")) {
		    mntPath = new StringBuffer(mntPath).append("/").toString();
	    }
	    if (canon.startsWith(mntPath)) {
		if (mount == null) {
		    mount = mnt;
		} else if (mnt.getPath().length() > mount.getPath().length()) {
		    mount = mnt;
		}
	    }
	}
	return mount;
    }

    private Map<String, String> jfs2eav;

    /**
     * Get the EA version of the specified mount.
     */
    private String getEAVersion(IMount mount) throws Exception {
	if (us.getFlavor() != IUnixSession.Flavor.AIX) {
	    throw new IllegalStateException(us.getFlavor().toString());
	}
	if (jfs2eav == null) {
	    List<String> lines = SafeCLI.multiLine("lsjfs2", us, IUnixSession.Timeout.S);
	    jfs2eav = new HashMap<String, String>();
	    List<String> tokens = StringTools.toList(StringTools.tokenize(lines.get(0), ":", false));
	    int mpIndex=-1, eafIndex=-1;
	    for (int i=0; i < tokens.size(); i++) {
		if ("#MountPoint".equalsIgnoreCase(tokens.get(i))) {
		    mpIndex = i;
		} else if ("EAformat".equalsIgnoreCase(tokens.get(i))) {
		    eafIndex = i;
		}
	    }
	    for (int i=1; i < lines.size(); i++) {
		tokens = StringTools.toList(StringTools.tokenize(lines.get(i), ":", false));
		jfs2eav.put(tokens.get(mpIndex), tokens.get(eafIndex));
	    }
	}
	return jfs2eav.get(mount.getPath());
    }
}
