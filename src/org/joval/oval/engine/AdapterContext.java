// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.engine;

import java.math.BigInteger;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Pattern;

import oval.schemas.common.MessageType;
import oval.schemas.definitions.core.EntityObjectAnySimpleType;
import oval.schemas.systemcharacteristics.core.VariableValueType;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IAdapterContext;
import org.joval.oval.OvalException;
import org.joval.util.JOVALSystem;

/**
 * The context class for an IAdapter implementation, which provides the interface between the IAdapter and the Engine.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class AdapterContext implements IAdapterContext {
    private IAdapter adapter;
    private Engine engine;

    // Implement IAdapterContext

    public void log(Level level, String message) {
	JOVALSystem.getLogger().logp(level, adapter.getClass().getName(), "unknown", message);
    }

    public void log(Level level, String message, Throwable thrown) {
	JOVALSystem.getLogger().logp(level, adapter.getClass().getName(), "unknown", message, thrown);
    }

    public List<String> resolve(String id, List<VariableValueType> vars) throws NoSuchElementException, OvalException {
	return engine.resolve(id, vars);
    }

    public void addObjectMessage(String objectId, MessageType message) {
	engine.addObjectMessage(objectId, message);
    }

    // Internal

    boolean active = false;

    void setActive(boolean active) {
	this.active = active;
    }

    boolean isActive() {
	return active;
    }

    AdapterContext(IAdapter adapter, Engine engine) {
	this.adapter = adapter;
	this.engine = engine;

	adapter.init(this);
    }

    IAdapter getAdapter() {
	return adapter;
    }
}
