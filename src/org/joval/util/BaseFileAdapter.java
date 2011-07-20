// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.regex.Matcher;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.definitions.core.EntityObjectStringType;
import oval.schemas.definitions.core.ObjectComponentType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.ObjectFactory;
import oval.schemas.systemcharacteristics.core.VariableValueType;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IAdapterContext;
import org.joval.intf.oval.IDefinitions;
import org.joval.intf.oval.ISystemCharacteristics;
import org.joval.oval.OvalException;
import org.joval.util.JOVALSystem;
import org.joval.util.Version;

/**
 * Base class for IFile-based IAdapters. Subclasses need only implement getObjectClass, getTestClass, evaluate,
 * createFileItems and createStorageItem methods.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class BaseFileAdapter implements IAdapter {
    protected IAdapterContext ctx;
    protected IDefinitions definitions;
    protected IFilesystem fs;
    protected Hashtable<String, List<String>> pathMap;
    protected oval.schemas.systemcharacteristics.core.ObjectFactory coreFactory;

    protected BaseFileAdapter(IFilesystem fs) {
	this.fs = fs;
	pathMap = new Hashtable<String, List<String>>();
	coreFactory = new ObjectFactory();
    }

    // Implement IAdapter

    public void init(IAdapterContext ctx) {
	definitions = ctx.getDefinitions();
	this.ctx = ctx;
    }

    /**
     * A generic implementation of the IAdapter.scan method for File-type objects.  Subclasses need only implement the
     * createFileItems and createStorageItem methods.
     */
    public void scan(ISystemCharacteristics sc) throws OvalException {
	try {
	    preScan();
	    Iterator <ObjectType>iter = definitions.iterateObjects(getObjectClass());
	    while (iter.hasNext()) {
		ObjectType obj = iter.next();
		if (isSet(obj)) {
		    // Set objects can be skipped, they only contain references to objects that will be scaned elsewhere.
		    continue;
		} else {
		    ctx.status(obj.getId());
		    List<VariableValueType> variableValueTypes = new Vector<VariableValueType>();
		    List<? extends ItemType> items = getItems(obj, variableValueTypes);
		    if (items.size() == 0) {
			MessageType msg = new MessageType();
			msg.setLevel(MessageLevelEnumeration.INFO);
			msg.setValue("Could not resolve any items for object");
			sc.setObject(obj.getId(), obj.getComment(), obj.getVersion(), FlagEnumeration.DOES_NOT_EXIST, msg);
		    } else {
			sc.setObject(obj.getId(), obj.getComment(), obj.getVersion(), FlagEnumeration.COMPLETE, null);
			for (ItemType itemType : items) {
			    JAXBElement<? extends ItemType> storageItem = createStorageItem(itemType);
			    BigInteger itemId = sc.storeItem(storageItem);
			    sc.relateItem(obj.getId(), itemId);
			}
		    }
		    for (VariableValueType var : variableValueTypes) {
			sc.storeVariable(var);
			sc.relateVariable(obj.getId(), var.getVariableId());
		    }
		}
	    }
	} finally {
	    postScan();
	}
    }

    public String getItemData(ObjectComponentType oc, ISystemCharacteristics sc) throws OvalException {
	throw new RuntimeException("getItemData not supported");
    }

    // Protected

    protected abstract void preScan();

    protected abstract void postScan();

    protected abstract JAXBElement<? extends ItemType> createStorageItem(ItemType item);

    protected abstract List<ItemType> createFileItems(ObjectType obj, IFile file) throws NoSuchElementException,
										  IOException, OvalException;

    // Internal

    final boolean isSet(ObjectType obj) {
	try {
	    Method isSetSet = obj.getClass().getMethod("isSetSet");
	    return ((Boolean)isSetSet.invoke(obj)).booleanValue();
	} catch (NoSuchMethodException e) {
	} catch (IllegalAccessException e) {
       	    ctx.log(Level.SEVERE, e.getMessage(), e);
	} catch (InvocationTargetException e) {
       	    ctx.log(Level.SEVERE, e.getMessage(), e);
	}
	return false;
    }

    final List<? extends ItemType> getItems(ObjectType obj, List<VariableValueType> variableValueTypes) throws OvalException {
	List<ItemType> items = new Vector<ItemType>();
	for (String path : getPathList(obj, variableValueTypes)) {
	    try {
		IFile f = fs.getFile(path);
		items.addAll(createFileItems(obj, f));
		f.close();
	    } catch (NoSuchElementException e) {
		// skip it
	    } catch (IOException e) {
		ctx.log(Level.WARNING, JOVALSystem.getMessage("ERROR_FILEOBJECT_ITEMS", obj.getId(), path), e);
	    }
	}
	return items;
    }

    /**
     * Get a list of String paths for this object.  This accommodates both searches and singletons.
     */
    final List<String> getPathList(ObjectType obj, List<VariableValueType> variables) throws OvalException {
	List<String> list = pathMap.get(obj.getId());
	if (list != null) {
	    return list;
	}

	//
	// Handle the possibility of a legacy-style FileAdapter, like Textfilecontent (pre-54).
	//
	boolean legacy = false;
	Method isSetFilepath=null, getFilepath=null;
	try {
	    isSetFilepath = obj.getClass().getMethod("isSetFilepath");
	    getFilepath = obj.getClass().getMethod("getFilepath");
	} catch (NoSuchMethodException e) {
	    legacy = true;
	}

	list = new Vector<String>();
	try {
	    Method isSetFilename, getFilename, isSetPath, getPath, isSetBehaviors, getBehaviors;
	    
	    isSetFilename = obj.getClass().getMethod("isSetFilename");
	    getFilename = obj.getClass().getMethod("getFilename");
	    isSetPath = obj.getClass().getMethod("isSetPath");
	    getPath = obj.getClass().getMethod("getPath");
	    isSetBehaviors = obj.getClass().getMethod("isSetBehaviors");
	    getBehaviors = obj.getClass().getMethod("getBehaviors");

	    if (!legacy && ((Boolean)isSetFilepath.invoke(obj)).booleanValue()) {
		EntityObjectStringType filepath = (EntityObjectStringType)getFilepath.invoke(obj);
		if (filepath.isSetVarRef()) {
		    String resolved = ctx.resolve(filepath.getVarRef(), variables);
		    if (OperationEnumeration.EQUALS == filepath.getOperation()) {
			list.add(resolved);
		    } else if (OperationEnumeration.PATTERN_MATCH == filepath.getOperation()) {
			list.addAll(fs.search(resolved));
		    } else {
			throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", filepath.getOperation()));
		    }
		} else {
		    list.add((String)filepath.getValue());
		}
	    } else if (((Boolean)isSetPath.invoke(obj)).booleanValue()) {
		EntityObjectStringType path = (EntityObjectStringType)getPath.invoke(obj);
		if (path.isSetVarRef()) {
		    String resolved = ctx.resolve(path.getVarRef(), variables);
		    if (OperationEnumeration.EQUALS == path.getOperation()) {
			list.add(resolved);
		    } else if (OperationEnumeration.PATTERN_MATCH == path.getOperation()) {
			list.addAll(fs.search(resolved));
		    } else {
			throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", path.getOperation()));
		    }
		} else if (OperationEnumeration.PATTERN_MATCH == path.getOperation()) {
		    list.addAll(fs.search((String)path.getValue()));
		} else if (OperationEnumeration.EQUALS == path.getOperation()) {
		    list.add((String)path.getValue());
		} else {
		    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", path.getOperation()));
		}

		if (((Boolean)isSetBehaviors.invoke(obj)).booleanValue()) {
		    Object behaviors = getBehaviors.invoke(obj);
		    BigInteger maxDepth = new BigInteger("-1");
		    String recurseDirection = "none";
		    if (behaviors != null) {
		        Method getMaxDepth = behaviors.getClass().getMethod("getMaxDepth");
		        maxDepth = (BigInteger)getMaxDepth.invoke(behaviors);
		        Method getRecurseDirection = behaviors.getClass().getMethod("getRecurseDirection");
		        recurseDirection = (String)getRecurseDirection.invoke(behaviors);
		    }
		    if ("none".equals(recurseDirection)) {
		        maxDepth = BigInteger.ZERO;
		    }
		    int depth = Integer.parseInt(maxDepth.toString());
		    list = getPaths(list, depth, recurseDirection);
		}

		if (((Boolean)isSetFilename.invoke(obj)).booleanValue()) {
		    EntityObjectStringType filename = null;
		    Object oFilename = getFilename.invoke(obj);
		    if (oFilename instanceof JAXBElement) {
			filename = (EntityObjectStringType)((JAXBElement)oFilename).getValue();
		    } else if (oFilename instanceof EntityObjectStringType) {
			filename = (EntityObjectStringType)oFilename;
		    } else {
			throw new OvalException(JOVALSystem.getMessage("ERROR_FILENAME_TYPE", oFilename.getClass().getName()));
		    }
		    if (filename.isSetValue()) {
			List<String> files = new Vector<String>();
			for (String pathString : list) {
			    files.add(pathString + fs.getDelimString() + (String)filename.getValue());
			}
			list = files;
		    } else if (filename.isSetVarRef()) {
			String resolved = ctx.resolve(filename.getVarRef(), variables);
			switch(filename.getOperation()) {
			  case PATTERN_MATCH: {
			    List<String> files = new Vector<String>();
			    for (String pathString : list) {
				files.addAll(fs.search(pathString, resolved));
			    }
			    list = files;
			    break;
			  }
			  default:
			    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION",
									   filename.getOperation()));
			}
		    }
		}
	    } else {
		throw new OvalException("Unknown file ObjectType type for " + obj.getId());
	    }
	} catch (NoSuchMethodException e) {
       	    ctx.log(Level.SEVERE, e.getMessage(), e);
	} catch (IllegalAccessException e) {
       	    ctx.log(Level.SEVERE, e.getMessage(), e);
	} catch (InvocationTargetException e) {
       	    ctx.log(Level.SEVERE, e.getMessage(), e);
	} catch (IOException e) {
       	    ctx.log(Level.WARNING, JOVALSystem.getMessage("ERROR_IO", e.getMessage()), e);
	} catch (NoSuchElementException e) {
       	    ctx.log(Level.FINER, JOVALSystem.getMessage("STATUS_NOT_FOUND", e.getMessage(), obj.getId()));
	}
	pathMap.put(obj.getId(), list);
	return list;
    }

    private List<String> getPaths(List<String> list, int depth, String recurseDirection) throws IOException {
	List<String> results = new Vector<String>();
	if (!"none".equals(recurseDirection) && depth != 0) {
	    for (String path : list) {
		IFile f = fs.getFile(path);
		if (f.exists() && f.isDirectory()) {
		    results.add(path);
		    if ("up".equals(recurseDirection)) {
			f.close();
		        int ptr = path.lastIndexOf(fs.getDelimString());
			if (ptr != -1) {
			    Vector<String> v = new Vector<String>();
			    v.add(path.substring(0, ptr + fs.getDelimString().length()));
			    results.addAll(getPaths(v, --depth, recurseDirection));
			}
		    } else { // recurse down
			String[] children = f.list();
			f.close();
			Vector<String> v = new Vector<String>();
			for (int i=0; i < children.length; i++) {
			    v.add(path + children[i] + fs.getDelimString());
			}
			results.addAll(getPaths(v, --depth, recurseDirection));
		    }
		}
	    }
	}
	return results;
    }
}
