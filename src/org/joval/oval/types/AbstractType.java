// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.types;

import oval.schemas.common.SimpleDatatypeEnumeration;

import org.joval.intf.oval.IType;
import org.joval.util.JOVALMsg;

/**
 * Abstract base type for all IType implementations.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
abstract class AbstractType implements IType {
    AbstractType(){}

    public IType cast(Type type) throws UnsupportedOperationException {
	if (type == getType()) {
	    return this;
	}
	switch(type) {
	  case RECORD:
	    throw new IllegalArgumentException(JOVALMsg.getMessage(JOVALMsg.ERROR_TYPE_CONVERSION, getType(), Type.RECORD));

	  default:
	    return TypeFactory.createType(type.getSimple(), getString());
	}
    }

    public IType cast(SimpleDatatypeEnumeration type) throws UnsupportedOperationException {
	return cast(TypeFactory.convertType(type));
    }

    @Override
    public boolean equals(Object obj) {
	if (obj instanceof IType) {
	    IType other = (IType)obj;
	    if (getType() == other.getType()) {
		// equality testing should not trigger type conversion
		try {
		    return compareTo(other) == 0;
		} catch (IllegalArgumentException e) {
		    return false;
		}
	    } else {
		return false;
	    }
	} else {
	    return false;
	}
    }

    @Override
    public int hashCode() {
	try {
	    return new StringBuffer(getType().toString()).append(".").append(getString()).toString().hashCode();
	} catch (UnsupportedOperationException e) {
	    return super.hashCode();
	}
    }
}
