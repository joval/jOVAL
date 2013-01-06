// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.independent;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.EntityObjectStringType;
import oval.schemas.definitions.independent.EntityObjectHashTypeType;
import oval.schemas.definitions.independent.FilehashObject;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.independent.FilehashItem;
import oval.schemas.results.core.ResultEnumeration;

import jsaf.intf.io.IFile;
import jsaf.intf.io.IFileEx;
import jsaf.intf.system.IBaseSession;
import jsaf.intf.system.ISession;
import jsaf.intf.unix.io.IUnixFileInfo;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.intf.windows.io.IWindowsFileInfo;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.io.LittleEndian;
import jsaf.io.StreamTool;
import jsaf.util.Base64;
import jsaf.util.Checksum;
import jsaf.util.SafeCLI;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Collects items for filehash OVAL objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class FilehashAdapter extends BaseFileAdapter<FilehashItem> {
    private Map<String, String[]> checksumMap;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof ISession) {
	    super.init((ISession)session);
	    checksumMap = new HashMap<String, String[]>();
	    classes.add(FilehashObject.class);
	}
	return classes;
    }

    // Protected

    protected Class getItemClass() {
	return FilehashItem.class;
    }

    /**
     * Parse the file as specified by the Object, and decorate the Item.
     */
    protected Collection<FilehashItem> getItems(ObjectType obj, ItemType base, IFile f, IRequestContext rc)
		throws IOException, CollectException {

	FilehashObject fObj = (FilehashObject)obj;
	FilehashItem baseItem = (FilehashItem)base;
	try {
	    String[] checksums = computeChecksums(f, getView(fObj.getBehaviors()));
	    return Arrays.asList(getItem(baseItem, checksums[MD5], checksums[SHA1]));
	} catch (IllegalArgumentException e) {
	    session.getLogger().warn(JOVALMsg.STATUS_NOT_FILE, f.getPath(), e.getMessage()); 
	} catch (Exception e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(e.getMessage());
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}

	@SuppressWarnings("unchecked")
	Collection<FilehashItem> empty = (Collection<FilehashItem>)Collections.EMPTY_LIST;
	return empty;
    }

    @Override
    protected List<InputStream> getPowershellModules() {
	return Arrays.asList(getClass().getResourceAsStream("Filehash.psm1"));
    }

    // Internal

    protected FilehashItem getItem(FilehashItem baseItem, String md5, String sha1) {
	FilehashItem item = Factories.sc.independent.createFilehashItem();
	item.setPath(baseItem.getPath());
	item.setFilename(baseItem.getFilename());
	item.setFilepath(baseItem.getFilepath());
	item.setWindowsView(baseItem.getWindowsView());

	EntityItemStringType md5Type = Factories.sc.core.createEntityItemStringType();
	if (md5 == null) {
	    md5Type.setStatus(StatusEnumeration.ERROR);
	} else {
	    md5Type.setValue(md5);
	}
	item.setMd5(md5Type);

	EntityItemStringType sha1Type = Factories.sc.core.createEntityItemStringType();
	if (sha1 == null) {
	    sha1Type.setStatus(StatusEnumeration.ERROR);
	} else {
	    sha1Type.setValue(sha1);
	}
	item.setSha1(sha1Type);

	return item;
    }

    private static final int MD5	= 0;
    private static final int SHA1	= 1;

    private String[] computeChecksums(IFile f, IWindowsSession.View view) throws Exception {
	IFileEx ext = f.getExtended();
	if (ext instanceof IWindowsFileInfo) {
	    //
	    // Only IWindiwsFileInfo.FILE_TYPE_DISK gets through
	    //
	    switch(((IWindowsFileInfo)ext).getWindowsFileType()) {
	      case IWindowsFileInfo.FILE_TYPE_UNKNOWN:
		throw new IllegalArgumentException("unknown");
	      case IWindowsFileInfo.FILE_TYPE_CHAR:
		throw new IllegalArgumentException("char");
	      case IWindowsFileInfo.FILE_TYPE_PIPE:
		throw new IllegalArgumentException("pipe");
	      case IWindowsFileInfo.FILE_TYPE_REMOTE:
		throw new IllegalArgumentException("remote");
	      case IWindowsFileInfo.FILE_ATTRIBUTE_DIRECTORY:
		throw new IllegalArgumentException("directory");
	    }
	} else if (ext instanceof IUnixFileInfo) {
	    String type = ((IUnixFileInfo)ext).getUnixFileType();
	    if (!type.equals(IUnixFileInfo.FILE_TYPE_REGULAR)) {
		throw new IllegalArgumentException(type);
	    }
	}
	if (checksumMap.containsKey(f.getPath())) {
	    return checksumMap.get(f.getPath());
	}
	String[] checksums = new String[2];
	switch(session.getType()) {
	  case UNIX: {
	    IUnixSession us = (IUnixSession)session;
	    switch(us.getFlavor()) {
	      case LINUX:
	      case MACOSX: {
		String temp = SafeCLI.exec("openssl dgst -hex -md5 " + f.getPath(), session, IUnixSession.Timeout.M);
		int ptr = temp.indexOf("= ");
		if (ptr > 0) {
		    checksums[MD5] = temp.substring(ptr+2).trim();
		}
		temp = SafeCLI.exec("openssl dgst -hex -sha1 " + f.getPath(), session, IUnixSession.Timeout.M);
		ptr = temp.indexOf("= ");
		if (ptr > 0) {
		    checksums[SHA1] = temp.substring(ptr+2).trim();
		}
		break;
	      }

	      case SOLARIS: {
		checksums[MD5] = SafeCLI.exec("digest -a md5 " + f.getPath(), session, IUnixSession.Timeout.M);
		checksums[SHA1] = SafeCLI.exec("digest -a sha1 " + f.getPath(), session, IUnixSession.Timeout.M);
		break;
	      }

	      case AIX: {
		String temp = SafeCLI.exec("csum -h MD5 " + f.getPath(), session, IUnixSession.Timeout.M);
		StringTokenizer tok = new StringTokenizer(temp);
		if (tok.countTokens() == 2) {
		    checksums[MD5] = tok.nextToken();
		}
		temp = SafeCLI.exec("csum -h SHA1 " + f.getPath(), session, IUnixSession.Timeout.M);
		tok = new StringTokenizer(temp);
		if (tok.countTokens() == 2) {
		    checksums[SHA1] = tok.nextToken();
		}
		break;
	      }
	    }
	    break;
	  }

	  case WINDOWS: {
	    String encoded = getRunspace(view).invoke("Get-FileHash -Algorithm MD5 -Path \"" + f.getPath() + "\"");
	    checksums[MD5] = LittleEndian.toHexString(Base64.decode(encoded));
	    encoded = getRunspace(view).invoke("Get-FileHash -Algorithm SHA1 -Path \"" + f.getPath() + "\"");
	    checksums[SHA1] = LittleEndian.toHexString(Base64.decode(encoded));
	    break;
	  }
	}
	checksumMap.put(f.getPath(), checksums);
	return checksums;
    }
}
