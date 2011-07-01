// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.windows.remote.wmi.process;

/**
 * Constants for the show window argument to the Win32ProcessStartup setShowWindow method.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IShowWindow {
    /**
     * Hides the window and activates another window.
     */
    short SW_HIDE = 0;

    /**
     * Activates and displays a window. If the window is minimized or maximized, the system restores it to the original size
     * and position. An application specifies this flag when displaying the window for the first time.
     */
    short SW_NORMAL = 1;

    /**
     * Activates the window, and displays it as a minimized window.
     */
    short SW_SHOWMINIMIZED = 2;

    /**
     * Activates the window, and displays it as a maximized window.
     */
    short SW_SHOWMAXIMIZED = 3;

    /**
     * Displays a window in its most recent size and position. This value is similar to SW_NORMAL, except that the window is
     * not activated.
     */
    short SW_SHOWNOACTIVATE = 4;

    /**
     * Activates the window, and displays it at the current size and position.
     */
    short SW_SHOW = 5;

    /**
     * Minimizes the specified window, and activates the next top-level window in the Z order.
     */
    short SW_MINIMIZE = 6;

    /**
     * Displays the window as a minimized window. This value is similar to SW_SHOWMINIMZED, except that the window is not
     * activated.
     */
    short SW_SHOWMINNOACTIVE = 7;

    /**
     * Displays the window at the current size and position. This value is similar to SW_SHOW, except that the window is not
     * activated.
     */
    short SW_SHOWNA = 8;

    /**
     * Activates and displays the window. If the window is minimized or maximized, the system restores it to the original size
     * and position. An application specifies this flag when restoring a minimized window.
     */
    short SW_RESTORE = 9;

    /**
     * Sets the show state based on the SW_ value that is specified in the STARTUPINFO structure passed to the CreateProcess
     * function by the program that starts the application.
     */
    short SW_SHOWDEFAULT = 10;

    /**
     * Windows Server 2003, Windows 2000, and Windows XP:  Minimizes a window, even when the thread that owns the window is
     * hung. Only use this flag when minimizing windows from a different thread.
     */
    short SW_FORCEMINIMIZE = 11;
}
