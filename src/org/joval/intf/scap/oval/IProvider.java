// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.scap.oval;

import java.util.Collection;

import scap.oval.common.MessageType;
import scap.oval.definitions.core.ObjectType;
import scap.oval.systemcharacteristics.core.ItemType;

import org.joval.scap.oval.CollectException;

/**
 * The interface for implementing a jOVAL item provider.  A provider knows how to retrieve items that correspond to an
 * ObjectType subclass, and the jOVAL engine uses providers to do this.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IProvider {
    /**
     * Retrieve items associated with the given object.  If no corresponding items are found, this method should return an
     * empty list, and the Engine will add a message indicating that no items were found.
     *
     * @param obj Contains the object for which items should be retrieved.  Many object types allow for the application
     *            of filters, however, it is not necessary for the IProvider to implement the filtering functionality because
     *            the engine will enforce them itself.  Additionally, any variable references will be computed by the engine
     *            itself, so the IProvider does not have to worry about them either.  As a side-effect of variable resolution,
     *            an IProvider may be called numerous times with different ObjectTypes, all having the same object ID.  An
     *            adapter should therefore make no effort to cache data keying on the object ID.
     *
     * @param rc  @see IRequestContext
     *
     * @throws CollectException if items cannot be collected for the request for some reason, such as an unsupported
     *            platform for the adapter, or an unsupported operation on the object.  The OVAL object will have a
     *            resulting status specified by the exception.
     */
    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException;

    /**
     * The interface for the argument supplied to an adapter getItems request, which provides a facility for adding messages
     * related to the request.
     */
    public interface IRequestContext {
	/**
	 * Associate a message (like an error) with the request object.
	 */
	public void addMessage(MessageType msg);
    }
}
