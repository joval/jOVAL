// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Simple utility for working with RFC822-formatted date strings.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RFC822 {
    static final SimpleDateFormat formats[] = { new SimpleDateFormat("EEE, d MMM yy HH:mm:ss z"),
						new SimpleDateFormat("EEE, d MMM yy HH:mm z"),
						new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z"),
						new SimpleDateFormat("EEE, d MMM yyyy HH:mm z"),
						new SimpleDateFormat("d MMM yy HH:mm z"),
						new SimpleDateFormat("d MMM yy HH:mm:ss z"),
						new SimpleDateFormat("d MMM yyyy HH:mm z"),
						new SimpleDateFormat("d MMM yyyy HH:mm:ss z")
					      };

    /**
     * Convert the supplied RFC822-formatted date-time string into a millis-since-epoch timestamp.
     */
    public static final long valueOf(String s) throws IllegalArgumentException {
	for (SimpleDateFormat sdf : formats) {
	    try {
		return sdf.parse(s).getTime();
	    } catch (ParseException e) {
	    }
	}
	throw new IllegalArgumentException(s);
    }

    public static final String toString(long tm) {
	return formats[0].format(new Date(tm));
    }
}
