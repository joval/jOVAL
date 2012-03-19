// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.unix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.MatchResult;
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
import oval.schemas.systemcharacteristics.unix.FileItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFileEx;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.ISession;
import org.joval.intf.unix.io.IUnixFileInfo;
import org.joval.intf.unix.io.IUnixFilesystem;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.io.StreamTool;
import org.joval.oval.CollectException;
import org.joval.oval.OvalException;
import org.joval.oval.TestException;
import org.joval.oval.adapter.independent.BaseFileAdapter;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Evaluates UNIX File OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class FileAdapter extends BaseFileAdapter {
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

    protected Object convertFilename(EntityItemStringType filename) {
	return JOVALSystem.factories.sc.unix.createFileItemFilename(filename);
    }

    protected ItemType createFileItem() {
	return JOVALSystem.factories.sc.unix.createFileItem();
    }

    protected Collection<JAXBElement<? extends ItemType>> getItems(ItemType base, IFile f, IRequestContext rc)
		throws CollectException, IOException, OvalException {

	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
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
    private void setItem(FileItem item, IFile f) throws IOException, CollectException {
	IFileEx info = f.getExtended();
	IUnixFileInfo ufi = null;
	if (info instanceof IUnixFileInfo) {
	    ufi = (IUnixFileInfo)info;
	} else {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNIX_FILE, f.getClass().getName());
	    throw new CollectException(msg, FlagEnumeration.NOT_APPLICABLE);
	}
	session.getLogger().trace(JOVALMsg.STATUS_UNIX_FILE, f.getPath());
	EntityItemIntType aTime = JOVALSystem.factories.sc.core.createEntityItemIntType();
	aTime.setDatatype(SimpleDatatypeEnumeration.INT.value());
	long at = f.accessTime();
	if (at == IFile.UNKNOWN_TIME) {
	    aTime.setStatus(StatusEnumeration.NOT_COLLECTED);
	} else {
	    aTime.setValue(Long.toString(at/1000L));
	}
	item.setATime(aTime);

	EntityItemIntType cTime = JOVALSystem.factories.sc.core.createEntityItemIntType();
	cTime.setDatatype(SimpleDatatypeEnumeration.INT.value());
	long ct = f.createTime();
	if (ct == IFile.UNKNOWN_TIME) {
	    cTime.setStatus(StatusEnumeration.NOT_COLLECTED);
	} else {
	    cTime.setValue(Long.toString(ct/1000L));
	}
	item.setCTime(cTime);

	EntityItemIntType mTime = JOVALSystem.factories.sc.core.createEntityItemIntType();
	mTime.setDatatype(SimpleDatatypeEnumeration.INT.value());
	long lm = f.lastModified();
	if (lm == IFile.UNKNOWN_TIME) {
	    mTime.setStatus(StatusEnumeration.NOT_COLLECTED);
	} else {
	    mTime.setValue(Long.toString(lm/1000L));
	}
	item.setMTime(mTime);

	EntityItemIntType sizeType = JOVALSystem.factories.sc.core.createEntityItemIntType();
	sizeType.setValue(Long.toString(f.length()));
	sizeType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	item.setSize(sizeType);

	EntityItemStringType type = JOVALSystem.factories.sc.core.createEntityItemStringType();
	type.setValue(ufi.getUnixFileType());
	item.setType(type);

	EntityItemIntType userId = JOVALSystem.factories.sc.core.createEntityItemIntType();
	userId.setValue(Integer.toString(ufi.getUserId()));
	userId.setDatatype(SimpleDatatypeEnumeration.INT.value());
	item.setUserId(userId);

	EntityItemIntType groupId = JOVALSystem.factories.sc.core.createEntityItemIntType();
	groupId.setValue(Integer.toString(ufi.getGroupId()));
	groupId.setDatatype(SimpleDatatypeEnumeration.INT.value());
	item.setGroupId(groupId);

	EntityItemBoolType uRead = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	uRead.setValue(Boolean.toString(ufi.uRead()));
	uRead.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setUread(uRead);

	EntityItemBoolType uWrite = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	uWrite.setValue(Boolean.toString(ufi.uWrite()));
	uWrite.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setUwrite(uWrite);

	EntityItemBoolType uExec = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	uExec.setValue(Boolean.toString(ufi.uExec()));
	uExec.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setUexec(uExec);

	EntityItemBoolType sUid = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	sUid.setValue(Boolean.toString(ufi.sUid()));
	sUid.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setSuid(sUid);

	EntityItemBoolType gRead = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	gRead.setValue(Boolean.toString(ufi.gRead()));
	gRead.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setGread(gRead);

	EntityItemBoolType gWrite = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	gWrite.setValue(Boolean.toString(ufi.gWrite()));
	gWrite.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setGwrite(gWrite);

	EntityItemBoolType gExec = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	gExec.setValue(Boolean.toString(ufi.gExec()));
	gExec.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setGexec(gExec);

	EntityItemBoolType sGid = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	sGid.setValue(Boolean.toString(ufi.sGid()));
	sGid.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setSgid(sGid);

	EntityItemBoolType oRead = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	oRead.setValue(Boolean.toString(ufi.oRead()));
	oRead.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setOread(oRead);

	EntityItemBoolType oWrite = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	oWrite.setValue(Boolean.toString(ufi.oWrite()));
	oWrite.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setOwrite(oWrite);

	EntityItemBoolType oExec = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	oExec.setValue(Boolean.toString(ufi.oExec()));
	oExec.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setOexec(oExec);

	EntityItemBoolType sticky = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	sticky.setValue(Boolean.toString(ufi.sticky()));
	sticky.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setSticky(sticky);

	EntityItemBoolType aclType = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	aclType.setValue(Boolean.toString(ufi.hasExtendedAcl()));
	aclType.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	item.setHasExtendedAcl(aclType);
    }
}
