// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.container;

/**
 * An exception class indicating a problem with the factory configuration of a plugin.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ContainerConfigurationException extends Exception {
    public ContainerConfigurationException(String message) {
	super(message);
    }

    public ContainerConfigurationException(Exception e) {
	super(e);
    }
}
