// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.util;

import oval.schemas.common.CheckEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.oval.OvalException;
import org.joval.util.JOVALSystem;

/**
 * @see http://oval.mitre.org/language/version5.9/ovaldefinition/documentation/oval-common-schema.html#CheckEnumeration
 */
public class CheckData extends OperatorData {
    public CheckData() {
	super();
    }

    public ResultEnumeration getResult(CheckEnumeration check) throws OvalException {
	ResultEnumeration result = ResultEnumeration.UNKNOWN;
	switch(check) {
	  case ALL:
	    if        (t > 0	&& f == 0	&& e == 0	&& u == 0	&& ne == 0	&& na >= 0) {
		return ResultEnumeration.TRUE;
	    } else if (t >= 0	&& f > 0	&& e >= 0	&& u >= 0	&& ne >= 0	&& na >= 0) {
		return ResultEnumeration.FALSE;
	    } else if (t >= 0	&& f == 0	&& e > 0	&& u >= 0	&& ne >= 0	&& na >= 0) {
		return ResultEnumeration.ERROR;
	    } else if (t >= 0	&& f == 0	&& e == 0	&& u > 0	&& ne >= 0	&& na >= 0) {
		return ResultEnumeration.UNKNOWN;
	    } else if (t >= 0	&& f == 0	&& e == 0	&& u == 0	&& ne > 0	&& na >= 0) {
		return ResultEnumeration.NOT_EVALUATED;
	    } else if (t == 0	&& f == 0	&& e == 0	&& u == 0	&& ne == 0	&& na > 0) {
		return ResultEnumeration.NOT_APPLICABLE;
	    }
	    break;

	  case AT_LEAST_ONE:
	    if        (t >= 1	&& f >= 0	&& e >= 0	&& u >= 0	&& ne >= 0	&& na >= 0) {
		return ResultEnumeration.TRUE;
	    } else if (t == 0	&& f > 0	&& e == 0	&& u == 0	&& ne == 0	&& na >= 0) {
		return ResultEnumeration.FALSE;
	    } else if (t == 0	&& f >= 0	&& e > 0	&& u >= 0	&& ne >= 0	&& na >= 0) {
		return ResultEnumeration.ERROR;
	    } else if (t == 0	&& f >= 0	&& e == 0	&& u > 0	&& ne >= 0	&& na >= 0) {
		return ResultEnumeration.UNKNOWN;
	    } else if (t == 0	&& f >= 0	&& e == 0	&& u == 0	&& ne > 0	&& na >= 0) {
		return ResultEnumeration.NOT_EVALUATED;
	    } else if (t == 0	&& f == 0	&& e == 0	&& u == 0	&& ne == 0	&& na > 0) {
		return ResultEnumeration.NOT_APPLICABLE;
	    }
	    break;

	  case ONLY_ONE:
	    if        (t == 1	&& f >= 0	&& e == 0	&& u == 0	&& ne == 0	&& na >= 0) {
		return ResultEnumeration.TRUE;
	    } else if (t > 1	&& f >= 0	&& e >= 0	&& u >= 0	&& ne >= 0	&& na >= 0) {
		return ResultEnumeration.FALSE;
	    } else if (t == 0	&& f > 0	&& e == 0	&& u == 0	&& ne == 0	&& na >= 0) {
		return ResultEnumeration.FALSE;
	    } else if (t < 2	&& f >= 0	&& e > 0	&& u >= 0	&& ne >= 0	&& na >= 0) {
		return ResultEnumeration.ERROR;
	    } else if (t < 2	&& f >= 0	&& e == 0	&& u > 0	&& ne >= 0	&& na >= 0) {
		return ResultEnumeration.UNKNOWN;
	    } else if (t < 2	&& f >= 0	&& e == 0	&& u == 0	&& ne > 0	&& na >= 0) {
		return ResultEnumeration.NOT_EVALUATED;
	    } else if (t == 0	&& f == 0	&& e == 0	&& u == 0	&& ne == 0	&& na > 0) {
		return ResultEnumeration.NOT_APPLICABLE;
	    }
	    break;

	  case NONE_SATISFY:
	    if        (t == 0	&& f > 0	&& e == 0	&& u == 0	&& ne == 0	&& na >= 0) {
		return ResultEnumeration.TRUE;
	    } else if (t > 0	&& f >= 0	&& e >= 0	&& u >= 0	&& ne >= 0	&& na >= 0) {
		return ResultEnumeration.FALSE;
	    } else if (t == 0	&& f >= 0	&& e > 0	&& u >= 0	&& ne >= 0	&& na >= 0) {
		return ResultEnumeration.ERROR;
	    } else if (t == 0	&& f >= 0	&& e == 0	&& u > 0	&& ne >= 0	&& na >= 0) {
		return ResultEnumeration.UNKNOWN;
	    } else if (t == 0	&& f >= 0	&& e == 0	&& u == 0	&& ne > 0	&& na >= 0) {
		return ResultEnumeration.NOT_EVALUATED;
	    } else if (t == 0	&& f == 0	&& e == 0	&& u == 0	&& ne == 0	&& na > 0) {
		return ResultEnumeration.NOT_APPLICABLE;
	    }
	    break;

	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_CHECK", check));
	}
	return result;
    }
}

