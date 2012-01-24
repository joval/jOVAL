// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.plugin;

import java.util.Collection;
import javax.xml.bind.JAXBElement;

import oval.schemas.systemcharacteristics.core.ItemType;

import org.joval.oval.CollectionException;
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
     * Identify the classes of a subclass of ObjectType for which this adapter knows how to retrieve item data.
     */
    public Class[] getObjectClasses();

    /**
     * The adapter should open any special resources it's going to need in order to scan objects on the machine.  The engine
     * will call this method after init, but before any call to getItems.
     */
    public boolean connect();

    /**
     * The adapter should release and/or clean-up any resource that it's opened for scanning purposes.  The engine will call
     * this method after all calls to getItems have been made to the adapter.
     */
    public void disconnect();

    /**
     * Retrieve items associated with the given object by scanning the machine.  The ItemTypes returned must be wrapped in a
     * JAXBElement so that they can be marshalled into an OvalSystemCharacteristics.  If no corresponding items are found,
     * this method should return an empty list, and the Engine will add a message indicating that no items were found.
     *
     * The IRequestContext contains the object for which items should be retrieved.  Many object types allow for the
     * application of filters, however, it is not necessary for the IAdapter to implement the filtering functionality because
     * the engine will enforce them itself.
     *
     * @see IRequestContext
     *
     * @throws CollectionException if items cannot be collected for the request for some reason, such as an unsupported
     *                       platform for the adapter, or an unsupported operation on the object.  The OVAL object will
     *                       have a resulting status of "not collected".
     * @throws OvalException if there has been an error which should stop all processing, such as propagation of an 
     *                       OvalException that has been thrown by a call to IRequestContext.resolve.
     */
    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext irc) throws OvalException, CollectionException;
}
