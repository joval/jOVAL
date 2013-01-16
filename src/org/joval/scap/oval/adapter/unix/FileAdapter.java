// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.unix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.MatchResult;

import jsaf.intf.io.IFile;
import jsaf.intf.io.IFileEx;
import jsaf.intf.system.IBaseSession;
import jsaf.intf.system.ISession;
import jsaf.intf.unix.io.IUnixFileInfo;
import jsaf.intf.unix.io.IUnixFilesystem;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.io.StreamTool;

import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.unix.FileObject;
import scap.oval.systemcharacteristics.core.EntityItemBoolType;
import scap.oval.systemcharacteristics.core.EntityItemIntType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.unix.FileItem;

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
public class FileAdapter extends BaseFileAdapter<FileItem> {
    private IUnixSession us;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IUnixSession) {
	    super.init((ISession)session);
	    us = (IUnixSession)session;
	    classes.add(FileObject.class);
	}
	return classes;
    }

    // Protected

    protected Class getItemClass() {
	return FileItem.class;
    }

    protected Collection<FileItem> getItems(ObjectType obj, ItemType base, IFile f, IRequestContext rc)
		throws CollectException, IOException {

	if (base instanceof FileItem) {
	    return Arrays.asList(setItem((FileItem)base, f));
	} else {
	    String message = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_ITEM, base.getClass().getName());
	    throw new CollectException(message, FlagEnumeration.ERROR);
	}
    }

    // Private

    /**
     * Decorate the Item with information about the file.
     */
    private FileItem setItem(FileItem item, IFile f) throws IOException, CollectException {
	IFileEx info = f.getExtended();
	IUnixFileInfo ufi = null;
	if (info instanceof IUnixFileInfo) {
	    ufi = (IUnixFileInfo)info;
	} else {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNIX_FILE, f.getClass().getName());
	    throw new CollectException(msg, FlagEnumeration.NOT_APPLICABLE);
	}
	session.getLogger().trace(JOVALMsg.STATUS_UNIX_FILE, f.getPath());
	EntityItemIntType aTime = Factories.sc.core.createEntityItemIntType();
	aTime.setDatatype(SimpleDatatypeEnumeration.INT.value());
	long at = f.accessTime();
	if (at == IFile.UNKNOWN_TIME) {
	    aTime.setStatus(StatusEnumeration.NOT_COLLECTED);
	} else {
	    aTime.setValue(Long.toString(at/1000L));
	}
	item.setATime(aTime);

	EntityItemIntType cTime = Factories.sc.core.createEntityItemIntType();
	cTime.setDatatype(SimpleDatatypeEnumeration.INT.value());
	long ct = f.createTime();
	if (ct == IFile.UNKNOWN_TIME) {
	    cTime.setStatus(StatusEnumeration.NOT_COLLECTED);
	} else {
	    cTime.setValue(Long.toString(ct/1000L));
	}
	item.setCTime(cTime);

	EntityItemIntType mTime = Factories.sc.core.createEntityItemIntType();
	mTime.setDatatype(SimpleDatatypeEnumeration.INT.value());
	long lm = f.lastModified();
	if (lm == IFile.UNKNOWN_TIME) {
	    mTime.setStatus(StatusEnumeration.NOT_COLLECTED);
	} else {
	    mTime.setValue(Long.toString(lm/1000L));
	}
	item.setMTime(mTime);

	EntityItemIntType sizeType = Factories.sc.core.createEntityItemIntType();
	sizeType.setValue(Long.toString(f.length()));
	sizeType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	item.setSize(sizeType);

	EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
	type.setValue(ufi.getUnixFileType());
	item.setType(type);

	EntityItemIntType userId = Factories.sc.core.createEntityItemIntType();
	userId.setValue(Integer.toString(ufi.getUserId()));
	userId.setDatatype(SimpleDatatypeEnumeration.INT.value());
	item.setUserId(userId);

	EntityItemIntType groupId = Factories.sc.core.createEntityItemIntType();
	groupId.setValue(Integer.toString(ufi.getGroupId()));
	groupId.setDatatype(SimpleDatatypeEnumeration.INT.value());
	item.setGroupId(groupId);

	EntityItemBoolType uRead = Factories.sc.core.createEntityItemBoolType();
	uRead.setValue(Boolean.toString(ufi.uRead()));
	uRead.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setUread(uRead);

	EntityItemBoolType uWrite = Factories.sc.core.createEntityItemBoolType();
	uWrite.setValue(Boolean.toString(ufi.uWrite()));
	uWrite.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setUwrite(uWrite);

	EntityItemBoolType uExec = Factories.sc.core.createEntityItemBoolType();
	uExec.setValue(Boolean.toString(ufi.uExec()));
	uExec.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setUexec(uExec);

	EntityItemBoolType sUid = Factories.sc.core.createEntityItemBoolType();
	sUid.setValue(Boolean.toString(ufi.sUid()));
	sUid.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setSuid(sUid);

	EntityItemBoolType gRead = Factories.sc.core.createEntityItemBoolType();
	gRead.setValue(Boolean.toString(ufi.gRead()));
	gRead.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setGread(gRead);

	EntityItemBoolType gWrite = Factories.sc.core.createEntityItemBoolType();
	gWrite.setValue(Boolean.toString(ufi.gWrite()));
	gWrite.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setGwrite(gWrite);

	EntityItemBoolType gExec = Factories.sc.core.createEntityItemBoolType();
	gExec.setValue(Boolean.toString(ufi.gExec()));
	gExec.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setGexec(gExec);

	EntityItemBoolType sGid = Factories.sc.core.createEntityItemBoolType();
	sGid.setValue(Boolean.toString(ufi.sGid()));
	sGid.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setSgid(sGid);

	EntityItemBoolType oRead = Factories.sc.core.createEntityItemBoolType();
	oRead.setValue(Boolean.toString(ufi.oRead()));
	oRead.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setOread(oRead);

	EntityItemBoolType oWrite = Factories.sc.core.createEntityItemBoolType();
	oWrite.setValue(Boolean.toString(ufi.oWrite()));
	oWrite.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setOwrite(oWrite);

	EntityItemBoolType oExec = Factories.sc.core.createEntityItemBoolType();
	oExec.setValue(Boolean.toString(ufi.oExec()));
	oExec.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setOexec(oExec);

	EntityItemBoolType sticky = Factories.sc.core.createEntityItemBoolType();
	sticky.setValue(Boolean.toString(ufi.sticky()));
	sticky.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setSticky(sticky);

	EntityItemBoolType aclType = Factories.sc.core.createEntityItemBoolType();
	aclType.setValue(Boolean.toString(ufi.hasExtendedAcl()));
	aclType.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setHasExtendedAcl(aclType);

	return item;
    }
}
