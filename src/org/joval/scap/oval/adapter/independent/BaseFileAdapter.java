// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.independent;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
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
import org.joval.intf.system.ISession;
import org.joval.intf.util.ISearchable;
import org.joval.intf.util.ISearchable.ICondition;
import org.joval.intf.unix.io.IUnixFilesystem;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;
import org.joval.util.Version;

/**
 * Base class for IFile-based IAdapters. Subclasses need only implement getItemClass and getItems
 * methods. The base class handles searches and caching of search results.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class BaseFileAdapter<T extends ItemType> implements IAdapter {
    protected static final int TYPE_EQUALITY		= ISearchable.TYPE_EQUALITY;
    protected static final int TYPE_PATTERN		= ISearchable.TYPE_PATTERN;
    protected static final int FIELD_PATH		= IFilesystem.FIELD_PATH;
    protected static final int FIELD_DIRNAME		= IFilesystem.FIELD_DIRNAME;
    protected static final int FIELD_BASENAME		= IFilesystem.FIELD_BASENAME;
    protected static final int FIELD_FILETYPE		= IFilesystem.FIELD_FILETYPE;
    protected static final int FIELD_FROM		= ISearchable.FIELD_FROM;
    protected static final int FIELD_DEPTH		= ISearchable.FIELD_DEPTH;

    private Pattern localFilter;

    protected ISession session;

    protected void init(ISession session) {
	this.session = session;
	if (session.getProperties().getBooleanProperty(IFilesystem.PROP_SEARCH_FOLLOW_LINKS)) {
	    searchFlags = ISearchable.FOLLOW_LINKS;
	} else {
	    searchFlags = ISearchable.NONE;
	}
    }

    // Implement IAdapter

    /**
     * Certain object types, like the macos.PlistObject and linux.Selinuxsecuritycontext, do not necessarily contain file
     * path data. Subclasses implementing adapters (based on this one) for such objects should therefore override this method
     * as necessary.
     */
    public Collection<T> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	String id = obj.getId();

	//
	// Get the appropriate IFilesystem
	//
	ReflectedFileObject fObj = new ReflectedFileObject(obj);
	ReflectedFileBehaviors behaviors = null;
	if (fObj.isSetBehaviors()) {
	    behaviors = fObj.getBehaviors();
	}
	IFilesystem fs = null;
	int winView = 0;
	if (session instanceof IWindowsSession) {
	    if (behaviors != null) {
		if ("32_bit".equals(behaviors.getWindowsView())) {
		    fs = ((IWindowsSession)session).getFilesystem(IWindowsSession.View._32BIT);
		    winView = 32;
		}
	    }
	    if (winView == 0 && ((IWindowsSession)session).supports(IWindowsSession.View._64BIT)) {
		winView = 64;
	    }
	}
	if (fs == null) {
	    fs = session.getFilesystem();
	}

	Collection<T> items = new Vector<T>();
	for (String path : getPathList(fObj, rc, fs)) {
	    IFile f = null;
	    try {
		f = fs.getFile(path);
		String dirPath = null;
		boolean isDirectory = f.isDirectory();
		if (isDirectory) {
		    dirPath = path;
		} else {
		    dirPath = f.getParent();
		}
		ReflectedFileItem fItem = new ReflectedFileItem();
		if (fObj.isSetFilepath()) {
		    if (isDirectory) {
			//
			// Object is looking for files, so skip over this directory
			//
			continue;
		    } else {
			EntityItemStringType filepathType = Factories.sc.core.createEntityItemStringType();
			filepathType.setValue(path);
			EntityItemStringType pathType = Factories.sc.core.createEntityItemStringType();
			pathType.setValue(getPath(path, f));
			EntityItemStringType filenameType = Factories.sc.core.createEntityItemStringType();
			filenameType.setValue(f.getName());
			fItem.setFilepath(filepathType);
			fItem.setPath(pathType);
			fItem.setFilename(filenameType);
		    }
		} else if (fObj.isSetFilename() && fObj.getFilename().getValue() != null) {
		    if (isDirectory) {
			//
			// Object is looking for files, so skip over this directory
			//
			continue;
		    } else {
			EntityItemStringType filepathType = Factories.sc.core.createEntityItemStringType();
			filepathType.setValue(path);
			EntityItemStringType pathType = Factories.sc.core.createEntityItemStringType();
			pathType.setValue(getPath(path, f));
			EntityItemStringType filenameType = Factories.sc.core.createEntityItemStringType();
			filenameType.setValue(f.getName());
			fItem.setFilepath(filepathType);
			fItem.setPath(pathType);
			fItem.setFilename(filenameType);
		    }
		} else if (fObj.isSetPath()) {
		    if (!isDirectory && fObj.isSetFilename() && fObj.getFilename().getValue() == null) {
			//
			// If xsi:nil is set for the filename element, we only want directories...
			//
			continue;
		    }

		    EntityItemStringType pathType = Factories.sc.core.createEntityItemStringType();
		    pathType.setValue(dirPath);
		    fItem.setPath(pathType);
		    if (!isDirectory) {
			EntityItemStringType filenameType = Factories.sc.core.createEntityItemStringType();
			filenameType.setValue(f.getName());
			fItem.setFilename(filenameType);
			EntityItemStringType filepathType = Factories.sc.core.createEntityItemStringType();
			filepathType.setValue(f.getPath());
			fItem.setFilepath(filepathType);
		    }
		} else {
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_FILE_SPEC, obj.getClass().getName(), id);
		    throw new CollectException(msg, FlagEnumeration.ERROR);
		}

		switch(winView) {
		  case 32:
		    fItem.setWindowsView("32_bit");
		    break;
		  case 64:
		    fItem.setWindowsView("64_bit");
		    break;
		}

		items.addAll(getItems(obj, fItem.it, f, rc));
	    } catch (FileNotFoundException e) {
		// skip it
	    } catch (ClassNotFoundException e) {
		session.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		session.getLogger().warn(JOVALMsg.ERROR_REFLECTION, e.getMessage(), id);
	    } catch (InstantiationException e) {
		session.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		session.getLogger().warn(JOVALMsg.ERROR_REFLECTION, e.getMessage(), id);
	    } catch (NoSuchMethodException e) {
		session.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		session.getLogger().warn(JOVALMsg.ERROR_REFLECTION, e.getMessage(), id);
	    } catch (IllegalAccessException e) {
		session.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		session.getLogger().warn(JOVALMsg.ERROR_REFLECTION, e.getMessage(), id);
	    } catch (InvocationTargetException e) {
		session.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		session.getLogger().warn(JOVALMsg.ERROR_REFLECTION, e.getMessage(), id);
	    } catch (IllegalArgumentException e) {
		session.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		session.getLogger().warn(JOVALMsg.ERROR_IO, path, e.getMessage());
	    } catch (IOException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		if (f == null) {
		    msg.setValue(e.getMessage());
		} else {
		    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_IO, path, e.getMessage()));
		}
		rc.addMessage(msg);
	    }
	}
	return items;
    }

    // Protected

    /**
     * Return the Class of the ItemTypes generated by the subclass.
     */
    protected abstract Class getItemClass();

    /**
     * Return a list of items to associate with the given ObjectType, based on information gathered from the IFile.
     *
     * @arg it the base ItemType containing filepath, path and filename information already populated
     */
    protected abstract Collection<T> getItems(ObjectType obj, ItemType it, IFile f, IRequestContext rc)
	throws IOException, CollectException;

    // Private

    /**
     * Get a list of String paths corresponding to this object's specifications. This accommodates searches (from pattern
     * match operations), singletons (from equals operations) and handles recursive searches specified by FileBehaviors.
     * The resulting collection only contains matches that exist.
     */
    private Collection<String> getPathList(ReflectedFileObject fObj, IRequestContext rc, IFilesystem fs)
		throws CollectException {

	Collection<IFile> files = new ArrayList<IFile>();
	try {
	    boolean search = false;
	    boolean local = false;
	    boolean followLinks = true; // follow links by default
	    String[] from = null;
	    ISearchable<IFile> searcher = fs.getSearcher();
	    List<ICondition> conditions = new ArrayList<ICondition>();

	    if (fObj.isSetFilepath() && fObj.getFilepath().getValue() != null) {
		//
		// Windows view is already handled, and FileBehaviors recursion is ignored in the Filepath case, so
		// all we need to do is add the discovered paths to the return list.
		//
		String filepath = (String)fObj.getFilepath().getValue();
		OperationEnumeration op = fObj.getFilepath().getOperation();
		switch(op) {
		  case EQUALS:
		    IFile file = fs.getFile(filepath);
		    if (file.exists()) {
			files.add(file);
		    }
		    break;

		  case PATTERN_MATCH: {
		    Pattern p = Pattern.compile(filepath);
		    from = fs.guessParent(p);
		    conditions.add(searcher.condition(FIELD_PATH, TYPE_PATTERN, p));
		    conditions.add(ISearchable.RECURSE);
		    search = true;
		    break;

		  default:
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		}
	    } else if (fObj.isSetPath() && fObj.getPath().getValue() != null) {
		String path = (String)fObj.getPath().getValue();
		String filename = null;
		if (fObj.isSetFilename() && fObj.getFilename().getValue() != null) {
		    filename = (String)fObj.getFilename().getValue();
		} else {
		    //
		    // If there is a search, we will only want directories.
		    //
		    conditions.add(IFilesystem.DIRECTORIES);
		}

		//
		// Convert behaviors into flags and search conditions.
		//
		int depth = ISearchable.DEPTH_UNLIMITED;
		if (fb != null && !"none".equals(fb.getRecurseDirection())) {
		    if (fb.getRecurse().indexOf("directories") == -1) {
			depth = 0;
		    } else {
			depth = fb.getDepth();
		    }
		    // Note - OVAL's idea of depth=1 is everyone else's idea of depth=2.
		    conditions.add(searcher.condition(FIELD_DEPTH, TYPE_EQUALITY, new Integer(depth > 0 ? depth+1 : depth)));
		    if (fb.getRecurse().indexOf("symlinks") == -1) {
			followLinks = false;
		    }
		    if ("defined".equals(fb.getRecurseFileSystem())) {
			conditions.add(IUnixFilesystem.XDEV);
		    } else if ("local".equals(fb.getRecurseFileSystem())) {
			local = true;
		    }
		}

		OperationEnumeration pathOp = fObj.getPath().getOperation();
		switch(pathOp) {
		  case EQUALS:
		    IFile file = fs.getFile(path);
		    if (file.isDirectory()) {
			if (fb == null) {
			    //
			    // Add candidate files to the list (for later filtering, below)
			    //
			    files.add(file);
			    if (filename != null) {
				files.addAll(Arrays.asList(file.listFiles()));
			    }
			} else {
			    if ("up".equals(fb.getRecurseDirection())) {
				for (int i=depth; i != 0; i--) {
				    String parentPath = file.getParent();
				    if (parentPath.equals(file.getName())) {
					// this means we've reached the top
					break;
				    } else {
					String lastFilename = file.getName();
					file = fs.getFile(parentPath);
					if (filename == null) {
					    files.add(file); // directories only
					} else {
					    for (String fname : file.list()) {
						if (!fname.equals(lastFilename)) {
						    files.add(fs.getFile(parentPath + fs.getDelimiter() + fname));
						}
					    }
					}
				    }
				}
			    } else {
				from = Arrays.asList(path).toArray(new String[1]);
				search = true;
			    }
			}
		    }
		    break;

		  case PATTERN_MATCH: {
		    Pattern p = Pattern.compile(path);
		    from = fs.guessParent(p);
		    conditions.add(searcher.condition(FIELD_DIRNAME, TYPE_PATTERN, p));
		    search = true;
		    break;

		  default:
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, pathOp);
		    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		}

		//
		// At this point, except for searches, the collection "files" will contain every IFile that matches
		// the path spec. So, if there is a filename spec, we use it to filter down the list.
		//
		if (filename != null) {
		    Iterator<IFile> iter = files.iterator();
		    OperationEnumeration filenameOp = fObj.getFilename().getOperation();
		    switch(filenameOp) {
		      case EQUALS:
			if (search) {
			    conditions.add(searcher.condition(FIELD_BASENAME, TYPE_EQUALITY, filename));
			} else {
			    while(iter.hasNext()) {
				IFile file = iter.next();
				if (!filename.equals(file.getName())) {
				    iter.remove();
				}
			    }
			}
			break;

		      case NOT_EQUAL:
			if (search) {
			    Pattern p = Pattern.compile("^{?!" + filename + ")$");
			    conditions.add(searcher.condition(FIELD_BASENAME, TYPE_PATTERN, p));
			} else {
			    while(iter.hasNext()) {
				IFile file = iter.next();
				if (filename.equals(file.getName())) {
				    iter.remove();
				}
			    }
			}
			break;

		      case PATTERN_MATCH: {
			Pattern p = Pattern.compile(filename);
			if (search) {
			    conditions.add(searcher.condition(FIELD_BASENAME, TYPE_PATTERN, p));
			} else {
			    while(iter.hasNext()) {
				IFile file = iter.next();
				if (!p.matcher(file.getName()).find()) {
				    iter.remove();
				}
			    }
			}
			break;
		      }

		      default:
			String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, filenameOp);
			throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		    }
		    list = files;
		}
	    } else {
		//
		// This has probably happened because one or more variables resolves to nothing.
		//
		session.getLogger().debug(JOVALMsg.getMessage(JOVALMsg.ERROR_BAD_FILE_OBJECT, fObj.getId()));
	    }

	    if (search) {
		if (followLinks) {
		    conditions.add(IUnixFilesystem.FOLLOW_LINKS);
		}
		if (from == null) {
		    Collection<IFilesystem.IMount> mounts = fs.getMounts(local ? localFilter : null);
		    from = new String[mounts.size()];
		    int i=0;
		    for (IFilesystem.IMount mount : mounts) {
			from[i++] = mount.getPath();
		    }
		}
		for (String s : from) {
		    List<ICondition> c = new ArrayList<ICondition>();
		    c.addAll(conditions);
		    c.add(searcher.condition(FIELD_FROM, TYPE_EQUALITY, s));
		    files.addAll(searcher.search(c));
		}
	    }
	} catch (FileNotFoundException e) {
	} catch (Exception e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(e.getMessage());
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return files;
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
	String id = null;
	boolean filenameNil = false;
	EntityObjectStringType filepath = null, path = null, filename = null;
	ReflectedFileBehaviors behaviors = null;

	ReflectedFileObject(ObjectType obj) {
	    this.obj = obj;

	    try {
		Method getId = obj.getClass().getMethod("getId");
		Object o = getId.invoke(obj);
		if (o != null) {
		    id = (String)o;
		}
	    } catch (NoSuchMethodException e) {
	    } catch (IllegalAccessException e) {
	    } catch (IllegalArgumentException e) {
	    } catch (InvocationTargetException e) {
	    }

	    try {
		Method getFilepath = obj.getClass().getMethod("getFilepath");
		Object o = getFilepath.invoke(obj);
		if (o != null) {
		    filepath = (EntityObjectStringType)o;
		}
	    } catch (NoSuchMethodException e) {
	    } catch (IllegalAccessException e) {
	    } catch (IllegalArgumentException e) {
	    } catch (InvocationTargetException e) {
	    }

	    try {
		Method getFilename = obj.getClass().getMethod("getFilename");
		Object o = getFilename.invoke(obj);
		if (o != null) {
		    if (o instanceof JAXBElement) {
			JAXBElement j = (JAXBElement)o;
			filenameNil = j.isNil();
			o = j.getValue();
		    }
		    filename = (EntityObjectStringType)o;
		}
	    } catch (NoSuchMethodException e) {
	    } catch (IllegalAccessException e) {
	    } catch (IllegalArgumentException e) {
	    } catch (InvocationTargetException e) {
	    }

	    try {
		Method getPath = obj.getClass().getMethod("getPath");
		Object o = getPath.invoke(obj);
		if (o != null) {
		    path = (EntityObjectStringType)o;
		}
	    } catch (NoSuchMethodException e) {
	    } catch (IllegalAccessException e) {
	    } catch (IllegalArgumentException e) {
	    } catch (InvocationTargetException e) {
	    }

	    try {
		Method getBehaviors = obj.getClass().getMethod("getBehaviors");
		Object o = getBehaviors.invoke(obj);
		if (o != null) {
		    behaviors = new ReflectedFileBehaviors(o);
		}
	    } catch (NoSuchMethodException e) {
	    } catch (IllegalAccessException e) {
	    } catch (IllegalArgumentException e) {
	    } catch (InvocationTargetException e) {
	    }
	}

	public ObjectType getObject() {
	    return obj;
	}

	public String getId() {
	    return id;
	}

	public boolean isSetFilepath() {
	    return filepath != null;
	}

	public EntityObjectStringType getFilepath() {
	    return filepath;
	}

	public boolean isFilenameNil() {
	    return filenameNil;
	}

	public boolean isSetFilename() {
	    return filename != null;
	}

	public EntityObjectStringType getFilename() {
	    return filename;
	}

	public boolean isSetPath() {
	    return path != null;
	}

	public EntityObjectStringType getPath() {
	    return path;
	}

	public boolean isSetBehaviors() {
	    return behaviors != null;
	}

	public ReflectedFileBehaviors getBehaviors() {
	    return behaviors;
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
	String windowsView = "64_bit";

	ReflectedFileBehaviors(Object obj)
		throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    if (obj != null) {
		Method getMaxDepth = obj.getClass().getMethod("getMaxDepth");
		maxDepth = (BigInteger)getMaxDepth.invoke(obj);

		Method getRecurseDirection = obj.getClass().getMethod("getRecurseDirection");
		recurseDirection = (String)getRecurseDirection.invoke(obj);

		try {
		    //
		    // Not applicable to Unix FileBehaviors
		    //
		    Method getWindowsView = obj.getClass().getMethod("getWindowsView");
		    windowsView = (String)getWindowsView.invoke(obj);
		} catch (NoSuchMethodException e) {
		    recurse = null;
		}

		try {
		    //
		    // Not applicable to Windows FileBehaviors
		    //
		    Method getRecurse = obj.getClass().getMethod("getRecurse");
		    recurse = (String)getRecurse.invoke(obj);
		} catch (NoSuchMethodException e) {
		    recurse = null;
		}

		Method getRecurseFileSystem = obj.getClass().getMethod("getRecurseFileSystem");
		recurseFS = (String)getRecurseFileSystem.invoke(obj);
	    }
	    if ("none".equals(recurseDirection)) {
		maxDepth = BigInteger.ZERO;
	    }
	}

	public String getRecurse() {
	    return recurse;
	}

	public String getRecurseDirection() {
	    return recurseDirection;
	}

	public int getDepth() {
	    return Integer.parseInt(maxDepth.toString());
	}

	public String getRecurseFileSystem() {
	    return recurseFS;
	}

	public String getWindowsView() {
	    return windowsView;
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
	Method setFilepath, setFilename, setPath, setStatus, setWindowsView, wrapFilename=null;
	Object factory;

	ReflectedFileItem() throws ClassNotFoundException, InstantiationException, NoSuchMethodException,
		IllegalAccessException, InvocationTargetException {

	    Class clazz = getItemClass();
	    String className = clazz.getName();
	    String packageName = clazz.getPackage().getName();
	    String unqualClassName = className.substring(packageName.length() + 1);
	    Class<?> factoryClass = Class.forName(packageName + ".ObjectFactory");
	    factory = factoryClass.newInstance();
	    Method createType = factoryClass.getMethod("create" + unqualClassName);
	    it = (ItemType)createType.invoke(factory);

	    Method[] methods = it.getClass().getMethods();
	    for (int i=0; i < methods.length; i++) {
		String name = methods[i].getName();
		if ("setFilepath".equals(name)) {
		    setFilepath = methods[i];
		} else if ("setFilename".equals(name)) {
		    setFilename = methods[i];
		    String filenameClassName = setFilename.getParameterTypes()[0].getName();
		    if (filenameClassName.equals(JAXBElement.class.getName())) {
			String methodName = "create" + unqualClassName + "Filename";
			wrapFilename = factoryClass.getMethod(methodName, EntityItemStringType.class);
		    }
		} else if ("setPath".equals(name)) {
		    setPath = methods[i];
		} else if ("setStatus".equals(name)) {
		    setStatus = methods[i];
		} else if ("setWindowsView".equals(name)) {
		    setWindowsView = methods[i];
		}
	    }
	}

	void setWindowsView(String view) {
	    try {
		if (setWindowsView != null) {
		    Class[] types = setWindowsView.getParameterTypes();
		    if (types.length == 1) {
			Class type = types[0];
			Object instance = Class.forName(type.getName());
			@SuppressWarnings("unchecked")
			Method setValue = type.getMethod("setValue", Object.class);
			setValue.invoke(instance, view);
			setWindowsView.invoke(it, instance);
		    }
		}
	    } catch (NoSuchMethodException e) {
	    } catch (IllegalAccessException e) {
	    } catch (IllegalArgumentException e) {
	    } catch (InvocationTargetException e) {
	    } catch (ClassNotFoundException e) {
	    }
	}

	void setFilename(EntityItemStringType filename)
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    if (setFilename != null) {
		if (wrapFilename == null) {
		    setFilename.invoke(it, filename);
		} else {
		    setFilename.invoke(it, wrapFilename.invoke(factory, filename));
		}
	    }
	}

	void setFilepath(EntityItemStringType filepath)
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    if (setFilepath != null) {
		setFilepath.invoke(it, filepath);
	    }
	}

	void setPath(EntityItemStringType path)
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    if (setPath != null) {
		setPath.invoke(it, path);
	    }
	}

	void setStatus(StatusEnumeration status)
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    if (setStatus != null) {
		setStatus.invoke(it, status);
	    }
	}
    }
}
