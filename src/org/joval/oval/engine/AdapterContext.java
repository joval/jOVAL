// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.engine;

import java.math.BigInteger;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Pattern;

import oval.schemas.common.MessageType;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.EntityStateSimpleBaseType;
import oval.schemas.systemcharacteristics.core.EntityItemSimpleBaseType;
import oval.schemas.systemcharacteristics.core.VariableValueType;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IAdapterContext;
import org.joval.oval.OvalException;
import org.joval.oval.TestException;
import org.joval.util.JOVALSystem;
import org.joval.util.Version;

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

    /**
     * Compare a state SimpleBaseType to an item SimpleBaseType.  If the item is null, this method returns false.  That
     * allows callers to simply check if the state is set before invoking the comparison.
     */
    public ResultEnumeration test(EntityStateSimpleBaseType state, EntityItemSimpleBaseType item)
		throws TestException, OvalException {
	if (item == null) {
	    return ResultEnumeration.NOT_APPLICABLE;
	} else {
	    switch(item.getStatus()) {
	      case NOT_COLLECTED:
		return ResultEnumeration.NOT_EVALUATED;

	      case ERROR:
		return ResultEnumeration.ERROR;

	      case DOES_NOT_EXIST:
		return ResultEnumeration.FALSE;
	    }
	}

	//
	// Check datatype compatibility; anything can be compared with a string.
	//
	SimpleDatatypeEnumeration stateDT = getDatatype(state.getDatatype());
	SimpleDatatypeEnumeration itemDT =  getDatatype(item.getDatatype());
	if (itemDT != stateDT) {
	    if (itemDT != SimpleDatatypeEnumeration.STRING && stateDT != SimpleDatatypeEnumeration.STRING) {
		throw new OvalException(JOVALSystem.getMessage("ERROR_DATATYPE_MISMATCH", stateDT, itemDT));
	    }
	}

	switch (state.getOperation()) {
	  case CASE_INSENSITIVE_EQUALS:
	    if (caseInsensitiveEquals(state, item)) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }

	  case EQUALS:
	    if (equals(state, item)) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }

	  case PATTERN_MATCH: // Always treat as Strings
	    if (item.getValue() == null) {
		return ResultEnumeration.FALSE;
	    } else if (Pattern.compile((String)state.getValue()).matcher((String)item.getValue()).find()) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }

	  case NOT_EQUAL:
	    if (equals(state, item)) {
		return ResultEnumeration.FALSE;
	    } else {
		return ResultEnumeration.TRUE;
	    }

	  case GREATER_THAN_OR_EQUAL:
	    if (greaterThanOrEqual(state, item)) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }

	  case GREATER_THAN:
	    if (greaterThan(state, item)) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }

	  case LESS_THAN_OR_EQUAL:
	    if (greaterThan(state, item)) {
		return ResultEnumeration.FALSE;
	    } else {
		return ResultEnumeration.TRUE;
	    }

	  case LESS_THAN:
	    if (greaterThanOrEqual(state, item)) {
		return ResultEnumeration.FALSE;
	    } else {
		return ResultEnumeration.TRUE;
	    }

	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", state.getOperation()));
	}
    }

    // Internal

    boolean equals(EntityStateSimpleBaseType state, EntityItemSimpleBaseType item)
		throws TestException, OvalException {
	switch(getDatatype(state.getDatatype())) {
	  case INT:
	    try {
		return new BigInteger((String)item.getValue()).equals(new BigInteger((String)state.getValue()));
	    } catch (NumberFormatException e) {
		throw new TestException(e);
	    }

	  case BOOLEAN:
	    return getBoolean((String)state.getValue()) == getBoolean((String)item.getValue());

	  case VERSION:
	    try {
		return new Version(item.getValue()).equals(new Version(state.getValue()));
	    } catch (NumberFormatException e) {
		throw new TestException(e);
	    }

	  case EVR_STRING:
	    try {
		return new Evr((String)item.getValue()).equals(new Evr((String)state.getValue()));
	    } catch (NumberFormatException e) {
		throw new TestException(e);
	    }

	  case STRING:
	    return ((String)state.getValue()).equals((String)item.getValue());

	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_OPERATION_DATATYPE",
							   state.getDatatype(), OperationEnumeration.EQUALS));
	}
    }

    boolean greaterThanOrEqual(EntityStateSimpleBaseType state, EntityItemSimpleBaseType item)
		throws TestException, OvalException {
	switch(getDatatype(state.getDatatype())) {
	  case INT:
	    try {
		return new BigInteger((String)item.getValue()).compareTo(new BigInteger((String)state.getValue())) >= 0;
	    } catch (NumberFormatException e) {
		throw new TestException(e);
	    }

	  case VERSION:
	    try {
		return new Version(item.getValue()).greaterThanOrEquals(new Version(state.getValue()));
	    } catch (NumberFormatException e) {
		throw new TestException(e);
	    }

	  case EVR_STRING:
	    try {
		return new Evr((String)item.getValue()).greaterThanOrEquals(new Evr((String)state.getValue()));
	    } catch (NumberFormatException e) {
		throw new TestException(e);
	    }

	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_OPERATION_DATATYPE",
							   state.getDatatype(), OperationEnumeration.GREATER_THAN_OR_EQUAL));
	}
    }

    boolean greaterThan(EntityStateSimpleBaseType state, EntityItemSimpleBaseType item)
		throws TestException, OvalException {
	switch(getDatatype(state.getDatatype())) {
	  case INT:
	    try {
		return new BigInteger((String)item.getValue()).compareTo(new BigInteger((String)state.getValue())) > 0;
	    } catch (NumberFormatException e) {
		throw new TestException(e);
	    }

	  case VERSION:
	    try {
		return new Version(item.getValue()).greaterThan(new Version(state.getValue()));
	    } catch (NumberFormatException e) {
		throw new TestException(e);
	    }

	  case EVR_STRING:
	    try {
		return new Evr((String)item.getValue()).greaterThan(new Evr((String)state.getValue()));
	    } catch (NumberFormatException e) {
		throw new TestException(e);
	    }

	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_OPERATION_DATATYPE",
							   state.getDatatype(), OperationEnumeration.GREATER_THAN_OR_EQUAL));
	}
    }

    boolean caseInsensitiveEquals(EntityStateSimpleBaseType state, EntityItemSimpleBaseType item)
		throws TestException, OvalException {
	switch(getDatatype(state.getDatatype())) {
	  case STRING:
	    return ((String)state.getValue()).equalsIgnoreCase((String)item.getValue());

	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_OPERATION_DATATYPE",
							   state.getDatatype(), OperationEnumeration.CASE_INSENSITIVE_EQUALS));
	}
    }

    /**
     * Convert the datatype String into a SimpleDatatypeEnumeration for use in switches.
     */
    SimpleDatatypeEnumeration getDatatype(String s) throws OvalException {
	if ("binary".equals(s)) {
	    return SimpleDatatypeEnumeration.BINARY;
	} else if ("boolean".equals(s)) {
	    return SimpleDatatypeEnumeration.BOOLEAN;
	} else if ("evr_string".equals(s)) {
	    return SimpleDatatypeEnumeration.EVR_STRING;
	} else if ("fileset_revision".equals(s)) {
	    return SimpleDatatypeEnumeration.FILESET_REVISION;
	} else if ("float".equals(s)) {
	    return SimpleDatatypeEnumeration.FLOAT;
	} else if ("int".equals(s)) {
	    return SimpleDatatypeEnumeration.INT;
	} else if ("ios_version".equals(s)) {
	    return SimpleDatatypeEnumeration.IOS_VERSION;
	} else if ("ipv4_address".equals(s)) {
	    return SimpleDatatypeEnumeration.IPV_4_ADDRESS;
	} else if ("ipv6_address".equals(s)) {
	    return SimpleDatatypeEnumeration.IPV_6_ADDRESS;
	} else if ("string".equals(s)) {
	    return SimpleDatatypeEnumeration.STRING;
	} else if ("version".equals(s)) {
	    return SimpleDatatypeEnumeration.VERSION;
	} else {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_DATATYPE", s));
	}
    }

    boolean getBoolean(String s) {
	if (s == null) {
	    return false;
	} else if (s.equalsIgnoreCase("true")) {
	    return true;
	} else if (s.equals("0")) {
	    return false;
	} else if (s.equalsIgnoreCase("false")) {
	    return false;
	} else if (s.length() == 0) {
	    return false;
	} else {
	    return true;
	}
    }

    AdapterContext(IAdapter adapter, Engine engine) {
	this.adapter = adapter;
	this.engine = engine;

	adapter.init(this);
    }

    IAdapter getAdapter() {
	return adapter;
    }

    class Evr {
	String epoch, version, release;

	Evr(String evr) {
	    int end = evr.indexOf(":");
	    epoch = evr.substring(0, end);
	    int begin = end+1;
	    end = evr.indexOf("-", begin);
	    version = evr.substring(begin, end);
	    release = evr.substring(end+1);
	}

	boolean greaterThanOrEquals(Evr evr) {
	    if (equals(evr)) {
		return true;
	    } else if (greaterThan(evr)) {
		return true;
	    } else {
		return false;
	    }
	}

	boolean greaterThan(Evr evr) {
	    if (Version.isVersion(epoch) && Version.isVersion(evr.epoch)) {
		if (new Version(epoch).greaterThan(new Version(evr.epoch))) {
		    return true;
		}
	    } else if (epoch.compareTo(evr.epoch) > 0) {
		return true;
	    }
	    if (Version.isVersion(version) && Version.isVersion(evr.version)) {
		if (new Version(version).greaterThan(new Version(evr.version))) {
		    return true;
		}
	    } else if (version.compareTo(evr.version) > 0) {
		return true;
	    }
	    if (Version.isVersion(release) && Version.isVersion(evr.release)) {
		if (new Version(release).greaterThan(new Version(evr.release))) {
		    return true;
		}
	    } else if (release.compareTo(evr.release) > 0) {
		return true;
	    }
	    return false;
	}

	public boolean equals(Object obj) {
	    if (obj instanceof Evr) {
		Evr evr = (Evr)obj;
		return epoch.equals(evr.epoch) && version.equals(evr.version) && release.equals(evr.release);
	    } else {
		return false;
	    }
	}
    }
}
