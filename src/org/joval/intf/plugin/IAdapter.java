// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.plugin;

import java.util.Collection;

import jsaf.intf.system.ISession;

import org.joval.intf.scap.oval.IProvider;
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
     * The notapplicable collection passed into the initializer will be populated with the classes of ObjectType sub-
     * classes that are supported by the adapter, but not applicable to the supplied session.
     *
     * @return the classes that the adapter supports for the specified session.
     */
    public Collection<Class> init(ISession session, Collection<Class> notapplicable);
}
