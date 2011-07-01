// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.engine;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import java.util.logging.Level;

import oval.schemas.systemcharacteristics.core.VariableValueType;

import org.joval.intf.oval.IDefinitions;
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
    private IDefinitions definitions;

    // Implement IAdapterContext

    public IDefinitions getDefinitions() {
	return engine.getDefinitions();
    }

    public void status(String objectId) {
	engine.producer.sendNotify(engine, Engine.MESSAGE_OBJECT, objectId);
    }

    public void log(Level level, String message) {
	JOVALSystem.getLogger().logp(level, adapter.getClass().getName(), "unknown", message);
    }

    public void log(Level level, String message, Throwable thrown) {
	JOVALSystem.getLogger().logp(level, adapter.getClass().getName(), "unknown", message, thrown);
    }

    public String resolve(String id, List<VariableValueType> list) throws NoSuchElementException, OvalException {
	return engine.resolve(id, list);
    }

    // Internal

    AdapterContext(IAdapter adapter, Engine engine) {
	this.adapter = adapter;
	this.engine = engine;

	adapter.init(this);
    }

    IAdapter getAdapter() {
	return adapter;
    }
}
