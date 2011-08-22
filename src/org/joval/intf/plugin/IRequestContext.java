// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.plugin;

import java.util.List;
import java.util.NoSuchElementException;

import oval.schemas.common.MessageType;
import oval.schemas.definitions.core.ObjectType;

import org.joval.oval.OvalException;
import org.joval.oval.ResolveException;

/**
 * The interface for the argument supplied to an adapter getItems request, specifying the ObjectType for which the request is
 * being made, a means of resolving variable values referenced within the object, and a facility for adding messages related
 * to the request.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IRequestContext {
    /**
     * Get the ObjectType definition for the request.
     */
    public ObjectType getObject();

    /**
     * Associate a message (like an error) with the request object.
     */
    public void addMessage(MessageType msg);

    /**
     * Resolve a variable.
     */
    public List<String> resolve(String variableId) throws NoSuchElementException, ResolveException, OvalException;
}
