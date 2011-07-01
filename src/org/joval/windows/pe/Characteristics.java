// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.windows.pe;

/**
 * ImageFileHeader characteristic constants.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface Characteristics {
    public int IMAGE_FILE_RELOCS_STRIPPED		= 0x0001;
    public int IMAGE_FILE_EXECUTABLE_IMAGE		= 0x0002;
    public int IMAGE_FILE_LINE_NUMS_STRIPPED		= 0x0004;
    public int IMAGE_FILE_LOCAL_SYMS_STRIPPED		= 0x0008;
    public int IMAGE_FILE_AGGRESIVE_WS_TRIM		= 0x0010;
    public int IMAGE_FILE_LARGE_ADDRESS_AWARE		= 0x0020;
    public int IMAGE_FILE_BYTES_REVERSED_LO		= 0x0080;
    public int IMAGE_FILE_32BIT_MACHINE			= 0x0100;
    public int IMAGE_FILE_DEBUG_STRIPPED		= 0x0200;
    public int IMAGE_FILE_REMOVABLE_RUN_FROM_SWAP 	= 0x0400;
    public int IMAGE_FILE_NET_RUN_FROM_SWAP		= 0x0800;
    public int IMAGE_FILE_SYSTEM			= 0x1000;
    public int IMAGE_FILE_DLL				= 0x2000;
    public int IMAGE_FILE_UP_SYSTEM_ONLY		= 0x4000;
    public int IMAGE_FILE_BYTES_REVERSED_HI		= 0x8000;
}
