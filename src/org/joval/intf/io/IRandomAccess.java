// Copyright (c) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.io;

import java.io.IOException;

/**
 * A platform-independent interface providing random-access to a file.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IRandomAccess {
    /**
     * Read buff.length bytes.
     */
    public void readFully(byte[] buff) throws IOException;

    /**
     * Close the IRandomAccess and its underlying resources.
     */
    public void close() throws IOException;

    /**
     * Set the position of the file pointer.
     */
    public void seek(long pos) throws IOException;

    /**
     * Read a byte.
     */
    public int read() throws IOException;

    /**
     * Read into a buffer.  Doesn't necessarily fill buff; returns the number of bytes read.
     */
    public int read(byte[] buff) throws IOException;

    /**
     * Read at most len bytes into a buffer, starting at offset.
     */
    public int read(byte[] buff, int offset, int len) throws IOException;

    /**
     * Return the length of the underlying file.
     */
    public long length() throws IOException;

    /**
     * Get the current position in the IRandomAccess.
     */
    public long getFilePointer() throws IOException;
}
