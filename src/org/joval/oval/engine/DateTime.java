// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.engine;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Date;
import java.util.TimeZone;

import oval.schemas.definitions.core.DateTimeFormatEnumeration;

import org.joval.os.windows.Timestamp;
import org.joval.util.JOVALMsg;

/**
 * A utility for converting String data to time data in conjunction with a DateTimeFormatEnumeration. See:
 * http://oval.mitre.org/language/version5.10/ovaldefinition/documentation/oval-definitions-schema.html#DateTimeFormatEnumeration
 *
 * @author David A. Solin
 * @version %I% %G%
 */
class DateTime {
    /**
     * Convert a string to a long time given a DateTimeFormatEnumeration.
     */
    static long getTime(String s, DateTimeFormatEnumeration format) throws IllegalArgumentException, ParseException {
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
