// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.ocil;

import ocil.schemas.core.TestActionConditionType;

/**
 * A TestActionConditionType that makes it possible to determine whether a boolean test action condition is a when_true,
 * or a when_false.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class BooleanTestActionConditionType extends TestActionConditionType {
    private boolean type;

    public BooleanTestActionConditionType(boolean type, TestActionConditionType prototype) {
	super();
	if (prototype.isSetArtifactRefs()) {
	    artifactRefs = prototype.getArtifactRefs();
	}
	if (prototype.isSetResult()) {
	    result = prototype.getResult();
	}
	if (prototype.isSetTestActionRef()) {
	    testActionRef = prototype.getTestActionRef();
	}
	this.type = type;
    }

    /**
     * @returns true when a when_true, false when a when_false.
     */
    public boolean getType() {
	return type;
    }
}
