Copyright (C) 2011, 2012 jOVAL.org. All rights reserved.

INTRODUCTION:

jovaldi is an OVAL definition interpreter that is intended to be a drop-in replacement for ovaldi.
It is written in Java, so it can be run on any machine for which a Java Virtual Machine is
available (currently, however, installer packages are only available for 32 and 64-bit Windows,
and 64-bit Linux).

jovaldi has the ability to accommodate "plugins", and therefore it adds a command-line option
that is not available in ovaldi. That option is:
  -plugin <string>  = name of the jovaldi plugin to use for the scan. DEFAULT="default"

Three plugins are distributed with jovaldi: the "default" plugin (which scans the local host),
the "remote" plugin (which can scan remote hosts) and the "offline" plugin (which can scan Cisco
IOS, Juniper JunOS and Apple iOS devices using configuration data in a file). Plugins are fed a
configuration via another command-line option:
  -config <string>  = name of the configuration file for the plugin. DEFAULT="config.properties"

The format of the plugin configuration file is specified in the help text of the plugin. For
more information, run: "jovaldi -plugin <name> -h"

This release (version [VERSION]) supports scanning of Windows, Linux, Solaris, AIX and MacOS X
host machines, Cisco devices running IOS and Juniper devices running JunOS. For a complete
listing of supported tests and platforms, see:
  http://joval.org/features

jovaldi has been written and tested on 64-bit Windows 7 using the 32-bit and 64-bit JRE versions
1.6.0_31 and 1.7.0_03, and 64-bit Fedora 15 Linux using the 64-bit JRE versions 1.6.0_31 and
1.7.0_03. The default plugin uses 32-bit or 64-bit DLLs on Windows. The remote plugin is written
in 100% pure platform-independent Java.
