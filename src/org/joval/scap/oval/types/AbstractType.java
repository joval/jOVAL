// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.types;

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

    public IType cast(Type type) throws TypeConversionException {
	if (type == getType()) {
	    return this;
	}
	switch(type) {
	  case RECORD:
	    throw new TypeConversionException(JOVALMsg.getMessage(JOVALMsg.ERROR_TYPE_INCOMPATIBLE, getType(), Type.RECORD));

	  default:
	    try {
		return TypeFactory.createType(type.getSimple(), getString());
	    } catch (Exception e) {
		throw new TypeConversionException(e);
	    }
	}
    }

    public IType cast(SimpleDatatypeEnumeration type) throws TypeConversionException {
	return cast(TypeFactory.convertType(type));
    }

    @Override
    public boolean equals(Object obj) {
	if (obj instanceof IType) {
	    IType other = (IType)obj;
	    if (getType() == other.getType()) {
		// equality testing should not trigger type conversion
		switch(getType()) {
		  case IPV_4_ADDRESS:
		  case IPV_6_ADDRESS:
		    return getString().equals(other.getString());

		  default:
		    try {
			return compareTo(other) == 0;
		    } catch (IllegalArgumentException e) {
			return false;
		    }
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
