// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.engine;

import oval.schemas.common.ExistenceEnumeration;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * @see http://oval.mitre.org/language/version5.9/ovaldefinition/documentation/oval-common-schema.html#ExistenceEnumeration
 */
class ExistenceData {
    int ex, de, er, nc;

    ExistenceData() {
	ex = 0;
	de = 0;
	er = 0;
	nc = 0;
    }

    void addStatus(StatusEnumeration status) {
	switch(status) {
	  case DOES_NOT_EXIST:
	    de++;
	    break;
	  case ERROR:
	    er++;
	    break;
	  case EXISTS:
	    ex++;
	    break;
	  case NOT_COLLECTED:
	    nc++;
	    break;
	}
    }

    ResultEnumeration getResult(ExistenceEnumeration existence) throws OvalException {
	ResultEnumeration result = ResultEnumeration.UNKNOWN;
	switch(existence) {
	  case ALL_EXIST:
	    if        (ex > 0	&& de == 0	&& er == 0	&& nc == 0) {
		return ResultEnumeration.TRUE;
	    } else if (ex == 0	&& de == 0	&& er == 0	&& nc == 0) {
		return ResultEnumeration.FALSE;
	    } else if (ex >= 0	&& de > 0	&& er >= 0	&& nc >= 0) {
		return ResultEnumeration.FALSE;
	    } else if (ex >= 0	&& de == 0	&& er > 0	&& nc >= 0) {
		return ResultEnumeration.ERROR;
	    } else if (ex >= 0	&& de == 0	&& er == 0	&& nc > 0) {
		return ResultEnumeration.UNKNOWN;
	    }
	    break;

	  case ANY_EXIST:
	    if        (ex >= 0	&& de >= 0	&& er == 0	&& nc >= 0) {
		return ResultEnumeration.TRUE;
	    } else if (ex > 0	&& de >= 0	&& er > 0	&& nc >= 0) {
		return ResultEnumeration.TRUE;
	    } else if (ex == 0	&& de >= 0	&& er > 0	&& nc >= 0) {
		return ResultEnumeration.ERROR;
	    }
	    break;

	  case AT_LEAST_ONE_EXISTS:
	    if        (ex > 0	&& de >= 0	&& er >= 0	&& nc >= 0) {
		return ResultEnumeration.TRUE;
	    } else if (ex == 0	&& de >= 0	&& er == 0	&& nc == 0) { // spec says "de > 0"
		return ResultEnumeration.FALSE;
	    } else if (ex == 0	&& de >= 0	&& er > 0	&& nc == 0) {
		return ResultEnumeration.ERROR;
	    } else if (ex == 0	&& de >= 0	&& er == 0	&& nc > 0) {
		return ResultEnumeration.UNKNOWN;
	    }
	    break;

	  case NONE_EXIST:
	    if        (ex == 0	&& de >= 0	&& er == 0	&& nc == 0) {
		return ResultEnumeration.TRUE;
	    } else if (ex > 0	&& de >= 0	&& er >= 0	&& nc >= 0) {
		return ResultEnumeration.FALSE;
	    } else if (ex == 0	&& de >= 0	&& er > 0	&& nc >= 0) {
		return ResultEnumeration.ERROR;
	    } else if (ex == 0	&& de >= 0	&& er == 0	&& nc > 0) {
		return ResultEnumeration.UNKNOWN;
	    }
	    break;

	  case ONLY_ONE_EXISTS:
	    if        (ex == 1	&& de >= 0	&& er == 0	&& nc == 0) {
		return ResultEnumeration.TRUE;
	    } else if (ex > 1	&& de >= 0	&& er >= 0	&& nc >= 0) {
		return ResultEnumeration.FALSE;
	    } else if (ex == 0	&& de >= 0	&& er == 0	&& nc == 0) {
		return ResultEnumeration.FALSE;
	    } else if (ex < 2	&& de >= 0	&& er > 0	&& nc >= 0) {
		return ResultEnumeration.ERROR;
	    } else if (ex < 2	&& de >= 0	&& er == 0	&& nc > 0) {
		return ResultEnumeration.UNKNOWN;
	    }
	    break;

	  default:
	    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_EXISTENCE, ex));
	}
	return result;
    }
}

