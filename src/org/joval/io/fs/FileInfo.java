// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io.fs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Pattern;

import org.joval.intf.io.IFileEx;
import org.joval.intf.util.tree.INode;
import org.joval.util.CachingHierarchy;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.StringTools;

/**
 * A FileInfo object contains information about a file. It can be constructed from a FileAccessor, or directly from
 * data gathered through other means (i.e., cached data).  Subclasses are used to store platform-specific file
 * information.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class FileInfo implements IFileEx {
    /**
     * Resolve an absolute path from a relative path from a base file path, given a delimiter.
     */
    public static String resolvePath(String origin, String delimiter, String rel) throws UnsupportedOperationException {
	if (rel.startsWith(delimiter)) {
	    return rel;
	} else {
	    Stack<String> stack = new Stack<String>();
	    for (String s : StringTools.toList(StringTools.tokenize(origin, delimiter))) {
		stack.push(s);
	    }
	    if (!stack.empty()) {
		stack.pop();
	    }
	    for (String next : StringTools.toList(StringTools.tokenize(rel, delimiter))) {
		if (next.equals(".")) {
		    // stay in the same place
		} else if (next.equals("..")) {
		    if (stack.empty()) {
			// links above root stay at root
		    } else {
			stack.pop();
		    }
		} else {
		    stack.push(next);
		}
	    }
	    StringBuffer path = new StringBuffer();
	    while(!stack.empty()) {
		StringBuffer elt = new StringBuffer(delimiter);
		path.insert(0, elt.append(stack.pop()).toString());
	    }
	    if (path.length() == 0) {
		return delimiter;
	    } else {
		return path.toString();
	    }
	}
    }

    public enum Type {FILE, DIRECTORY, LINK;}

    public long ctime, mtime, atime, length;
    public Type type;
    public String canonicalPath;

    public FileInfo() {}

    public FileInfo(FileAccessor access, Type type) throws IOException {
	this.type = type;
	ctime = access.getCtime();
	mtime = access.getMtime();
	atime = access.getAtime();
	length = access.getLength();
	canonicalPath = access.getCanonicalPath();
    }

    public FileInfo(long ctime, long mtime, long atime, Type type, long length) {
	this(ctime, mtime, atime, type, length, null);
    }

    public FileInfo(long ctime, long mtime, long atime, Type type, long length, String linkTarget) {
	this.ctime = ctime;
	this.mtime = mtime;
	this.atime = atime;
	this.type = type;
	this.length = length;
	this.canonicalPath = linkTarget;
    }
}
