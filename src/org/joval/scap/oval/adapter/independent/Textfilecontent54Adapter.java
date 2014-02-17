// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.independent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.Message;
import jsaf.intf.io.IFile;
import jsaf.intf.system.IComputerSystem;
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
import org.joval.scap.oval.Batch;
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

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IComputerSystem) {
	    try {
		baseInit((IComputerSystem)session);
		classes.add(Textfilecontent54Object.class);
	    } catch (UnsupportedOperationException e) {
		// doesn't support ISession.getFilesystem()
		notapplicable.add(Textfilecontent54Object.class);
	    }
	} else {
	    notapplicable.add(Textfilecontent54Object.class);
	}
	return classes;
    }

    // Implement IBatch

    @Override
    public Collection<IResult> exec() {
	Map<IRequest, IResult> resultMap = new HashMap<IRequest, IResult>();
	Map<String, Collection<IRequest>> requestMap = new HashMap<String, Collection<IRequest>>();
	try {
	    //
	    // Organize requests and files by path.
	    //
	    String[] paths = queuePaths();
	    IFile[] files = session.getFilesystem().getFiles(paths);
	    Map<String, IFile> fileMap = new HashMap<String, IFile>();
	    for (int i=0; i < paths.length; i++) {
		String path = paths[i];
		fileMap.put(path, files[i]);
		IRequest request = queue.get(i);
		if (requestMap.containsKey(path)) {
		    requestMap.get(path).add(request);
		} else {
		    ArrayList<IRequest> rl = new ArrayList<IRequest>();
		    rl.add(request);
		    requestMap.put(path, rl);
		}
	    }

	    //
	    // Iterate across distinct paths, so each file actually is retrieved only once.
	    //
	    for (Map.Entry<String, Collection<IRequest>> entry : requestMap.entrySet()) {
		IFile f = fileMap.get(entry.getKey());
		if (f != null && f.isFile() && f.exists()) {
		    String content = readASCIIString(f.getInputStream());
		    for (IRequest request : entry.getValue()) {
			TextfilecontentItem baseItem = (TextfilecontentItem)getBaseItem(request.getObject(), f);
			if (baseItem == null) continue;

			List<TFCData> data = Arrays.asList(new TFCData(baseItem, content));
			IRequestContext rc = request.getContext();
			try {
			    Collection<TextfilecontentItem> items = getItemsInternal(request.getObject(), data, rc);
			    resultMap.put(request, new Batch.Result(items, rc));
			} catch (CollectException e) {
			    resultMap.put(request, new Batch.Result(e, rc));
			} catch (Exception e) {
			    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
			    resultMap.put(request, new Batch.Result(new CollectException(e, FlagEnumeration.ERROR), rc));
			}
		    }
		} else {
		    Collection<TextfilecontentItem> empty = Collections.<TextfilecontentItem>emptyList();
		    for (IRequest request : entry.getValue()) {
			resultMap.put(request, new Batch.Result(empty, request.getContext()));
		    }
		}
	    }
	} catch (IOException e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    for (IRequest request : queue) {
		IRequestContext rc = request.getContext();
		resultMap.put(request, new Batch.Result(new CollectException(e, FlagEnumeration.ERROR), rc));
	    }
	}
	queue = null;
	return resultMap.values();
    }

    // Protected

    protected Class getItemClass() {
	return TextfilecontentItem.class;
    }

    protected Collection<TextfilecontentItem> getItems(ObjectType obj, Collection<IFile> files, IRequestContext rc)
		throws CollectException {

	Collection<TFCData> data = new ArrayList<TFCData>();
	for (IFile f : files) {
	    try {
		TextfilecontentItem baseItem = (TextfilecontentItem)getBaseItem(obj, f);
		String content = readASCIIString(f.getInputStream());
		data.add(new TFCData(baseItem, content));
	    } catch (IOException e) {
		session.getLogger().warn(Message.ERROR_IO, f.getPath(), e.getMessage());
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		rc.addMessage(msg);
	    }
	}
	return getItemsInternal(obj, data, rc);
    }

    // Private

    private Collection<TextfilecontentItem> getItemsInternal(ObjectType obj, Collection<TFCData> data, IRequestContext rc)
		throws CollectException {

	Textfilecontent54Object tfcObj = (Textfilecontent54Object)obj;
	Collection<TextfilecontentItem> items = new ArrayList<TextfilecontentItem>();
	for (TFCData datum : data) {
	    try {
		TextfilecontentItem baseItem = datum.getBaseItem();
		if (baseItem != null) {
		    String s = datum.getASCIIContent();

		    //
		    // Find all the matching items
		    //
		    Collection<TextfilecontentItem> allItems = new ArrayList<TextfilecontentItem>();
		    OperationEnumeration op = tfcObj.getPattern().getOperation();
		    Pattern pattern = StringTools.pattern((String)tfcObj.getPattern().getValue(), getFlags(tfcObj));
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
		    int instanceNum = Integer.parseInt((String)tfcObj.getInstance().getValue());
		    op = tfcObj.getInstance().getOperation();
		    for (TextfilecontentItem item : allItems) {
			int inum = Integer.parseInt((String)item.getInstance().getValue());
			switch(op) {
			  case EQUALS:
			    if (inum == instanceNum) {
				items.add(item);
			    }
			    break;
			  case LESS_THAN:
			    if (inum < instanceNum) {
				items.add(item);
			    }
			    break;
			  case LESS_THAN_OR_EQUAL:
			    if (inum <= instanceNum) {
				items.add(item);
			    }
			    break;
			  case GREATER_THAN:
			    if (inum > instanceNum) {
				items.add(item);
			    }
			    break;
			  case GREATER_THAN_OR_EQUAL:
			    if (inum >= instanceNum) {
				items.add(item);
			    }
			    break;
			  default:
			    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
			    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
			}
		    }
		}
	    } catch (PatternSyntaxException e) {
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage());
		throw new CollectException(msg, FlagEnumeration.ERROR);
	    } catch (Exception e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		rc.addMessage(msg);
	    }
	}
	return items;
    }

    /**
     * Build items for all matches of p in s.
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

    private int getFlags(Textfilecontent54Object tfcObj) {
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
	return flags;
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
		sb.append(StringTools.toChars(buff, 0, len, StringTools.ASCII));
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

    class TFCData {
	private TextfilecontentItem baseItem;
	private String asciiContent;

	TFCData(TextfilecontentItem baseItem, String asciiContent) {
	    this.baseItem = baseItem;
	    this.asciiContent = asciiContent;
	}

	String getASCIIContent() {
	    return asciiContent;
	}

	TextfilecontentItem getBaseItem() {
	    return baseItem;
	}
    }
}
