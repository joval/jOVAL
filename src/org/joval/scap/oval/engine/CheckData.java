// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.engine;

import scap.oval.common.CheckEnumeration;
import scap.oval.common.OperationEnumeration;
import scap.oval.results.ResultEnumeration;

import org.joval.scap.oval.OvalException;
import org.joval.util.JOVALMsg;

/**
 * @see http://oval.mitre.org/language/version5.9/ovaldefinition/documentation/oval-common-schema.html#CheckEnumeration
 */
class CheckData extends OperatorData {
    CheckData() {
	super(false);
    }

    ResultEnumeration getResult(CheckEnumeration check) throws OvalException {
	ResultEnumeration result = ResultEnumeration.UNKNOWN;
	if (check == null) {
	    check = CheckEnumeration.ALL; // documented as the default value
	}
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
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_CHECK, check));
	}
	return result;
    }
}

