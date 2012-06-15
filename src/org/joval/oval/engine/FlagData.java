// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.engine;

import oval.schemas.systemcharacteristics.core.FlagEnumeration;

import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;

/**
 * An object with a var_ref may be broken by the engine into a multitude of concrete objects.  The purpose of this class
 * is to aggregate all the flags to come up with a single result.
 */
class FlagData {
    int e=0, c=0, i=0, d=0, nc=0, na=0;

    FlagData() {
    }

    void add(FlagEnumeration flag) {
	switch(flag) {
	  case ERROR:
	    e++;
	    break;
	  case COMPLETE:
	    c++;
	    break;
	  case INCOMPLETE:
	    i++;
	    break;
	  case DOES_NOT_EXIST:
	    d++;
	    break;
	  case NOT_COLLECTED:
	    nc++;
	    break;
	  case NOT_APPLICABLE:
	    na++;
	    break;
	}
    }

    FlagEnumeration getFlag() throws OvalException {
	if (e > 0) {
	    return FlagEnumeration.ERROR;
	} else if (c > 0 &&	i == 0 &&	d >= 0 &&	nc == 0 &&	na >= 0) {
	    return FlagEnumeration.COMPLETE;
	} else if (c >= 0 &&	i > 0 &&	d >= 0 &&	nc >= 0 &&	na >= 0) {
	    return FlagEnumeration.INCOMPLETE;
	} else if (c == 0 &&	i == 0 &&	d > 0 &&	nc == 0 &&	na >= 0) {
	    return FlagEnumeration.DOES_NOT_EXIST;
	} else if (c == 0 &&	i == 0 &&	d == 0 &&	nc > 0 &&	na == 0) {
	    return FlagEnumeration.NOT_COLLECTED;
	} else if (c == 0 &&	i == 0 &&	d == 0 &&	nc == 0 &&	na >= 0) {
	    return FlagEnumeration.NOT_APPLICABLE;
	} else {
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_FLAG, e, c, i, d, nc, na));
	}
    }
}

