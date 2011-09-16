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

import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Simple utility for computing checksums.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Checksum {
    public static String getMD5Checksum(File f) throws IOException {
	InputStream in = null;
	try {
	    in = new FileInputStream(f);
	    return getMD5Checksum(in);
	} finally {
	    try {
		if (in != null) {
		    in.close();
		}
	    } catch (IOException e) {
		JOVALSystem.getLogger().warn(JOVALMsg.ERROR_FILE_CLOSE, f.toString());
		JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
    }

    public static String getMD5Checksum(String data) throws IOException {
	return getMD5Checksum(new ByteArrayInputStream(data.getBytes()));
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
	    throw new IOException(e.getMessage());
	}
    }
}
