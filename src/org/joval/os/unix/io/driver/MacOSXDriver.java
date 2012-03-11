// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.io.driver;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IReader;
import org.joval.intf.unix.io.IUnixFileInfo;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.util.IProperty;
import org.joval.io.fs.FileInfo;
import org.joval.io.PerishableReader;
import org.joval.os.unix.io.IUnixFilesystemDriver;
import org.joval.os.unix.io.UnixFileInfo;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.SafeCLI;
import org.joval.util.StringTools;

/**
 * IUnixFilesystemDriver implementation for Mac OS X.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class MacOSXDriver implements IUnixFilesystemDriver {
    private IUnixSession us;
    private IProperty props;
    private LocLogger logger;

    public MacOSXDriver(IUnixSession us) {
	this.us = us;
	props = us.getProperties();
	logger = us.getLogger();
    }

    // Implement IUnixFilesystemDriver

    public List<String> listMounts(long timeout) throws Exception {
	List<String> fsTypeFilter = new Vector<String>();
	String filterStr = props.getProperty(IFilesystem.PROP_PRELOAD_FSTYPE_FILTER);
	if (filterStr != null) {
	    fsTypeFilter = StringTools.toList(StringTools.tokenize(filterStr, ":", true));
	}

	List<String> mounts = new Vector<String>();
	int lineNum = 0;
	StringBuffer command = new StringBuffer("df");
	int filterSize = fsTypeFilter.size();
	if (filterSize > 0) {
	    command.append(" -T no");
	    for (int i=0; i < filterSize; i++) {
		if (i > 0) {
		    command.append(",");
		}
		command.append(fsTypeFilter.get(i));
	    }
	}
	for (String line : SafeCLI.multiLine(command.toString(), us, timeout)) {
	    if (lineNum++ > 0) { // skip the first line
		StringTokenizer tok = new StringTokenizer(line);
		String mountPoint = null;
		while(tok.hasMoreTokens()) {
		    mountPoint = tok.nextToken();
		}
		if (mountPoint.startsWith(us.getFilesystem().getDelimiter())) {
		    logger.info(JOVALMsg.STATUS_FS_PRELOAD_MOUNT, mountPoint, "?");
		    mounts.add(mountPoint);
		}
	    }
	}
	return mounts;
    }

    public String getFindCommand() {
	return "find %MOUNT% -print0 -mount | xargs -0 " + getStatCommand();
    }

    public String getStatCommand() {
//return "stat -f %p,%u,%g,%z,%B,%a,%m,%N";
	return "ls -ldn";
    }

    public UnixFileInfo nextFileInfo(Iterator<String> lines) {
	String line = null;
	if (lines.hasNext()) {
	    line = lines.next();
	} else {
	    return null;
	}

	char unixType = line.charAt(0);
	String permissions = line.substring(1, 10);
	boolean hasExtendedAcl = false;
	if (line.charAt(10) == '+') {
	    hasExtendedAcl = true;
	}

	StringTokenizer tok = new StringTokenizer(line.substring(11));
	String linkCount = tok.nextToken();
	int uid = -1;
	try {
	    uid = Integer.parseInt(tok.nextToken());
	} catch (NumberFormatException e) {
	    //DAS -- could be, e.g., 4294967294 (illegal "nobody" value)
	}
	int gid = -1;
	try {
	    gid = Integer.parseInt(tok.nextToken());
	} catch (NumberFormatException e) {
	    //DAS -- could be, e.g., 4294967294 (illegal "nobody" value)
	}

	FileInfo.Type type = FileInfo.Type.FILE;
	switch(unixType) {
	  case IUnixFileInfo.DIR_TYPE:
	    type = FileInfo.Type.DIRECTORY;
	    break;

	  case IUnixFileInfo.LINK_TYPE:
	    type = FileInfo.Type.LINK;
	    break;

	  case IUnixFileInfo.CHAR_TYPE:
	  case IUnixFileInfo.BLOCK_TYPE:
	    int ptr = -1;
	    if ((ptr = line.indexOf(",")) > 11) {
		tok = new StringTokenizer(line.substring(ptr+1));
	    }
	    break;
	}

	long length = 0;
	try {
	    length = Long.parseLong(tok.nextToken());
	} catch (NumberFormatException e) {
	}

	long mtime = FileInfo.UNKNOWN_TIME;
	String dateStr = tok.nextToken("/").trim();
	try {
	    if (dateStr.indexOf(":") == -1) {
		mtime = new SimpleDateFormat("MMM dd yyyy").parse(dateStr).getTime();
	    } else {
		mtime = new SimpleDateFormat("MMM dd HH:mm").parse(dateStr).getTime();
	    }
	} catch (ParseException e) {
	    e.printStackTrace();
	}

	String path = null, linkPath = null;
	int begin = line.indexOf(us.getFilesystem().getDelimiter());
	if (begin > 0) {
	    int end = line.indexOf("->");
	    if (end == -1) {
	        path = line.substring(begin).trim();
	    } else if (end > begin) {
	        path = line.substring(begin, end).trim();
		linkPath = line.substring(end+2).trim();
	    }
	}

	return new UnixFileInfo(FileInfo.UNKNOWN_TIME, mtime, FileInfo.UNKNOWN_TIME, type, length, linkPath,
				unixType, permissions, uid, gid, hasExtendedAcl, path);
    }
}
