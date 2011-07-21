// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.independent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.ExistenceEnumeration;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.definitions.core.EntityObjectStringType;
import oval.schemas.definitions.core.EntityStateAnySimpleType;
import oval.schemas.definitions.core.EntityStateStringType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.StateType;
import oval.schemas.definitions.independent.Textfilecontent54Object;
import oval.schemas.definitions.independent.Textfilecontent54State;
import oval.schemas.definitions.independent.Textfilecontent54Test;
import oval.schemas.systemcharacteristics.core.EntityItemAnySimpleType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.EntityItemVersionType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.core.VariableValueType;
import oval.schemas.systemcharacteristics.independent.ObjectFactory;
import oval.schemas.systemcharacteristics.independent.TextfilecontentItem;
import oval.schemas.results.core.ResultEnumeration;
import oval.schemas.results.core.TestedItemType;
import oval.schemas.results.core.TestedVariableType;
import oval.schemas.results.core.TestType;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IAdapterContext;
import org.joval.intf.oval.IDefinitions;
import org.joval.intf.oval.ISystemCharacteristics;
import org.joval.io.StreamTool;
import org.joval.oval.OvalException;
import org.joval.util.BaseFileAdapter;
import org.joval.util.JOVALSystem;
import org.joval.util.Version;

/**
 * Evaluates Textfilecontent54Test OVAL tests.
 *
 * DAS: Specify a maximum file size supported for multi-line behavior support.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Textfilecontent54Adapter extends BaseFileAdapter {
    protected ObjectFactory independentFactory;

    public Textfilecontent54Adapter(IFilesystem fs) {
	super(fs);
	independentFactory = new ObjectFactory();
    }

    // Implement IAdapter

    public Class getObjectClass() {
	return Textfilecontent54Object.class;
    }

    public Class getTestClass() {
	return Textfilecontent54Test.class;
    }

    public Class getStateClass() {
	return Textfilecontent54State.class;
    }

    public Class getItemClass() {
	return TextfilecontentItem.class;
    }

    public ResultEnumeration compare(StateType st, ItemType it) throws OvalException {
	Textfilecontent54State state = (Textfilecontent54State)st;
	TextfilecontentItem item = (TextfilecontentItem)it;

	String pattern = (String)state.getText().getValue();
        if (item.getPattern().getValue().equals(pattern)) {
	    return ResultEnumeration.TRUE;
	} else {
	    return ResultEnumeration.FALSE;
	}
    }

    // Private

    protected JAXBElement<? extends ItemType> createStorageItem(ItemType item) {
	return independentFactory.createTextfilecontentItem((TextfilecontentItem)item);
    }

    protected Object convertFilename(EntityItemStringType filename) {
	return filename;
    }

    protected ItemType createFileItem() {
	return independentFactory.createTextfilecontentItem();
    }

    /**
     * Parse the file as specified by the Object, and decorate the Item.
     */
    protected List<? extends ItemType> getItems(ItemType base, ObjectType obj, IFile f) throws IOException {
	List<ItemType> list = new Vector<ItemType>();
	if (base instanceof TextfilecontentItem && obj instanceof Textfilecontent54Object) {
	    setItem((TextfilecontentItem)base, (Textfilecontent54Object)obj, f);
	    list.add(base);
	}
	return list;
    }

    // Private

    /**
     * Parse the file as specified by the Object, and decorate the Item.
     */
    private void setItem(TextfilecontentItem item, Textfilecontent54Object tfcObj, IFile file) throws IOException {
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
	    Pattern p = Pattern.compile((String)tfcObj.getPattern().getValue(), flags);

	    EntityItemStringType patternType = coreFactory.createEntityItemStringType();
	    patternType.setValue(p.toString());
	    item.setPattern(patternType);
	    EntityItemIntType instanceType = coreFactory.createEntityItemIntType();
	    instanceType.setDatatype(tfcObj.getInstance().getDatatype());
	    instanceType.setValue(tfcObj.getInstance().getValue());
	    item.setInstance(instanceType);

	    //
	    // Read the whole file into a buffer and search for the pattern
	    //
	    String text = null;
	    byte[] buff = new byte[256];
	    int len = 0;
	    StringBuffer sb = new StringBuffer();
	    in = file.getInputStream();
	    while ((len = in.read(buff)) > 0) {
		sb.append(toCharArray(buff), 0, len);
	    }
	    String s = sb.toString();
	    Matcher m = p.matcher(s);
	    if (m.find()) {
		MatchResult mr = m.toMatchResult();
		text = s.substring(mr.start(), mr.end());
	    }

	    if (text != null) {
		EntityItemAnySimpleType textType = coreFactory.createEntityItemAnySimpleType();
		textType.setValue(text);
		item.setText(textType);
	    }
	} catch (PatternSyntaxException e) {
	    MessageType msg = new MessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(e.getMessage());
	    item.getMessage().add(msg);
	} finally {
	    if (in != null) {
		try {
		    in.close();
		} catch (IOException e) {
		    ctx.log(Level.WARNING, JOVALSystem.getMessage("ERROR_FILE_STREAM_CLOSE", file.toString()), e);
		}
	    }
	}
    }

    private char[] toCharArray(byte[] buff) {
	char[] ca = new char[buff.length];
	for (int i=0; i < buff.length; i++) {
	    ca[i] = (char)buff[i];
	}
	return ca;
    }
}
