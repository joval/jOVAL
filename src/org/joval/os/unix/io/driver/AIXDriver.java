// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.io.driver;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IReader;
import org.joval.intf.unix.io.IUnixFileInfo;
import org.joval.intf.unix.io.IUnixFilesystem;
import org.joval.intf.unix.io.IUnixFilesystemDriver;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.util.ISearchable;
import org.joval.io.AbstractFilesystem.FileInfo;
import org.joval.io.PerishableReader;
import org.joval.os.unix.io.UnixFilesystem.UnixFileInfo;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;
import org.joval.util.StringTools;

/**
 * IUnixFilesystemDriver implementation for AIX.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class AIXDriver extends AbstractDriver {
    public AIXDriver(IUnixSession session) {
	super(session);
    }

    // Implement IUnixFilesystemDriver

    public Collection<IFilesystem.IMount> getMounts(Pattern typeFilter) throws Exception {
	Collection<IFilesystem.IMount> mounts = new ArrayList<IFilesystem.IMount>();
	int lineNum = 0;
	for (String line : SafeCLI.multiLine("mount", session, IUnixSession.Timeout.S)) {
	    if (lineNum++ > 1) { // skip the first two lines
		int mpToken = 1;
		switch(line.charAt(0)) {
		  case ' ':
		  case '\t':
		    mpToken = 1; // mount-point is the second token
		    break;
		  default:
		    mpToken = 2; // mount-point is the third token
		    break;
		}
		StringTokenizer tok = new StringTokenizer(line);
		String mountPoint = null, fsType = null;
		for (int i=0; i <= mpToken; i++) {
		    mountPoint = tok.nextToken();
		}
		fsType = tok.nextToken();

		if (typeFilter != null && typeFilter.matcher(fsType).find()) {
		    logger.info(JOVALMsg.STATUS_FS_MOUNT_SKIP, mountPoint, fsType);
		} else if (mountPoint.startsWith(IUnixFilesystem.DELIM_STR)) {
		    logger.info(JOVALMsg.STATUS_FS_MOUNT_ADD, mountPoint, fsType);
		    mounts.add(new Mount(mountPoint, fsType));
		}
	    }
	}
	return mounts;
    }

    public String getFindCommand(String from, int depth, int flags, String pattern) {
	StringBuffer cmd = new StringBuffer("find");
	if (isSetFlag(ISearchable.FLAG_FOLLOW_LINKS, flags)) {
	    cmd.append(" -L");
	}
	cmd.append(" ").append(from);
	if (depth != ISearchable.DEPTH_UNLIMITED) {
	    cmd.append("\\( -type d -a -exec sh -c 'echo $1 | awk -F/ '\\''{print $(");
	    cmd.append(Integer.toString(depth));
	    cmd.append("+1)}'\\''|grep \"\\([^ ]\\)\" >/dev/null' {} {} \\; -prune -print \\)");
	}
	if (isSetFlag(ISearchable.FLAG_CONTAINERS, flags)) {
	    cmd.append(" -type d");
	}
	if (isSetFlag(IUnixFilesystem.FLAG_XDEV, flags)) {
	    cmd.append(" -xdev");
	}
	if (pattern != null) {
	    cmd.append(" | grep -E \"").append(pattern).append("\"");
	}
	cmd.append(" | xargs -i ").append(getStatCommand()).append(" '{}'");
	return cmd.toString();
    }

    public String getStatCommand() {
	return "ls -dn";
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

	long mtime = IFile.UNKNOWN_TIME;
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
	int begin = line.indexOf(session.getFilesystem().getDelimiter());
	if (begin > 0) {
	    int end = line.indexOf("->");
	    if (end == -1) {
		path = line.substring(begin).trim();
	    } else if (end > begin) {
		path = line.substring(begin, end).trim();
		linkPath = line.substring(end+2).trim();
	    }
	}

	return new UnixFileInfo(IFile.UNKNOWN_TIME, mtime, IFile.UNKNOWN_TIME, type, length, path, linkPath,
				unixType, permissions, uid, gid, hasExtendedAcl);
    }
}
