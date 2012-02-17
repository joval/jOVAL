// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.xml;

import javax.xml.transform.Source;

/**
 * Interface of an XML structure that is transformable.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface ITransformable {
    /**
     * Get a source that can be used for performing transforms.
     */
    public Source getSource();
}

