// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.independent;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.independent.Textfilecontent54Object;
import oval.schemas.systemcharacteristics.core.EntityItemAnySimpleType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.independent.TextfilecontentItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.io.IFile;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.ISession;
import org.joval.oval.CollectionException;
import org.joval.oval.OvalException;
import org.joval.oval.ResolveException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.StringTools;

/**
 * Evaluates Textfilecontent54Test OVAL tests.
 *
 * DAS: Specify a maximum file size supported
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Textfilecontent54Adapter extends TextfilecontentAdapter {
    public Textfilecontent54Adapter(ISession session) {
	super(session);
    }

    // Implement IAdapter

    /**
     * @override
     */
    public Class getObjectClass() {
	return Textfilecontent54Object.class;
    }

    // Protected

    /**
     * Parse the file as specified by the Object, and decorate the Item.
     *
     * @override
     */
    protected Collection<JAXBElement<? extends ItemType>> getItems(ItemType base, IFile f, IRequestContext rc)
		throws IOException, CollectionException, OvalException {

	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();

	TextfilecontentItem baseItem = null;
	if (base instanceof TextfilecontentItem) {
	    baseItem = (TextfilecontentItem)base;
	}
	Textfilecontent54Object tfcObj = null;
	if (rc.getObject() instanceof Textfilecontent54Object) {
	    tfcObj = (Textfilecontent54Object)rc.getObject();
	}

	if (baseItem != null && tfcObj != null) {
	    InputStream in = null;
	    try {
		int flags = 0;
		if (tfcObj.isSetBehaviors()) {
		    if (tfcObj.getBehaviors().isMultiline()) {
			flags |= Pattern.MULTILINE;
		    }
		    if (tfcObj.getBehaviors().isIgnoreCase()) {
			flags |= Pattern.CASE_INSENSITIVE;
		    }
		    if (tfcObj.getBehaviors().isSingleline()) {
			flags |= Pattern.DOTALL;
		    }
		} else {
		    flags = Pattern.MULTILINE;
		}
		Collection<Pattern> patterns = new Vector<Pattern>();
		if (tfcObj.getPattern().isSetVarRef()) {
		    for (String value : rc.resolve(tfcObj.getPattern().getVarRef())) {
			patterns.add(Pattern.compile(value, flags));
		    }
		} else {
		    patterns.add(Pattern.compile((String)tfcObj.getPattern().getValue(), flags));
		}

		//
		// Read the whole file into a buffer and search for the pattern
		//
		byte[] buff = new byte[256];
		int len = 0;
		StringBuffer sb = new StringBuffer();
		in = f.getInputStream();
		while ((len = in.read(buff)) > 0) {
		    sb.append(StringTools.toCharArray(buff), 0, len);
		}
		String s = sb.toString();

		OperationEnumeration op = tfcObj.getPattern().getOperation();
		switch(op) {
		  case PATTERN_MATCH:
		    for (Pattern p : patterns) {
			items.addAll(getItems(p, baseItem, s));
		    }
		    break;

		  default:
		    throw new CollectionException(JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op));
		}
	    } catch (PatternSyntaxException e) {
		JOVALSystem.getLogger().warn(JOVALMsg.ERROR_PATTERN, e.getMessage());
		throw new IOException(e);
	    } catch (ResolveException e) {
		MessageType msg = JOVALSystem.factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		String s = JOVALSystem.getMessage(JOVALMsg.ERROR_RESOLVE_VAR, tfcObj.getPattern().getVarRef(), e.getMessage());
		msg.setValue(s);
		rc.addMessage(msg);
	    } finally {
		if (in != null) {
		    try {
			in.close();
		    } catch (IOException e) {
			JOVALSystem.getLogger().warn(JOVALMsg.ERROR_FILE_STREAM_CLOSE, f.toString());
		    }
		}
	    }
	}
	return items;
    }
}
