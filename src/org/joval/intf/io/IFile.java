// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.io;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Pattern;

/**
 * A platform-independent abstraction of a File.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IFile extends IFileMetadata {
    /**
     * Flags, used to specify the behavior of the IFile when it is initially retrieved.
     */
    enum Flags {
       /**
	* Flag indicating that this IFile's information should never be cached.
	*/
       NOCACHE,

       /**
	* Read-only access to a file that can be expected to change continuously (i.e., be growing in size).
	*/
       READVOLATILE,

       /**
	* Simple read-only access to the IFile. Prohibits mkdir, getOutputStream, delete, and getRandomAccess("rw").
	*/
       READONLY,

       /**
	* Read-write access to the IFile. Allows mkdir, getOutputStream, delete, and getRandomAccess("rw").
	*/
       READWRITE;
    }

    /**
     * Create a directory at this IFile's path.
     */
    public boolean mkdir();

    /**
     * Get an InputStream of the file contents.
     */
    public InputStream getInputStream() throws IOException;

    /**
     * Get an OutputStream to the file.  Start at the beginning (overwrite) or the end (append) as dictated.
     */
    public OutputStream getOutputStream(boolean append) throws IOException;

    /**
     * Get random access.
     */
    public IRandomAccess getRandomAccess(String mode) throws IllegalArgumentException, IOException;

    /**
     * For a directory, list the names of the subdirectories.
     */
    public String[] list() throws IOException;

    /**
     * For a directory, lists all the child files (Flags inherited).
     */
    public IFile[] listFiles() throws IOException;

    /**
     * For a directory, lists all the child files (Flags inherited) whose names match the specified pattern.
     */
    public IFile[] listFiles(Pattern p) throws IOException;

    /**
     * For a directory, retrieves an IFile for the child file with the specified name. Flags are inherited.
     */
    public IFile getChild(String name) throws IOException;

    /**
     * Delete the file.
     */
    public void delete() throws IOException;
}
