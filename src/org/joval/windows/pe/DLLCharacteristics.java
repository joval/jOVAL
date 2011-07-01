// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.windows.pe;

/**
 * Characteristice specific to DLLs, for ImageFileHeader.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface DLLCharacteristics {
    public int IMAGE_DLLCHARACTERISTICS_DYNAMIC_BASE		= 0x0040;
    public int IMAGE_DLLCHARACTERISTICS_FORCE_INTEGRITY 	= 0x0080;
    public int IMAGE_DLLCHARACTERISTICS_NX_COMPAT 		= 0x0100;
    public int IMAGE_DLLCHARACTERISTICS_NO_ISOLATION 		= 0x0200;
    public int IMAGE_DLLCHARACTERISTICS_NO_SEH 			= 0x0400;
    public int IMAGE_DLLCHARACTERISTICS_NO_BIND 		= 0x0800;
    public int IMAGE_DLLCHARACTERISTICS_WDM_DRIVER 		= 0x2000;
    public int IMAGE_DLLCHARACTERISTICS_TERMINAL_SERVER_AWARE 	= 0x8000;
}
