// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xccdf.engine;

import xccdf.schemas.core.ResultEnumType;

/**
 * DAS: How should the results from multiple CheckContentRefTypes really be combined?
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RuleResult {
    private ResultEnumType result = ResultEnumType.NOTCHECKED;

    public RuleResult() {}

    public void add(ResultEnumType result) {
	this.result = result;
    }

    public ResultEnumType getResult() {
	return result;
    }
}
