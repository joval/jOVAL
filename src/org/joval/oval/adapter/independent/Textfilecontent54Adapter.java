// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.independent;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.independent.Textfilecontent54Object;
import oval.schemas.systemcharacteristics.core.EntityItemAnySimpleType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.independent.TextfilecontentItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.io.IFile;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.ISession;
import org.joval.oval.CollectException;
import org.joval.oval.Factories;
import org.joval.util.JOVALMsg;
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
    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof ISession) {
	    super.init((ISession)session);
	    classes.add(Textfilecontent54Object.class);
	}
	return classes;
    }

    // Protected

    /**
     * Parse the file as specified by the Object, and decorate the Item.
     */
    @Override
    protected Collection<TextfilecontentItem> getItems(ObjectType obj, ItemType base, IFile f, IRequestContext rc)
		throws IOException, CollectException {

	Collection<TextfilecontentItem> items = new HashSet<TextfilecontentItem>();

	TextfilecontentItem baseItem = null;
	if (base instanceof TextfilecontentItem) {
	    baseItem = (TextfilecontentItem)base;
	}
	Textfilecontent54Object tfcObj = null;
	if (obj instanceof Textfilecontent54Object) {
	    tfcObj = (Textfilecontent54Object)obj;
	}

	if (baseItem != null && tfcObj != null) {
	    InputStream in = null;
	    try {
		//
		// Construct all the necessary patterns
		//
		int flags = 0;
		if (tfcObj.isSetBehaviors()) {
		    if (tfcObj.getBehaviors().getMultiline()) {
			flags |= Pattern.MULTILINE;
		    }
		    if (tfcObj.getBehaviors().getIgnoreCase()) {
			flags |= Pattern.CASE_INSENSITIVE;
		    }
		    if (tfcObj.getBehaviors().getSingleline()) {
			flags |= Pattern.DOTALL;
		    }
		} else {
		    flags = Pattern.MULTILINE;
		}
		Pattern pattern = Pattern.compile(StringTools.regexPerl2Java((String)tfcObj.getPattern().getValue()), flags);

		//
		// Read the whole file into a buffer to search for the pattern
		//
		byte[] buff = new byte[256];
		int len = 0;
		StringBuffer sb = new StringBuffer();
		in = f.getInputStream();
		while ((len = in.read(buff)) > 0) {
		    sb.append(StringTools.toASCIICharArray(buff), 0, len);
		}
		String s = sb.toString();

		Collection<TextfilecontentItem> allItems = new Vector<TextfilecontentItem>();
		OperationEnumeration op = tfcObj.getPattern().getOperation();
		switch(op) {
		  case PATTERN_MATCH:
		    allItems.addAll(getItems(pattern, baseItem, s));
		    break;

		  default:
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		}

		//
		// Filter by instance numbers
		//
		String instanceNum = (String)tfcObj.getInstance().getValue();
		op = tfcObj.getInstance().getOperation();
		switch(op) {
		  case EQUALS:
		    for (TextfilecontentItem item : allItems) {
			if (((String)item.getInstance().getValue()).equals(instanceNum)) {
			    items.add(item);
			}
		    }
		    break;

		  case LESS_THAN:
		    for (TextfilecontentItem item : allItems) {
			int inum = Integer.parseInt((String)item.getInstance().getValue());
			int comp = Integer.parseInt(instanceNum);
			if (inum < comp) {
			    items.add(item);
			}
		    }
		    break;

		  case LESS_THAN_OR_EQUAL:
		    for (TextfilecontentItem item : allItems) {
			int inum = Integer.parseInt((String)item.getInstance().getValue());
			int comp = Integer.parseInt(instanceNum);
			if (inum <= comp) {
			    items.add(item);
			}
		    }
		    break;

		  case GREATER_THAN:
		    for (TextfilecontentItem item : allItems) {
			int inum = Integer.parseInt((String)item.getInstance().getValue());
			int comp = Integer.parseInt(instanceNum);
			if (inum > comp) {
			    items.add(item);
			}
		    }
		    break;

		  case GREATER_THAN_OR_EQUAL:
		    for (TextfilecontentItem item : allItems) {
			int inum = Integer.parseInt((String)item.getInstance().getValue());
			int comp = Integer.parseInt(instanceNum);
			if (inum >= comp) {
			    items.add(item);
			}
		    }
		    break;

		  case PATTERN_MATCH: {
		    Pattern p = Pattern.compile(instanceNum);
		    for (TextfilecontentItem item : allItems) {
			if (p.matcher((String)item.getInstance().getValue()).find()) {
			    items.add(item);
			}
		    }
		    break;
		  }

		  default:
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		}
	    } catch (PatternSyntaxException e) {
		session.getLogger().warn(JOVALMsg.ERROR_PATTERN, e.getMessage());
		throw new IOException(e);
	    } catch (IllegalArgumentException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		rc.addMessage(msg);
	    } finally {
		if (in != null) {
		    try {
			in.close();
		    } catch (IOException e) {
			session.getLogger().warn(JOVALMsg.ERROR_FILE_STREAM_CLOSE, f.toString());
		    }
		}
	    }
	}
	return items;
    }
}
