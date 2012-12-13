// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.windows.registry;

import java.math.BigInteger;

/**
 * Interface to a Windows registry QWORD value.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IQwordValue extends IValue {
    /**
     * Get the data.
     */
    public BigInteger getData();
}
