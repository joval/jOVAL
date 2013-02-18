// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.xccdf.engine;

import scap.xccdf.CcOperatorEnumType;
import scap.xccdf.ResultEnumType;

/**
 * Expansion of the XCCDF check result truth tables, for the evaluation of multiple results.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class CheckData {
    private int p=0, f=0, u=0, e=0, n=0, k=0, s=0, i=0;
    private boolean negate;

    public CheckData(boolean negate) {
	this.negate = negate;
    }

    public void add(ResultEnumType result) {
	switch(result) {
	  case PASS:
	    p++;
	    break;
	  case FAIL:
	    f++;
	    break;
	  case UNKNOWN:
	    u++;
	    break;
	  case ERROR:
	    e++;
	    break;
	  case NOTAPPLICABLE:
	    n++;
	    break;
	  case NOTCHECKED:
	    k++;
	    break;
	  case NOTSELECTED:
	    s++;
	    break;
	  case INFORMATIONAL:
	    i++;
	    break;
	}
    }

    public ResultEnumType getResult(CcOperatorEnumType operator) {
	switch(operator) {
	  case AND:
	    if (       p > 0 && f == 0 && u == 0 && e == 0 && n >= 0 && k >= 0 && s >= 0 && i >= 0) {
		if (negate) {
		    return ResultEnumType.FAIL;
		} else {
		    return ResultEnumType.PASS;
		}
	    } else if (p >= 0 && f > 0 && u >= 0 && e >= 0 && n >= 0 && k >= 0 && s >= 0 && i >= 0) {
		if (negate) {
		    return ResultEnumType.PASS;
		} else {
		    return ResultEnumType.FAIL;
		}
	    } else if (p >= 0 && f == 0 && u > 0 && e >= 0 && n >= 0 && k >= 0 && s >= 0 && i >= 0) {
		return ResultEnumType.UNKNOWN;
	    } else if (p >= 0 && f == 0 && u >= 0 && e > 0 && n >= 0 && k >= 0 && s >= 0 && i >= 0) {
		return ResultEnumType.ERROR;
	    } else if (p == 0 && f == 0 && u == 0 && e == 0 && n > 0 && k >= 0 && s >= 0 && i >= 0) {
		return ResultEnumType.NOTAPPLICABLE;
	    } else if (p == 0 && f == 0 && u == 0 && e == 0 && n == 0 && k > 0 && s >= 0 && i >= 0) {
		return ResultEnumType.NOTCHECKED;
	    } else if (p == 0 && f == 0 && u == 0 && e == 0 && n == 0 && k == 0 && s > 0 && i >= 0) {
		return ResultEnumType.NOTSELECTED;
	    } else if (p == 0 && f == 0 && u == 0 && e == 0 && n == 0 && k == 0 && s == 0 && i > 0) {
		return ResultEnumType.INFORMATIONAL;
	    }
	    break;

	  case OR:
	    if (       p > 0 && f >= 0 && u >= 0 && e >= 0 && n >= 0 && k >= 0 && s >= 0 && i >= 0) {
		if (negate) {
		    return ResultEnumType.FAIL;
		} else {
		    return ResultEnumType.PASS;
		}
	    } else if (p == 0 && f > 0 && u == 0 && e == 0 && n >= 0 && k >= 0 && s >= 0 && i >= 0) {
		if (negate) {
		    return ResultEnumType.PASS;
		} else {
		    return ResultEnumType.FAIL;
		}
	    } else if (p == 0 && f >= 0 && u > 0 && e == 0 && n >= 0 && k >= 0 && s >= 0 && i >= 0) {
		return ResultEnumType.UNKNOWN;
	    } else if (p == 0 && f >= 0 && u == 0 && e > 0 && n >= 0 && k >= 0 && s >= 0 && i >= 0) {
		return ResultEnumType.ERROR;
	    } else if (p == 0 && f == 0 && u == 0 && e == 0 && n > 0 && k >= 0 && s >= 0 && i >= 0) {
		return ResultEnumType.NOTAPPLICABLE;
	    } else if (p == 0 && f == 0 && u == 0 && e == 0 && n == 0 && k > 0 && s >= 0 && i >= 0) {
		return ResultEnumType.NOTCHECKED;
	    } else if (p == 0 && f == 0 && u == 0 && e == 0 && n == 0 && k == 0 && s > 0 && i >= 0) {
		return ResultEnumType.NOTSELECTED;
	    } else if (p == 0 && f == 0 && u == 0 && e == 0 && n == 0 && k == 0 && s == 0 && i >= 0) {
		return ResultEnumType.INFORMATIONAL;
	    }
	    break;
	}
	return ResultEnumType.UNKNOWN;
    }
}
