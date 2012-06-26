// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.independent;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.independent.TextfilecontentObject;
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
 * Evaluates TextfilecontentTest OVAL tests.
 *
 * DAS: Specify a maximum file size supported
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class TextfilecontentAdapter extends BaseFileAdapter<TextfilecontentItem> {
    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof ISession) {
	    super.init((ISession)session);
	    classes.add(TextfilecontentObject.class);
	}
	return classes;
    }

    // Protected

    protected Class getItemClass() {
	return TextfilecontentItem.class;
    }

    /**
     * Parse the file as specified by the Object, and decorate the Item.
     */
    protected Collection<TextfilecontentItem> getItems(ObjectType obj, ItemType base, IFile f, IRequestContext rc)
		throws IOException, CollectException {

	Collection<TextfilecontentItem> items = new Vector<TextfilecontentItem>();

	TextfilecontentItem baseItem = null;
	if (base instanceof TextfilecontentItem) {
	    baseItem = (TextfilecontentItem)base;
	}
	TextfilecontentObject tfcObj = null;
	if (obj instanceof TextfilecontentObject) {
	    tfcObj = (TextfilecontentObject)obj;
	}

	if (baseItem != null && tfcObj != null) {
	    InputStream in = null;
	    try {
		//
		// Read the whole file into a buffer and search for the pattern
		//
		byte[] buff = new byte[256];
		int len = 0;
		StringBuffer sb = new StringBuffer();
		in = f.getInputStream();
		while ((len = in.read(buff)) > 0) {
		    sb.append(StringTools.toASCIICharArray(buff), 0, len);
		}
		String s = sb.toString();

		OperationEnumeration op = tfcObj.getLine().getOperation();
		switch (op) {
		  case PATTERN_MATCH: {
		    Pattern p = Pattern.compile(StringTools.regexPerl2Java((String)tfcObj.getLine().getValue()));
		    for (TextfilecontentItem item : getItems(p, baseItem, s)) {
			EntityItemStringType lineType = Factories.sc.core.createEntityItemStringType();
			lineType.setValue(tfcObj.getLine().getValue());
			item.setLine(lineType);
			items.add(item);
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

    protected Collection<TextfilecontentItem> getItems(Pattern p, TextfilecontentItem baseItem, String s) {
	Matcher m = p.matcher(s);
	Collection<TextfilecontentItem> items = new Vector<TextfilecontentItem>();
	for (int instanceNum=1; m.find(); instanceNum++) {
	    TextfilecontentItem item = Factories.sc.independent.createTextfilecontentItem();
	    item.setPath(baseItem.getPath());
	    item.setFilename(baseItem.getFilename());
	    item.setFilepath(baseItem.getFilepath());
	    item.setWindowsView(baseItem.getWindowsView());

	    EntityItemStringType patternType = Factories.sc.core.createEntityItemStringType();
	    patternType.setValue(p.toString());
	    item.setPattern(patternType);
	    EntityItemIntType instanceType = Factories.sc.core.createEntityItemIntType();
	    instanceType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    instanceType.setValue(Integer.toString(instanceNum));
	    item.setInstance(instanceType);

	    EntityItemAnySimpleType textType = Factories.sc.core.createEntityItemAnySimpleType();
	    textType.setValue(m.group());
	    item.setText(textType);
	    int sCount = m.groupCount();
	    if (sCount > 0) {
		for (int sNum=1; sNum <= sCount; sNum++) {
		    String group = m.group(sNum);
		    if (group != null) {
			EntityItemAnySimpleType sType = Factories.sc.core.createEntityItemAnySimpleType();
			sType.setValue(group);
			item.getSubexpression().add(sType);
		    }
		}
	    }

	    items.add(item);
	}
	return items;
    }
}
