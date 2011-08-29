// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.unix.io;

import java.io.IOException;

import org.joval.intf.io.IFile;

/**
 * Defines extended attributes of a file on Unix.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IUnixFile extends IFile {
    public String getUnixFileType() throws IOException;

    public int getUserId() throws IOException;

    public int getGroupId() throws IOException;

    public boolean uRead() throws IOException;

    public boolean uWrite() throws IOException;

    public boolean uExec() throws IOException;

    public boolean sUid() throws IOException;

    public boolean gRead() throws IOException;

    public boolean gWrite() throws IOException;

    public boolean gExec() throws IOException;

    public boolean sGid() throws IOException;

    public boolean oRead() throws IOException;

    public boolean oWrite() throws IOException;

    public boolean oExec() throws IOException;

    public boolean sticky() throws IOException;

    public boolean hasExtendedAcl() throws IOException;
}
