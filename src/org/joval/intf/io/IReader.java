// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.io;

import java.io.IOException;

import org.joval.intf.util.ILoggable;

/**
 * A convenience interface for reading data.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IReader extends ILoggable {
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
     *
     * @throws EOFException if the end of the stream is reached
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

    /**
     * Returns true if the end of the underlying stream has been reached.
     */
    boolean checkEOF();

    /**
     * Set a checkpoint to which the reader can be reset.
     */
    void setCheckpoint(int readAheadLimit) throws IOException;

    /**
     * Return the stream to the last mark position.
     */
    void restoreCheckpoint() throws IOException;
}
