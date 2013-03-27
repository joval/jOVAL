// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval;

import java.util.Collection;

import scap.oval.definitions.core.ObjectType;
import scap.oval.systemcharacteristics.core.ItemType;

import org.joval.intf.scap.oval.IBatch.IRequest;
import org.joval.intf.scap.oval.IBatch.IResult;
import org.joval.intf.scap.oval.IProvider.IRequestContext;
import org.joval.scap.oval.CollectException;

/**
 * Implementation of an IBatch.IRequest and IBatch.IResult.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Batch {
    public static class Request implements IRequest {
        private ObjectType obj;
        private IRequestContext ctx;

        public Request(ObjectType obj, IRequestContext ctx) {
            this.obj = obj;
            this.ctx = ctx;
        }

        public ObjectType getObject() {
            return obj;
        }

        public IRequestContext getContext() {
            return ctx;
        }
    }

    public static class Result implements IResult {
	private Collection<? extends ItemType> items;
	private CollectException ex;
	private IRequestContext rc;

	/**
	 * Create a result with a collection error.
	 */
	public Result(CollectException ex, IRequestContext rc) {
	    this.ex = ex;
	    this.rc = rc;
	}

	/**
	 * Create a result with an item list.
	 */
	public Result(Collection<? extends ItemType> items, IRequestContext rc) {
	    this.items = items;
	    this.rc = rc;
	}

	// Implement IBatch.IResult

	public Collection<? extends ItemType> getItems() throws CollectException {
	    if (ex == null) {
		return items;
	    } else {
		throw ex;
	    }
	}

	public IRequestContext getContext() {
	    return rc;
	}
    }
}
