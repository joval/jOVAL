// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io;

import java.io.InputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

import org.joval.intf.io.IRandomAccess;

import org.joval.util.JOVALMsg;

/**
 * Utility class for reading/getting Little-Endian byte-ordered numbers from byte buffers and streams.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class LittleEndian {
    public static final String toHexString(byte b) {
	String s = Integer.toHexString(b & 0xFF);
	if (s.length() == 1) {
	    s = "0" + s;
	}
	return s;
    }

    public static final String toHexString(short s) {
	return Integer.toHexString(s & 0xFFFF);
    }

    public static final String toHexString(int i) {
	return Integer.toHexString(i);
    }

    public static final String toHexString(long l) {
	return Long.toHexString(l & 0xFFFFFFFFFFFFFFFFL);
    }

    public static final short getShort(byte[] buff) {
	return getShort(buff, 0);
    }

    public static final short getShort(byte[] buff, int offset) {
	return (short)((buff[offset] << 0) | (buff[offset + 1] << 8));
    }

    public static final short getUShort(byte[] buff) {
	return getUShort(buff, 0);
    }

    public static final short getUShort(byte[] buff, int offset) {
	return (short)((buff[offset] & 0xFF) | ((buff[offset + 1] & 0xFF) << 8));
    }

    /**
     * Read a signed 2-byte short.
     */
    public static final short readShort(InputStream in) throws IOException {
	byte[] buff = new byte[2];
	StreamTool.readFully(in, buff);
	return getShort(buff, 0);
    }

    /**
     * Read an unsigned 2-byte short.
     */
    public static final short readUShort(InputStream in) throws IOException {
	byte[] buff = new byte[2];
	StreamTool.readFully(in, buff);
	return getUShort(buff, 0);
    }

    public static final short readUShort(IRandomAccess ra) throws IOException {
	byte[] buff = new byte[2];
	ra.readFully(buff);
	return getUShort(buff, 0);
    }

    public static final int getInt(byte[] buff) {
	return getInt(buff, 0);
    }

    public static final int getInt(byte[] buff, int offset) {
	return buff[offset] | (buff[offset + 1] << 8) | (buff[offset + 2] << 16) | (buff[offset + 3] << 24);
    }

    public static final int getUInt(byte[] buff) {
	return getUInt(buff, 0);
    }

    public static final int getUInt(byte[] buff, int offset) {
	return  (buff[offset] & 0xFF)             |
		((buff[offset + 1] & 0xFF) << 8)  |
		((buff[offset + 2] & 0xFF) << 16) |
		((buff[offset + 3] & 0xFF) << 24);
    }

    /**
     * Read a signed 4-byte int (AKA DWORD).
     */
    public static final int readInt(InputStream in) throws IOException {
	byte[] buff = new byte[4];
	StreamTool.readFully(in, buff);
	return getInt(buff, 0);
    }

    /**
     * Read an unsigned 4-byte int (AKA DWORD).
     */
    public static final int readUInt(InputStream in) throws IOException {
	byte[] buff = new byte[4];
	StreamTool.readFully(in, buff);
	return getUInt(buff, 0);
    }

    public static final int readUInt(IRandomAccess ra) throws IOException {
	byte[] buff = new byte[4];
	ra.readFully(buff);
	return getUInt(buff, 0);
    }

    public static final long getLong(byte[] buff) {
	return getLong(buff, 0);
    }

    public static final long getLong(byte[] buff, int offset) {
	return (long)getInt(buff, offset) + (((long)getInt(buff, offset + 4)) << 32);
    }

    public static final long getULong(byte[] buff) {
	return getULong(buff, 0);
    }

    public static final long getULong(byte[] buff, int offset) {
	return (long)getUInt(buff, offset) + (((long)getUInt(buff, offset + 4)) << 32);
    }

    /**
     * Read a signed 8-byte Long.
     */
    public static final long readLong(InputStream in) throws IOException {
	byte[] buff = new byte[8];
	StreamTool.readFully(in, buff);
	return getLong(buff, 0);
    }

    /**
     * Read an unsigned 8-byte Long.
     */
    public static final long readULong(IRandomAccess ra) throws IOException {
	byte[] buff = new byte[8];
	ra.readFully(buff);
	return getULong(buff, 0);
    }

    /**
     * Read an unsigned 8-byte Long.
     */
    public static final long readULong(InputStream in) throws IOException {
	byte[] buff = new byte[8];
	StreamTool.readFully(in, buff);
	return getULong(buff, 0);
    }

    /**
     * Fetch a null-terminated UTF16LE String from an array.  If the length is unknown, pass in a -1 and
     * this method will find the length.  If offset+len exceeds the size of the buffer, this method will also
     * compute the correct length automatically.
     */
    public static final String getSzUTF16LEString(byte[] buff, int offset, int len) {
	try {
	    if (len == -1 || (offset+len) > buff.length) {
		len = 0;
		for (int i=offset; i < buff.length; ) {
		    byte b1 = buff[i++];
		    byte b2 = buff[i++];
		    if (b1 == 0 && b2 == 0) {
			break;
		    } else {
			len += 2;
		    }
		}
	    } else {
		//
		// Strip any trailing NULLs before constructing the string
		//
		for (int i=offset+len; i > (offset + 2); ) {
		    byte b2 = buff[--i];
		    byte b1 = buff[--i];
		    if (b1 == 0 && b2 == 0) {
			len -= 2;
		    }
		}
	    }
	    return new String(buff, offset, len, Charset.forName("UTF-16LE"));
	} catch (UnsupportedCharsetException e) {
	    e.printStackTrace();
	} catch (IllegalCharsetNameException e) {
	    e.printStackTrace();
	} catch (IllegalArgumentException e) {
	    e.printStackTrace();
	}
	return null;
    }

    /**
     * Read a null-terminated string.
     */
    public static final String readSzUTF16LEString(IRandomAccess ra) throws IOException {
	byte[] buff = new byte[512];
	int len = 0;
	while (true) {
	    byte b1 = (byte)ra.read();
	    if (b1 == -1) {
		throw new EOFException(JOVALMsg.getMessage(JOVALMsg.ERROR_EOF));
	    }
	    byte b2 = (byte)ra.read();
	    if (b1 == -1) {
		throw new EOFException(JOVALMsg.getMessage(JOVALMsg.ERROR_EOF));
	    }

	    if (b1 == 0 && b2 == 0) {
		break; // Reached the null!
	    } else if (len < buff.length) {
		buff[len++] = b1;
		buff[len++] = b2;
	    } else {
		byte[] buff2 = new byte[buff.length + 512];
		for (int i=0; i < buff.length; i++) {
		    buff2[i] = buff[i];
		}
		buff = buff2;
		buff[len++] = b1;
		buff[len++] = b2;
	    }
	}
	return getSzUTF16LEString(buff, 0, len);
    }

    /**
     * Get a byte[] padding to 32-bit align within the buffer from the offset.
     *
     * @arg fileOffset is the offset from the start of the file to the start of the buffer itself.
     * @arg offset is the offset within the buffer from which to start padding.
     */
    public static final byte[] get32BitAlignPadding(byte[] buff, int offset, int fileOffset) {
	int pos = fileOffset + offset;
	int mod = pos % 4;
	int paddingLen = 0;
	if (mod > 0) {
	    paddingLen = 4 - mod;
	}
	int maxlength = buff.length - offset;
	if (paddingLen > maxlength) {
	    paddingLen = maxlength;
	}
	byte[] buff2 = new byte[paddingLen];
	for (int i=0; i < paddingLen; i++) {
	    buff2[i] = buff[offset+i];
	}
	return buff2;
    }

    /**
     * Create a buffer and read into it in order to align the file pointer to a 32-bit alignment.
     */
    public static final byte[] read32BitAlignPadding(IRandomAccess ra) throws IOException {
	int pos = (int)ra.getFilePointer();
	int mod = pos % 4;
	int paddingLen = 0;
	if (mod > 0) {
	    paddingLen = 4 - mod;
	}
	byte[] buff = new byte[paddingLen];
	ra.readFully(buff);
	return buff;
    }
}
