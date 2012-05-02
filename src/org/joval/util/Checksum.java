// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Simple utility for computing checksums.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Checksum {
    public enum Algorithm {
	MD5("MD5"),
	SHA1("SHA-1");

	String value;

	Algorithm(String value) {
	    this.value = value;
	}

	String value() {
	    return value;
	}
    }

    public static String getChecksum(File f, Algorithm algorithm) throws IOException {
	InputStream in = null;
	try {
	    in = new FileInputStream(f);
	    return getChecksum(in, algorithm);
	} finally {
	    try {
		if (in != null) {
		    in.close();
		}
	    } catch (IOException e) {
	    }
	}
    }

    public static String getChecksum(String data, Algorithm algorithm) {
	try {
	    return getChecksum(new ByteArrayInputStream(data.getBytes()), algorithm);
	} catch (IOException e) {
	    throw new RuntimeException(e);
	}
    }

    public static String getChecksum(InputStream in, Algorithm algorithm) throws IOException {
        byte[] buff = createChecksum(in, algorithm);
        String str = "";
        for (int i=0; i < buff.length; i++) {
          str += Integer.toString((buff[i]&0xff) + 0x100, 16).substring(1);
        }
        return str;
    }

    public static byte[] createChecksum(InputStream in, Algorithm algorithm) throws IOException {
	try {
            byte[] buff = new byte[512];
            MessageDigest digest = MessageDigest.getInstance(algorithm.value());
            int len = 0;
            while ((len = in.read(buff)) > 0) {
        	digest.update(buff, 0, len);
            }
            in.close();
            return digest.digest();
	} catch (NoSuchAlgorithmException e) {
	    throw new IOException(e.getMessage());
	}
    }
}
