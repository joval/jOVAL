// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.xccdf.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;

import scap.xccdf.CheckType;
import scap.xccdf.InstanceResultType;
import scap.xccdf.ResultEnumType;

/**
 * Implementation of ISystem.IResult.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class CheckResult implements ISystem.IResult {
    private Type type;
    private ResultEnumType result;
    private CheckType check;
    private InstanceResultType inst;
    private Collection<ISystem.IResult> results;

    CheckResult(ResultEnumType result, CheckType check) {
	this(result, check, null);
    }

    CheckResult(ResultEnumType result, CheckType check, InstanceResultType inst) {
        type = Type.SINGLE;
        this.result = result;
        this.check = check;
        this.inst = inst;
    }

    CheckResult() {
        type = Type.MULTI;
        results = new ArrayList<ISystem.IResult>();
    }

    // Implement IResult

    public Type getType() {
        return type;
    }

    public ResultEnumType getResult() throws NoSuchElementException {
        if (result == null) {
            throw new NoSuchElementException();
        } else {
            return result;
        }
    }

    public CheckType getCheck() throws NoSuchElementException {
        if (check == null) {
            throw new NoSuchElementException();
        } else {
            return check;
        }
    }

    public InstanceResultType getInstance() throws NoSuchElementException {
        if (inst == null) {
            throw new NoSuchElementException();
        } else {
            return inst;
        }
    }

    public Collection<ISystem.IResult> getResults() throws NoSuchElementException {
        if (results == null) {
            throw new NoSuchElementException();
        } else {
            return results;
        }
    }
}
