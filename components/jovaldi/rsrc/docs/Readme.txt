Copyright (C) 2011-2013 jOVAL.org. All rights reserved.

INTRODUCTION:

jovaldi is an OVAL definition interpreter that is intended to be a drop-in
replacement for ovaldi.  It is written in Java, and is compatible with both
Java 1.6 and 1.7, so you can run it on any machine for which a suitable Java
Virtual Machine is available.

USAGE:

jovaldi has the ability to accommodate jOVAL plugins, and therefore it adds a
command-line option that is not available in ovaldi. That option is:
  -plugin <string>  = name of the jovaldi plugin to use for the scan.
                      DEFAULT="default"

The default plugin, which is part of the jOVAL open source project, can be used
to scan the local host. Commercial plugins are also available, including a
"remote" plugin (which can scan remote hosts) and an "offline" plugin (which
can scan Cisco IOS, Juniper JunOS and Apple iOS devices using configuration
data in a file). Plugins are installed in the plugin/ directory, and can be fed
a configuration at runtime via the -config command-line option:
  -config <string>  = name of the configuration file for the plugin.
                      DEFAULT="config.properties"

The format of the plugin configuration file is specified in the help text of the
plugin. For more information, run: "jovaldi -plugin <name> -h"

This release (version [VERSION]) supports scanning of Windows, Linux, Solaris,
AIX and MacOS X host machines using the default plugin. For a complete listing
of supported tests and platforms, see:
  http://joval.org/features

AVAILABILITY:

Since the 5.10.1.2 release which introduced jOVAL(TM) Professional, jOVAL.org
no longer distributes jovaldi in binary form. Anyone interested in using jovaldi
may pull (or clone) and build the open-source project for themselves:
  http://github.com/joval/jOVAL

Redistribution of the open-source version of jOVAL is permitted under the terms
of the GNU Affero Public License. Commercial licensing is also available. For
more information, contact jOVAL.org:
  http://joval.org/contact
