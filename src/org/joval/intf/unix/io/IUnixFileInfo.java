// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.unix.io;

import java.util.NoSuchElementException;

import org.joval.intf.io.IFileEx;

/**
 * Defines extended attributes about a file on Unix.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IUnixFileInfo extends IFileEx {
    /**
     * Key for SELinux extended data. Data will be of the form "[user]:[role]:[type]:[level]".
     * @see http://selinuxproject.org/page/SELinux_contexts
     */
    String SELINUX_DATA = "selinux";

    char DIR_TYPE   = 'd';
    char FIFO_TYPE  = 'p';
    char LINK_TYPE  = 'l';
    char BLOCK_TYPE = 'b';
    char CHAR_TYPE  = 'c';
    char SOCK_TYPE  = 's';
    char FILE_TYPE  = '-';

    String getUnixFileType();

    int getUserId();

    int getGroupId();

    boolean uRead();

    boolean uWrite();

    boolean uExec();

    boolean sUid();

    boolean gRead();

    boolean gWrite();

    boolean gExec();

    boolean sGid();

    boolean oRead();

    boolean oWrite();

    boolean oExec();

    boolean sticky();

    boolean hasExtendedAcl();

    /**
     * Get extended data about the file, such as SELINUX_DATA.
     */
    String getExtendedData(String key) throws NoSuchElementException;
}
