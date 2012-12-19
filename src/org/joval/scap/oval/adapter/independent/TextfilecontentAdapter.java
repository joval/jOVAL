// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.independent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.ISession;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
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
	Collection<Class> classes = new ArrayList<Class>();
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

	TextfilecontentItem baseItem = (TextfilecontentItem)base;
	TextfilecontentObject tfcObj = (TextfilecontentObject)obj;
	Collection<TextfilecontentItem> items = new ArrayList<TextfilecontentItem>();
	BufferedReader reader = null;
	try {
	    //
	    // Buffer the whole file into a List of lines
	    //
	    List<String> lines = new ArrayList<String>();
	    reader = new BufferedReader(new InputStreamReader(f.getInputStream(), StringTools.ASCII));
	    String line = null;
	    while ((line = reader.readLine()) != null) {
		lines.add(line);
	    }

	    OperationEnumeration op = tfcObj.getLine().getOperation();
	    switch (op) {
	      case PATTERN_MATCH: {
		Pattern p = Pattern.compile(StringTools.regexPosix2Java((String)tfcObj.getLine().getValue()));
		items.addAll(getItems(p, baseItem, lines));
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
	    if (reader != null) {
		try {
		    reader.close();
		} catch (IOException e) {
		    session.getLogger().warn(JOVALMsg.ERROR_FILE_STREAM_CLOSE, f.toString());
		}
	    }
	}
	return items;
    }

    // Private

    /**
     * Build all the item matches (old-style).
     */
    protected Collection<TextfilecontentItem> getItems(Pattern p, TextfilecontentItem baseItem, List<String> lines) {
	Collection<TextfilecontentItem> items = new ArrayList<TextfilecontentItem>();
	for (String line : lines) {
	    EntityItemStringType lineType = Factories.sc.core.createEntityItemStringType();
	    lineType.setValue(line);

	    Matcher m = p.matcher(line);
	    if (m.find()) {
		TextfilecontentItem item = Factories.sc.independent.createTextfilecontentItem();
		item.setPath(baseItem.getPath());
		item.setFilename(baseItem.getFilename());
		item.setWindowsView(baseItem.getWindowsView());
		item.setLine(lineType);

		int sCount = m.groupCount();
		for (int sNum=0; sNum <= sCount; sNum++) {
		    String group = m.group(sNum);
		    if (group != null) {
			EntityItemAnySimpleType sType = Factories.sc.core.createEntityItemAnySimpleType();
			sType.setValue(group);
			item.getSubexpression().add(sType);
		    }
		}
		items.add(item);
	    }
	}
	return items;
    }
}
