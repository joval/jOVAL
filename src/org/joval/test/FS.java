// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.ISession;

public class FS {
    private ISession session;
    private IEnvironment env;
    private IFilesystem fs;

    public FS(ISession session) {
	this.session = session;
	fs = session.getFilesystem();
//	fs = ((org.joval.intf.windows.system.IWindowsSession)session).getFilesystem(org.joval.intf.windows.system.IWindowsSession.View._32BIT);
	env = session.getEnvironment();
    }

    public synchronized void test(String path) {
	InputStream in = null;
	try {
	    if (path.startsWith("search:")) {
		path = path.substring(7);
		Collection<String> list = fs.search(Pattern.compile(path), false);
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
		    in = fs.getInputStream(path);
		    String cs = getMD5Checksum(in);
		    System.out.println("Path:  " + path);
		    System.out.println(" Size: " + f.length());
		    System.out.println("  md5: " + cs);
		}
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

    public static String getMD5Checksum(InputStream in) throws IOException {
        byte[] buff = createChecksum(in);
        String str = "";
        for (int i=0; i < buff.length; i++) {
          str += Integer.toString((buff[i]&0xff) + 0x100, 16).substring(1);
        }
        return str;
    }

    public static byte[] createChecksum(InputStream in) throws IOException {
        try {
            byte[] buff = new byte[1024];
            MessageDigest digest = MessageDigest.getInstance("MD5");
            int len = 0;
            while ((len = in.read(buff)) > 0) {
                digest.update(buff, 0, len);
            }
            in.close();
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException (e.getMessage());
        }
    }

    private String getFileName(IFile f) {
	String path = f.getLocalName();
	int ptr = path.lastIndexOf(fs.getDelimiter());
	if (ptr > 0) {
	    return path.substring(ptr+1);
	} else {
	    return path;
	}
    }
}
