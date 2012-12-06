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
import java.security.Provider;

/**
 * Simple utility for computing checksums.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Checksum {
    public enum Algorithm {
	MD5("MD5"),
	SHA1("SHA-1"),
	SHA224("SHA-224"),
	SHA256("SHA-256"),
	SHA384("SHA-384"),
	SHA512("SHA-512");

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

    public static String getChecksum(byte[] buff, Algorithm algorithm) {
        byte[] cs = createChecksum(buff, algorithm);
        String str = "";
        for (int i=0; i < cs.length; i++) {
          str += Integer.toString((cs[i]&0xff) + 0x100, 16).substring(1);
        }
        return str;
    }

    public static byte[] createChecksum(byte[] buff, Algorithm algorithm) {
	MessageDigest digest = getDigest(algorithm);
       	digest.update(buff, 0, buff.length);
        return digest.digest();
    }

    public static byte[] createChecksum(InputStream in, Algorithm algorithm) throws IOException {
	MessageDigest digest = getDigest(algorithm);
        byte[] buff = new byte[512];
        int len = 0;
        while ((len = in.read(buff)) > 0) {
       	    digest.update(buff, 0, len);
        }
        in.close();
        return digest.digest();
    }

    // Private

    /**
     * Use the BouncyCastle JCE implementation for algorithms not supplied by the default JCE (e.g., SHA-224).
     */
    private static final String ALT_PROVIDER_NAME = "org.bouncycastle.jce.provider.BouncyCastleProvider";
    private static Provider ALT_PROVIDER;

    private static MessageDigest getDigest(Algorithm algorithm) {
        MessageDigest digest = null;
	try {
            digest = MessageDigest.getInstance(algorithm.value());
	} catch (NoSuchAlgorithmException e) {
	    if (ALT_PROVIDER == null) {
		//
		// Use introspection to load the alternate provider, so as to make the dependency optional.
		//
		try {
		    ALT_PROVIDER = (Provider)Class.forName(ALT_PROVIDER_NAME).newInstance();
		} catch (Exception e2) {
		    throw new RuntimeException(e);
		}
	    }
	    try {
        	digest = MessageDigest.getInstance(algorithm.value(), ALT_PROVIDER);
	    } catch (NoSuchAlgorithmException e2) {
		throw new RuntimeException(e);
	    }
	}
	return digest;
    }
}
