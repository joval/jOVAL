// Copyright (C) 2014 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.functions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jsaf.util.StringTools;

import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.MessageType;
import scap.oval.definitions.core.RegexCaptureFunctionType;

import org.joval.intf.scap.oval.IType;
import org.joval.scap.oval.Factories;
import org.joval.scap.oval.OvalException;
import org.joval.scap.oval.types.StringType;
import org.joval.scap.oval.types.TypeFactory;
import org.joval.util.JOVALMsg;

/**
 * Implementation for the regex capture function type.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RegexCaptureFunction implements IFunction {
    public RegexCaptureFunction() {
    }

    // Implement IFunction

    public Class<?> getFunctionType() {
	return RegexCaptureFunctionType.class;
    }

    public Collection<IType> compute(Object obj, IFunctionContext fc) throws NoSuchElementException,
		UnsupportedOperationException, IllegalArgumentException, ResolveException, OvalException {

	if (getFunctionType().isInstance(obj)) {
	    RegexCaptureFunctionType function = (RegexCaptureFunctionType)obj;
	    Pattern p = StringTools.pattern(function.getPattern());
	    Collection<IType> values = new ArrayList<IType>();
	    for (IType value : fc.resolveComponent(fc.getComponent(function))) {
		Matcher m = p.matcher(value.getString());
		if (m.groupCount() >= 1) {
		    if (m.find()) {
			values.add(TypeFactory.createType(IType.Type.STRING, m.group(1)));
		    } else {
			values.add(StringType.EMPTY);
		    }
		} else {
		    values.add(StringType.EMPTY);
		    MessageType message = Factories.common.createMessageType();
		    message.setLevel(MessageLevelEnumeration.WARNING);
		    message.setValue(JOVALMsg.getMessage(JOVALMsg.WARNING_REGEX_GROUP, p.pattern()));
		    fc.addMessage(message);
		}
	    }
	    return values;
	} else {
	    throw new IllegalArgumentException(obj.toString());
	}
    }
}
