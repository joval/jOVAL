// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io;

import java.io.IOException;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Pattern;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.util.tree.INode;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * The abstract base class for all IFile implementations.  This class implements the INode portion of the IFile interface,
 * by relying on a full implementation of the methods unique to IFiles.
 *
 * Subclasses need only implement the getCanonicalPath method of INode (in addition to all the non-inherited methods of IFile).
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class BaseFile implements IFile {
    protected IFilesystem fs;

    protected BaseFile(IFilesystem fs) {
	this.fs = fs;
    }

    // Implement INode

    public abstract String getCanonicalPath();

    public Type getType() {
	try {
	    if (isLink()) {
		return Type.LINK;
	    } else if (isDirectory()) {
		return Type.BRANCH;
	    }
	} catch (IOException e) {
	    JOVALSystem.getLogger().warn(JOVALMsg.ERROR_IO, toString(), e.getMessage());
	    JOVALSystem.getLogger().debug(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return Type.LEAF;
    }

    public INode getChild(String name) throws NoSuchElementException, UnsupportedOperationException {
	for (INode child : getChildren()) {
	    if (child.getName().equals(name)) {
		return child;
	    }
	}
	throw new NoSuchElementException(name);
    }

    public Collection<INode> getChildren() throws NoSuchElementException, UnsupportedOperationException {
	return getChildren(null);
    }

    public Collection<INode> getChildren(Pattern p) throws NoSuchElementException, UnsupportedOperationException {
	try {
	    if (!isDirectory()) {
		throw new UnsupportedOperationException(getPath());
	    }
	    Collection<INode> children = new Vector<INode>();
	    IFile[] files = listFiles();
	    for (int i=0; i < files.length; i++) {
		if (p == null || p.matcher(files[i].getName()).find()) {
		    children.add(files[i]);
		}
	    }
	    return children;
	} catch (IOException e) {
	    JOVALSystem.getLogger().warn(JOVALMsg.ERROR_IO, toString(), e.getMessage());
	    JOVALSystem.getLogger().debug(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	throw new UnsupportedOperationException(getPath());
    }

    public boolean hasChildren() throws UnsupportedOperationException {
	try {
	    if (isDirectory()) {
		String[] sa = list();
		if (sa == null) {
		    return false;
		} else {
		    return sa.length > 0;
		}
	    }
	} catch (IOException e) {
	    JOVALSystem.getLogger().warn(JOVALMsg.ERROR_IO, toString(), e.getMessage());
	    JOVALSystem.getLogger().debug(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	throw new UnsupportedOperationException(getPath());
    }

    public String getPath() {
	return getLocalName();
    }
}
