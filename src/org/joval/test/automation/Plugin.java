// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.test.automation;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.joval.identity.SimpleCredentialStore;
import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.util.IProducer;
import org.joval.plugin.RemotePlugin;
import org.joval.util.JOVALSystem;
import org.joval.util.StringTools;

/**
 * A test class that runs jOVAL through all the relevant OVAL test content and generates a report.
 *
 * @author David A. Solin
 */
class Plugin extends RemotePlugin {
    static final SimpleCredentialStore SCS = new SimpleCredentialStore();
    static {
	setCredentialStore(SCS);
	try {
	    setDataDirectory(new File("."));
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    private IFilesystem fs;

    Plugin(Properties props) {
	super(props.getProperty(SimpleCredentialStore.PROP_HOSTNAME));
	SCS.add(props);
    }

    IBaseSession.Type getSessionType() {
	return session.getType();
    }

    IUnixSession.Flavor getSessionFlavor() {
	return ((IUnixSession)session).getFlavor();
    }

    String getHostname() {
	return hostname;
    }

    /**
     * Install the validation support files from the specified testDir to the target host machine.
     */
    void installSupportFiles(File testDir) throws IOException {
	File f = new File((testDir), "ValidationSupportFiles.zip");

	if (!f.exists()) {
	    System.out.println("Warning: no available validation support files to install");
	} else {
	    fs = session.getFilesystem();
	    ZipFile zip = new ZipFile(f, ZipFile.OPEN_READ);
    
	    IFile root = null;
	    switch(session.getType()) {
	      case WINDOWS:
		root = fs.getFile("C:\\ValidationSupportFiles\\");
		break;
    
	      case UNIX:
		root = fs.getFile("/tmp/ValidationSupportFiles");
		break;
    
	      default:
		throw new IOException("Unsupported type: " + session.getType());
	    }
    
	    Enumeration<? extends ZipEntry> entries = zip.entries();
	    while (entries.hasMoreElements()) {
		ZipEntry entry = entries.nextElement();
		StringBuffer sb = new StringBuffer(root.getPath());
		for (String s : StringTools.toList(StringTools.tokenize(entry.getName(), "/"))) {
		    if (sb.length() > 0) {
			sb.append(fs.getDelimiter());
		    }
		    sb.append(s);
		}
		String name = sb.toString();
		mkdir(root, entry);
		if (!entry.isDirectory()) {
		    IFile newFile = fs.getFile(name);
		    System.out.println("Installing file " + newFile.getLocalName());
		    byte[] buff = new byte[2048];
		    InputStream in = zip.getInputStream(entry);
		    OutputStream out = newFile.getOutputStream(false);
		    try {
			int len = 0;
			while((len = in.read(buff)) > 0) {
			    out.write(buff, 0, len);
			}
			in.close();
		    } finally {
			if (out != null) {
			    try {
				out.close();
			    } catch (IOException e) {
				e.printStackTrace();
			    }
			}
		    }
		}
	    }
	}
    }

    // Private

    private void mkdir(IFile dir, ZipEntry entry) throws IOException {
	if (!dir.exists() && !dir.mkdir()) {
	    throw new IOException("Failed to create " + dir.getLocalName());
	}

	String subdir = null;
	if (entry.isDirectory()) {
	    subdir = entry.getName();
	} else {
	    String s = entry.getName();
	    subdir = s.substring(0, s.lastIndexOf("/"));
	}

	for (String subdirName : StringTools.toList(StringTools.tokenize(subdir, "/"))) {
	    dir = fs.getFile(dir.getPath() + fs.getDelimiter() + subdirName);
	    if (!dir.exists()) {
		dir.mkdir();
	    }
	}
    }
}
