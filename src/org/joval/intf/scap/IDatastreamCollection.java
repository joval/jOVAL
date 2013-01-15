// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.scap;

import java.util.Collection;
import java.util.NoSuchElementException;

import scap.datastream.DataStreamCollection;

import org.joval.scap.ScapException;

/**
 * Interface defining an SCAP datastream, supporting access to OVAL, OCIL and SCE documents contained within.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IDatastreamCollection {
    /**
     * Get the raw SCAP datastream collection object from the data model.
     */
    DataStreamCollection getDSCollection();

    /**
     * Get the IDs of the streams defined in the collection.
     */
    Collection<String> getStreamIds();

    /**
     * Get a specific datastream.
     */
    IDatastream getDatastream(String id) throws NoSuchElementException, ScapException;
}
