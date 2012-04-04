// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.net;

/**
 * An interface for describing a CIDR address.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface ICIDR<T> {
    /**
     * Determine whether or not this CIDR address contains another.
     */
    boolean contains(T other);
}
