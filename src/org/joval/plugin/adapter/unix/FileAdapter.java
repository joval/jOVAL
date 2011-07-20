// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.unix;

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
import java.util.StringTokenizer;
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
import oval.schemas.definitions.unix.FileObject;
import oval.schemas.definitions.unix.FileState;
import oval.schemas.definitions.unix.FileTest;
import oval.schemas.systemcharacteristics.core.EntityItemAnySimpleType;
import oval.schemas.systemcharacteristics.core.EntityItemBoolType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.core.VariableValueType;
import oval.schemas.systemcharacteristics.unix.ObjectFactory;
import oval.schemas.systemcharacteristics.unix.FileItem;
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
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.io.StreamTool;
import org.joval.oval.OvalException;
import org.joval.util.BaseFileAdapter;
import org.joval.util.JOVALSystem;

/**
 * Evaluates UNIX File OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class FileAdapter extends BaseFileAdapter {
    private ISession session;
    protected ObjectFactory unixFactory;

    public FileAdapter(ISession session) {
	super(session.getFilesystem());
	this.session = session;
	unixFactory = new ObjectFactory();
    }

    // Implement IAdapter

    public Class getObjectClass() {
	return FileObject.class;
    }

    public Class getTestClass() {
	return FileTest.class;
    }

    public void evaluate(TestType testResult, ISystemCharacteristics sc) throws OvalException {
	String testId = testResult.getTestId();
	FileTest test = definitions.getTest(testId, FileTest.class);
	String objectId = test.getObject().getObjectRef();
	FileState state = null;
	if (test.isSetState() && test.getState().get(0).isSetStateRef()) {
	    String stateId = test.getState().get(0).getStateRef();
	    state = definitions.getState(stateId, FileState.class);
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

	Iterator<ItemType> items = sc.getItemsByObjectId(objectId).iterator();
	switch(test.getCheckExistence()) {
	  case NONE_EXIST: {
	    boolean foundOne = false;
	    while(items.hasNext()) {
		ItemType it = items.next();
		if (it instanceof FileItem) {
		    FileItem item = (FileItem)it;
		    TestedItemType testedItem = JOVALSystem.resultsFactory.createTestedItemType();
		    testedItem.setItemId(item.getId());
		    switch(item.getStatus()) {
		      case EXISTS:
			testedItem.setResult(ResultEnumeration.TRUE);
			trueCount++;
			break;
		      case DOES_NOT_EXIST:
			testedItem.setResult(ResultEnumeration.FALSE);
			falseCount++;
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
		    throw new OvalException(JOVALSystem.getMessage("ERROR_INSTANCE",
								   FileItem.class.getName(), it.getClass().getName()));
		}
	    }
	    result = trueCount == 0;
	  }

	  case AT_LEAST_ONE_EXISTS: {
	    while(items.hasNext()) {
		ItemType it = items.next();
		if (it instanceof FileItem) {
		    FileItem item = (FileItem)it;
		    TestedItemType testedItem = JOVALSystem.resultsFactory.createTestedItemType();
		    testedItem.setItemId(item.getId());
		    switch(item.getStatus()) {
		      case EXISTS:
			if (state == null) {
			    trueCount++;
			    testedItem.setResult(ResultEnumeration.NOT_EVALUATED);
			} else if (compare(state, item)) {
			    trueCount++;
			    testedItem.setResult(ResultEnumeration.TRUE);
			} else {
			    falseCount++;
			    testedItem.setResult(ResultEnumeration.FALSE);
			}
			break;
		      case DOES_NOT_EXIST:
			testedItem.setResult(ResultEnumeration.FALSE);
			falseCount++;
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
		    throw new OvalException(JOVALSystem.getMessage("ERROR_INSTANCE",
								   FileItem.class.getName(), it.getClass().getName()));
		}
	    }
	    switch(test.getCheck()) {
	      case ALL:
		result = falseCount == 0 && trueCount > 0;
		break;
	      case AT_LEAST_ONE:
		result = trueCount > 0;
		break;
	      case ONLY_ONE:
		result = trueCount == 1 && falseCount == 0 && errorCount == 0;
		break;
	      default:
		throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_CHECK", test.getCheck()));
	    }
	    break;
	  }

	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_EXISTENCE", test.getCheckExistence()));
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
	return unixFactory.createFileItem((FileItem)item);
    }

    protected List<ItemType> createFileItems(ObjectType obj, IFile file) throws NoSuchElementException,
										IOException, OvalException {
	FileObject fObj = null;
	if (obj instanceof FileObject) {
	    fObj = (FileObject)obj;
	} else {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_INSTANCE",
							   getObjectClass().getName(), obj.getClass().getName()));
	}

	FileItem fItem = unixFactory.createFileItem();
	String path = file.getLocalName();
	boolean fileExists = file.exists();
	boolean dirExists = fileExists;
	String dirPath = path.substring(0, path.lastIndexOf(fs.getDelimString()));
	if (!fileExists) {
	    throw new NoSuchElementException(path);
	}

	if (fObj.isSetFilepath()) {
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
		    fItem.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		}
	    }
	    fItem.setFilepath(filepathType);
	    fItem.setPath(pathType);
	    fItem.setFilename(unixFactory.createFileItemFilename(filenameType));
	} else if (fObj.isSetFilename()) {
	    EntityItemStringType filepathType = coreFactory.createEntityItemStringType();
	    filepathType.setValue(path);
	    EntityItemStringType pathType = coreFactory.createEntityItemStringType();
	    pathType.setValue(dirPath);
	    EntityItemStringType filenameType = coreFactory.createEntityItemStringType();
	    filenameType.setValue(path.substring(path.lastIndexOf(fs.getDelimString())+1));
	    if (fileExists) {
		fItem.setFilepath(filepathType);
		fItem.setPath(pathType);
		fItem.setFilename(unixFactory.createFileItemFilename(filenameType));
	    } else if (dirExists) {
		fItem.setPath(pathType);
		filenameType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		fItem.setFilename(unixFactory.createFileItemFilename(filenameType));
	    } else {
		pathType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		fItem.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		fItem.setPath(pathType);
	    }
	} else if (fObj.isSetPath()) {
	    EntityItemStringType pathType = coreFactory.createEntityItemStringType();
	    pathType.setValue(dirPath);
	    if (!fileExists) {
		pathType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    }
	    fItem.setPath(pathType);
	} else {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_TEXTFILECONTENT_SPEC", fObj.getId()));
	}

	if (fileExists) {
	    setItem(fItem, file);
	} else if (!dirExists) {
	    throw new NoSuchElementException("No file or parent directory");
	}

	List<ItemType> fList = new Vector<ItemType>();
	fList.add(fItem);
	return fList;
    }

    // Private

    private boolean compare(FileState state, FileItem item) throws OvalException {
	if (state.isSetType()) {
	    return ((String)state.getType().getValue()).equals((String)item.getType().getValue());
	}
	if (state.isSetUread()) {
	    return ((String)state.getUread().getValue()).equals((String)item.getUread().getValue());
	}
	if (state.isSetUwrite()) {
	    return ((String)state.getUwrite().getValue()).equals((String)item.getUwrite().getValue());
	}
	if (state.isSetUexec()) {
	    return ((String)state.getUexec().getValue()).equals((String)item.getUexec().getValue());
	}
	if (state.isSetSuid()) {
	    return ((String)state.getSuid().getValue()).equals((String)item.getSuid().getValue());
	}
	if (state.isSetUserId()) {
	    return ((String)state.getUserId().getValue()).equals((String)item.getUserId().getValue());
	}
	if (state.isSetGread()) {
	    return ((String)state.getGread().getValue()).equals((String)item.getGread().getValue());
	}
	if (state.isSetGwrite()) {
	    return ((String)state.getGwrite().getValue()).equals((String)item.getGwrite().getValue());
	}
	if (state.isSetGexec()) {
	    return ((String)state.getGexec().getValue()).equals((String)item.getGexec().getValue());
	}
	if (state.isSetSgid()) {
	    return ((String)state.getSgid().getValue()).equals((String)item.getSgid().getValue());
	}
	if (state.isSetGroupId()) {
	    return ((String)state.getGroupId().getValue()).equals((String)item.getGroupId().getValue());
	}
	if (state.isSetOread()) {
	    return ((String)state.getOread().getValue()).equals((String)item.getOread().getValue());
	}
	if (state.isSetOwrite()) {
	    return ((String)state.getOwrite().getValue()).equals((String)item.getOwrite().getValue());
	}
	if (state.isSetOexec()) {
	    return ((String)state.getOexec().getValue()).equals((String)item.getOexec().getValue());
	}
	if (state.isSetSticky()) {
	    return ((String)state.getSticky().getValue()).equals((String)item.getSticky().getValue());
	}
	if (state.isSetHasExtendedAcl()) {
	    return ((String)state.getHasExtendedAcl().getValue()).equals((String)item.getHasExtendedAcl().getValue());
	}
	if (state.isSetCTime()) {
	    return ((String)state.getCTime().getValue()).equals((String)item.getCTime().getValue());
	}
	if (state.isSetATime()) {
	    return ((String)state.getATime().getValue()).equals((String)item.getATime().getValue());
	}
	if (state.isSetMTime()) {
	    return ((String)state.getMTime().getValue()).equals((String)item.getMTime().getValue());
	}
	throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_STATE", state.getId()));
    }

    /**
     * Decorate the Item with information about the file.
     */
    private void setItem(FileItem item, IFile file) throws IOException {
	EntityItemIntType aTimeType = coreFactory.createEntityItemIntType();
	aTimeType.setValue(Long.toString(file.accessTime()));
	item.setATime(aTimeType);

	EntityItemIntType cTimeType = coreFactory.createEntityItemIntType();
	cTimeType.setValue(Long.toString(file.createTime()));
	item.setCTime(cTimeType);

	EntityItemIntType mTimeType = coreFactory.createEntityItemIntType();
	mTimeType.setValue(Long.toString(file.lastModified()));
	item.setMTime(mTimeType);

	try {
	    Lstat ls = new Lstat(file.getLocalName());

	    EntityItemStringType type = coreFactory.createEntityItemStringType();
	    type.setValue(ls.getType());
	    item.setType(type);

	    EntityItemIntType userId = coreFactory.createEntityItemIntType();
	    userId.setValue(Integer.toString(ls.getUserId()));
	    item.setUserId(userId);

	    EntityItemIntType groupId = coreFactory.createEntityItemIntType();
	    groupId.setValue(Integer.toString(ls.getGroupId()));
	    item.setGroupId(groupId);

	    EntityItemBoolType uRead = coreFactory.createEntityItemBoolType();
	    uRead.setValue(Boolean.toString(ls.uRead()));
	    item.setUread(uRead);

	    EntityItemBoolType uWrite = coreFactory.createEntityItemBoolType();
	    uWrite.setValue(Boolean.toString(ls.uWrite()));
	    item.setUwrite(uWrite);

	    EntityItemBoolType uExec = coreFactory.createEntityItemBoolType();
	    uExec.setValue(Boolean.toString(ls.uExec()));
	    item.setUexec(uExec);

	    EntityItemBoolType sUid = coreFactory.createEntityItemBoolType();
	    sUid.setValue(Boolean.toString(ls.sUid()));
	    item.setSuid(sUid);

	    EntityItemBoolType gRead = coreFactory.createEntityItemBoolType();
	    gRead.setValue(Boolean.toString(ls.gRead()));
	    item.setGread(gRead);

	    EntityItemBoolType gWrite = coreFactory.createEntityItemBoolType();
	    gWrite.setValue(Boolean.toString(ls.gWrite()));
	    item.setGwrite(gWrite);

	    EntityItemBoolType gExec = coreFactory.createEntityItemBoolType();
	    gExec.setValue(Boolean.toString(ls.gExec()));
	    item.setGexec(gExec);

	    EntityItemBoolType sGid = coreFactory.createEntityItemBoolType();
	    sGid.setValue(Boolean.toString(ls.sGid()));
	    item.setSgid(sGid);

	    EntityItemBoolType oRead = coreFactory.createEntityItemBoolType();
	    oRead.setValue(Boolean.toString(ls.oRead()));
	    item.setOread(oRead);

	    EntityItemBoolType oWrite = coreFactory.createEntityItemBoolType();
	    oWrite.setValue(Boolean.toString(ls.oWrite()));
	    item.setOwrite(oWrite);

	    EntityItemBoolType oExec = coreFactory.createEntityItemBoolType();
	    oExec.setValue(Boolean.toString(ls.oExec()));
	    item.setOexec(oExec);

	    EntityItemBoolType sticky = coreFactory.createEntityItemBoolType();
	    sticky.setValue(Boolean.toString(ls.sticky()));
	    item.setSticky(sticky);

	    EntityItemBoolType aclType = coreFactory.createEntityItemBoolType();
	    aclType.setValue(Boolean.toString(ls.hasExtendedAcl()));
	    item.setHasExtendedAcl(aclType);
	} catch (Exception e) {
e.printStackTrace();
	    throw new IOException (e);
	}
    }

    class Lstat {
	private boolean hasExtendedAcl = false;
	private String permissions;
	private int uid, gid;
	private char type;

	Lstat(String path) throws Exception {
	    IProcess p = session.createProcess("/usr/bin/ls -n " + path);
	    p.start();
	    BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    String line = br.readLine();
	    br.close();

	    type = line.charAt(0);
	    permissions = line.substring(1, 10);
	    if (line.charAt(11) == '+') {
		hasExtendedAcl = true;
	    }
	    StringTokenizer tok = new StringTokenizer(line.substring(12));
	    int uid = Integer.parseInt(tok.nextToken());
	    int gid = Integer.parseInt(tok.nextToken());
	}

	String getType() {
	    switch(type) {
	      case 'd':
		return "directory";
	      case 'p':
		return "fifo";
	      case 'l':
		return "symlink";
	      case 'b':
		return "block";
	      case 'c':
		return "character";
	      case 's':
		return "socket";
	      case '-':
	      default:
		return "file";
	    }
	}

	int getUserId() {
	    return uid;
	}

	int getGroupId() {
	    return gid;
	}

	boolean uRead() {
	    return permissions.charAt(0) == 'r';
	}

	boolean uWrite() {
	    return permissions.charAt(1) == 'w';
	}

	boolean uExec() {
	    return permissions.charAt(2) != '-';
	}

	boolean sUid() {
	    return permissions.charAt(2) == 's';
	}

	boolean gRead() {
	    return permissions.charAt(3) == 'r';
	}

	boolean gWrite() {
	    return permissions.charAt(4) == 'w';
	}

	boolean gExec() {
	    return permissions.charAt(5) != '-';
	}

	boolean sGid() {
	    return permissions.charAt(5) == 's';
	}

	boolean oRead() {
	    return permissions.charAt(6) == 'r';
	}

	boolean oWrite() {
	    return permissions.charAt(7) == 'w';
	}

	boolean oExec() {
	    return permissions.charAt(8) != '-';
	}

	boolean sticky() {
	    return permissions.charAt(8) == 't';
	}

	boolean hasExtendedAcl() {
	    return hasExtendedAcl;
	}
    }
}
