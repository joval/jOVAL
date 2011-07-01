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

    public void evaluate(TestType testResult, ISystemCharacteristics sc) throws OvalException {
	String testId = testResult.getTestId();
	Textfilecontent54Test testDefinition = definitions.getTest(testId, Textfilecontent54Test.class);
	String objectId = testDefinition.getObject().getObjectRef();
	Textfilecontent54State state = null;
	if (testDefinition.isSetState() && testDefinition.getState().get(0).isSetStateRef()) {
	    String stateId = testDefinition.getState().get(0).getStateRef();
	    state = definitions.getState(stateId, Textfilecontent54State.class);
	} else {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_STATE_MISSING", testId));
	}

	for (VariableValueType var : sc.getVariablesByObjectId(objectId)) {
	    TestedVariableType testedVariable = JOVALSystem.resultsFactory.createTestedVariableType();
	    testedVariable.setVariableId(var.getVariableId());
	    testedVariable.setValue(var.getValue());
	    testResult.getTestedVariable().add(testedVariable);
	}

	boolean result = false;
	int trueCount=0, falseCount=0, errorCount=0;
	if (sc.getObject(objectId).getFlag() == FlagEnumeration.ERROR) {
	    errorCount++;
	}

	if (testDefinition.getCheckExistence() != ExistenceEnumeration.AT_LEAST_ONE_EXISTS) {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_EXISTENCE" , testDefinition.getCheckExistence()));
	}
	if (state.isSetText()) {
	    if (OperationEnumeration.PATTERN_MATCH == state.getText().getOperation()) {
		String pattern = (String)state.getText().getValue();
		for (ItemType it : sc.getItemsByObjectId(objectId)) {
		    if (it instanceof TextfilecontentItem) {
			TextfilecontentItem item = (TextfilecontentItem)it;
			TestedItemType testedItem = JOVALSystem.resultsFactory.createTestedItemType();
			testedItem.setItemId(item.getId());
			switch(item.getStatus()) {
			  case EXISTS:
			    if (item.getPattern().getValue().equals(pattern)) {
				testedItem.setResult(ResultEnumeration.TRUE);
				trueCount++;
			    } else {
				testedItem.setResult(ResultEnumeration.FALSE);
				falseCount++;
			    }
			    break;
			  case DOES_NOT_EXIST:
			    testedItem.setResult(ResultEnumeration.NOT_APPLICABLE);
			    break;
			  case ERROR:
			    testedItem.setResult(ResultEnumeration.ERROR);
			    errorCount++;
			    break;
			  default:
			    testedItem.setResult(ResultEnumeration.NOT_EVALUATED);
			    break;
			}
			testResult.getTestedItem().add(testedItem);
		    } else {
			throw new OvalException("Not a TextfilecontentItem: " + it.getClass().getName());
		    }
		}
	    } else {
		throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", state.getText().getOperation()));
	    }
	} else {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_STATE_BAD", state.getId()));
	}

	if (errorCount > 0) {
	    testResult.setResult(ResultEnumeration.ERROR);
	} else if (result) {
	    testResult.setResult(ResultEnumeration.TRUE);
	} else {
	    testResult.setResult(ResultEnumeration.FALSE);
	}
    }

    // Private

    protected void preScan() {}

    protected void postScan() {}

    protected JAXBElement<? extends ItemType> createStorageItem(ItemType item) {
	return independentFactory.createTextfilecontentItem((TextfilecontentItem)item);
    }

    protected ItemType createFileItem(ObjectType obj, IFile file) throws NoSuchElementException, IOException, OvalException {
	Textfilecontent54Object tfcObj = null;
	if (obj instanceof Textfilecontent54Object) {
	    tfcObj = (Textfilecontent54Object)obj;
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
//	    dirExists = fs.getFile(dirPath).exists();
	}

	if (tfcObj.isSetFilepath()) {
	    EntityItemStringType filepathType = coreFactory.createEntityItemStringType();
	    filepathType.setValue(path);
	    EntityItemStringType pathType = coreFactory.createEntityItemStringType();
	    pathType.setValue(dirPath);
	    EntityItemStringType filenameType = coreFactory.createEntityItemStringType();
	    filenameType.setValue(path.substring(path.lastIndexOf(fs.getDelimString())+1));
	    if (!fileExists) {
		filepathType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		filenameType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		if (!dirExists) {
		    pathType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		}
	    }
	    tfcItem.setFilepath(filepathType);
	    tfcItem.setPath(pathType);
	    tfcItem.setFilename(filenameType);
	} else if (tfcObj.isSetFilename()) {
	    EntityItemStringType pathType = coreFactory.createEntityItemStringType();
	    pathType.setValue(dirPath);
	    EntityItemStringType filenameType = coreFactory.createEntityItemStringType();
	    filenameType.setValue(path.substring(path.lastIndexOf(fs.getDelimString())+1));
	    if (fileExists) {
		tfcItem.setPath(pathType);
		tfcItem.setFilename(filenameType);
	    } else if (dirExists) {
/* -- Ovaldi does nothing
		fItem.setPath(pathType);
		filenameType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		fItem.setFilename(windowsFactory.createFileItemFilename(filenameType));
*/
	    } else {
		pathType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		tfcItem.setPath(pathType);
	    }
	} else {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_TEXTFILECONTENT_SPEC", tfcObj.getId()));
	}

	if (fileExists) {
	    setItem(tfcItem, tfcObj, file);
	} else if (!dirExists) {
	    throw new NoSuchElementException("No file or parent directory");
	}

	return tfcItem;
    }

    // Private

    /**
     * Parse the file as specified by the Object, and decorate the Item.
     */
    private void setItem(TextfilecontentItem item, Textfilecontent54Object tfcObj, IFile file) throws IOException {
	InputStream in = null;
	try {
	    Pattern p = Pattern.compile((String)tfcObj.getPattern().getValue());
	    EntityItemStringType patternType = coreFactory.createEntityItemStringType();
	    patternType.setValue(p.toString());
	    item.setPattern(patternType);
	    EntityItemIntType instanceType = coreFactory.createEntityItemIntType();
	    instanceType.setDatatype(tfcObj.getInstance().getDatatype());
	    instanceType.setValue(tfcObj.getInstance().getValue());
	    item.setInstance(instanceType);

	    String text = null;
	    boolean multiline = false;
	    if (tfcObj.isSetBehaviors()) {
		multiline = tfcObj.getBehaviors().isMultiline();
	    }
	    if (multiline) {
		//
		// Read the whole file into a buffer and search for the pattern
		//
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
	    } else {
		//
		// Read line-by-line and search for the pattern
		//
		in = file.getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String line = null;
		while ((line = br.readLine()) != null) {
		    Matcher m = p.matcher(line);
		    if (m.find()) {
			MatchResult mr = m.toMatchResult();
			text = line.substring(mr.start(), mr.end());
			break;
		    }
		}
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
		    ctx.log(Level.WARNING, "Error closing stream: " + file.toString(), e);
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
