// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.logging.Level;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.definitions.core.EntityObjectStringType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.systemcharacteristics.core.EntityItemAnySimpleType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.EntityItemVersionType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.oval.OvalException;
import org.joval.util.JOVALSystem;
import org.joval.util.Version;

/**
 * Base class for IFile-based IAdapters. Subclasses need only implement getObjectClass, createFileItem, convertFilename
 * and getItems methods.  The base class handles searches and caching of search results.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class BaseFileAdapter implements IAdapter {
    protected IFilesystem fs;
    protected Hashtable<String, List<String>> pathMap;

    protected BaseFileAdapter(IFilesystem fs) {
	this.fs = fs;
	pathMap = new Hashtable<String, List<String>>();
    }

    // Implement IAdapter

    public boolean connect() {
	return true;
    }

    public void disconnect() {
    }

    public List<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws OvalException {
	ObjectType obj = rc.getObject();
	String id = obj.getId();
	if (!obj.getClass().getName().equals(getObjectClass().getName())) {
	    throw new OvalException(JOVALSystem.getMessage("ERROR_INSTANCE",
							   getObjectClass().getName(), obj.getClass().getName()));
	}
	List<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	for (String path : getPathList(rc)) {
	    IFile f = null;
	    try {
		ReflectedFileObject fObj = new ReflectedFileObject(obj);
		ReflectedFileItem fItem = new ReflectedFileItem();

		f = fs.getFile(path);
		if (!f.exists()) {
		    throw new NoSuchElementException(path);
		}

		String dirPath = null;
		boolean isDirectory = f.isDirectory();
		if (isDirectory) {
		    dirPath = path;
		} else {
		    dirPath = path.substring(0, path.lastIndexOf(fs.getDelimString()));
		}

		if (fObj.isSetFilepath()) {
		    if (isDirectory) {
			//
			// Object is looking for files, so skip over this directory
			//
			continue;
		    } else {
			EntityItemStringType filepathType = JOVALSystem.factories.sc.core.createEntityItemStringType();
			filepathType.setValue(path);
			EntityItemStringType pathType = JOVALSystem.factories.sc.core.createEntityItemStringType();
			pathType.setValue(dirPath);
			EntityItemStringType filenameType = JOVALSystem.factories.sc.core.createEntityItemStringType();
			filenameType.setValue(path.substring(path.lastIndexOf(fs.getDelimString())+1));
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
			EntityItemStringType filepathType = JOVALSystem.factories.sc.core.createEntityItemStringType();
			filepathType.setValue(path);
			EntityItemStringType pathType = JOVALSystem.factories.sc.core.createEntityItemStringType();
			pathType.setValue(dirPath);
			EntityItemStringType filenameType = JOVALSystem.factories.sc.core.createEntityItemStringType();
			filenameType.setValue(path.substring(path.lastIndexOf(fs.getDelimString())+1));
			fItem.setFilepath(filepathType);
			fItem.setPath(pathType);
			fItem.setFilename(filenameType);
		    }
		} else if (fObj.isSetPath()) {
		    EntityItemStringType pathType = JOVALSystem.factories.sc.core.createEntityItemStringType();
		    pathType.setValue(dirPath);
		    fItem.setPath(pathType);
		    if (!isDirectory) {
			EntityItemStringType filenameType = JOVALSystem.factories.sc.core.createEntityItemStringType();
			filenameType.setValue(path.substring(path.lastIndexOf(fs.getDelimString())+1));
			fItem.setFilename(filenameType);
			EntityItemStringType filepathType = JOVALSystem.factories.sc.core.createEntityItemStringType();
			filepathType.setValue(path);
			fItem.setFilepath(filepathType);
		    }
		} else {
		    throw new OvalException(JOVALSystem.getMessage("ERROR_TEXTFILECONTENT_SPEC", id));
		}

		items.addAll(getItems(fItem.it, f, rc));
	    } catch (NoSuchElementException e) {
		// skip it
	    } catch (NoSuchMethodException e) {
		JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_FILEOBJECT_ITEMS", id, path), e);
	    } catch (IllegalAccessException e) {
		JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_FILEOBJECT_ITEMS", id, path), e);
	    } catch (IllegalArgumentException e) {
		JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_FILEOBJECT_ITEMS", id, path), e);
	    } catch (InvocationTargetException e) {
		JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_FILEOBJECT_ITEMS", id, path), e);
	    } catch (IOException e) {
		MessageType msg = JOVALSystem.factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		if (f == null) {
		    msg.setValue(e.getMessage());
		} else {
		    msg.setValue(JOVALSystem.getMessage("ERROR_IO", f.getLocalName(), e.getMessage()));
		}
		rc.addMessage(msg);
	    } finally {
		if (f != null) {
		    try {
			f.close();
		    } catch (IOException e) {
			JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_FILE_CLOSE", e.getMessage()));
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
	getItems(ItemType it, IFile f, IRequestContext rc) throws IOException, OvalException;

    // Internal

    /**
     * Get a list of String paths for this object.  This accommodates searches (from pattern match operations),
     * singletons (from equals operations) and handles recursive searches specified by FileBehaviors.
     */
    final List<String> getPathList(IRequestContext rc) throws OvalException {
	ObjectType obj = rc.getObject();
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
		    filepaths.addAll(rc.resolve(filepath.getVarRef()));
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
		    paths.addAll(rc.resolve(path.getVarRef()));
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
		    list = getPaths(list, fb.getDepth(), fb.getRecurseDirection(), fb.getRecurse());
		} else if (patternMatch) {
		    //
		    // Wildcard pattern matches are really supposed to be recursive searches, unfortunately
		    //
		    List<String> newList = new Vector<String>();
		    for (String value : list) {
			if (((String)path.getValue()).indexOf(".*") != -1 ||
			    ((String)path.getValue()).indexOf(".+") != -1) {
			    List<String> l = new Vector<String>();
			    l.add(value);
			    newList.addAll(getPaths(l, -1, "down", "directories"));
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
			    fnames.addAll(rc.resolve(filename.getVarRef()));
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

				  case NOT_EQUAL: {
				    IFile f = fs.getFile(pathString);
				    if (f.exists() && f.isDirectory()) {
					String[] children = f.list();
					f.close();
					for (int i=0; i < children.length; i++) {
					    if (!fname.equals(children[i])) {
						if (pathString.endsWith(fs.getDelimString())) {
						    files.add(pathString + fname);
						} else {
						    files.add(pathString + fs.getDelimString() + fname);
						}
					    }
					}
				    }
				    break;
				  }
 
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
		throw new OvalException("ERROR_BAD_FILEOBJECT" + obj.getId());
	    }
	} catch (NoSuchMethodException e) {
       	    JOVALSystem.getLogger().log(Level.SEVERE, e.getMessage(), e);
	} catch (IllegalAccessException e) {
       	    JOVALSystem.getLogger().log(Level.SEVERE, e.getMessage(), e);
	} catch (InvocationTargetException e) {
       	    JOVALSystem.getLogger().log(Level.SEVERE, e.getMessage(), e);
	} catch (IOException e) {
       	    JOVALSystem.getLogger().log(Level.WARNING, e.getMessage(), e);
	} catch (NoSuchElementException e) {
       	    JOVALSystem.getLogger().log(Level.FINER, JOVALSystem.getMessage("STATUS_NOT_FOUND", e.getMessage(), obj.getId()));
	}
	pathMap.put(obj.getId(), list);
	return list;
    }

    /**
     * Crawls recursively based on FileBehaviors.
     */
    private List<String> getPaths(List<String> list, int depth, String recurseDirection, String recurse) {
	if ("none".equals(recurseDirection) || depth == 0) {
	    return list;
	} else {
	    List<String> results = new Vector<String>();
	    for (String path : list) {
		JOVALSystem.getLogger().log(Level.FINE, JOVALSystem.getMessage("STATUS_FS_RECURSE", path));
		if (!path.endsWith(fs.getDelimString())) {
		    path += fs.getDelimString();
		}
		IFile f = null;
		try {
		    f = fs.getFile(path);
		    if (f.exists()) {
			if (recurse.indexOf("symlinks") == -1 && f.isLink()) {
System.out.println("DAS: skipping symlink " + path);
			    // skip the symlink
			} else if (recurse.indexOf("directories") == -1 && f.isDirectory()) {
System.out.println("DAS: skipping directory " + path);
			    // skip the directory
			} else {
System.out.println("DAS: recursing directory " + path);
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
				    results.addAll(getPaths(v, --depth, recurseDirection, recurse));
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
				    results.addAll(getPaths(v, --depth, recurseDirection, recurse));
				}
			    }
			}
		    }
		} catch (IOException e) {
		    if (f == null) {
			JOVALSystem.getLogger().log(Level.WARNING, e.getMessage(), e);
		    } else {
			JOVALSystem.getLogger().log(Level.WARNING,
						    JOVALSystem.getMessage("ERROR_IO", f.getLocalName(), e.getMessage()));
		    }
		} finally {
		    if (f != null) {
			try {
			    f.close();
			} catch (IOException e) {
			    JOVALSystem.getLogger().log(Level.WARNING,
							JOVALSystem.getMessage("ERROR_FILE_CLOSE", e.getMessage()));
			}
		    }
		}
	    }
	    //
	    // Eliminate any duplicates, and remove any trailing slashes
	    //
	    List<String> deduped = new Vector<String>();
	    for (String s : results) {
		if (s.endsWith(fs.getDelimString())) {
		    s = s.substring(0, s.lastIndexOf(fs.getDelimString()));
		}
		if (!deduped.contains(s)) {
		    deduped.add(s);
		}
	    }
	    return deduped;
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
	String recurse = "symlinks and directories";
	String recurseFS = "all";

	ReflectedFileBehaviors(Object obj)
		throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    if (obj != null) {
		Method getMaxDepth = obj.getClass().getMethod("getMaxDepth");
		maxDepth = (BigInteger)getMaxDepth.invoke(obj);
		Method getRecurseDirection = obj.getClass().getMethod("getRecurseDirection");
		recurseDirection = (String)getRecurseDirection.invoke(obj);
		Method getRecurse = obj.getClass().getMethod("getRecurse");
		recurse = (String)getRecurse.invoke(obj);
		Method getRecurseFileSystem = obj.getClass().getMethod("getRecurseFileSystem");
		recurseFS = (String)getRecurseFileSystem.invoke(obj);
		if (!"all".equals(recurseFS)) {
		    JOVALSystem.getLogger().log(Level.SEVERE,
						JOVALSystem.getMessage("ERROR_UNSUPPORTED_BEHAVIOR", recurseFS));
		}
	    }
	    if ("none".equals(recurseDirection)) {
		maxDepth = BigInteger.ZERO;
	    }
	}

	String getRecurse() {
	    return recurse;
	}

	String getRecurseDirection() {
	    return recurseDirection;
	}

	int getDepth() {
	    return Integer.parseInt(maxDepth.toString());
	}

	String getRecurseFileSystem() {
	    return recurseFS;
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
