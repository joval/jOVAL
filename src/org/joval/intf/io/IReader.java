// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.io;

import java.io.IOException;

/**
 * A convenience interface for reading data.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IReader {
    /**
     * Read a byte.
     */
    int read() throws IOException;

    /**
     * Read a line of text, or read up until the end of the underlying stream.  Returns null if the stream is closed
     * or the end of the input has previously been reached.
     */
    String readLine() throws IOException;

    /**
     * Read buff.length bytes.
     */
    void readFully(byte[] buff) throws IOException;

    /**
     * Read from the stream until the specified byte is encountered, and return the bytes read.
     */
    byte[] readUntil(int ch) throws IOException;

    /**
     * Close the reader.  After this method is called, all read subsequent calls will fail.
     */
    void close() throws IOException;

    /**
     * Returns true iff the close() method has ever been called.
     */
    boolean checkClosed();
}
