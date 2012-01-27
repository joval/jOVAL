// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.ssh.io;

/**
 * Error codes for SFTP, as near as I can tell.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
interface SftpError {
    int OK = 0;
    int EOF = 1;
    int NO_SUCH_FILE = 2;
    int PERMISSION_DENIED = 3;
    int FAILURE = 4;
    int BAD_MESSAGE = 5;
    int NO_CONNECTION = 6;
    int CONNECTION_LOST = 7;
    int OP_UNSUPPORTED = 8;
    int INVALID_HANDLE = 9;
    int NO_SUCH_PATH = 10;
    int FILE_ALREADY_EXISTS = 11;
    int WRITE_PROTECT = 12;
    int NO_MEDIA = 13;
    int NO_SPACE_ON_FILESYSTEM = 14;
    int QUOTA_EXCEEDED = 15;
    int UNKNOWN_PRINCIPAL = 16;
    int LOCK_CONFLICT = 17;
    int DIR_NOT_EMPTY = 18;
    int NOT_A_DIRECTORY = 19;
    int INVALID_FILENAME = 20;
    int LINK_LOOP = 21;
    int CANNOT_DELETE = 22;
    int INVALID_PARAMETER = 23;
    int FILE_IS_A_DIRECTORY = 24;
    int BYTE_RANGE_LOCK_CONFLICT = 25;
    int BYTE_RANGE_LOCK_REFUSED = 26;
    int DELETE_PENDING = 27;
    int FILE_CORRUPT = 28;
    int OWNER_INVALID = 29;
    int GROUP_INVALID = 30;
    int NO_MATCHING_BYTE_RANGE_LOCK = 31;
}
