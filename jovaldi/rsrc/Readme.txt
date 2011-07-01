Copyright (C) 2011 jOVAL.org.  All rights reserved.                                                 |

INTRODUCTION:

jovaldi is an OVAL definition interpreter that is intended to be a drop-in replacement for ovaldi.  It
is written in Java, so it can be run from any machine for which a Java Virtual Machine is available
(currently, however, installer packages are only available for 32-bit Windows and 64-bit Linux).

jovaldi has the ability to accommodate "plugins", and therefore it adds a command-line option that is
not available in ovaldi.  That option is:
  -j <string>  = name of the jovaldi plugin to use for the scan.  DEFAULT="default"

Two plugins are distributed with jovaldi: the "default" plugin (which scans the local host) and the
"remote" plugin (which can scan remote hosts).  Plugins can specify their own command-line options as
well.  To see a print-out of options for a plugin, use:
  jovaldi -j <string> -h

This release (version 0.A.4) supports scanning of Windows and Linux host machines, and implements the
following OVAL tests:

  ind-family
  ind-textfilecontent54
  linux-rpminfo
  windows-file (version and product_version checks only)
  windows-registry
  windows-wmi

jovaldi has been written and tested on 32-bit Windows XP and 64-bit Windows 7 using the 32-bit JRE
version 1.6.0_26, and 64-bit Fedora 15 Linux using the 64-bit JRE version 1.6.0_26.  The default plugin
uses 32-bit DLLs on WIndows.  The remote plugin is written in 100% pure platform-independent Java.
