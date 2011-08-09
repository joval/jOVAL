// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.unix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.StateType;
import oval.schemas.definitions.unix.FileObject;
import oval.schemas.definitions.unix.FileState;
import oval.schemas.definitions.unix.FileTest;
import oval.schemas.systemcharacteristics.core.EntityItemBoolType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.core.VariableValueType;
import oval.schemas.systemcharacteristics.unix.ObjectFactory;
import oval.schemas.systemcharacteristics.unix.FileItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IAdapterContext;
import org.joval.intf.system.IProcess;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.io.StreamTool;
import org.joval.oval.OvalException;
import org.joval.util.BaseFileAdapter;
import org.joval.util.JOVALSystem;

/**
 * Evaluates UNIX File OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class FileAdapter extends BaseFileAdapter {
    private IUnixSession session;
    protected ObjectFactory unixFactory;

    public FileAdapter(IUnixSession session, IFilesystem fs) {
	super(fs);
	this.session = session;
	unixFactory = new ObjectFactory();
    }

    // Implement IAdapter

    public Class getObjectClass() {
	return FileObject.class;
    }

    public Class getStateClass() {
	return FileState.class;
    }

    public Class getItemClass() {
	return FileItem.class;
    }

    public boolean connect() {
	return session != null;
    }

    public ResultEnumeration compare(StateType st, ItemType it) throws OvalException {
	if (compare((FileState)st, (FileItem)it)) {
	    return ResultEnumeration.TRUE;
	} else {
	    return ResultEnumeration.FALSE;
	}
    }

    // Protected

    protected Object convertFilename(EntityItemStringType filename) {
	return unixFactory.createFileItemFilename(filename);
    }

    protected ItemType createFileItem() {
	return unixFactory.createFileItem();
    }

    protected List<JAXBElement<? extends ItemType>>
	getItems(ItemType base, ObjectType obj, IFile f, List<VariableValueType> vars) throws IOException, OvalException {

	List<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	if (base instanceof FileItem) {
	    setItem((FileItem)base, f);
	    items.add(unixFactory.createFileItem((FileItem)base));
	}
	return items;
    }

    // Private

    private boolean compare(FileState state, FileItem item) throws OvalException {
	if (state.isSetType()) {
	    return ((String)state.getType().getValue()).equals((String)item.getType().getValue());
	}
	if (state.isSetUread()) {
	    return ((String)state.getUread().getValue()).equals((String)item.getUread().getValue());
	}
	if (state.isSetUwrite()) {
	    return ((String)state.getUwrite().getValue()).equals((String)item.getUwrite().getValue());
	}
	if (state.isSetUexec()) {
	    return ((String)state.getUexec().getValue()).equals((String)item.getUexec().getValue());
	}
	if (state.isSetSuid()) {
	    return ((String)state.getSuid().getValue()).equals((String)item.getSuid().getValue());
	}
	if (state.isSetUserId()) {
	    return ((String)state.getUserId().getValue()).equals((String)item.getUserId().getValue());
	}
	if (state.isSetGread()) {
	    return ((String)state.getGread().getValue()).equals((String)item.getGread().getValue());
	}
	if (state.isSetGwrite()) {
	    return ((String)state.getGwrite().getValue()).equals((String)item.getGwrite().getValue());
	}
	if (state.isSetGexec()) {
	    return ((String)state.getGexec().getValue()).equals((String)item.getGexec().getValue());
	}
	if (state.isSetSgid()) {
	    return ((String)state.getSgid().getValue()).equals((String)item.getSgid().getValue());
	}
	if (state.isSetGroupId()) {
	    return ((String)state.getGroupId().getValue()).equals((String)item.getGroupId().getValue());
	}
	if (state.isSetOread()) {
	    return ((String)state.getOread().getValue()).equals((String)item.getOread().getValue());
	}
	if (state.isSetOwrite()) {
	    return ((String)state.getOwrite().getValue()).equals((String)item.getOwrite().getValue());
	}
	if (state.isSetOexec()) {
	    return ((String)state.getOexec().getValue()).equals((String)item.getOexec().getValue());
	}
	if (state.isSetSticky()) {
	    return ((String)state.getSticky().getValue()).equals((String)item.getSticky().getValue());
	}
	if (state.isSetHasExtendedAcl()) {
	    return ((String)state.getHasExtendedAcl().getValue()).equals((String)item.getHasExtendedAcl().getValue());
	}
	if (state.isSetCTime()) {
	    return ((String)state.getCTime().getValue()).equals((String)item.getCTime().getValue());
	}
	if (state.isSetATime()) {
	    return ((String)state.getATime().getValue()).equals((String)item.getATime().getValue());
	}
	if (state.isSetMTime()) {
	    return ((String)state.getMTime().getValue()).equals((String)item.getMTime().getValue());
	}
	throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_STATE", state.getId()));
    }

    /**
     * Decorate the Item with information about the file.
     */
    private void setItem(FileItem item, IFile file) throws IOException {
	EntityItemIntType aTime = coreFactory.createEntityItemIntType();
	aTime.setValue(Long.toString(file.accessTime()/1000L));
	aTime.setDatatype(SimpleDatatypeEnumeration.INT.value());
	item.setATime(aTime);

	EntityItemIntType cTime = coreFactory.createEntityItemIntType();
	cTime.setStatus(StatusEnumeration.NOT_COLLECTED);
	cTime.setDatatype(SimpleDatatypeEnumeration.INT.value());
	item.setCTime(cTime);

	EntityItemIntType mTime = coreFactory.createEntityItemIntType();
	mTime.setValue(Long.toString(file.lastModified()/1000L));
	mTime.setDatatype(SimpleDatatypeEnumeration.INT.value());
	item.setMTime(mTime);

	try {
	    Lstat ls = new Lstat(file.getLocalName());

	    EntityItemStringType type = coreFactory.createEntityItemStringType();
	    type.setValue(ls.getType());
	    item.setType(type);

	    EntityItemIntType userId = coreFactory.createEntityItemIntType();
	    userId.setValue(Integer.toString(ls.getUserId()));
	    userId.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    item.setUserId(userId);

	    EntityItemIntType groupId = coreFactory.createEntityItemIntType();
	    groupId.setValue(Integer.toString(ls.getGroupId()));
	    groupId.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    item.setGroupId(groupId);

	    EntityItemBoolType uRead = coreFactory.createEntityItemBoolType();
	    uRead.setValue(Boolean.toString(ls.uRead()));
	    uRead.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    item.setUread(uRead);

	    EntityItemBoolType uWrite = coreFactory.createEntityItemBoolType();
	    uWrite.setValue(Boolean.toString(ls.uWrite()));
	    uWrite.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    item.setUwrite(uWrite);

	    EntityItemBoolType uExec = coreFactory.createEntityItemBoolType();
	    uExec.setValue(Boolean.toString(ls.uExec()));
	    uExec.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    item.setUexec(uExec);

	    EntityItemBoolType sUid = coreFactory.createEntityItemBoolType();
	    sUid.setValue(Boolean.toString(ls.sUid()));
	    sUid.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    item.setSuid(sUid);

	    EntityItemBoolType gRead = coreFactory.createEntityItemBoolType();
	    gRead.setValue(Boolean.toString(ls.gRead()));
	    gRead.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    item.setGread(gRead);

	    EntityItemBoolType gWrite = coreFactory.createEntityItemBoolType();
	    gWrite.setValue(Boolean.toString(ls.gWrite()));
	    gWrite.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    item.setGwrite(gWrite);

	    EntityItemBoolType gExec = coreFactory.createEntityItemBoolType();
	    gExec.setValue(Boolean.toString(ls.gExec()));
	    gExec.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    item.setGexec(gExec);

	    EntityItemBoolType sGid = coreFactory.createEntityItemBoolType();
	    sGid.setValue(Boolean.toString(ls.sGid()));
	    sGid.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    item.setSgid(sGid);

	    EntityItemBoolType oRead = coreFactory.createEntityItemBoolType();
	    oRead.setValue(Boolean.toString(ls.oRead()));
	    oRead.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    item.setOread(oRead);

	    EntityItemBoolType oWrite = coreFactory.createEntityItemBoolType();
	    oWrite.setValue(Boolean.toString(ls.oWrite()));
	    oWrite.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    item.setOwrite(oWrite);

	    EntityItemBoolType oExec = coreFactory.createEntityItemBoolType();
	    oExec.setValue(Boolean.toString(ls.oExec()));
	    oExec.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    item.setOexec(oExec);

	    EntityItemBoolType sticky = coreFactory.createEntityItemBoolType();
	    sticky.setValue(Boolean.toString(ls.sticky()));
	    sticky.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    item.setSticky(sticky);

	    EntityItemBoolType aclType = coreFactory.createEntityItemBoolType();
	    aclType.setValue(Boolean.toString(ls.hasExtendedAcl()));
	    aclType.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    item.setHasExtendedAcl(aclType);
	} catch (Exception e) {
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_UNIX_FILE", e.getMessage()), e);
	    throw new IOException (e);
	}
    }

    class Lstat {
	private boolean hasExtendedAcl = false;
	private String permissions;
	private int uid, gid;
	private char type;

	Lstat(String path) throws Exception {
	    String command = null;
	    switch(session.getFlavor()) {
	      case SOLARIS: {
		command = "/usr/bin/ls -n " + path;
		break;
	      }

	      case LINUX: {
		command = "/bin/ls -dn " + path;
		break;
	      }

	      default:
		throw new RuntimeException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_UNIX_FLAVOR", session.getFlavor()));
	    }

	    IProcess p = session.createProcess(command);
	    p.start();
	    BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    String line = br.readLine();
	    br.close();
	    type = line.charAt(0);
	    permissions = line.substring(1, 10);
	    if (line.charAt(11) == '+') {
		hasExtendedAcl = true;
	    }
	    StringTokenizer tok = new StringTokenizer(line.substring(12));
	    int uid = Integer.parseInt(tok.nextToken());
	    int gid = Integer.parseInt(tok.nextToken());
	}

	String getType() {
	    switch(type) {
	      case 'd':
		return "directory";
	      case 'p':
		return "fifo";
	      case 'l':
		return "symlink";
	      case 'b':
		return "block";
	      case 'c':
		return "character";
	      case 's':
		return "socket";
	      case '-':
	      default:
		return "file";
	    }
	}

	int getUserId() {
	    return uid;
	}

	int getGroupId() {
	    return gid;
	}

	boolean uRead() {
	    return permissions.charAt(0) == 'r';
	}

	boolean uWrite() {
	    return permissions.charAt(1) == 'w';
	}

	boolean uExec() {
	    return permissions.charAt(2) != '-';
	}

	boolean sUid() {
	    return permissions.charAt(2) == 's';
	}

	boolean gRead() {
	    return permissions.charAt(3) == 'r';
	}

	boolean gWrite() {
	    return permissions.charAt(4) == 'w';
	}

	boolean gExec() {
	    return permissions.charAt(5) != '-';
	}

	boolean sGid() {
	    return permissions.charAt(5) == 's';
	}

	boolean oRead() {
	    return permissions.charAt(6) == 'r';
	}

	boolean oWrite() {
	    return permissions.charAt(7) == 'w';
	}

	boolean oExec() {
	    return permissions.charAt(8) != '-';
	}

	boolean sticky() {
	    return permissions.charAt(8) == 't';
	}

	boolean hasExtendedAcl() {
	    return hasExtendedAcl;
	}
    }
}
