// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.independent;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.intf.io.IFile;
import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.StringTools;
import jsaf.util.SafeCLI;

import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.MessageType;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.independent.TextfilecontentObject;
import scap.oval.systemcharacteristics.core.EntityItemAnySimpleType;
import scap.oval.systemcharacteristics.core.EntityItemIntType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.independent.TextfilecontentItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

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

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	try {
	    baseInit(session);
	    classes.add(TextfilecontentObject.class);
	} catch (UnsupportedOperationException e) {
	    // doesn't support ISession.getFilesystem
	    notapplicable.add(TextfilecontentObject.class);
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
	try {
	    //
	    // Retrieve matching lines
	    //
	    List<String> lines = new ArrayList<String>();
	    switch(session.getType()) {
	      //
	      // Leverage grep on known flavors of Unix
	      //
	      case UNIX:
		IUnixSession us = (IUnixSession)session;
		StringBuffer sb = new StringBuffer("cat ").append(f.getPath().replace(" ", "\\ ")).append(" | ");
		boolean handled = false;
		switch(us.getFlavor()) {
		  case SOLARIS:
		    sb.append("/usr/xpg4/bin/grep -E ");
		    handled = true;
		    break;

		  case AIX:
		  case LINUX:
		  case MACOSX:
		    sb.append("grep ");
		    handled = true;
		    break;
		}
		if (handled) {
		    Pattern p = StringTools.pattern((String)tfcObj.getLine().getValue());
		    sb.append("\"").append(p.pattern().replace("\"","\\\"")).append("\"");
		    try {
			lines = SafeCLI.multiLine(sb.toString(), session, IUnixSession.Timeout.M);
		    } catch (Exception e) {
			session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
			throw new CollectException(e.getMessage(), FlagEnumeration.ERROR);
		    }
		    break;
		}
		// else fall-thru

	      //
	      // Use the IFilesystem by default
	      //
	      default:
		BufferedReader reader = null;
		try {
		    reader = new BufferedReader(new InputStreamReader(f.getInputStream(), StringTools.ASCII));
		    String line = null;
		    Pattern p = StringTools.pattern((String)tfcObj.getLine().getValue());
		    while ((line = reader.readLine()) != null) {
			if (p.matcher(line).find()) {
			    lines.add(line);
			}
		    }
		} finally {
		    if (reader != null) {
			try {
			    reader.close();
			} catch (IOException e) {
			    session.getLogger().warn(JOVALMsg.ERROR_FILE_STREAM_CLOSE, f.toString());
			}
		    }
		}
		break;
	    }

	    OperationEnumeration op = tfcObj.getLine().getOperation();
	    switch (op) {
	      case PATTERN_MATCH: {
		Pattern p = StringTools.pattern((String)tfcObj.getLine().getValue());
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
	}
	return items;
    }

    // Private

    /**
     * Build all the item matches (lines should be pre-filtered to contain matches to the pattern).
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
