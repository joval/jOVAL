// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.io.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.joval.intf.io.IRandomAccess;

/**
 * File access layer implementation base class. Every CacheFile is backed by a FileAccessor, which is responsible for
 * interacting directly with a file.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class FileAccessor {
    public FileAccessor() {}

    public abstract boolean exists();
    public abstract FileInfo getInfo() throws IOException;
    public abstract long getCtime() throws IOException;
    public abstract long getMtime() throws IOException;
    public abstract long getAtime() throws IOException;
    public abstract long getLength() throws IOException;
    public abstract IRandomAccess getRandomAccess(String mode) throws IOException;
    public abstract InputStream getInputStream() throws IOException;
    public abstract OutputStream getOutputStream(boolean append) throws IOException;
    public abstract String getCanonicalPath() throws IOException;
    public abstract String[] list() throws IOException;
    public abstract boolean mkdir();
    public abstract void delete() throws IOException;
}
