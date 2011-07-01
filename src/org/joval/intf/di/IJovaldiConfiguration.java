// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.di;

import java.io.File;
import java.util.List;
import java.util.Properties;

/**
 * Interface specification for a plugin to the Jovaldi command-line application.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IJovaldiConfiguration {
    File getSystemCharacteristicsInputFile();
    boolean printingHelp();
}
