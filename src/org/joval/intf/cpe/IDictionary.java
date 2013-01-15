// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.cpe;

import cpe.schemas.dictionary.ItemType;
import cpe.schemas.dictionary.ListType;

/**
 * Interface defining a CPE dictionary document.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IDictionary {
    public ListType getCpeList();

    public ItemType getItem(String cpeName);
}
