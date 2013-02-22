// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.xml;

import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBContext;
import javax.xml.transform.Source;

import org.joval.scap.ScapException;

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
    public Source getSource() throws JAXBException, ScapException;

    /**
     * Get the JAXB root element.
     */
    public Object getRootObject();

    /**
     * Get a JAXBContext for the document.
     */
    public JAXBContext getJAXBContext() throws JAXBException;
}

