// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import org.joval.util.JOVALSystem;

/**
 * A utility for dealing with Windows timestamps.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Timestamp {
    private final static BigInteger CNANOS_1601to1970	= new BigInteger("116444736000000000");
    private final static BigInteger TEN_K		= new BigInteger("10000");
    private final static BigInteger MILLIS_1601to1970	= new BigInteger("11644473600000");
    private final static SimpleDateFormat WMIDATEFORMAT = new SimpleDateFormat("yyyyMMddHHmmssZ");

    /**
     * Given a Java timestamp, return a Windows-style decimal timestamp, converted to a String.  Note that the last 4
     * digits will always be 0, as there is only enough information to express the time in milliseconds.
     */
    public static String toWindowsTimestamp(long javaTS) {
	BigInteger tm = new BigInteger(new Long(javaTS).toString());
	tm = tm.multiply(TEN_K); // 10K 100 nanosecs in one millisec
	return tm.add(CNANOS_1601to1970).toString();
    }

    /**
     * Given a WBEM timestamp of the form yyyyMMddHHmmss.SSSSSSsutc, return a Windows-style decimal timestamp.  The last
     * digit will always be a 0, as there is only enough information to express the time in microseconds.
     */
    public static String toWindowsTimestamp(String wmistr) throws NumberFormatException, ParseException {
	StringBuffer sb = new StringBuffer(wmistr.substring(0,14));
	sb.append(wmistr.substring(21,22));
	int utcMinOffset = Integer.parseInt(wmistr.substring(22));
	int utcHrs = utcMinOffset/60;
	int utcMins = utcMinOffset % 60;
	String s = String.format("%02d%02d", utcHrs, utcMins);
	sb.append(s);
	long secondsSince1970 = WMIDATEFORMAT.parse(sb.toString()).getTime()/1000L;
	StringBuffer sb2 = new StringBuffer(Long.toString(secondsSince1970));
	BigInteger tm = new BigInteger(sb2.append(wmistr.substring(15, 21)).append("0").toString()); //cnanos
	return tm.add(CNANOS_1601to1970).toString();
    }

    /**
     * Given the number of 100-nanosecond ticks since 1601, return the number of milliseconds since 1970.
     */
    public static long getTime(BigInteger timestamp) {
	String s = timestamp.toString(10);
	if (s.length() > 4) {
	    s = s.substring(0, s.length()-4); // divide by 10k
	}
	BigInteger tm = new BigInteger(s).subtract(MILLIS_1601to1970);
	return tm.longValue();
    }
}
