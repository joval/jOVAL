// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.engine;

import java.math.BigInteger;
import java.math.BigDecimal;

import oval.schemas.common.ComplexDatatypeEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;

import oval.schemas.definitions.core.LiteralComponentType;
import oval.schemas.systemcharacteristics.core.EntityItemBinaryType;
import oval.schemas.systemcharacteristics.core.EntityItemBoolType;
import oval.schemas.systemcharacteristics.core.EntityItemComplexBaseType;
import oval.schemas.systemcharacteristics.core.EntityItemEVRStringType;
import oval.schemas.systemcharacteristics.core.EntityItemFieldType;
import oval.schemas.systemcharacteristics.core.EntityItemFilesetRevisionType;
import oval.schemas.systemcharacteristics.core.EntityItemFloatType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemIOSVersionType;
import oval.schemas.systemcharacteristics.core.EntityItemIPAddressType;
import oval.schemas.systemcharacteristics.core.EntityItemSimpleBaseType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.EntityItemVersionType;

import org.joval.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * A class that provides type-aware access to OVAL item data.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class TypedData {
    enum Type {
	SIMPLE, COMPLEX;
    }

    static final TypedData EMPTY_STRING = new TypedData("");

    static SimpleDatatypeEnumeration getSimpleDatatype(String datatype) throws IllegalArgumentException {
	for (SimpleDatatypeEnumeration type : SimpleDatatypeEnumeration.values()) {
	    if (type.value().equals(datatype)) {
		return type;
	    }
	}
	throw new IllegalArgumentException(datatype);
    }

    static ComplexDatatypeEnumeration getComplexDatatype(String datatype) throws IllegalArgumentException {
	for (ComplexDatatypeEnumeration type : ComplexDatatypeEnumeration.values()) {
	    if (type.value().equals(datatype)) {
		return type;
	    }
	}
	throw new IllegalArgumentException(datatype);
    }

    private Type type;
    private Object value;

    TypedData(SimpleDatatypeEnumeration type, String value) {
	init(type, value);
    }

    TypedData(EntityItemFieldType field) throws IllegalArgumentException {
	switch(field.getStatus()) {
	  case EXISTS:
	    init(field.getDatatype(), (String)field.getValue());
	    break;

	  default:
	    throw new IllegalArgumentException(JOVALMsg.getMessage(JOVALMsg.ERROR_TYPED_STATUS, field.getStatus()));
	}
    }

    TypedData(LiteralComponentType literal) {
	init(literal.getDatatype(), (String)literal.getValue());
    }

    TypedData(EntityItemSimpleBaseType data) throws IllegalArgumentException {
	switch(data.getStatus()) {
	  case EXISTS:
	    init(data.getDatatype(), (String)data.getValue());
	    break;

	  default:
	    throw new IllegalArgumentException(JOVALMsg.getMessage(JOVALMsg.ERROR_TYPED_STATUS, data.getStatus()));
	}
    }

    TypedData(String value) {
	init(SimpleDatatypeEnumeration.STRING, value);
    }

    TypedData(long value) {
	init(SimpleDatatypeEnumeration.INT, Long.toString(value));
    }

    TypedData(int value) {
	init(SimpleDatatypeEnumeration.INT, Integer.toString(value));
    }

    TypedData(BigDecimal value) {
	init(SimpleDatatypeEnumeration.INT, value.toString());
    }

    TypedData(BigInteger value) {
	init(SimpleDatatypeEnumeration.INT, value.toString());
    }

    TypedData(String datatype, String value) {
	init(datatype, value);
    }

    TypedData(EntityItemComplexBaseType data) {
	type = Type.COMPLEX;
	value = data;
    }

    @Override
    public boolean equals(Object obj) {
	if (obj instanceof TypedData) {
	    TypedData other = (TypedData)obj;
	    if (other.type == type) {
		if (type == Type.SIMPLE) {
		    EntityItemSimpleBaseType data = (EntityItemSimpleBaseType)value;
		    EntityItemSimpleBaseType otherData = (EntityItemSimpleBaseType)other.value;
		    return data.getDatatype().equals(otherData.getDatatype()) &&
			   ((String)data.getValue()).equals((String)otherData.getValue());
		} else {
		    EntityItemComplexBaseType data = (EntityItemComplexBaseType)value;
		    EntityItemComplexBaseType otherData = (EntityItemComplexBaseType)other.value;
		    return data.equals(otherData);
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
	if (type == Type.SIMPLE) {
	    EntityItemSimpleBaseType data = (EntityItemSimpleBaseType)value;
	    return new StringBuffer(data.getDatatype()).append(".").append((String)data.getValue()).toString().hashCode();
	} else {
	    return super.hashCode();
	}
    }

    Type getType() {
	return type;
    }

    Object getValue() {
	return value;
    }

    ComplexDatatypeEnumeration getComplexType() throws UnsupportedOperationException {
	if (type == Type.SIMPLE) {
	    throw new UnsupportedOperationException("getComplexType");
	}
	return getComplexDatatype(((EntityItemComplexBaseType)value).getDatatype());
    }

    SimpleDatatypeEnumeration getSimpleType() throws UnsupportedOperationException {
	if (type == Type.COMPLEX) {
	    throw new UnsupportedOperationException("getSimpleType");
	}
	return getSimpleDatatype(((EntityItemSimpleBaseType)value).getDatatype());
    }

    String getString() throws UnsupportedOperationException {
	if (type == Type.COMPLEX) {
	    throw new UnsupportedOperationException("getSimpleType");
	}
	return (String)((EntityItemSimpleBaseType)value).getValue();
    }

    int getInt() throws UnsupportedOperationException {
	return Integer.parseInt(getString());
    }

    byte[] getBytes() throws UnsupportedOperationException {
	return getString().getBytes();
    }

    // Private

    private void init(String datatype, String value) {
	init(getSimpleDatatype(datatype), value);
    }

    private void init(SimpleDatatypeEnumeration type, String value) {
	this.type = Type.SIMPLE;
	switch(type) {
	  case BINARY:
	    EntityItemBinaryType binary = Factories.sc.core.createEntityItemBinaryType();
	    binary.setDatatype(type.value());
	    binary.setValue(value);
	    this.value = binary;
	    break;

	  case BOOLEAN:
	    EntityItemBoolType bool = Factories.sc.core.createEntityItemBoolType();
	    bool.setDatatype(type.value());
	    bool.setValue(value);
	    this.value = bool;
	    break;

	  case EVR_STRING:
	    EntityItemEVRStringType evr = Factories.sc.core.createEntityItemEVRStringType();
	    evr.setDatatype(type.value());
	    evr.setValue(value);
	    this.value = evr;
	    break;

	  case FILESET_REVISION:
	    EntityItemFilesetRevisionType fileset = Factories.sc.core.createEntityItemFilesetRevisionType();
	    fileset.setDatatype(type.value());
	    fileset.setValue(value);
	    this.value = fileset;
	    break;

	  case FLOAT:
	    EntityItemFloatType flt = Factories.sc.core.createEntityItemFloatType();
	    flt.setDatatype(type.value());
	    flt.setValue(value);
	    this.value = flt;
	    break;

	  case INT:
	    EntityItemIntType integer = Factories.sc.core.createEntityItemIntType();
	    integer.setDatatype(type.value());
	    integer.setValue(value);
	    this.value = integer;
	    break;

	  case IOS_VERSION:
	    EntityItemIOSVersionType ios = Factories.sc.core.createEntityItemIOSVersionType();
	    ios.setDatatype(type.value());
	    ios.setValue(value);
	    this.value = ios;
	    break;

	  case IPV_4_ADDRESS:
	  case IPV_6_ADDRESS:
	    EntityItemIPAddressType address = Factories.sc.core.createEntityItemIPAddressType();
	    address.setDatatype(type.value());
	    address.setValue(value);
	    this.value = address;
	    break;

	  case VERSION:
	    EntityItemVersionType version = Factories.sc.core.createEntityItemVersionType();
	    version.setDatatype(type.value());
	    version.setValue(value);
	    this.value = version;
	    break;

	  case STRING:
	  default:
	    EntityItemStringType str = Factories.sc.core.createEntityItemStringType();
	    str.setDatatype(type.value());
	    str.setValue(value);
	    this.value = str;
	    break;
	}
    }
}
