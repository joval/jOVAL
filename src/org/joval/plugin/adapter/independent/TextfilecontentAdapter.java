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
import oval.schemas.definitions.independent.TextfilecontentObject;
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
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.StringTools;

/**
 * Evaluates TextfilecontentTest OVAL tests.
 *
 * DAS: Specify a maximum file size supported
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class TextfilecontentAdapter extends BaseFileAdapter {
    public TextfilecontentAdapter(ISession system) {
	super(system);
    }

    // Implement IAdapter

    private static Class[] objectClasses = {TextfilecontentObject.class};

    public Class[] getObjectClasses() {
	return objectClasses;
    }

    // Protected

    protected Object convertFilename(EntityItemStringType filename) {
	return filename;
    }

    protected ItemType createFileItem() {
	return JOVALSystem.factories.sc.independent.createTextfilecontentItem();
    }

    /**
     * Parse the file as specified by the Object, and decorate the Item.
     */
    protected Collection<JAXBElement<? extends ItemType>> getItems(ItemType base, IFile f, IRequestContext rc)
		throws IOException, CollectionException, OvalException {

	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();

	TextfilecontentItem baseItem = null;
	if (base instanceof TextfilecontentItem) {
	    baseItem = (TextfilecontentItem)base;
	}
	TextfilecontentObject tfcObj = null;
	if (rc.getObject() instanceof TextfilecontentObject) {
	    tfcObj = (TextfilecontentObject)rc.getObject();
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
		    sb.append(StringTools.toCharArray(buff), 0, len);
		}
		String s = sb.toString();

		OperationEnumeration op = tfcObj.getLine().getOperation();
		switch (op) {
		  case PATTERN_MATCH: {
		    Pattern p = Pattern.compile((String)tfcObj.getLine().getValue());
		    for (JAXBElement<TextfilecontentItem>item : getItems(p, baseItem, s)) {
			EntityItemStringType lineType = JOVALSystem.factories.sc.core.createEntityItemStringType();
			lineType.setValue(tfcObj.getLine().getValue());
			item.getValue().setLine(lineType);
			items.add(item);
		    }
		    break;
		  }

		  default:
		    throw new CollectionException(JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op));
		}
	    } catch (PatternSyntaxException e) {
		JOVALSystem.getLogger().warn(JOVALMsg.ERROR_PATTERN, e.getMessage());
		throw new IOException(e);
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

    protected Collection<JAXBElement<TextfilecontentItem>> getItems(Pattern p, TextfilecontentItem baseItem, String s) {
	Matcher m = p.matcher(s);
	Collection<JAXBElement<TextfilecontentItem>> items = new Vector<JAXBElement<TextfilecontentItem>>();
	for (int instanceNum=1; m.find(); instanceNum++) {
	    TextfilecontentItem item = (TextfilecontentItem)createFileItem();
	    item.setPath(baseItem.getPath());
	    item.setFilename(baseItem.getFilename());

	    EntityItemStringType patternType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    patternType.setValue(p.toString());
	    item.setPattern(patternType);
	    EntityItemIntType instanceType = JOVALSystem.factories.sc.core.createEntityItemIntType();
	    instanceType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    instanceType.setValue(Integer.toString(instanceNum));
	    item.setInstance(instanceType);

	    EntityItemAnySimpleType textType = JOVALSystem.factories.sc.core.createEntityItemAnySimpleType();
	    textType.setValue(m.group());
	    item.setText(textType);
	    int sCount = m.groupCount();
	    if (sCount > 0) {
		for (int sNum=1; sNum <= sCount; sNum++) {
		    EntityItemAnySimpleType sType = JOVALSystem.factories.sc.core.createEntityItemAnySimpleType();
		    sType.setValue(m.group(sNum));
		    item.getSubexpression().add(sType);
		}
	    }

	    items.add(JOVALSystem.factories.sc.independent.createTextfilecontentItem(item));
	}
	return items;
    }
}
