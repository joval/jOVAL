// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.windows.identity;

/**
 * Representation of a Windows Access Control Entity (ACE).
 *
 * @see http://msdn.microsoft.com/en-us/library/windows/desktop/aa374896%28v=vs.85%29.aspx
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IACE {
    int KEY_QUERY_VALUE		= 0x0001;
    int KEY_SET_VALUE		= 0x0002;
    int KEY_CREATE_SUB_KEY	= 0x0004;
    int KEY_ENUMERATE_SUB_KEYS	= 0x0008;
    int KEY_NOTIFY		= 0x0010;
    int KEY_CREATE_LINK		= 0x0020;
    int KEY_WOW64_64_KEY	= 0x0100;
    int KEY_WOW64_32_KEY	= 0x0200;
    int KEY_WOW64_RES		= 0x0300;
    int KEY_WRITE		= 0x20006;
    int KEY_READ		= 0x20019;
    int KEY_EXECUTE		= 0x20019;
    int KEY_ALL_ACCESS		= 0xF003F;

    int FILE_READ_DATA		= 1;
    int FILE_WRITE_DATA		= 2;
    int FILE_APPEND_DATA	= 4;
    int FILE_READ_EA		= 8;
    int FILE_WRITE_EA		= 16;
    int FILE_EXECUTE		= 32;
    int FILE_DELETE		= 64;
    int FILE_READ_ATTRIBUTES	= 128;
    int FILE_WRITE_ATTRIBUTES	= 256;

    int GENERIC_ALL		= 0x10000000;
    int GENERIC_EXECUTE		= 0x20000000;
    int GENERIC_WRITE		= 0x40000000;
    int GENERIC_READ		= 0x80000000;

    int STANDARD_DELETE 	= 0x10000;
    int STANDARD_READ_CONTROL	= 0x20000;
    int STANDARD_WRITE_DAC	= 0x40000;
    int STANDARD_WRITE_OWNER	= 0x80000;
    int STANDARD_SYNCHRONIZE	= 0x100000;

    int FLAGS_OBJECT_INHERIT	= 1;
    int FLAGS_CONTAINER_INHERIT	= 2;
    int FLAGS_NO_PROPAGATE	= 4;
    int FLAGS_INHERIT_ONLY	= 8;
    int FLAGS_INHERITED		= 16;

    int ACCESS_SYSTEM_SECURITY	= 0x1000000;

    // for SACL ACEs only
    int SUCCESSFUL_ACCESS_ACE_FLAG	= 64;
    int FAILED_ACCESS_ACE_FLAG		= 128;

    int getFlags();
    int getAccessMask();
    String getSid();
}
