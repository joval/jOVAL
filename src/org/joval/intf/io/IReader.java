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
    String readLine() throws IOException;
    void readFully(byte[] buff) throws IOException;
    byte[] readUntil(int ch) throws IOException;
    void close() throws IOException;
    int read() throws IOException;
}
