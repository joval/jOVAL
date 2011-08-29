// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.wmi.query;

import java.util.Iterator;
import java.util.logging.Level;

import org.jinterop.dcom.core.JIArray;
import org.jinterop.dcom.core.JIString;
import org.jinterop.dcom.core.JIUnsignedByte;
import org.jinterop.dcom.core.JIVariant;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.jinterop.dcom.impls.automation.IJIDispatch;
import org.jinterop.dcom.common.JIException;

import com.h9labs.jwbem.SWbemProperty;

import org.joval.io.LittleEndian;
import org.joval.intf.windows.wmi.ISWbemObject;
import org.joval.intf.windows.wmi.ISWbemObjectSet;
import org.joval.intf.windows.wmi.ISWbemProperty;
import org.joval.intf.windows.wmi.ISWbemPropertySet;
import org.joval.util.JOVALSystem;
import org.joval.os.windows.wmi.WmiException;

/**
 * Wrapper for an SWbemProperty.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SimpleSWbemProperty implements ISWbemProperty {
    private SWbemProperty property;

    SimpleSWbemProperty(SWbemProperty property) {
	this.property = property;
    }

    // Implement ISWbemProperty

    public String getName() throws WmiException {
	try {
	    return property.getName();
	} catch (JIException e) {
	    throw new WmiException(e);
	}
    }

    public Object getValue() throws WmiException {
	try {
	    return property.getValue();
	} catch (JIException e) {
	    throw new WmiException(e);
	}
    }

    public Integer getValueAsInteger() throws WmiException {
	try {
	    return property.getValueAsInteger();
	} catch (JIException e) {
	    throw new WmiException(e);
	}
    }
    
    public Long getValueAsLong() throws WmiException {
	try {
	    return property.getValueAsLong();
	} catch (JIException e) {
	    throw new WmiException(e);
	}
    }

    public Boolean getValueAsBoolean() throws WmiException {
	try {
	    return property.getValueAsBoolean();
	} catch (JIException e) {
	    throw new WmiException(e);
	}
    }

    /**
     * Returns null if the value is not a String.
     */
    public String getValueAsString() throws WmiException {
	try {
	    return getValueAsString(property.getValue());
	} catch (JIException e) {
	    throw new WmiException(e);
	}
    }

    /**
     * Returns null if the value is not an Array.
     */
    public String[] getValueAsArray() throws WmiException {
	try {
	    Object obj = property.getValue();
	    if (obj instanceof JIArray) {
		JIVariant[] va = (JIVariant[])((JIArray)obj).getArrayInstance();
		int len = va.length;
		String[] sa = new String[len];
		for (int i=0; i < len; i++) {
		    sa[i] = va[i].getObjectAsString2();
		}
		return sa;
	    } else {
		return null;
	    }
	} catch (JIException e) {
	    throw new WmiException(e);
	}
    }

    // Private

    private String getValueAsString(Object obj) throws JIException {
	if (obj instanceof JIString) {
	    return ((JIString)obj).getString();
	} else if (obj instanceof IJIComObject) {
	    IJIDispatch dispatch = (IJIDispatch)JIObjectFactory.narrowObject((IJIComObject)obj);
	    return getValueAsString(dispatch.get("value"));
	} else if (obj instanceof JIVariant) {
	    JIVariant var = (JIVariant)obj;
	    if (var.isArray()) {
		JIArray array = var.getObjectAsArray();
		Object[] oa = (Object[])array.getArrayInstance();
		StringBuffer sb = new StringBuffer();
		for (int i=0; i < oa.length; i++) {
		    sb.append(getValueAsString(oa[i]));
		}
		return sb.toString();
	    } else {
		return getValueAsString(var.getObject());
	    }
	} else if (obj instanceof JIUnsignedByte) {
	    JIUnsignedByte ub = (JIUnsignedByte)obj;
	    return LittleEndian.toHexString(ub.getValue().byteValue());
	} else {
	    JOVALSystem.getLogger().log(Level.WARNING,
					JOVALSystem.getMessage("ERROR_WMI_STR_CONVERSION", obj.getClass().getName()));
	    return null;
	}
    }
}
