// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.plugin;

import java.util.Collection;

import oval.schemas.definitions.core.ObjectType;
import oval.schemas.systemcharacteristics.core.ItemType;

import org.joval.intf.system.IBaseSession;
import org.joval.oval.CollectException;
import org.joval.oval.OvalException;

/**
 * The interface for implementing a jOVAL plug-in adapter.  An adapter knows how to retrieve items that correspond to an
 * ObjectType subclass from a host, and the jOVAL engine uses adapters to do this.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IAdapter {
    /**
     * The adapter is initialized by being provided with an active (connected) session object. Implementors should assume
     * that this method will only be called once for any instance of the adapter.
     *
     * @return The object classes for which this adapter knows how to retrieve item data, for the supplied session.
     */
    public Collection<Class> init(IBaseSession session);

    /**
     * Retrieve items associated with the given object by scanning the machine.  If no corresponding items are found,
     * this method should return an empty list, and the Engine will add a message indicating that no items were found.
     *
     * @param obj Contains the object for which items should be retrieved.  Many object types allow for the application
     *            of filters, however, it is not necessary for the IAdapter to implement the filtering functionality because
     *            the engine will enforce them itself.  Additionally, any variable references will be computed by the engine
     *            itself, so the IAdapter does not have to worry about them either.  As a side-effect of variable resolution,
     *            an IAdapter may be called numerous times with different ObjectTypes, all having the same object ID.  An
     *            adapter should therefore make no effort to cache data keying on the object ID.
     *
     * @param rc  @see IRequestContext
     *
     * @throws CollectException if items cannot be collected for the request for some reason, such as an unsupported
     *                       platform for the adapter, or an unsupported operation on the object.  The OVAL object will
     *                       have a resulting status specified by the exception.
     * @throws OvalException if there has been an error which should stop all processing, such as propagation of an 
     *                       OvalException that has been thrown by a call to IRequestContext.resolve.
     */
    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws OvalException, CollectException;
}
