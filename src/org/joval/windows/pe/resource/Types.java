// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.windows.pe.resource;

/**
 * An interface defining the resource type constants, and their corresponding String names.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface Types {
    public int RT_0		= 0;
    public int RT_CURSOR	= 1;
    public int RT_BITMAP	= 2;
    public int RT_ICON		= 3;
    public int RT_MENU		= 4;
    public int RT_DIALOG	= 5;
    public int RT_STRING	= 6;
    public int RT_FONTDIR	= 7;
    public int RT_FONT		= 8;
    public int RT_ACCELERATOR	= 9;
    public int RT_RCDATA	= 10;
    public int RT_MESSAGETABLE	= 11;
    public int RT_GROUP_CURSOR	= 12;
    public int RT_13		= 13;
    public int RT_GROUP_ICON	= 14;
    public int RT_15		= 15;
    public int RT_VERSION	= 16;
    public int RT_DLGINCLUDE	= 17;
    public int RT_18		= 18;
    public int RT_PLUGPLAY	= 19;
    public int RT_VXD		= 20;
    public int RT_ANICURSOR	= 21;
    public int RT_ANIICON	= 22;
    public int RT_HTML		= 23;
    public int RT_MANIFEST	= 24;

    public String[] NAMES = {"???_0",
			     "CURSOR",
			     "BITMAP",
			     "ICON",
			     "MENU",
			     "DIALOG",
			     "STRING",
			     "FONTDIR",
			     "FONT",
			     "ACCELERATORS",
			     "RCDATA",
			     "MESSAGETABLE",
			     "GROUP_CURSOR",
			     "???_13",
			     "GROUP_ICON",
			     "???_15",
			     "VERSION",
			     "DLGINCLUDE",
			     "???_18",
			     "PLUGPLAY",
			     "VXD",
			     "ANICURSOR",
			     "ANIICON",
			     "HTML",
			     "MANIFEST" };
}
