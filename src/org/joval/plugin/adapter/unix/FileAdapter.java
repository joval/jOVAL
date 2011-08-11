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
import org.joval.oval.TestException;
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

    public FileAdapter(IUnixSession session, IFilesystem fs) {
	super(fs);
	this.session = session;
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

    public ResultEnumeration compare(StateType st, ItemType it) throws TestException, OvalException {
	FileState state = (FileState)st;
	FileItem item = (FileItem)it;

	if (state.isSetFilepath()) {
	    ResultEnumeration result = ctx.test(state.getFilepath(), item.getFilepath());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetPath()) {
	    ResultEnumeration result = ctx.test(state.getPath(), item.getPath());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetFilename()) {
	    ResultEnumeration result = ctx.test(state.getFilename(), item.getFilename().getValue());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetType()) {
	    ResultEnumeration result = ctx.test(state.getType(), item.getType());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetUread()) {
	    ResultEnumeration result = ctx.test(state.getUread(), item.getUread());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetUwrite()) {
	    ResultEnumeration result = ctx.test(state.getUwrite(), item.getUwrite());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetUexec()) {
	    ResultEnumeration result = ctx.test(state.getUexec(), item.getUexec());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetSuid()) {
	    ResultEnumeration result = ctx.test(state.getSuid(), item.getSuid());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetUserId()) {
	    ResultEnumeration result = ctx.test(state.getUserId(), item.getUserId());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetGread()) {
	    ResultEnumeration result = ctx.test(state.getGread(), item.getGread());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetGwrite()) {
	    ResultEnumeration result = ctx.test(state.getGwrite(), item.getGwrite());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetGexec()) {
	    ResultEnumeration result = ctx.test(state.getGexec(), item.getGexec());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetSgid()) {
	    ResultEnumeration result = ctx.test(state.getSgid(), item.getSgid());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetGroupId()) {
	    ResultEnumeration result = ctx.test(state.getGroupId(), item.getGroupId());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetOread()) {
	    ResultEnumeration result = ctx.test(state.getOread(), item.getOread());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetOwrite()) {
	    ResultEnumeration result = ctx.test(state.getOwrite(), item.getOwrite());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetOexec()) {
	    ResultEnumeration result = ctx.test(state.getOexec(), item.getOexec());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetSticky()) {
	    ResultEnumeration result = ctx.test(state.getSticky(), item.getSticky());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetHasExtendedAcl()) {
	    ResultEnumeration result = ctx.test(state.getHasExtendedAcl(), item.getHasExtendedAcl());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetCTime()) {
	    ResultEnumeration result = ctx.test(state.getCTime(), item.getCTime());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetATime()) {
	    ResultEnumeration result = ctx.test(state.getATime(), item.getATime());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.isSetMTime()) {
	    ResultEnumeration result = ctx.test(state.getMTime(), item.getMTime());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	return ResultEnumeration.TRUE;
    }

    // Protected

    protected Object convertFilename(EntityItemStringType filename) {
	return JOVALSystem.factories.sc.unix.createFileItemFilename(filename);
    }

    protected ItemType createFileItem() {
	return JOVALSystem.factories.sc.unix.createFileItem();
    }

    protected List<JAXBElement<? extends ItemType>>
	getItems(ItemType base, ObjectType obj, IFile f, List<VariableValueType> vars) throws IOException, OvalException {

	List<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	if (base instanceof FileItem) {
	    setItem((FileItem)base, f);
	    items.add(JOVALSystem.factories.sc.unix.createFileItem((FileItem)base));
	}
	return items;
    }

    // Private

    /**
     * Decorate the Item with information about the file.
     */
    private void setItem(FileItem item, IFile file) throws IOException {
	EntityItemIntType aTime = JOVALSystem.factories.sc.core.createEntityItemIntType();
	aTime.setValue(Long.toString(file.accessTime()/1000L));
	aTime.setDatatype(SimpleDatatypeEnumeration.INT.value());
	item.setATime(aTime);

	EntityItemIntType cTime = JOVALSystem.factories.sc.core.createEntityItemIntType();
	cTime.setStatus(StatusEnumeration.NOT_COLLECTED);
	cTime.setDatatype(SimpleDatatypeEnumeration.INT.value());
	item.setCTime(cTime);

	EntityItemIntType mTime = JOVALSystem.factories.sc.core.createEntityItemIntType();
	mTime.setValue(Long.toString(file.lastModified()/1000L));
	mTime.setDatatype(SimpleDatatypeEnumeration.INT.value());
	item.setMTime(mTime);

	try {
	    Lstat ls = new Lstat(file.getLocalName());

	    EntityItemStringType type = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    type.setValue(ls.getType());
	    item.setType(type);

	    EntityItemIntType userId = JOVALSystem.factories.sc.core.createEntityItemIntType();
	    userId.setValue(Integer.toString(ls.getUserId()));
	    userId.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    item.setUserId(userId);

	    EntityItemIntType groupId = JOVALSystem.factories.sc.core.createEntityItemIntType();
	    groupId.setValue(Integer.toString(ls.getGroupId()));
	    groupId.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    item.setGroupId(groupId);

	    EntityItemBoolType uRead = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	    uRead.setValue(Boolean.toString(ls.uRead()));
	    uRead.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    item.setUread(uRead);

	    EntityItemBoolType uWrite = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	    uWrite.setValue(Boolean.toString(ls.uWrite()));
	    uWrite.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    item.setUwrite(uWrite);

	    EntityItemBoolType uExec = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	    uExec.setValue(Boolean.toString(ls.uExec()));
	    uExec.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    item.setUexec(uExec);

	    EntityItemBoolType sUid = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	    sUid.setValue(Boolean.toString(ls.sUid()));
	    sUid.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    item.setSuid(sUid);

	    EntityItemBoolType gRead = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	    gRead.setValue(Boolean.toString(ls.gRead()));
	    gRead.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    item.setGread(gRead);

	    EntityItemBoolType gWrite = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	    gWrite.setValue(Boolean.toString(ls.gWrite()));
	    gWrite.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    item.setGwrite(gWrite);

	    EntityItemBoolType gExec = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	    gExec.setValue(Boolean.toString(ls.gExec()));
	    gExec.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    item.setGexec(gExec);

	    EntityItemBoolType sGid = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	    sGid.setValue(Boolean.toString(ls.sGid()));
	    sGid.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    item.setSgid(sGid);

	    EntityItemBoolType oRead = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	    oRead.setValue(Boolean.toString(ls.oRead()));
	    oRead.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    item.setOread(oRead);

	    EntityItemBoolType oWrite = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	    oWrite.setValue(Boolean.toString(ls.oWrite()));
	    oWrite.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    item.setOwrite(oWrite);

	    EntityItemBoolType oExec = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	    oExec.setValue(Boolean.toString(ls.oExec()));
	    oExec.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    item.setOexec(oExec);

	    EntityItemBoolType sticky = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	    sticky.setValue(Boolean.toString(ls.sticky()));
	    sticky.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    item.setSticky(sticky);

	    EntityItemBoolType aclType = JOVALSystem.factories.sc.core.createEntityItemBoolType();
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
