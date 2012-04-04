// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.types;

import oval.schemas.common.SimpleDatatypeEnumeration;

/**
 * Generic type interface.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IType<T> extends Comparable<T> {
    SimpleDatatypeEnumeration getType();
}
