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
import oval.schemas.systemcharacteristics.core.EntityItemAnySimpleType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.EntityItemVersionType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.ObjectFactory;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
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
 * Base class for IFile-based IAdapters. Subclasses need only implement get[X]Class, compare, createFileItems and
 * createStorageItem methods.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class BaseFileAdapter implements IAdapter {
    protected static final String DATATYPE_INT		= "int";
    protected static final String DATATYPE_BOOL		= "boolean";
    protected static final String DATATYPE_STRING	= "string";
    protected static final String DATATYPE_VERSION	= "version";

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
    }

    public String getItemData(ObjectComponentType oc, ISystemCharacteristics sc) throws OvalException {
	throw new RuntimeException("getItemData not supported");
    }

    // Protected

    protected abstract JAXBElement<? extends ItemType> createStorageItem(ItemType item);

    /**
     * Return either an EntityItemStringType or a JAXBElement<EntityItemStringType>, as appropriate for the relevant ItemType.
     */
    protected abstract Object convertFilename(EntityItemStringType filename);

    /**
     * Create and return an instance of the appropriate ItemType.
     */
    protected abstract ItemType createFileItem();

    /**
     * Return a list of items to associate with the given ObjectType, based on information gathered from the IFile.
     */
    protected abstract List<? extends ItemType> getItems(ItemType base, ObjectType obj, IFile f) throws IOException;

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
	if (!obj.getClass().getName().equals(getObjectClass().getName())) {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_INSTANCE",
							   getObjectClass().getName(), obj.getClass().getName()));
	}
	ItemType it = createFileItem();
	List<ItemType> items = new Vector<ItemType>();
	for (String path : getPathList(obj, variableValueTypes)) {
	    IFile f = null;
	    try {
		ReflectedFileObject fObj = new ReflectedFileObject(obj);
		ReflectedFileItem fItem = new ReflectedFileItem(it);
	
		f = fs.getFile(path);
		boolean fileExists = f.exists();
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
		    fItem.setFilename(filenameType);
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
			fItem.setFilename(filenameType);
		    } else if (dirExists) {
			fItem.setPath(pathType);
			filenameType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
			fItem.setFilename(filenameType);
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
		    throw new OvalException(JOVALSystem.getMessage("ERROR_TEXTFILECONTENT_SPEC", obj.getId()));
		}

		if (fileExists) {
		    items.addAll(getItems(it, obj, f));
		} else if (!dirExists) {
		    throw new NoSuchElementException("No file or parent directory");
		}
	    } catch (NoSuchElementException e) {
		// skip it
	    } catch (NoSuchMethodException e) {
		ctx.log(Level.WARNING, JOVALSystem.getMessage("ERROR_FILEOBJECT_ITEMS", obj.getId(), path), e);
	    } catch (IllegalAccessException e) {
		ctx.log(Level.WARNING, JOVALSystem.getMessage("ERROR_FILEOBJECT_ITEMS", obj.getId(), path), e);
	    } catch (IllegalArgumentException e) {
		ctx.log(Level.WARNING, JOVALSystem.getMessage("ERROR_FILEOBJECT_ITEMS", obj.getId(), path), e);
	    } catch (InvocationTargetException e) {
		ctx.log(Level.WARNING, JOVALSystem.getMessage("ERROR_FILEOBJECT_ITEMS", obj.getId(), path), e);
	    } catch (IOException e) {
		ctx.log(Level.WARNING, JOVALSystem.getMessage("ERROR_FILEOBJECT_ITEMS", obj.getId(), path), e);
	    } finally {
		if (f != null) {
		    try {
			f.close();
		    } catch (IOException e) {
		    }
		}
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

	list = new Vector<String>();
	try {
	    ReflectedFileObject fObj = new ReflectedFileObject(obj);

	    if (fObj.isSetFilepath()) {
		EntityObjectStringType filepath = fObj.getFilepath();
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
	    } else if (fObj.isSetPath()) {
		EntityObjectStringType path = fObj.getPath();
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

		if (fObj.isSetBehaviors()) {
		    ReflectedFileBehaviors fb = fObj.getBehaviors();
		    list = getPaths(list, fb.getDepth(), fb.getRecurseDirection());
		}

		if (fObj.isSetFilename()) {
		    EntityObjectStringType filename = fObj.getFilename();
		    if (filename == null) {
			// False positive for isSetFilename -- happens with nil
		    } else if (filename.isSetValue()) {
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

    /**
     * A reflection proxy for:
     *     oval.schemas.definitions.independent.Textfilecontent54Object
     *     oval.schemas.definitions.independent.TextfilecontentObject
     *     oval.schemas.definitions.unix.FileObject
     *     oval.schemas.definitions.windows.FileObject
     */
    class ReflectedFileObject {
	ObjectType obj;
	boolean legacy = false;
	Method isSetFilepath=null, getFilepath=null;
	Method isSetFilename, getFilename, isSetPath, getPath, isSetBehaviors, getBehaviors;

	ReflectedFileObject(ObjectType obj) throws NoSuchMethodException {
	    this.obj = obj;

	    try {
		isSetFilepath = obj.getClass().getMethod("isSetFilepath");
		getFilepath = obj.getClass().getMethod("getFilepath");
	    } catch (NoSuchMethodException e) {
		legacy = true;
	    }

	    isSetFilename = obj.getClass().getMethod("isSetFilename");
	    getFilename = obj.getClass().getMethod("getFilename");
	    isSetPath = obj.getClass().getMethod("isSetPath");
	    getPath = obj.getClass().getMethod("getPath");
	    isSetBehaviors = obj.getClass().getMethod("isSetBehaviors");
	    getBehaviors = obj.getClass().getMethod("getBehaviors");
	}

	boolean isSetFilepath()
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    if (legacy) {
		return false;
	    } else {
		return ((Boolean)isSetFilepath.invoke(obj)).booleanValue();
	    }
	}

	EntityObjectStringType getFilepath()
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    if (legacy) {
		return null;
	    } else {
		return (EntityObjectStringType)getFilepath.invoke(obj);
	    }
	}

	boolean isSetFilename()
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    return ((Boolean)isSetFilename.invoke(obj)).booleanValue();
	}

	EntityObjectStringType getFilename()
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    Object o = getFilename.invoke(obj);
	    if (o instanceof JAXBElement) {
		return (EntityObjectStringType)(((JAXBElement)o).getValue());
	    } else {
		return (EntityObjectStringType)o;
	    }
	}

	boolean isSetPath()
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    return ((Boolean)isSetPath.invoke(obj)).booleanValue();
	}

	EntityObjectStringType getPath()
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    return (EntityObjectStringType)getPath.invoke(obj);
	}

	boolean isSetBehaviors()
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    return ((Boolean)isSetBehaviors.invoke(obj)).booleanValue();
	}

	ReflectedFileBehaviors getBehaviors()
		throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    return new ReflectedFileBehaviors(getBehaviors.invoke(obj));
	}
    }

    /**
     * A reflection proxy for:
     *     oval.schemas.definitions.independent.FileBehaviors
     *     oval.schemas.definitions.unix.FileBehaviors
     *     oval.schemas.definitions.windows.FileBehaviors
     */
    class ReflectedFileBehaviors {
	BigInteger maxDepth = new BigInteger("-1");
	String recurseDirection = "none";

	ReflectedFileBehaviors(Object obj)
		throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    if (obj != null) {
	        Method getMaxDepth = obj.getClass().getMethod("getMaxDepth");
	        maxDepth = (BigInteger)getMaxDepth.invoke(obj);
	        Method getRecurseDirection = obj.getClass().getMethod("getRecurseDirection");
	        recurseDirection = (String)getRecurseDirection.invoke(obj);
	    }
	    if ("none".equals(recurseDirection)) {
	        maxDepth = BigInteger.ZERO;
	    }
	}

	String getRecurseDirection() {
	    return recurseDirection;
	}

	int getDepth() {
	    return Integer.parseInt(maxDepth.toString());
	}
    }

    /**
     * A reflection proxy for:
     *     oval.schemas.systemcharacteristics.independent.TextfilecontentItem
     *     oval.schemas.systemcharacteristics.unix.FileItem
     *     oval.schemas.systemcharacteristics.windows.FileItem
     */
    class ReflectedFileItem {
	ItemType it;
	Method setFilepath, setFilename, setPath, setStatus;

	ReflectedFileItem(ItemType it) {
	    this.it = it;
	    Method[] methods = it.getClass().getMethods();
	    for (int i=0; i < methods.length; i++) {
		String name = methods[i].getName();
		if ("setFilepath".equals(name)) {
		    setFilepath = methods[i];
		} else if ("setFilename".equals(name)) {
		    setFilename = methods[i];
		} else if ("setPath".equals(name)) {
		    setPath = methods[i];
		} else if ("setStatus".equals(name)) {
		    setStatus = methods[i];
		}
	    }
	}

	void setFilename(EntityItemStringType filename)
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    setFilename.invoke(it, convertFilename(filename));
	}

	void setFilepath(EntityItemStringType filepath)
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    setFilepath.invoke(it, filepath);
	}

	void setPath(EntityItemStringType path)
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    setPath.invoke(it, path);
	}

	void setStatus(StatusEnumeration status)
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    setStatus.invoke(it, status);
	}
    }
}
