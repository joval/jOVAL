// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.juniper.system;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import org.joval.intf.cisco.system.ITechSupport;

/**
 * An interface for accessing data from the "request support information" JunOS command.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface ISupportInformation extends ITechSupport {
    String GLOBAL = "show configuration";
}
