// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.independent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.GZIPInputStream;

import jsaf.intf.io.IFile;
import jsaf.intf.system.IBaseSession;
import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.Base64;
import jsaf.util.SafeCLI;
import jsaf.util.StringTools;

import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.MessageType;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.independent.Textfilecontent54Object;
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
 * Retrieves Textfilecontent54Test OVAL items.
 *
 * DAS: Specify a maximum file size supported
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Textfilecontent54Adapter extends BaseFileAdapter<TextfilecontentItem> {
    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof ISession) {
	    super.init((ISession)session);
	    classes.add(Textfilecontent54Object.class);
	}
	return classes;
    }

    // Protected

    protected Class getItemClass() {
	return TextfilecontentItem.class;
    }

    protected Collection<TextfilecontentItem> getItems(ObjectType obj, ItemType base, IFile f, IRequestContext rc)
		throws IOException, CollectException {

	Collection<TextfilecontentItem> items = new HashSet<TextfilecontentItem>();
	TextfilecontentItem baseItem = (TextfilecontentItem)base;
	Textfilecontent54Object tfcObj = (Textfilecontent54Object)obj;
	try {
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
	    Pattern pattern = Pattern.compile(StringTools.regexPosix2Java((String)tfcObj.getPattern().getValue()), flags);

	    String s = null;
	    switch(session.getType()) {
	      //
	      // On Unix, leverage cat, gzip and uuencode or base64 to read the file contents from the command-line. This
	      // makes it possible to leverage elevated privileges, if they're set.
	      //
	      case UNIX: {
		IUnixSession us = (IUnixSession)session;
		StringBuffer sb = new StringBuffer();
		StringBuffer cmd = new StringBuffer("cat ").append(f.getPath().replace(" ", "\\ ")).append(" | gzip -c");
		List<String> lines = null;
		switch(us.getFlavor()) {
		  //
		  // Use entire output of base64
		  //
		  case LINUX:
		    cmd.append(" | base64 -");
		    for (String line : SafeCLI.multiLine(cmd.toString(), session, IUnixSession.Timeout.M)) {
			sb.append(line);
		    }
		    break;

		  //
		  // Skip first and last line of uuencode output
		  //
		  case AIX:
		  case MACOSX:
		  case SOLARIS:
		    cmd.append(" | uuencode -m -");
		    List<String> output = SafeCLI.multiLine(cmd.toString(), session, IUnixSession.Timeout.M);
		    int end = output.size() - 1;
		    for (int i=1; i < end; i++) {
			sb.append(output.get(i));
		    }
		    break;
		}
		if (sb.length() > 0) {
		    s = readASCIIString(new GZIPInputStream(new ByteArrayInputStream(Base64.decode(sb.toString()))));
		    break;
		}
		// else fall-thru
	      }

	      //
	      // By default, use the IFile to read the contents into the String.
	      //
	      default:
		s = readASCIIString(f.getInputStream());
		break;
	    }

	    //
	    // Find all the matching items
	    //
	    Collection<TextfilecontentItem> allItems = new ArrayList<TextfilecontentItem>();
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
	    // Filter the matches by instance number
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
	} catch (IOException e) {
	    throw e;
	} catch (Exception e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(e.getMessage());
	    rc.addMessage(msg);
	}
	return items;
    }

    // Private

    /**
     * Built items for all matches of p in s.
     */
    private Collection<TextfilecontentItem> getItems(Pattern p, TextfilecontentItem baseItem, String s) {
	Matcher m = p.matcher(s);
	Collection<TextfilecontentItem> items = new ArrayList<TextfilecontentItem>();
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
	    String textValue = m.group();
	    //
	    // Ignore the trailing null match, if there is one.
	    //
	    if (textValue != null || items.size() == 0) {
                textType.setValue(textValue);
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
	}
	return items;
    }

    /**
     * Read the contents of an InputStream as ASCII into a single String.
     */
    private String readASCIIString(InputStream in) throws IOException {
	try {
	    byte[] buff = new byte[256];
	    int len = 0;
	    StringBuffer sb = new StringBuffer();
	    while ((len = in.read(buff)) > 0) {
		sb.append(StringTools.toASCIICharArray(buff), 0, len);
	    }
	    return sb.toString();
	} finally {
	    if (in != null) {
		try {
		    in.close();
		} catch (IOException e) {
		}
	    }
	}
    }
}
