// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.constants;

/**
 * Constants for the create flags argument to the Win32ProcessStartup setShowWindow method.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface ICreateFlags {
    /**
     * If this flag is set, the calling process is treated as a debugger, and the new process is being debugged. The system
     * notifies the debugger of all debug events that occur in the process being debugged. On Windows 95 and Windows 98
     * operating systems, this flag is not valid if the new process is a 16-bit application.
     */
    int Debug_Process = 1;

    /**
     * If this flag is not set and the calling process is being debugged, the new process becomes another process being
     * debugged. If the calling process is not a process of being debugged, no debugging-related actions occur.
     */
    int Debug_Only_This_Process = 2;

    /**
     * The primary thread of the new process is created in a suspended state and does not run until the ResumeThread method is
     * called.
     */
    int Create_Suspended = 4;

    /**
     * For console processes, the new process does not have access to the console of the parent process. This flag cannot be
     * used if the Create_New_Console flag is set.
     */
    int Detached_Process = 8;

    /**
     * This new process has a new console, instead of inheriting the parent console. This flag cannot be used with the
     * Detached_Process flag.
     */
    int Create_New_Console = 16;

    /**
     * This new process is the root process of a new process group. The process group includes all of the processes that are
     * descendants of this root process. The process identifier of the new process group is the same as the process identifier
     * that is returned in the ProcessID property of the Win32_Process class. Process groups are used by the
     * GenerateConsoleCtrlEvent method to enable the sending of either a CTRL+C signal or a CTRL+BREAK signal to a group of
     * console processes.
     */
    int Create_New_Process_Group = 512;

    /**
     * The environment settings listed in the EnvironmentVariables property use Unicode characters. If this flag is not set,
     * the environment block uses ANSI characters.
     */
    int Create_Unicode_Environment = 1024;

    /**
     * Newly created processes are given the system default error mode of the calling process instead of inheriting the error
     * mode of the parent process. This flag is useful for multithreaded shell applications that run with hard errors disabled.
     */
    int Create_Default_Error_Mode = 67108864;

    /**
     * Used in order for the created process not to be limited by the job object.
     */
    int CREATE_BREAKAWAY_FROM_JOB = 16777216;
}
