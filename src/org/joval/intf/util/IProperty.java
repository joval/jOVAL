// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.util;

import java.util.Iterator;

import org.joval.util.PropertyUtil;

/**
 * An interface representing something that can have properties.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IProperty extends Iterable<String> {
    String getProperty(String key);

    int getIntProperty(String key);

    long getLongProperty(String key);

    boolean getBooleanProperty(String key);

    void setProperty(String key, String value);
}
