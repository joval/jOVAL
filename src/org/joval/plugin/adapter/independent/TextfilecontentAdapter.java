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
import oval.schemas.definitions.core.ObjectComponentType;
import oval.schemas.definitions.core.ObjectRefType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.StateRefType;
import oval.schemas.definitions.core.StateType;
import oval.schemas.definitions.independent.TextfilecontentObject;
import oval.schemas.definitions.independent.TextfilecontentState;
import oval.schemas.definitions.independent.TextfilecontentTest;
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

    public Class getTestClass() {
	return TextfilecontentTest.class;
    }

    public void evaluate(TestType testResult, ISystemCharacteristics sc) throws OvalException {
	String testId = testResult.getTestId();
	TextfilecontentTest testDefinition = definitions.getTest(testId, TextfilecontentTest.class);
	String objectId = testDefinition.getObject().getObjectRef();
	TextfilecontentState state = null;
	if (testDefinition.isSetState() && testDefinition.getState().get(0).isSetStateRef()) {
	    String stateId = testDefinition.getState().get(0).getStateRef();
	    state = definitions.getState(stateId, TextfilecontentState.class);
	}

	for (VariableValueType var : sc.getVariablesByObjectId(objectId)) {
	    TestedVariableType testedVariable = JOVALSystem.resultsFactory.createTestedVariableType();
	    testedVariable.setVariableId(var.getVariableId());
	    testedVariable.setValue(var.getValue());
	    testResult.getTestedVariable().add(testedVariable);
	}

	int trueCount=0, falseCount=0, errorCount=0;
	if (sc.getObject(objectId).getFlag() == FlagEnumeration.ERROR) {
	    errorCount++;
	}

	if (testDefinition.getCheckExistence() != ExistenceEnumeration.AT_LEAST_ONE_EXISTS) {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_EXISTENCE" , testDefinition.getCheckExistence()));
	}

	for (ItemType it : sc.getItemsByObjectId(objectId)) {
	    if (it instanceof TextfilecontentItem) {
		TextfilecontentItem item = (TextfilecontentItem)it;
		TestedItemType testedItem = JOVALSystem.resultsFactory.createTestedItemType();
		testedItem.setItemId(item.getId());
		if (match(state, item)) {
		    trueCount++;
		    testedItem.setResult(ResultEnumeration.TRUE);
		} else {
		    falseCount++;
		    testedItem.setResult(ResultEnumeration.FALSE);
		}
		testResult.getTestedItem().add(testedItem);
	    } else {
		throw new OvalException(JOVALSystem.getMessage("ERROR_INSTANCE",
					TextfilecontentItem.class.getName(), it.getClass().getName()));
	    }
	}

	if (trueCount > 0) {
	    testResult.setResult(ResultEnumeration.TRUE);
	} else {
	    testResult.setResult(ResultEnumeration.FALSE);
	}
    }

    // Overrides

    protected void preScan() {}

    protected void postScan() {}

    protected JAXBElement<? extends ItemType> createStorageItem(ItemType item) {
	return independentFactory.createTextfilecontentItem((TextfilecontentItem)item);
    }

    protected List<ItemType> createFileItems(ObjectType obj, IFile file) throws NoSuchElementException,
										IOException, OvalException {
	TextfilecontentObject tfcObj = null;
	if (obj instanceof TextfilecontentObject) {
	    tfcObj = (TextfilecontentObject)obj;
	} else {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_INSTANCE",
							   getObjectClass().getName(), obj.getClass().getName()));
	}

	TextfilecontentItem tfcItem = independentFactory.createTextfilecontentItem();
	String path = file.getLocalName();
	boolean fileExists = file.exists();
	boolean dirExists = fileExists;
	String dirPath = path.substring(0, path.lastIndexOf(fs.getDelimString()));
	if (!fileExists) {
	    throw new NoSuchElementException(path);
	}



















	if (tfcObj.isSetFilename()) {
	    EntityItemStringType filepathType = coreFactory.createEntityItemStringType();
	    filepathType.setValue(path);
	    EntityItemStringType pathType = coreFactory.createEntityItemStringType();
	    pathType.setValue(dirPath);
	    EntityItemStringType filenameType = coreFactory.createEntityItemStringType();
	    filenameType.setValue(path.substring(path.lastIndexOf(fs.getDelimString())+1));
	    if (fileExists) {
		tfcItem.setFilepath(filepathType);
		tfcItem.setPath(pathType);
		tfcItem.setFilename(filenameType);
	    } else if (dirExists) {
		tfcItem.setPath(pathType);
		filenameType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		tfcItem.setFilename(filenameType);
	    } else {
		pathType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		tfcItem.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		tfcItem.setPath(pathType);
	    }
	} else {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_TEXTFILECONTENT_SPEC", tfcObj.getId()));
	}

	List<ItemType> tfcList = null;
	if (fileExists) {
	    tfcList = getItems(tfcItem, tfcObj, file);
	} else if (!dirExists) {
	    throw new NoSuchElementException("No file or parent directory");
	}

	return tfcList;
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

    /**
     * Parse the file as specified by the Object, and decorate the Item.
     */
    private List<ItemType> getItems(TextfilecontentItem baseItem, TextfilecontentObject tfcObj, IFile file) throws IOException {
	InputStream in = null;
	List<ItemType> items = new Vector<ItemType>();
	try {
	    Pattern p = Pattern.compile((String)tfcObj.getLine().getValue());

	    //
	    // Read the file line-by-line and search for the pattern.  Add matching lines as subexpression elements.
	    //
	    in = file.getInputStream();
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
		    items.add(item);
		}
	    }
	} catch (PatternSyntaxException e) {
	    JOVALSystem.getLogger().log(Level.WARNING, e.getMessage(), e);
	} finally {
	    if (in != null) {
		try {
		    in.close();
		} catch (IOException e) {
		    ctx.log(Level.WARNING, JOVALSystem.getMessage("ERROR_FILE_STREAM_CLOSE", file.toString()), e);
		}
	    }
	}
	return items;
    }
}
