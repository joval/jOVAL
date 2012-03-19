// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.pe;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IRandomAccess;
import org.joval.intf.util.ILoggable;

import org.joval.intf.util.tree.INode;
import org.joval.os.windows.pe.resource.ImageResourceDataEntry;
import org.joval.os.windows.pe.resource.ImageResourceDirectory;
import org.joval.os.windows.pe.resource.ImageResourceDirectoryEntry;
import org.joval.os.windows.pe.resource.Types;
import org.joval.os.windows.pe.resource.version.StringFileInfo;
import org.joval.os.windows.pe.resource.version.StringStructure;
import org.joval.os.windows.pe.resource.version.StringTable;
import org.joval.os.windows.pe.resource.version.Var;
import org.joval.os.windows.pe.resource.version.VarFileInfo;
import org.joval.os.windows.pe.resource.version.VsFixedFileInfo;
import org.joval.os.windows.pe.resource.version.VsVersionInfo;
import org.joval.util.JOVALMsg;
import org.joval.util.tree.TreeHash;

/**
 * Provides simple access to PE file header information.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Header implements ILoggable {
    private LocLogger logger;
    private TreeHash<Object> resources;
    private ImageDOSHeader dosHeader;
    private ImageNTHeaders ntHeader;
    private VsVersionInfo versionInfo;
    private VarFileInfo varFileInfo;

    public Header(IFile file, LocLogger logger) throws IllegalArgumentException, IOException {
	resources = new TreeHash<Object>("ImageResourceDirs", "/");
	this.logger = logger;
	if (file.isFile()) {
	    if (file.length() == 0) {
		throw new IllegalArgumentException("Zero length: " + file.getPath());
	    }

	    IRandomAccess ra = null;
	    try {
		ra = file.getRandomAccess("r");
		dosHeader = new ImageDOSHeader(ra);
		ra.seek((long)dosHeader.getELFHeaderRVA());
		ntHeader = new ImageNTHeaders(ra);
		long rba = ntHeader.getResourceBaseAddress(ImageDataDirectory.RESOURCE_TABLE);
		if (rba == 0) {
//
// DAS: there is no resource section in the PE file
//
		} else {
		    ra.seek(rba);
		    ImageResourceDirectory root = new ImageResourceDirectory(ra);
		    long rva = ntHeader.getImageDirEntryRVA(ImageDataDirectory.RESOURCE_TABLE);
		    traverse("", root, ra, rba, rva);
		}
	    } finally {
		if (ra != null) {
		    try {
		        ra.close();
		    } catch (IOException e) {
			logger.warn(JOVALMsg.ERROR_FILE_STREAM_CLOSE, file.getPath());
			logger.error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		    }
		}
	    }
	} else {
	    throw new IllegalArgumentException("Not a file: " + file.getPath());
	}
    }

    public ImageDOSHeader getDOSHeader() {
	return dosHeader;
    }

    public ImageNTHeaders getNTHeader() {
	return ntHeader;
    }

    public VsVersionInfo getVersionInfo() {
	return versionInfo;
    }

    public Object getResource(String path) throws NoSuchElementException {
	return resources.getData(path);
    }

    public void debugPrint(PrintStream out) {
        dosHeader.debugPrint(out);
        ntHeader.debugPrint(out);
	for (INode node : resources.getRoot().getChildren()) {
	    debugPrint(0, node, out);
	}
    }

    // Implement ILoggable

    public LocLogger getLogger() {
	return logger;
    }

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    // Private

    private void debugPrint(int level, INode node, PrintStream out) {
	StringBuffer sb = new StringBuffer();
	for (int i=0; i < level; i++) {
	    sb.append("  ");
	}
	String indent = sb.toString();

	switch(node.getType()) {
	  case TREE:
	  case BRANCH:
	    out.print(indent);
	    out.println("Dir: " + node.getName());
	    for (INode child : node.getChildren()) {
		debugPrint(level + 1, child, out);
	    }
	    break;

	  case LEAF:
	    out.print(indent);
	    out.println("Node: " + node.getName() + " {");
	    Object obj = resources.getData(node.getPath());
	    if (obj instanceof VsVersionInfo) {
		((VsVersionInfo)obj).debugPrint(out, level + 1);
	    } else if (obj instanceof ImageResourceDataEntry) {
		((ImageResourceDataEntry)obj).debugPrint(out, level + 1);
	    } else {
		out.print(indent);
		out.println("  Unknown Node Datatype: " + obj.getClass().getName());
	    }
	    out.print(indent);
	    out.println("}");
	}
    }

    /**
     * Recursively load resources.  Currently the only resource type that is fully implemented is VS_VERSIONINFO.
     */
    private void traverse(String path, ImageResourceDirectory dir, IRandomAccess ra, long rba, long rva) throws IOException {
        ImageResourceDirectoryEntry[] entries = dir.getChildEntries();
        for (int i=0; i < entries.length; i++) {
            ImageResourceDirectoryEntry entry = entries[i];
            String name = entry.getName(ra, rba);
            if (entry.isDir()) {
                int type = entry.getType();
                if (path.length() == 0 && type >= 0 && type < Types.NAMES.length) {
                    name = Types.NAMES[type];
                }
                ra.seek(rba + entry.getOffset());
                traverse(path + name + "/", new ImageResourceDirectory(ra), ra, rba, rva);
            } else {
                ImageResourceDataEntry de = entry.getDataEntry(ra, rba, rva);
		if (path.startsWith(Types.NAMES[Types.RT_VERSION])) {
		    ra.seek(de.getDataAddress());
		    versionInfo = new VsVersionInfo(ra);
		    resources.putData(path + name, versionInfo);
		} else {
		    resources.putData(path + name, de);
		}
            }
        }
    }
}
