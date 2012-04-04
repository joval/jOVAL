// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.engine;

import java.math.BigInteger;
import java.math.BigDecimal;

import oval.schemas.common.ComplexDatatypeEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;

import oval.schemas.definitions.core.EntityStateFieldType;
import oval.schemas.definitions.core.EntityStateRecordType;
import oval.schemas.definitions.core.EntityStateSimpleBaseType;
import oval.schemas.definitions.core.LiteralComponentType;
import oval.schemas.systemcharacteristics.core.EntityItemAnySimpleType;
import oval.schemas.systemcharacteristics.core.EntityItemFieldType;
import oval.schemas.systemcharacteristics.core.EntityItemRecordType;
import oval.schemas.systemcharacteristics.core.EntityItemSimpleBaseType;

import org.joval.oval.types.BinaryType;
import org.joval.oval.types.BooleanType;
import org.joval.oval.types.EvrStringType;
import org.joval.oval.types.FilesetRevisionType;
import org.joval.oval.types.FloatType;
import org.joval.oval.types.IntType;
import org.joval.oval.types.IosVersionType;
import org.joval.oval.types.Ip4AddressType;
import org.joval.oval.types.Ip6AddressType;
import org.joval.oval.types.IType;
import org.joval.oval.types.RecordType;
import org.joval.oval.types.StringType;
import org.joval.oval.types.VersionType;
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

    /**
     * Utility method to obtain a SimpleDatatypeEnumeration from a String.
     */
    static SimpleDatatypeEnumeration getSimpleDatatype(String datatype) throws IllegalArgumentException {
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
    static ComplexDatatypeEnumeration getComplexDatatype(String datatype) throws IllegalArgumentException {
	for (ComplexDatatypeEnumeration type : ComplexDatatypeEnumeration.values()) {
	    if (type.value().equals(datatype)) {
		return type;
	    }
	}
	throw new IllegalArgumentException(JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_DATATYPE, datatype));
    }

    /**
     * Perform item type conversion.
     *
     * @throws IllegalArgumentException if there is a problem with the base item
     * @throws UnsupportedOperationException if the base item is incompatible with the target type
     */
    static EntityItemSimpleBaseType cast(EntityItemSimpleBaseType base, SimpleDatatypeEnumeration type)
		 throws IllegalArgumentException {

	TypedData converted = new TypedData(type, (String)base.getValue());
	EntityItemAnySimpleType result = Factories.sc.core.createEntityItemAnySimpleType();
	result.setDatatype(type.value());
	result.setValue(converted.value.toString());
	return result;
    }

    /**
     * Attempt to convert to a new datatype. If the datatype is identical, the original argument is returned.
     *
     * @throws IllegalArgumentException if an attempt is made to cast a COMPLEX type, or if the cast cannot be performed.
     */
    static TypedData cast(TypedData base, SimpleDatatypeEnumeration type) throws IllegalArgumentException {
	if (base.getType() == Type.COMPLEX) {
	    throw new IllegalArgumentException(Type.COMPLEX.toString());
	}
	if (type == base.value.getType()) {
	    return base;
	} else {
	    return new TypedData(type, base.value.toString());
	}
    }

    // Constructors

    TypedData(IType type) {
	init(type.getType(), type.toString());
    }

    TypedData(SimpleDatatypeEnumeration type, String value) {
	init(type, value);
    }

    TypedData(String datatype, String value) {
	init(datatype, value);
    }

    TypedData(EntityItemRecordType data) throws IllegalArgumentException {
	type = Type.COMPLEX;
	RecordType record = new RecordType();
	for (EntityItemFieldType field : data.getField()) {
	    record.addField(field.getName(), new TypedData(field).getValue());
	}
	value = record;
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

    TypedData(EntityStateRecordType data) {
	type = Type.COMPLEX;
	RecordType record = new RecordType();
	for (EntityStateFieldType field : data.getField()) {
	    record.addField(field.getName(), new TypedData(field).getValue());
	}
	value = record;
    }

    TypedData(EntityStateFieldType field) {
	init(field.getDatatype(), (String)field.getValue());
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

    TypedData(EntityStateSimpleBaseType data) {
	init(data.getDatatype(), (String)data.getValue());
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
	init(SimpleDatatypeEnumeration.FLOAT, value.toString());
    }

    TypedData(BigInteger value) {
	init(SimpleDatatypeEnumeration.INT, value.toString());
    }

    @Override
    public boolean equals(Object obj) {
	if (obj instanceof TypedData) {
	    TypedData other = (TypedData)obj;
	    if (other.type == type) {
		if (type == Type.SIMPLE) {
		    if (value.getClass().getName().equals(other.value.getClass().getName())) {
			@SuppressWarnings("unchecked")
			IType<Object> t1 = (IType<Object>)value;
			@SuppressWarnings("unchecked")
			IType<Object> t2 = (IType<Object>)other.value;
			return t1.compareTo(t2) == 0;
		    } else {
			return false;
		    }
		} else {
		    if (value instanceof RecordType && other.value instanceof RecordType) {
			return 0 == ((RecordType)value).compareTo((RecordType)other.value);
		    } else {
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
	if (type == Type.SIMPLE) {
	    return new StringBuffer(value.getType().value()).append(".").append(value.toString()).toString().hashCode();
	} else {
	    return super.hashCode();
	}
    }

    Type getType() {
	return type;
    }

    SimpleDatatypeEnumeration getSimpleType() throws UnsupportedOperationException {
	if (type == Type.COMPLEX) {
	    throw new UnsupportedOperationException("getSimpleType");
	}
	return getSimpleDatatype(value.getType().value());
    }

    ComplexDatatypeEnumeration getComplexType() throws UnsupportedOperationException {
	if (type == Type.SIMPLE) {
	    throw new UnsupportedOperationException("getComplexType");
	}
	return ComplexDatatypeEnumeration.RECORD;
    }

    IType getValue() {
	return value;
    }

    String getString() throws UnsupportedOperationException {
	if (type == Type.COMPLEX) {
	    throw new UnsupportedOperationException("getSimpleType");
	}
	return value.toString();
    }

    int getInt() throws UnsupportedOperationException, IllegalArgumentException {
	if (type == Type.COMPLEX) {
	    throw new UnsupportedOperationException("getSimpleType");
	}
	switch(getSimpleDatatype(value.getType().value())) {
	  case INT:
	    return ((IntType)value).getData().intValue();

	  default:
	    return ((IntType)cast(this, SimpleDatatypeEnumeration.INT).value).getData().intValue();
	}
    }

    // Private

    private Type type;		// SIMPLE or COMPLEX
    private IType value;	// Raw value data

    private void init(String datatype, String value) throws IllegalArgumentException {
	init(getSimpleDatatype(datatype), value);
    }

    private void init(SimpleDatatypeEnumeration type, String value) throws IllegalArgumentException {
	this.type = Type.SIMPLE;

	switch(type) {
	  case BINARY:
	    this.value = new BinaryType(value);
	    break;

	  case BOOLEAN:
	    this.value = new BooleanType(value);
	    break;

	  case EVR_STRING:
	    this.value = new EvrStringType(value);
	    break;

	  case FILESET_REVISION:
	    this.value = new FilesetRevisionType(value);
	    break;

	  case FLOAT:
	    this.value = new FloatType(value);
	    break;

	  case INT:
	    this.value = new IntType(value);
	    break;

	  case IOS_VERSION:
	    this.value = new IosVersionType(value);
	    break;

	  case IPV_4_ADDRESS:
	    this.value = new Ip4AddressType(value);
	    break;

	  case IPV_6_ADDRESS:
	    this.value = new Ip6AddressType(value);
	    break;

	  case VERSION:
	    this.value = new VersionType(value);
	    break;

	  case STRING:
	  default:
	    this.value = new StringType(value);
	    break;
	}
    }
}
