// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.oval;

import java.io.File;

import oval.schemas.systemcharacteristics.core.OvalSystemCharacteristics;

/**
 * Interface defining OVAL System Characteristics.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface ISystemCharacteristics {
    /**
     * Return a raw OVAL system characteristics object containing the underlying data.
     */
    public OvalSystemCharacteristics getOvalSystemCharacteristics();

    /**
     * Serialize the OVAL system characteristics to the specified file.
     */
    public void write(File f);
}
