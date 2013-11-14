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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.Message;
import jsaf.intf.io.IFile;
import jsaf.intf.io.IFileEx;
import jsaf.intf.system.IComputerSystem;
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
import jsaf.util.StringTools;

import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.MessageType;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.core.EntityObjectStringType;
import scap.oval.definitions.independent.EntityObjectHashTypeType;
import scap.oval.definitions.independent.FilehashObject;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.independent.FilehashItem;

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
    private int maxArgLength = -1;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IComputerSystem) {
	    try {
		baseInit((IComputerSystem)session);
		classes.add(FilehashObject.class);
	    } catch (UnsupportedOperationException e) {
		// doesn't support ISession.getFilesystem()
		notapplicable.add(FilehashObject.class);
	    }
	} else {
	    notapplicable.add(FilehashObject.class);
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
    protected Collection<FilehashItem> getItems(ObjectType obj, Collection<IFile> files, IRequestContext rc)
		throws CollectException {

	FilehashObject fObj = (FilehashObject)obj;

	//
	// Filter out any bad files
	//
	List<FilehashItem> items = new ArrayList<FilehashItem>();
	List<IFile> checkedFiles = new ArrayList<IFile>();
	for (IFile f : files) {
	    try {
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
		FilehashItem item = (FilehashItem)getBaseItem(obj, f);
		if (item != null) {
		    items.add(item);
		    checkedFiles.add(f);
		}
	    } catch (IllegalArgumentException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.INFO);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.STATUS_NOT_FILE, f.getPath(), e.getMessage())); 
		rc.addMessage(msg);
	    } catch (IOException e) {
		session.getLogger().warn(Message.ERROR_IO, f.getPath(), e.getMessage());
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		rc.addMessage(msg);
	    }
	}

	try {
	    IWindowsSession.View view = getView(fObj.getBehaviors());
	    int startIndex = 0;
	    List<IFile> sublist;
	    while((sublist = nextList(startIndex, checkedFiles)) != null) {
		List<String> md5s = computeChecksums(sublist, view, MD5);
		List<String> sha1s = computeChecksums(sublist, view, SHA1);
		for (int i=0; i < sublist.size(); i++) {
		    setItem(items.get(startIndex + i), md5s.get(i), sha1s.get(i));
		}
		startIndex += sublist.size();
	    }
	    return items;
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new CollectException(e.getMessage(), FlagEnumeration.ERROR);
	}
    }

    @Override
    protected List<InputStream> getPowershellModules() {
	return Arrays.asList(getClass().getResourceAsStream("Filehash.psm1"));
    }

    // Internal

    protected void setItem(FilehashItem item, String md5, String sha1) {
	EntityItemStringType md5Type = Factories.sc.core.createEntityItemStringType();
	if (md5.startsWith("BAD LINE:")) {
	    md5Type.setStatus(StatusEnumeration.ERROR);
	} else {
	    md5Type.setValue(md5);
	}
	item.setMd5(md5Type);

	EntityItemStringType sha1Type = Factories.sc.core.createEntityItemStringType();
	if (sha1.startsWith("BAD LINE:")) {
	    sha1Type.setStatus(StatusEnumeration.ERROR);
	} else {
	    sha1Type.setValue(sha1);
	}
	item.setSha1(sha1Type);
    }

    private static final int MD5	= 0;
    private static final int SHA1	= 1;

    /**
     * Compute checksums for a list of files. All the files must exist, and they must all be regular files, or this
     * routine will fail.
     *
     * @throws IllegalArgumentException if the file f is not a "regular" file; exception message is the file type.
     */
    private List<String> computeChecksums(List<IFile> files, IWindowsSession.View view, int algorithm) throws Exception {
	//
	// Build the input list
	//
	StringBuffer sb = new StringBuffer();
	if (session instanceof IWindowsSession) {
	    for (IFile f : files) {
		session.getLogger().info(JOVALMsg.STATUS_FILEHASH, algorithm == MD5 ? "md5" : "sha1", f.getPath());
		if (sb.length() > 0) {
		    sb.append(",");
		}
		sb.append("'").append(f.getPath()).append("'");
	    }
	} else {
	    for (IFile f : files) {
		session.getLogger().info(JOVALMsg.STATUS_FILEHASH, algorithm == MD5 ? "md5" : "sha1", f.getPath());
		if (sb.length() > 0) {
		    sb.append("\\n");
		}
		sb.append(f.getPath());
	    }
	}

	StringBuffer cmd = null;
	List<String> checksums = new ArrayList<String>();
	switch(session.getType()) {
	  case UNIX:
	    IUnixSession us = (IUnixSession)session;
	    switch(us.getFlavor()) {
	      case LINUX:
	      case MACOSX:
		cmd = new StringBuffer("printf \"").append(sb.toString()).append("\\n\"");
		cmd.append(" | xargs -I{} openssl dgst -hex");
		switch(algorithm) {
		  case MD5:
		    cmd.append(" -md5");
		    break;
		  case SHA1:
		    cmd.append(" -sha1");
		    break;
		}
		cmd.append(" '{}'");
		for (String line : SafeCLI.multiLine(cmd.toString(), session, IUnixSession.Timeout.M)) {
		    int ptr = line.indexOf("= ");
		    if (ptr > 0) {
			checksums.add(line.substring(ptr+2).trim());
		    } else if (line.length() > 0) {
			checksums.add("BAD LINE: " + line);
		    }
		}
		break;

	      case SOLARIS:
		cmd = new StringBuffer("/usr/bin/echo \"").append(sb.toString()).append("\"");
		cmd.append(" | xargs -I{} digest -a");
		switch(algorithm) {
		  case MD5:
		    cmd.append(" md5");
		    break;
		  case SHA1:
		    cmd.append(" sha1");
		    break;
		}
		cmd.append(" '{}'");
		for (String line : SafeCLI.multiLine(cmd.toString(), session, IUnixSession.Timeout.M)) {
		    if (line.length() > 0) {
			checksums.add(line);
		    }
		}
		break;

	      case AIX:
		cmd = new StringBuffer("/usr/bin/echo \"").append(sb.toString()).append("\"");
		cmd.append(" | xargs -I{} csum -h");
		switch(algorithm) {
		  case MD5:
		    cmd.append(" MD5");
		    break;
		  case SHA1:
		    cmd.append(" SHA1");
		    break;
		}
		cmd.append(" '{}'");
		Iterator<String> iter = SafeCLI.manyLines(cmd.toString(), null, us);
		while(iter.hasNext()) {
		    String line = iter.next();
		    StringTokenizer tok = new StringTokenizer(line);
		    if (tok.countTokens() == 2) {
			checksums.add(tok.nextToken());
		    } else if (line.length() > 0) {
			checksums.add("BAD LINE: " + line);
		    }
		}
		break;
	    }
	    break;

	  case WINDOWS:
	    cmd = new StringBuffer(sb.toString()).append(" | Get-FileHash -Algorithm");
	    switch(algorithm) {
	      case MD5:
		cmd.append(" MD5");
		break;
	      case SHA1:
		cmd.append(" SHA1");
		break;
	    }
	    cmd.append(" | Transfer-Encode");
	    byte[] buff = Base64.decode(getRunspace(view).invoke(cmd.toString()));
	    String data = new String(buff, StringTools.UTF8);
	    for (String line : data.split("\r\n")) {
		if (line.length() > 0) {
		    checksums.add(line);
		}
	    }
	    break;
	}
	if (checksums.size() != files.size()) {
	    session.getLogger().warn(JOVALMsg.WARNING_FILEHASH_LINES, checksums.size(), files.size());
	    throw new Exception(combine(checksums));
	}
	return checksums;
    }

    private String combine(List<String> lines) {
	StringBuffer sb = new StringBuffer();
	for (String line : lines) {
	    if (sb.length() > 0) {
		sb.append("\n");
	    }
	    sb.append(line);
	}
	return sb.toString();
    }

    /**
     * Determine the maximum size of a (Unix) command-line.
     */
    private int getMaxArgLength() throws Exception {
	if (maxArgLength == -1 && session.getType() == ISession.Type.UNIX) {
	    int envSize = Integer.parseInt(SafeCLI.exec("env | wc -c", session, IUnixSession.Timeout.S).trim());
	    int maxSize = Integer.parseInt(SafeCLI.exec("getconf ARG_MAX", session, IUnixSession.Timeout.S).trim());
	    maxArgLength = maxSize - envSize;
	}
	return maxArgLength;
    }

    /**
     * Get the next sublist of files whose single-pass command will fit inside maxArgLength.
     */
    private List<IFile> nextList(int start, List<IFile> master) throws Exception {
	if (start >= master.size()) {
	    return null;
	}

	int maxBytes = getMaxArgLength() - 50; // leave 50 bytes for other command chars
	if (maxBytes > 0) {
	    int padding = 0;
	    switch(session.getType()) {
	      case UNIX:
		padding = 2; // escape-n
		break;
	      case WINDOWS:
		padding = 3; // quotes and comma
		break;
	    }
	    int end = start;
	    int len = 0;
	    for (int i=start; i < master.size(); i++) {
		int next = padding + master.get(i).getPath().getBytes(StringTools.UTF8).length;
		if ((len + next) > maxBytes) {
		    return master.subList(start, end);
		}
		len += next;
		end++;
	    }
	}
	return master.subList(start, master.size());
    }
}
