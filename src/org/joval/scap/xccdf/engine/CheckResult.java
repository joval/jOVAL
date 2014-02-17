// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.xccdf.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;

import scap.xccdf.CheckType;
import scap.xccdf.MessageType;
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
    private Collection<MessageType> messages;
    private Collection<ISystem.IResult> results;

    CheckResult(ResultEnumType result, CheckType check) {
        type = Type.SINGLE;
        this.result = result;
        this.check = check;
    }

    CheckResult() {
        type = Type.MULTI;
        results = new ArrayList<ISystem.IResult>();
    }

    void addMessage(MessageType message) {
	if (messages == null) {
	    messages = new ArrayList<MessageType>();
	}
	messages.add(message);
    }

    // Implement IResult

    public Type getType() {
        return type;
    }

    public Collection<MessageType> getMessages() {
	if (messages == null) {
	    return Collections.<MessageType>emptyList();
	} else {
	    return messages;
	}
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

    public Collection<ISystem.IResult> getResults() throws NoSuchElementException {
        if (results == null) {
            throw new NoSuchElementException();
        } else {
            return results;
        }
    }
}
