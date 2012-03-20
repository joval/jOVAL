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
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
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
import org.joval.oval.OvalException;
import org.joval.oval.ResolveException;
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
    protected Collection<JAXBElement<? extends ItemType>> getItems(ItemType base, IFile f, IRequestContext rc)
		throws IOException, CollectException, OvalException {

	Collection<JAXBElement<? extends ItemType>> items = new HashSet<JAXBElement<? extends ItemType>>();

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
		//
		// Construct all the necessary patterns
		//
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
			patterns.add(Pattern.compile(StringTools.regexPerl2Java(value), flags));
		    }
		} else {
		    patterns.add(Pattern.compile(StringTools.regexPerl2Java((String)tfcObj.getPattern().getValue()), flags));
		}

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

		Collection<JAXBElement<? extends ItemType>> allItems = new Vector<JAXBElement<? extends ItemType>>();
		OperationEnumeration op = tfcObj.getPattern().getOperation();
		switch(op) {
		  case PATTERN_MATCH:
		    for (Pattern p : patterns) {
			allItems.addAll(getItems(p, baseItem, s));
		    }
		    break;

		  default:
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		}

		//
		// Filter by instance numbers
		//
		Collection<String> instances = new Vector<String>();
		if (tfcObj.getInstance().isSetVarRef()) {
		    for (String value : rc.resolve(tfcObj.getInstance().getVarRef())) {
			instances.add(((String)value).trim());
		    }
		} else {
		    instances.add((String)tfcObj.getInstance().getValue());
		}
		for (String instanceNum : instances) {
		    op = tfcObj.getInstance().getOperation();
		    switch(op) {
		      case EQUALS:
			for (JAXBElement<? extends ItemType> elt : allItems) {
			    TextfilecontentItem item = (TextfilecontentItem)elt.getValue();
			    if (((String)item.getInstance().getValue()).equals(instanceNum)) {
				items.add(elt);
			    }
			}
			break;

		      case LESS_THAN:
			for (JAXBElement<? extends ItemType> elt : allItems) {
			    TextfilecontentItem item = (TextfilecontentItem)elt.getValue();
			    int inum = Integer.parseInt((String)item.getInstance().getValue());
			    int comp = Integer.parseInt(instanceNum);
			    if (inum < comp) {
				items.add(elt);
			    }
			}
			break;

		      case LESS_THAN_OR_EQUAL:
			for (JAXBElement<? extends ItemType> elt : allItems) {
			    TextfilecontentItem item = (TextfilecontentItem)elt.getValue();
			    int inum = Integer.parseInt((String)item.getInstance().getValue());
			    int comp = Integer.parseInt(instanceNum);
			    if (inum <= comp) {
				items.add(elt);
			    }
			}
			break;

		      case GREATER_THAN:
			for (JAXBElement<? extends ItemType> elt : allItems) {
			    TextfilecontentItem item = (TextfilecontentItem)elt.getValue();
			    int inum = Integer.parseInt((String)item.getInstance().getValue());
			    int comp = Integer.parseInt(instanceNum);
			    if (inum > comp) {
				items.add(elt);
			    }
			}
			break;

		      case GREATER_THAN_OR_EQUAL:
			for (JAXBElement<? extends ItemType> elt : allItems) {
			    TextfilecontentItem item = (TextfilecontentItem)elt.getValue();
			    int inum = Integer.parseInt((String)item.getInstance().getValue());
			    int comp = Integer.parseInt(instanceNum);
			    if (inum >= comp) {
				items.add(elt);
			    }
			}
			break;

		      case PATTERN_MATCH: {
			Pattern p = Pattern.compile(instanceNum);
			for (JAXBElement<? extends ItemType> elt : allItems) {
			    TextfilecontentItem item = (TextfilecontentItem)elt.getValue();
			    if (p.matcher((String)item.getInstance().getValue()).find()) {
				items.add(elt);
			    }
			}
			break;
		      }

		      default:
			String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
			throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		    }
		}
	    } catch (PatternSyntaxException e) {
		session.getLogger().warn(JOVALMsg.ERROR_PATTERN, e.getMessage());
		throw new IOException(e);
	    } catch (ResolveException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		String s = JOVALMsg.getMessage(JOVALMsg.ERROR_RESOLVE_VAR, tfcObj.getPattern().getVarRef(), e.getMessage());
		msg.setValue(s);
		rc.addMessage(msg);
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
