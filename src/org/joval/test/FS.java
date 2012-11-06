// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.ISession;
import org.joval.intf.util.ISearchable;
import org.joval.util.Checksum;

public class FS {
    private ISession session;

    public FS(IBaseSession session) {
	if (session instanceof ISession) {
	    this.session = (ISession)session;
	}
    }

    public synchronized void test(String path) {
	InputStream in = null;
	try {
//fs = ((org.joval.intf.windows.system.IWindowsSession)session).getFilesystem(org.joval.intf.windows.system.IWindowsSession.View._32BIT);
	    IFilesystem fs = session.getFilesystem();
	    IEnvironment env = session.getEnvironment();
	    if (path.startsWith("search:")) {
		path = path.substring(7);
		Collection<String> list = fs.getSearcher().search(Pattern.compile(path), ISearchable.FOLLOW_LINKS);
		System.out.println("Found " + list.size() + " matches");
		for (String item : list) {
		    System.out.println("Match: " + item);
		}
	    } else {
		String exp = env.expand(path);
		if (!path.equals(exp)) {
		    System.out.println(path + " -> " + exp);
		} else {
		    System.out.println(path);
		}
		IFile f = fs.getFile(exp);
		if (f.isLink()) {
		    System.out.println(path + " is a link to " + f.getCanonicalPath());
		}
		if (f.isDirectory()) {
		    String[] children = f.list();
		    for (int i=0; i < children.length; i++) {
			System.out.println(children[i]);
		    }
		} else if (f.isFile()) {
		    in = f.getInputStream();
		    String cs = Checksum.getChecksum(in, Checksum.Algorithm.MD5);
		    System.out.println("Path:  " + path);
		    System.out.println(" Size: " + f.length());
		    System.out.println("  md5: " + cs);
		}
		System.out.println("Canonical path: " + f.getCanonicalPath());
	    }
	} catch (PatternSyntaxException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	} finally {
	    try {
		if (in != null) {
		    in.close();
		}
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
    }
}
