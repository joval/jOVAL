// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.windows.wmi;

import java.util.Iterator;

import com.jacob.com.Dispatch;
import com.jacob.com.SafeArray;
import com.jacob.com.Variant;
import com.jacob.com.VariantUtilities;

import org.joval.intf.windows.wmi.ISWbemProperty;
import org.joval.io.LittleEndian;
import org.joval.windows.wmi.WmiException;

/**
 * Wrapper for an SWbemProperty.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SWbemProperty implements ISWbemProperty {
    private Dispatch dispatch;
    private String name;
    private Variant value;

    SWbemProperty(Dispatch dispatch) {
	this.dispatch = dispatch;
	name = Dispatch.call(dispatch, "Name").toString();
	value = Dispatch.call(dispatch, "Value");
    }

    // Implement ISWbemProperty

    public String getName() throws WmiException {
	return name;
    }

    public Object getValue() throws WmiException {
        return value;
    }

    public Integer getValueAsInteger() throws WmiException {
	return value.getInt();
    }
    
    public Long getValueAsLong() throws WmiException {
	return value.getLong();
    }

    public Boolean getValueAsBoolean() throws WmiException {
	return value.getBoolean();
    }

    /**
     * Returns null if the value is not a String.
     */
    public String getValueAsString() throws WmiException {
	if (value.isNull()) {
	    return null;
	} else {
	    return getString(value);
	}
    }

    /**
     * Returns null if the value is not an Array.
     */
    public String[] getValueAsArray() throws WmiException {
	if (value.isNull()) {
	    return null;
	} else {
	    return value.toSafeArray().toStringArray();
	}
    }

    // Private

    private String getString(Variant var) {
	if (var.isNull()) {
	    return null;
	} else {
	    int type = var.getvt();
	    switch(type) {
	      case Variant.VariantString:
		return value.toString();

	      case Variant.VariantInt:
		return Integer.toString(value.getInt());

	      case Variant.VariantObject:
		return value.toString();

	      case Variant.VariantByte:
		return LittleEndian.toHexString(var.getByte());

	      case Variant.VariantDispatch:
		return getString(Dispatch.get(value.toDispatch(), "value"));

	      default:
		if (Variant.VariantArray == (Variant.VariantArray & type)) {
		    SafeArray sa = var.toSafeArray();
		    int arrayType = (Variant.VariantTypeMask & type);
System.out.println("DAS: arrayType=" + arrayType);
		    switch(arrayType) {
		      case Variant.VariantByte:
			return new String(sa.toByteArray());

		      default: {
			Variant[] va = sa.toVariantArray();
			StringBuffer sb = new StringBuffer();
			for (int i=0; i < va.length; i++) {
			    sb.append(getString(va[i]));
			}
			return sb.toString();
		      }
		    }
		} else {
		    return value.toString();
		}
	    }


	}
    }
}
