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

    public List<String> resolve(String id, List<VariableValueType> list) throws NoSuchElementException, OvalException {
	return engine.resolve(id, list);
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

	SimpleDatatypeEnumeration stateDT = getDatatype(state.getDatatype());
	SimpleDatatypeEnumeration itemDT =  getDatatype(item.getDatatype());
	switch (stateDT) {
	  case VERSION:
	    if (itemDT == SimpleDatatypeEnumeration.STRING) { // A state is allowed to cast a String to a Version
		break;
	    }

	  default:
	    if (itemDT != stateDT) {
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
	    return Boolean.getBoolean((String)state.getValue()) == Boolean.getBoolean((String)item.getValue());

	  case VERSION:
	    try {
		return new Version(item.getValue()).equals(new Version(state.getValue()));
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

    AdapterContext(IAdapter adapter, Engine engine) {
	this.adapter = adapter;
	this.engine = engine;

	adapter.init(this);
    }

    IAdapter getAdapter() {
	return adapter;
    }
}
