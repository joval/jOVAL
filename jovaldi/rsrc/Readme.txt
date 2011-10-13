Copyright (C) 2011 jOVAL.org.  All rights reserved.

INTRODUCTION:

jovaldi is an OVAL definition interpreter that is intended to be a drop-in replacement for ovaldi.
It is written in Java, so it can be run from any machine for which a Java Virtual Machine is
available (currently, however, installer packages are only available for 32-bit Windows and
64-bit Linux).

jovaldi has the ability to accommodate "plugins", and therefore it adds a command-line option
that is not available in ovaldi.  That option is:
  -plugin <string>  = name of the jovaldi plugin to use for the scan.  DEFAULT="default"

Two plugins are distributed with jovaldi: the "default" plugin (which scans the local host) and
the "remote" plugin (which can scan remote hosts).  Plugins are fed a configuration via another
command-line option:
  -config <string>  = name of the configuration file for the plugin.  DEFAULT="config.properties"

The format of the plugin configuration file is specified in the help text of the plugin.  For
more information, run: "jovaldi -plugin <name> -h"

This release (version A.5.10.1) supports scanning of Windows, Linux and Solaris host machines and
Cisco devices running IOS.  For a complete listing of supported tests, see:

http://joval.org/features

jovaldi has been written and tested on 32-bit Windows XP and 64-bit Windows 7 using the 32-bit
and 64-bit JRE version 1.6.0_26, and 64-bit Fedora 15 Linux using the 64-bit JRE version 1.6.0_26.
The default plugin uses 32-bit or 64-bit DLLs on WIndows.  The remote plugin is written in 100%
pure platform-independent Java.
