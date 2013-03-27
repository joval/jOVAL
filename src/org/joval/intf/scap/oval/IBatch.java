// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.scap.oval;

import java.util.Collection;

import scap.oval.definitions.core.ObjectType;
import scap.oval.systemcharacteristics.core.ItemType;

import org.joval.scap.oval.CollectException;

/**
 * The interface for implementing a batching jOVAL item provider, which can queue up multiple objects and retrieve items
 * for the entire queue in a single call.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IBatch {
    /**
     * Queue up a request for items.
     *
     * @return true if the request has been queued, and false if it cannot be queued for any reason.
     */
    public boolean queue(IRequest request);

    /**
     * Obtain results for all the queued requests.
     */
    public Collection<IResult> exec();

    /**
     * An interface for a request.
     */
    public interface IRequest {
	/**
	 * Returns the request context for the request.
	 */
	IProvider.IRequestContext getContext();

	/**
	 * Returns the object specification of the request.
	 */
	ObjectType getObject();
    }

    /**
     * An interface for a result, which maps to a discrete queue instance.
     */
    public interface IResult {
	/**
	 * Returns the request context that accompanied the request.
	 */
	IProvider.IRequestContext getContext();

	/**
	 * Returns the items collected by the adapter, or throws an error.
	 */
	Collection<? extends ItemType> getItems() throws CollectException;
    }
}
