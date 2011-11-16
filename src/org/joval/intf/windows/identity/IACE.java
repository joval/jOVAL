// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.windows.identity;

/**
 * Representation of a Windows Access Control Entity (ACE).
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IACE {
    int ACCESS_SYSTEM_SECURITY	= 16777216;

    int FILE_APPEND_DATA	= 4;
    int FILE_DELETE		= 64;
    int FILE_EXECUTE		= 32;
    int FILE_READ_ATTRIBUTES	= 128;
    int FILE_READ_DATA		= 1;
    int FILE_READ_EA		= 8;
    int FILE_WRITE_ATTRIBUTES	= 256;
    int FILE_WRITE_DATA		= 2;
    int FILE_WRITE_EA		= 16;

    int FLAGS_CONTAINER_INHERIT	= 2;
    int FLAGS_INHERIT_ONLY	= 8;
    int FLAGS_INHERITED		= 16;
    int FLAGS_NO_PROPAGATE	= 4;
    int FLAGS_OBJECT_INHERIT	= 1;

    int GENERIC_ALL		= 268435456;
    int GENERIC_EXECUTE		= 536870912;
    int GENERIC_READ		= -2147483648;
    int GENERIC_WRITE		= 1073741824;

    int STANDARD_DELETE 	= 65536;
    int STANDARD_READ_CONTROL	= 131072;
    int STANDARD_SYNCHRONIZE	= 1048576;
    int STANDARD_WRITE_DAC	= 262144;
    int STANDARD_WRITE_OWNER	= 524288;

    int getAccessMask();
    String getSid();
}
