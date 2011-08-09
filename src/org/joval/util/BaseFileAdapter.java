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
import org.joval.oval.OvalException;
import org.joval.util.JOVALSystem;
import org.joval.util.Version;

/**
 * Base class for IFile-based IAdapters. Subclasses need only implement get[X]Class, compare, createFileItem, convertFilename
 * and getItems methods.  The base class handles searches and caching of search results.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class BaseFileAdapter implements IAdapter {
    protected IAdapterContext ctx;
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
	this.ctx = ctx;
    }

    public boolean connect() {
	return true;
    }

    public void disconnect() {
    }

    public List<JAXBElement<? extends ItemType>> getItems(ObjectType obj, List<VariableValueType> vars) throws OvalException {
	if (!obj.getClass().getName().equals(getObjectClass().getName())) {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_INSTANCE",
							   getObjectClass().getName(), obj.getClass().getName()));
	}
	List<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	for (String path : getPathList(obj, vars)) {
	    IFile f = null;
	    try {
		ReflectedFileObject fObj = new ReflectedFileObject(obj);
		ReflectedFileItem fItem = new ReflectedFileItem();
	
		f = fs.getFile(path);
		boolean isDirectory = f.isDirectory();
		boolean fileExists = f.exists();
		boolean dirExists = fileExists;
		String dirPath = null;
		if (isDirectory) {
		    dirPath = path;
		} else {
		    dirPath = path.substring(0, path.lastIndexOf(fs.getDelimString()));
		}

/*DAS -- should the path end with the delimiter string, or not?
		if (!dirPath.endsWith(fs.getDelimString())) {
		    dirPath += fs.getDelimString();
		}
*/
		if (!fileExists) {
		    throw new NoSuchElementException(path);
		}

		if (fObj.isSetFilepath()) {
		    if (isDirectory) {
			//
			// Object is looking for files, so skip over this directory
			//
			continue;
		    } else {
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
		    }
		} else if (fObj.isSetFilename() && fObj.getFilename() != null) {
		    if (isDirectory) {
			//
			// Object is looking for files, so skip over this directory
			//
			continue;
		    } else {
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
		    }
		} else if (fObj.isSetPath()) {
		    EntityItemStringType pathType = coreFactory.createEntityItemStringType();
		    pathType.setValue(dirPath);
		    if (!fileExists) {
			pathType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		    }
		    fItem.setPath(pathType);
		    if (!isDirectory) {
			EntityItemStringType filenameType = coreFactory.createEntityItemStringType();
			filenameType.setValue(path.substring(path.lastIndexOf(fs.getDelimString())+1));
			fItem.setFilename(filenameType);
			EntityItemStringType filepathType = coreFactory.createEntityItemStringType();
			filepathType.setValue(path);
			fItem.setFilepath(filepathType);
		    }
		} else {
		    throw new OvalException(JOVALSystem.getMessage("ERROR_TEXTFILECONTENT_SPEC", obj.getId()));
		}

		if (fileExists) {
		    items.addAll(getItems(fItem.it, obj, f, vars));
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
		MessageType msg = new MessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		ctx.addObjectMessage(obj.getId(), msg);
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

    // Protected

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
     *
     * @arg it the base ItemType containing filepath, path and filename information already populated
     */
    protected abstract List<JAXBElement<? extends ItemType>>
	getItems(ItemType it, ObjectType obj, IFile f, List<VariableValueType> vars) throws IOException, OvalException;

    // Internal

    /**
     * Get a list of String paths for this object.  This accommodates searches (from pattern match operations),
     * singletons (from equals operations) and handles recursive searches specified by FileBehaviors.
     */
    final List<String> getPathList(ObjectType obj, List<VariableValueType> vars) throws OvalException {
	List<String> list = pathMap.get(obj.getId());
	if (list != null) {
	    return list;
	}

	list = new Vector<String>();
	try {
	    ReflectedFileObject fObj = new ReflectedFileObject(obj);

	    boolean patternMatch = false;
	    if (fObj.isSetFilepath()) {
		List<String> filepaths = new Vector<String>();
		EntityObjectStringType filepath = fObj.getFilepath();
		if (filepath.isSetVarRef()) {
		    filepaths.addAll(ctx.resolve(filepath.getVarRef(), vars));
		} else {
		    filepaths.add((String)filepath.getValue());
		}
		switch(filepath.getOperation()) {
		  case EQUALS:
		    list.addAll(filepaths);
		    break;
		  case PATTERN_MATCH:
		    patternMatch = true;
		    for (String value : filepaths) {
			list.addAll(fs.search(value));
		    }
		    break;
		  default:
		    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", filepath.getOperation()));
		}
	    } else if (fObj.isSetPath()) {
		List<String> paths = new Vector<String>();
		EntityObjectStringType path = fObj.getPath();
		if (path.isSetVarRef()) {
		    paths.addAll(ctx.resolve(path.getVarRef(), vars));
		} else if (path.isSetValue()) {
		    paths.add((String)path.getValue());
		}
		switch(path.getOperation()) {
		  case EQUALS:
		    list.addAll(paths);
		    break;
		  case PATTERN_MATCH:
		    patternMatch = true;
		    for (String value : paths) {
			list.addAll(fs.search(value));
		    }
		    break;
		  default:
		    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", path.getOperation()));
		}

		if (fObj.isSetBehaviors()) {
		    ReflectedFileBehaviors fb = fObj.getBehaviors();
		    list = getPaths(list, fb.getDepth(), fb.getRecurseDirection());
		} else if (patternMatch) {
		    //
		    // Wildcard pattern matches are really supposed to be recursive searches, unfortunately
		    //
		    List<String> newList = new Vector<String>();
		    for (String value : list) {
			if (((String)path.getValue()).indexOf(".*") != -1) {
			    List<String> l = new Vector<String>();
			    l.add(value);
			    newList.addAll(getPaths(l, -1, "down"));
			}
		    }
		    for (String value : newList) {
			if (!list.contains(value)) {
			    list.add(value);
			}
		    }
		}

		if (fObj.isSetFilename()) {
		    EntityObjectStringType filename = fObj.getFilename();
		    if (filename == null) {
			// False positive for isSetFilename -- happens with nil
		    } else {
			List<String> fnames = new Vector<String>();
			if (filename.isSetVarRef()) {
			    fnames.addAll(ctx.resolve(filename.getVarRef(), vars));
			} else {
			    fnames.add((String)filename.getValue());
			}
			List<String> files = new Vector<String>();
			for (String pathString : list) {
			    for (String fname : fnames) {
				switch(filename.getOperation()) {
				  case PATTERN_MATCH:
				    files.addAll(fs.search(pathString, fname));
				    break;
    
				  case EQUALS:
				    if (pathString.endsWith(fs.getDelimString())) {
					files.add(pathString + fname);
				    } else {
					files.add(pathString + fs.getDelimString() + fname);
				    }
				    break;
    
				  default:
				    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION",
										   filename.getOperation()));
				}
			    }
			}
			list = files;
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

    /**
     * Crawls recursively based on FileBehaviors.
     */
    private List<String> getPaths(List<String> list, int depth, String recurseDirection) throws IOException {
	if ("none".equals(recurseDirection) || depth == 0) {
	    return list;
	} else {
	    List<String> results = new Vector<String>();
	    for (String path : list) {
		if (!path.endsWith(fs.getDelimString())) {
		    path += fs.getDelimString();
		}
		IFile f = fs.getFile(path);
		if (f.exists() && f.isDirectory()) {
		    results.add(path);
		    if ("up".equals(recurseDirection)) {
			f.close();
		        int ptr = 0;
			if (path.endsWith(fs.getDelimString())) {
			    path = path.substring(0, path.lastIndexOf(fs.getDelimString()));
			}
			ptr = path.lastIndexOf(fs.getDelimString());
			if (ptr != -1) {
			    Vector<String> v = new Vector<String>();
			    v.add(path.substring(0, ptr + fs.getDelimString().length()));
			    results.addAll(getPaths(v, --depth, recurseDirection));
			}
		    } else { // recurse down
			String[] children = f.list();
			f.close();
			if (children != null) {
			    Vector<String> v = new Vector<String>();
			    for (int i=0; i < children.length; i++) {
				if (path.endsWith(fs.getDelimString())) {
				    v.add(path + children[i]);
				} else {
				    v.add(path + fs.getDelimString() + children[i]);
				}
			    }
			    results.addAll(getPaths(v, --depth, recurseDirection));
			}
		    }
		}
	    }
	    return results;
	}
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
	Method getId;
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

	    getId = obj.getClass().getMethod("getId");
	    isSetFilename = obj.getClass().getMethod("isSetFilename");
	    getFilename = obj.getClass().getMethod("getFilename");
	    isSetPath = obj.getClass().getMethod("isSetPath");
	    getPath = obj.getClass().getMethod("getPath");
	    isSetBehaviors = obj.getClass().getMethod("isSetBehaviors");
	    getBehaviors = obj.getClass().getMethod("getBehaviors");
	}

	String getId()
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    return (String)getId.invoke(obj);
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

	ReflectedFileItem() {
	    it = createFileItem();
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
