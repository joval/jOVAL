// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xccdf.engine;

import xccdf.schemas.core.ResultEnumType;

/**
 * DAS: This is just my unofficial guess at how to roll-up multiple results.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RuleResult {
    private int err=0, fail=0, inf=0, na=0, nc=0, ns=0, pass=0, unk=0;

    public RuleResult() {}

    public void add(ResultEnumType result) {
	switch(result) {
	  case ERROR:
	    err++;
	    break;
	  case FAIL:
	    fail++;
	    break;
	  case INFORMATIONAL:
	    inf++;
	    break;
	  case NOTAPPLICABLE:
	    na++;
	    break;
	  case NOTCHECKED:
	    nc++;
	    break;
	  case NOTSELECTED:
	    ns++;
	    break;
	  case PASS:
	    pass++;
	    break;
	  case UNKNOWN:
	    unk++;
	    break;
	}
    }

    public ResultEnumType getResult() {
	if	  (pass > 0  && fail == 0 && err == 0 && inf >= 0 && na >= 0 && nc == 0 && ns >= 0 && unk == 0) {
	    return ResultEnumType.PASS;
	} else if (pass >= 0 && fail > 0  && err >= 0 && inf >= 0 && na >= 0 && nc >= 0 && ns >= 0 && unk >= 0) {
	    return ResultEnumType.FAIL;
	} else if (pass >= 0 && fail == 0 && err > 0  && inf >= 0 && na >= 0 && nc >= 0 && ns >= 0 && unk >= 0) {
	    return ResultEnumType.ERROR;
	} else if (pass >= 0 && fail == 0 && err == 0 && inf > 0  && na >= 0 && nc == 0 && ns >= 0 && unk == 0) {
	    return ResultEnumType.INFORMATIONAL;
	} else if (pass == 0 && fail == 0 && err == 0 && inf == 0 && na > 0  && nc == 0 && ns >= 0 && unk == 0) {
	    return ResultEnumType.NOTAPPLICABLE;
	} else if (pass == 0 && fail == 0 && err == 0 && inf == 0 && na == 0 && nc > 0  && ns >= 0 && unk == 0) {
	    return ResultEnumType.NOTCHECKED;
	} else if (pass == 0 && fail == 0 && err == 0 && inf == 0 && na == 0 && nc == 0 && ns > 0  && unk == 0) {
	    return ResultEnumType.NOTSELECTED;
	} else if (pass == 0 && fail == 0 && err == 0 && inf == 0 && na == 0 && nc == 0 && ns == 0 && unk >= 0) {
	    return ResultEnumType.UNKNOWN;
	}
	return ResultEnumType.UNKNOWN;
    }
}
