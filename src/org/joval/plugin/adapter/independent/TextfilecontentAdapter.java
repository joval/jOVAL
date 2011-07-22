// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.independent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.StateType;
import oval.schemas.definitions.independent.TextfilecontentObject;
import oval.schemas.definitions.independent.TextfilecontentState;
import oval.schemas.definitions.independent.TextfilecontentTest;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.independent.ObjectFactory;
import oval.schemas.systemcharacteristics.independent.TextfilecontentItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IAdapterContext;
import org.joval.oval.OvalException;
import org.joval.util.BaseFileAdapter;
import org.joval.util.JOVALSystem;

/**
 * Evaluates TextfilecontentTest OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class TextfilecontentAdapter extends BaseFileAdapter {
    protected ObjectFactory independentFactory;

    public TextfilecontentAdapter(IFilesystem fs) {
	super(fs);
	independentFactory = new ObjectFactory();
    }

    // Implement IAdapter

    public Class getObjectClass() {
	return TextfilecontentObject.class;
    }

    public Class getStateClass() {
	return TextfilecontentState.class;
    }

    public Class getItemClass() {
	return TextfilecontentItem.class;
    }

    public ResultEnumeration compare(StateType st, ItemType it) throws OvalException {
	if (match((TextfilecontentState)st, (TextfilecontentItem)it)) {
	    return ResultEnumeration.TRUE;
	} else {
	    return ResultEnumeration.FALSE;
	}
    }

    // Protected

    protected Object convertFilename(EntityItemStringType filename) {
	return filename;
    }

    protected ItemType createFileItem() {
	return independentFactory.createTextfilecontentItem();
    }

    /**
     * Parse the file as specified by the Object, and decorate the Item.
     */
    protected List<JAXBElement<? extends ItemType>> getItems(ItemType base, ObjectType obj, IFile f) throws IOException {
	List<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();

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
		Pattern p = Pattern.compile((String)tfcObj.getLine().getValue());
    
		//
		// Read the file line-by-line and search for the pattern.  Add matching lines as subexpression elements.
		//
		in = f.getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String line = null;
		while ((line = br.readLine()) != null) {
		    Matcher m = p.matcher(line);
		    if (m.find()) {
			TextfilecontentItem item = independentFactory.createTextfilecontentItem();
			item.setPath(baseItem.getPath());
			item.setFilename(baseItem.getFilename());
			EntityItemStringType lineType = coreFactory.createEntityItemStringType();
			lineType.setValue(line);
			item.setLine(lineType);
			items.add(independentFactory.createTextfilecontentItem(item));
		    }
		}
	    } catch (PatternSyntaxException e) {
		JOVALSystem.getLogger().log(Level.WARNING, e.getMessage(), e);
	    } finally {
		if (in != null) {
		    try {
			in.close();
		    } catch (IOException e) {
			ctx.log(Level.WARNING, JOVALSystem.getMessage("ERROR_FILE_STREAM_CLOSE", f.toString()), e);
		    }
		}
	    }
	}
	return items;
    }

    // Private

    private boolean match(TextfilecontentState state, TextfilecontentItem item) {
	if (state == null) {
	    return item.getSubexpression().size() > 0; // existence check only -- the item found matching lines
	} else {
	    Pattern p = Pattern.compile((String)state.getSubexpression().getValue());
	    return p.matcher((String)item.getLine().getValue()).find();
	}
    }
}
