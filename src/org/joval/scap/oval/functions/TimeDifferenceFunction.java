// Copyright (C) 2014 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.functions;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.NoSuchElementException;

import jsaf.intf.system.IComputerSystem;
import jsaf.intf.system.ISession;
import jsaf.provider.windows.Timestamp;

import scap.oval.definitions.core.DateTimeFormatEnumeration;
import scap.oval.definitions.core.TimeDifferenceFunctionType;

import org.joval.intf.scap.oval.IType;
import org.joval.scap.oval.OvalException;
import org.joval.scap.oval.types.TypeFactory;
import org.joval.util.JOVALMsg;

/**
 * Implementation for the time difference function type.
 *
 * @see <a href="http://oval.mitre.org/language/version5.10/ovaldefinition/documentation/oval-definitions-schema.html#DateTimeFormatEnumeration">DateTimeFormatEnumeration</a>
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class TimeDifferenceFunction implements IFunction {
    public TimeDifferenceFunction() {
    }

    // Implement IFunction

    public Class<?> getFunctionType() {
	return TimeDifferenceFunctionType.class;
    }

    public Collection<IType> compute(Object obj, IFunctionContext fc) throws NoSuchElementException,
		UnsupportedOperationException, IllegalArgumentException, ResolveException, OvalException {

	if (getFunctionType().isInstance(obj)) {
	    TimeDifferenceFunctionType function = (TimeDifferenceFunctionType)obj;
	    Collection<IType> values = new ArrayList<IType>();
	    List<Object> children = function.getObjectComponentOrVariableComponentOrLiteralComponent();
	    Collection<IType> ts1;
	    Collection<IType> ts2;
	    if (children.size() == 1) {
		function.setFormat1(DateTimeFormatEnumeration.SECONDS_SINCE_EPOCH);
		ts1 = new ArrayList<IType>();
		try {
		    long tm = System.currentTimeMillis() / 1000L;
		    if (fc.getSession() instanceof IComputerSystem) {
			//
			// If the target is a computer, get the time from its clock, not ours.
			//
			tm = ((IComputerSystem)fc.getSession()).getTime() / 1000L;
		    }
		    ts1.add(TypeFactory.createType(IType.Type.INT, Long.toString(tm)));
		} catch (Exception e) {
		    // NB: if the ISession is not an IComputerSystem, we get a ClassCastException, which is fine
		    throw new ResolveException(e);
		}
		ts2 = fc.resolveComponent(children.get(0));
	    } else if (children.size() == 2) {
		ts1 = fc.resolveComponent(children.get(0));
		ts2 = fc.resolveComponent(children.get(1));
	    } else {
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_BAD_TIMEDIFFERENCE, Integer.toString(children.size()));
		throw new ResolveException(msg);
	    }
	    for (IType time1 : ts1) {
		try {
		    long tm1 = getTime(time1.getString(), function.getFormat1());
		    for (IType time2 : ts2) {
			long tm2 = getTime(time2.getString(), function.getFormat2());
			long diff = (tm1 - tm2)/1000L; // convert diff to seconds
			values.add(TypeFactory.createType(IType.Type.INT, Long.toString(diff)));
		    }
		} catch (IllegalArgumentException e) {
		    throw new ResolveException(e.getMessage());
		} catch (ParseException e) {
		    throw new ResolveException(e.getMessage());
		}
	    }
	    return values;
	} else {
	    throw new IllegalArgumentException(obj.toString());
	}
    }

    // Private

    /**
     * Convert a string to a long time given a DateTimeFormatEnumeration.
     */
    private long getTime(String s, DateTimeFormatEnumeration format) throws IllegalArgumentException, ParseException {
	String sdf = null;

	switch(format) {
	  case SECONDS_SINCE_EPOCH:
	    return Long.parseLong(s) * 1000L;

	  case WIN_FILETIME:
	    return Timestamp.getTime(new BigInteger(s, 16));

	  case DAY_MONTH_YEAR:
	    switch(s.length()) {
	      case 8:
	      case 9:
	      case 10:
		if (s.charAt(1) == '/' || s.charAt(2) == '/') {
		    sdf = "dd/MM/yyyy";
		} else {
		    sdf = "dd-MM-yyyy";
		}
		break;

	      case 18:
	      case 19:
	      case 20:
		if (s.charAt(1) == '/' || s.charAt(2) == '/') {
		    sdf = "dd/MM/yyyy HH:mm:ss";
		} else {
		    sdf = "dd-MM-yyyy HH:mm:ss";
		}
		break;

	      default:
		break;
	    }
	    break;

	  case MONTH_DAY_YEAR:
	    switch(s.length()) {
	      case 8:
	      case 9:
	      case 10:
		if (s.charAt(1) == '/' || s.charAt(2) == '/') {
		    sdf = "MM/dd/yyyy";
		} else {
		    sdf = "MM-dd-yyyy";
		}
		break;

	      case 11:
	      case 12:
		sdf = "MMM, dd yyyy";
		break;

	      case 17:
	      case 18:
	      case 19:
	      case 20:
	      case 21:
		if (s.charAt(1) == '/' || s.charAt(2) == '/') {
		    sdf = "MM/dd/yyyy HH:mm:ss";
		    break;
		} else if (s.charAt(1) == '-' || s.charAt(2) == '-') {
		    sdf = "MM-dd-yyyy HH:mm:ss";
		    break;
		} else if (s.charAt(3) == ',') {
		    sdf = "MMM, dd yyyy HH:mm:ss";
		    break;
		}
		// fall-through

	      default:
		if (s.indexOf(":") == -1) {
		    sdf = "MMMMM, dd yyyy";
		} else {
		    if (s.indexOf(",") == -1) {
			sdf = "MMMMM dd yyyy HH:mm:ss";
		    } else {
			sdf = "MMMMM, dd yyyy HH:mm:ss";
		    }
		}
		break;
	    }
	    break;

	  case YEAR_MONTH_DAY: {
	    switch(s.length()) {
	      case 8:
	      case 9:
	      case 10:
		if (s.charAt(4) == '/') {
		    sdf = "yyyy/MM/dd";
		} else if (s.charAt(4) == '-') {
		    sdf = "yyyy-MM-dd";
		} else {
		    sdf = "yyyyMMdd";
		}
		break;

	      case 15:
		sdf = "yyyyMMdd'T'HHmmss";
		break;

	      case 19:
		if (s.charAt(4) == '/') {
		    sdf = "yyyy/MM/dd HH:mm:ss";
		} else {
		    sdf = "yyyy-MM-dd HH:mm:ss";
		}
		break;

	      default:
		break;
	    }
	    break;
	  }
	}

	if (sdf == null) {
	    throw new IllegalArgumentException(JOVALMsg.getMessage(JOVALMsg.ERROR_ILLEGAL_TIME, format, s));
	} else {
	    SimpleDateFormat df = new SimpleDateFormat(sdf);
	    df.setTimeZone(TimeZone.getTimeZone("GMT"));
	    Date date = df.parse(s);
	    df.applyPattern("dd MMM yyyy HH:mm:ss z");
	    return date.getTime();
	}
    }
}
