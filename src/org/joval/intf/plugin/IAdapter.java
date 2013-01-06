// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.plugin;

import java.util.Collection;

import jsaf.intf.system.IBaseSession;

import org.joval.intf.oval.IProvider;
import org.joval.scap.oval.CollectException;

/**
 * The interface for implementing a jOVAL plug-in adapter.  An adapter knows how to retrieve items that correspond to an
 * ObjectType subclass from a host, and the jOVAL engine uses adapters to do this.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IAdapter extends IProvider {
    /**
     * The adapter is initialized by being provided with an active (connected) jSAF session object. Implementors should
     * assume that this method will only be called once for any instance of the adapter.
     *
     * @return The object classes for which this adapter knows how to retrieve item data, for the supplied session.
     */
    public Collection<Class> init(IBaseSession session);
}
