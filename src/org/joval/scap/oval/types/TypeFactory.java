// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.types;

import scap.oval.common.ComplexDatatypeEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.EntitySimpleBaseType;
import scap.oval.systemcharacteristics.core.EntityItemFieldType;
import scap.oval.systemcharacteristics.core.EntityItemSimpleBaseType;

import org.joval.intf.scap.oval.IType;
import org.joval.util.JOVALMsg;

/**
 * Factory for types.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class TypeFactory {
    /**
     * Utility method to obtain a SimpleDatatypeEnumeration from a String.
     */
    public static SimpleDatatypeEnumeration getSimpleDatatype(String datatype) throws IllegalArgumentException {
	for (SimpleDatatypeEnumeration type : SimpleDatatypeEnumeration.values()) {
	    if (type.value().equals(datatype)) {
		return type;
	    }
	}
	throw new IllegalArgumentException(JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_DATATYPE, datatype));
    }

    /**
     * Utility method to obtain a ComplexDatatypeEnumeration from a String.
     */
    public static ComplexDatatypeEnumeration getComplexDatatype(String datatype) throws IllegalArgumentException {
	for (ComplexDatatypeEnumeration type : ComplexDatatypeEnumeration.values()) {
	    if (type.value().equals(datatype)) {
		return type;
	    }
	}
	throw new IllegalArgumentException(JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_DATATYPE, datatype));
    }

    public static IType.Type convertType(SimpleDatatypeEnumeration type) {
	switch(type) {
	  case BINARY:
	    return IType.Type.BINARY;
	  case BOOLEAN:
	    return IType.Type.BOOLEAN;
	  case EVR_STRING:
	    return IType.Type.EVR_STRING;
	  case FILESET_REVISION:
	    return IType.Type.FILESET_REVISION;
	  case FLOAT:
	    return IType.Type.FLOAT;
	  case INT:
	    return IType.Type.INT;
	  case IOS_VERSION:
	    return IType.Type.IOS_VERSION;
	  case IPV_4_ADDRESS:
	    return IType.Type.IPV_4_ADDRESS;
	  case IPV_6_ADDRESS:
	    return IType.Type.IPV_6_ADDRESS;
	  case VERSION:
	    return IType.Type.VERSION;
	}
	return IType.Type.STRING;
    }

    /**
     * Create a simple IType from an Object or State entity.
     */
    public static IType createType(EntitySimpleBaseType base) throws IllegalArgumentException {
	return createType(getSimpleDatatype(base.getDatatype()), (String)base.getValue());
    }

    /**
     * Create a simple IType from an Item entity.
     */
    public static IType createType(EntityItemSimpleBaseType base) throws IllegalArgumentException {
	return createType(getSimpleDatatype(base.getDatatype()), (String)base.getValue());
    }

    /**
     * Create a simple IType from a record item field.
     */
    public static IType createType(EntityItemFieldType field) throws IllegalArgumentException {
	return createType(getSimpleDatatype(field.getDatatype()), (String)field.getValue());
    }

    /**
     * Create a simple IType from a String.
     */
    public static IType createType(SimpleDatatypeEnumeration type, String value) {
	return createType(convertType(type), value);
    }

    /**
     * Create a simple IType from a String.
     *
     * @throws IllegalArgumentException if type == IType.Type.RECORD
     */
    public static IType createType(IType.Type type, String value) throws IllegalArgumentException {
	switch(type) {
	  case BINARY:
	    return new BinaryType(value);

	  case BOOLEAN:
	    return new BooleanType(value);

	  case EVR_STRING:
	    return new EvrStringType(value);

	  case FILESET_REVISION:
	    return new FilesetRevisionType(value);

	  case FLOAT:
	    return new FloatType(value);

	  case INT:
	    return new IntType(value);

	  case IOS_VERSION:
	    return new IosVersionType(value);

	  case IPV_4_ADDRESS:
	    return new Ip4AddressType(value);

	  case IPV_6_ADDRESS:
	    return new Ip6AddressType(value);

	  case VERSION:
	    return new VersionType(value);

	  case RECORD:
	    throw new IllegalArgumentException(type.toString());

	  case STRING:
	  default:
	    return new StringType(value);
	}
    }
}
