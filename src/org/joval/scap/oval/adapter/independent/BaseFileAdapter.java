// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.independent;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import jsaf.Message;
import jsaf.intf.io.IFile;
import jsaf.intf.io.IFilesystem;
import jsaf.intf.system.ISession;
import jsaf.intf.util.ISearchable;
import jsaf.intf.util.ISearchable.ICondition;
import jsaf.intf.unix.io.IUnixFilesystem;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.intf.windows.io.IWindowsFilesystem;
import jsaf.intf.windows.powershell.IRunspace;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.util.SafeCLI;

import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.MessageType;
import scap.oval.common.OperationEnumeration;
import scap.oval.definitions.core.EntityObjectStringType;
import scap.oval.definitions.core.ObjectType;
import scap.oval.systemcharacteristics.core.EntityItemAnySimpleType;
import scap.oval.systemcharacteristics.core.EntityItemIntType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.scap.oval.IBatch;
import org.joval.scap.oval.Batch;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;
import org.joval.xml.XSITools;

/**
 * Base class for IFile-based IAdapters. Subclasses need only implement getItemClass and getItems
 * methods. The base class handles searches and caching of search results.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class BaseFileAdapter<T extends ItemType> implements IAdapter, IBatch {
    protected static final int TYPE_EQUALITY		= ISearchable.TYPE_EQUALITY;
    protected static final int TYPE_INEQUALITY		= ISearchable.TYPE_INEQUALITY;
    protected static final int TYPE_PATTERN		= ISearchable.TYPE_PATTERN;
    protected static final int FIELD_PATH		= IFilesystem.FIELD_PATH;
    protected static final int FIELD_DIRNAME		= IFilesystem.FIELD_DIRNAME;
    protected static final int FIELD_BASENAME		= IFilesystem.FIELD_BASENAME;
    protected static final int FIELD_FILETYPE		= IFilesystem.FIELD_FILETYPE;
    protected static final int FIELD_FSTYPE		= IFilesystem.FIELD_FSTYPE;
    protected static final int FIELD_FROM		= ISearchable.FIELD_FROM;
    protected static final int FIELD_DEPTH		= ISearchable.FIELD_DEPTH;

    private IRunspace rs, rs32;
    private String localFsType;

    protected ISession session;

    /**
     * All subclasses should invoke this method inside their IAdapter.init implementations.
     *
     * @throws UnsupportedOperationException if the ISession doesn't support getFilesystem().
     */
    protected void baseInit(ISession session) throws UnsupportedOperationException {
	this.session = session;
	switch(session.getType()) {
	  case WINDOWS:
	    localFsType = IWindowsFilesystem.FsType.FIXED.value();
	    break;

	  case UNIX:
	    try {
		for (IFilesystem.IMount mount : session.getFilesystem().getMounts()) {
		    if ("/".equals(mount.getPath())) {
			localFsType = mount.getType();
			break;
		    }
		}
	    } catch (IOException e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    break;

	  default:
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_SESSION_TYPE, session.getType());
	    throw new UnsupportedOperationException(msg);
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

	ReflectedFileObject fObj = new ReflectedFileObject(obj);
	ReflectedFileBehaviors behaviors = null;
	if (fObj.isSetBehaviors()) {
	    behaviors = fObj.getBehaviors();
	}
	IFilesystem fs = session.getFilesystem();
	IWindowsSession.View view = getView(behaviors);

	//
	// On Windows, make sure we get the appropriate IFilesystem
	//
	if (session instanceof IWindowsSession) {
	    IWindowsSession ws = (IWindowsSession)session;
	    if (ws.supports(view)) {
		fs = ws.getFilesystem(view);
	    } else {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.INFO);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.WARNING_WINDOWS_VIEW, view.toString()));
		rc.addMessage(msg);

		@SuppressWarnings("unchecked")
		List<T> empty = (List<T>)Collections.EMPTY_LIST;
		return empty;
	    }
	}
	Collection<IFile> files = getFiles(fObj, behaviors, rc, fs);

	//
	// Subclasses will be taking these files and calling getBaseItem to synthesize ItemType objects with
	// them, and getItemType checks to see whether the file is a directory.  If the file is a link, this
	// means there will be a stat call on the link target to determine what kind of file it is.  If there
	// are a lot of links, then there will be a lot of these calls.  Hence, to improve performance (on
	// Unix), we resolve all the 1st-level link targets in a single pass now.
	//
	if (session instanceof IUnixSession) {
	    try {
		ArrayList<String> links = new ArrayList<String>();
		for (IFile f : files) {
		    if (f.isLink()) {
			links.add(f.getLinkPath());
		    }
		}
		if (links.size() > 1) { // if == 1, just let the stat call happen later on
		    fs.getFiles(links.toArray(new String[links.size()]));
		}
	    } catch (IOException e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
	return getItems(obj, files, rc);
    }

    // Implement IBatch

    private List<IRequest> queue;

    public boolean queue(IRequest request) {
	if (batchable(request)) {
	    if (queue == null) {
		queue = new ArrayList<IRequest>();
	    }
	    queue.add(request);
	    return true;
	} else {
	    return false;
	}
    }

    public Collection<IResult> exec() {
	//
	// Create a list of paths associated with each queued object.
	//
	ArrayList<String> paths = new ArrayList<String>(queue.size());
	for (IRequest request : queue) {
	    ReflectedFileObject fObj = new ReflectedFileObject(request.getObject());
	    if (fObj.isSetFilepath()) {
		paths.add((String)fObj.getFilepath().getValue());
	    } else {
		StringBuffer sb = new StringBuffer((String)fObj.getPath().getValue());
		if (!sb.toString().endsWith(session.getFilesystem().getDelimiter())) {
		    sb.append(session.getFilesystem().getDelimiter());
		}
		sb.append((String)fObj.getFilename().getValue());
		paths.add(sb.toString());
	    }
	}

	IWindowsSession.View view = null;
	if (session instanceof IWindowsSession) {
	    view = ((IWindowsSession)session).getNativeView();
	}
	Collection<IResult> results = new ArrayList<IResult>();
	try {
	    //
	    // For each path (and therefore object), process the file and add the corresponding result.
	    //
	    IFile[] files = session.getFilesystem().getFiles(paths.toArray(new String[paths.size()]));
	    for (int i=0; i < paths.size(); i++) {
		IRequestContext rc = queue.get(i).getContext();
		ObjectType obj = queue.get(i).getObject();
		ReflectedFileObject fObj = new ReflectedFileObject(obj);
		IFile f = files[i];
		if (f == null) {
		    results.add(new Batch.Result(new ArrayList<T>(), rc));
		} else {
		    try {
			results.add(new Batch.Result(getItems(obj, Arrays.asList(f), rc), rc));
		    } catch (CollectException e) {
			results.add(new Batch.Result(e, rc));
		    } catch (Exception e) {
			session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
			results.add(new Batch.Result(new CollectException(e, FlagEnumeration.ERROR), rc));
		    }
		}
	    }
	} catch (IOException e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    for (IRequest request : queue) {
		IRequestContext rc = request.getContext();
		results.add(new Batch.Result(new CollectException(e, FlagEnumeration.ERROR), rc));
	    }
	}
	queue = null;
	return results;
    }

    // Protected

    /**
     * Return the Class of the ItemTypes generated by the subclass.
     */
    protected abstract Class getItemClass();

    /**
     * Subclasses can override this method to apply additional search conditions. This can make searches for filtered
     * objects vastly more efficient.
     */
    protected List<ISearchable.ICondition> getConditions(ObjectType obj) throws PatternSyntaxException, CollectException {
	@SuppressWarnings("unchecked")
	List<ISearchable.ICondition> empty = (List<ISearchable.ICondition>)Collections.EMPTY_LIST;
	return empty;
    }

    /**
     * Return a list of items to associate with the given ObjectType, based on information gathered from the given IFiles.
     */
    protected abstract Collection<T> getItems(ObjectType obj, Collection<IFile> files, IRequestContext rc)
	throws CollectException;

    /**
     * Windows-specific object type subclasses should override by supplying streams to any Powershell assemblies that must be
     * loaded into requested runspaces (i.e., for modules) using the getRunspace convenience method, below.
     */
    protected List<InputStream> getPowershellAssemblies() {
	@SuppressWarnings("unchecked")
	List<InputStream> empty = (List<InputStream>)Collections.EMPTY_LIST;
	return empty;
    }

    /**
     * Windows-specific object type subclasses should override by supplying streams to any Powershell modules that must be
     * loaded into requested runspaces using the getRunspace convenience method, below.
     */
    protected List<InputStream> getPowershellModules() {
	@SuppressWarnings("unchecked")
	List<InputStream> empty = (List<InputStream>)Collections.EMPTY_LIST;
	return empty;
    }

    /**
     * Get a runspace with the specified view, or create it if there isn't one yet.
     */
    protected IRunspace getRunspace(IWindowsSession.View view) throws Exception {
	switch(view) {
	  case _32BIT:
	    if (rs32 == null) {
		rs32 = createRunspace(view);
	    }
	    return rs32;

	  default:
	    if (rs == null) {
		rs = createRunspace(view);
	    }
	    return rs;
	}
    }

    /**
     * Return an ItemType of the appropriate sub-type, with the path, filepath and filename entities populated according
     * to the specified IFile.
     *
     * @return null if there is no such file or directory
     */
    protected ItemType getBaseItem(ObjectType obj, IFile f) throws IOException {
	String id = obj.getId();
	String path = f.getPath();
	try {
	    ReflectedFileObject fObj = new ReflectedFileObject(obj);
	    String dirPath = null;
	    boolean isDirectory = f.isDirectory();
	    if (isDirectory) {
		dirPath = path;
	    } else {
		dirPath = f.getParent();
	    }
	    ReflectedFileItem fItem = new ReflectedFileItem();
	    if (session instanceof IWindowsSession) {
		ReflectedFileBehaviors behaviors = fObj.getBehaviors();
		if (behaviors == null) {
		    fItem.setWindowsView(((IWindowsSession)session).getNativeView());
		} else {
		    fItem.setWindowsView(behaviors.getWindowsView());
		}
	    }
	    if (isDirectory) {
		if (fObj.isFilenameNil()) {
		    EntityItemStringType pathType = Factories.sc.core.createEntityItemStringType();
		    pathType.setValue(dirPath);
		    fItem.setPath(pathType);
		    return fItem.it;
		}
	    } else {
		EntityItemStringType filepathType = Factories.sc.core.createEntityItemStringType();
		filepathType.setValue(path);
		fItem.setFilepath(filepathType);
		EntityItemStringType pathType = Factories.sc.core.createEntityItemStringType();
		pathType.setValue(f.getParent());
		fItem.setPath(pathType);
		EntityItemStringType filenameType = Factories.sc.core.createEntityItemStringType();
		filenameType.setValue(f.getName());
		fItem.setFilename(filenameType);
		return fItem.it;
	    }
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
	    session.getLogger().warn(Message.ERROR_IO, path, e.getMessage());
	}
	return null;
    }

    protected IWindowsSession.View getView(scap.oval.definitions.independent.FileBehaviors behaviors) {
	if (session instanceof IWindowsSession) {
	    if (behaviors != null && behaviors.isSetWindowsView()) {
		String s = behaviors.getWindowsView();
		if ("32_bit".equals(s)) {
		    return IWindowsSession.View._32BIT;
		} else if ("64_bit".equals(s)) {
		    return IWindowsSession.View._64BIT;
		}
	    }
	    return ((IWindowsSession)session).getNativeView();
	} else {
	    return null;
	}
    }

    protected IWindowsSession.View getView(scap.oval.definitions.windows.FileBehaviors behaviors) {
	if (session instanceof IWindowsSession) {
	    if (behaviors != null && behaviors.isSetWindowsView()) {
		String s = behaviors.getWindowsView();
		if ("32_bit".equals(s)) {
		    return IWindowsSession.View._32BIT;
		} else if ("64_bit".equals(s)) {
		    return IWindowsSession.View._64BIT;
		}
	    }
	    return ((IWindowsSession)session).getNativeView();
	} else {
	    return null;
	}
    }

    // Private

    private IWindowsSession.View getView(ReflectedFileBehaviors behaviors) {
	if (session instanceof IWindowsSession) {
	    if (behaviors != null) {
		String s = behaviors.getWindowsView();
		if ("32_bit".equals(s)) {
		    return IWindowsSession.View._32BIT;
		} else if ("64_bit".equals(s)) {
		    return IWindowsSession.View._64BIT;
		}
	    }
	    return ((IWindowsSession)session).getNativeView();
	} else {
	    return null;
	}
    }

    /**
     * Determine whether or not a request can be batched. Only requests about individual regular files are batchable.
     */
    private boolean batchable(IRequest request) {
	ReflectedFileObject fObj = new ReflectedFileObject(request.getObject());
	return  (
		    fObj.isSetFilepath() &&
		    fObj.getFilepath().getOperation() == OperationEnumeration.EQUALS
		)
		||
		(
		    !fObj.isSetBehaviors() &&
		    fObj.isSetFilename() &&
		    !fObj.isFilenameNil() &&
		    fObj.getFilename().getOperation() == OperationEnumeration.EQUALS &&
		    fObj.getPath().getOperation() == OperationEnumeration.EQUALS
		);
    }

    /**
     * Create a runspace with the specified view. Modules supplied by getPowershellModules() will be auto-loaded before
     * the runspace is returned.  Returns null for non-Windows sessions.
     */
    private IRunspace createRunspace(IWindowsSession.View view) throws Exception {
	IRunspace result = null;
	if (session instanceof IWindowsSession) {
	    IWindowsSession ws = (IWindowsSession)session;
	    for (IRunspace runspace : ws.getRunspacePool().enumerate()) {
		if (view == runspace.getView()) {
		    result = runspace;
		    break;
		} 
	    }
	    if (result == null) {
		result = ws.getRunspacePool().spawn(view);
	    }
	    for (InputStream in : getPowershellAssemblies()) {
		result.loadAssembly(in);
	    }
	    for (InputStream in : getPowershellModules()) {
		result.loadModule(in);
	    }
	}
	return result;
    }

    /**
     * Get all the IFiles corresponding to this object's specifications. This accommodates searches (from pattern
     * match operations), singletons (from equals operations) and handles recursive crawling specified by FileBehaviors.
     * The resulting collection only contains matches that exist.
     */
    private Collection<IFile> getFiles(ReflectedFileObject fObj, ReflectedFileBehaviors fb, IRequestContext rc,
		IFilesystem fs) throws CollectException {

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

		  case CASE_INSENSITIVE_EQUALS: {
		    Pattern p = Pattern.compile("^(?i)" + Matcher.quoteReplacement(filepath) + "$");
		    from = searcher.guessParent(p, Boolean.TRUE);
		    conditions.add(searcher.condition(FIELD_PATH, TYPE_PATTERN, p));
		    conditions.add(ISearchable.RECURSE);
		    search = true;
		    break;
		  }

		  case PATTERN_MATCH: {
		    Pattern p = Pattern.compile(filepath);
		    from = searcher.guessParent(p, Boolean.TRUE);
		    conditions.add(searcher.condition(FIELD_PATH, TYPE_PATTERN, p));
		    conditions.add(ISearchable.RECURSE);
		    search = true;
		    break;
		  }

		  default:
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		}
	    } else if (fObj.isSetPath() && fObj.getPath().getValue() != null) {
		String path = (String)fObj.getPath().getValue();
		String filename = null;
		if (fObj.isFilenameNil()) {
		    // If there is a search, we will only want directories.
		    conditions.add(IFilesystem.DIRECTORIES);
		} else {
		    filename = (String)fObj.getFilename().getValue();
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
		    //
		    // In OVAL, depth applies only to directories. So when looking for regular files, it is necessary to
		    // add one to any non-zero depth, in order to reach the files, using the maxdepth concept of GNU find.
		    //
		    if (filename == null) {
			conditions.add(searcher.condition(FIELD_DEPTH, TYPE_EQUALITY, new Integer(depth)));
		    } else {
			Integer effectiveDepth = new Integer(depth > 0 ? depth+1 : depth);
			conditions.add(searcher.condition(FIELD_DEPTH, TYPE_EQUALITY, effectiveDepth));
		    }
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
		    if (file.exists() && file.isDirectory()) {
			if (fb == null) {
			    //
			    // Add candidate dirs, which will be handled below
			    //
			    files.add(file);
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
					files.add(file);
				    }
				}
			    } else if ("down".equals(fb.getRecurseDirection())) {
				from = new String[] {path};
				search = true;
			    } else {
				files.add(file);
			    }
			}
		    }
		    break;

		  case CASE_INSENSITIVE_EQUALS: {
		    Pattern p = Pattern.compile("^(?i)" + Matcher.quoteReplacement(path) + "$");
		    from = searcher.guessParent(p);
		    conditions.add(searcher.condition(FIELD_DIRNAME, TYPE_PATTERN, p));
		    search = true;
		    break;
		  }

		  case PATTERN_MATCH: {
		    Pattern p = Pattern.compile(path);
		    from = searcher.guessParent(p);
		    conditions.add(searcher.condition(FIELD_DIRNAME, TYPE_PATTERN, p));
		    search = true;
		    break;
		  }

		  default:
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, pathOp);
		    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		}

		//
		// At this point, except for searches, the collection "files" will contain every directory IFile
		// that matches the path spec. So, if there is a filename spec, we replace it with matches, or convert
		// to a search.
		//
		if (filename != null) {
		    Iterator<IFile> iter = files.iterator();
		    OperationEnumeration filenameOp = fObj.getFilename().getOperation();
		    switch(filenameOp) {
		      case EQUALS:
			if (search) {
			    SafeCLI.checkArgument(filename, session);
			    conditions.add(searcher.condition(FIELD_BASENAME, TYPE_EQUALITY, filename));
			} else {
			    Collection<IFile> results = new ArrayList<IFile>();
			    while(iter.hasNext()) {
				IFile dir = iter.next();
				IFile file = dir.getChild(filename);
				if (file.exists()) {
				    results.add(file);
				}
			    }
			    files = results;
			}
			break;

		      case NOT_EQUAL:
		      case CASE_INSENSITIVE_EQUALS:
		      case PATTERN_MATCH: {
			if (!search) {
			    //
			    // Convert to a depth=1 search of basenames from the matching directories found above
			    //
			    Collection<String> paths = new ArrayList<String>();
			    while(iter.hasNext()) {
				paths.add(iter.next().getPath());
			    }
			    files = new ArrayList<IFile>();
			    search = true;
			    from = paths.toArray(new String[paths.size()]);
			    conditions.add(searcher.condition(FIELD_DEPTH, TYPE_EQUALITY, new Integer(1)));
			}
			if (filenameOp == OperationEnumeration.CASE_INSENSITIVE_EQUALS) {
			    Pattern p = Pattern.compile("^(?i)" + Matcher.quoteReplacement(filename) + "$");
			    conditions.add(searcher.condition(FIELD_BASENAME, TYPE_PATTERN, p));
			} else if (filenameOp == OperationEnumeration.PATTERN_MATCH) {
			    Pattern p = Pattern.compile(filename);
			    conditions.add(searcher.condition(FIELD_BASENAME, TYPE_PATTERN, p));
			} else {
			    SafeCLI.checkArgument(filename, session);
			    conditions.add(searcher.condition(FIELD_BASENAME, TYPE_INEQUALITY, filename));
			}
			break;
		      }

		      default:
			String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, filenameOp);
			throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		    }
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
		if (local) {
		    conditions.add(searcher.condition(FIELD_FSTYPE, TYPE_EQUALITY, localFsType));
		}
		conditions.addAll(getConditions(fObj.getObject()));
		if (from == null) {
		    Collection<IFilesystem.IMount> mounts = null;
		    if (local) {
			Pattern p = Pattern.compile(new StringBuffer("^").append(localFsType).append("$").toString());
			mounts = fs.getMounts(p, true);
		    } else {
			mounts = fs.getMounts();
		    }
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
     *     scap.oval.definitions.independent.Textfilecontent54Object
     *     scap.oval.definitions.independent.TextfilecontentObject
     *     scap.oval.definitions.unix.FileObject
     *     scap.oval.definitions.windows.FileObject
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
			filenameNil = XSITools.isNil(j);
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
     *     scap.oval.definitions.independent.FileBehaviors
     *     scap.oval.definitions.unix.FileBehaviors
     *     scap.oval.definitions.windows.FileBehaviors
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

	/**
	 * @see http://oval.mitre.org/language/version5.10.1/ovaldefinition/documentation/independent-definitions-schema.html#FileBehaviors
	 */
	public String getRecurse() {
	    if (session instanceof IWindowsSession) {
		return "directories";
	    } else {
		return recurse;
	    }
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
     *     scap.oval.systemcharacteristics.independent.TextfilecontentItem
     *     scap.oval.systemcharacteristics.unix.FileItem
     *     scap.oval.systemcharacteristics.windows.FileItem
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

	void setWindowsView(IWindowsSession.View view) {
	    if (view != null) {
		String viewString = null;
		switch(view) {
		  case _32BIT:
		    viewString = "32_bit";
		    break;
		  case _64BIT:
		    viewString = "64_bit";
		    break;
		}
		setWindowsView(viewString);
	    }
	}

	void setWindowsView(String viewString) {
	    if (viewString != null) {
		try {
		    if (setWindowsView != null) {
			Class[] types = setWindowsView.getParameterTypes();
			if (types.length == 1) {
			    Class type = types[0];
			    Object instance = Class.forName(type.getName()).newInstance();
			    @SuppressWarnings("unchecked")
			    Method setValue = type.getMethod("setValue", Object.class);
			    setValue.invoke(instance, viewString);
			    setWindowsView.invoke(it, instance);
			}
		    }
		} catch (NoSuchMethodException e) {
		} catch (IllegalAccessException e) {
		} catch (IllegalArgumentException e) {
		} catch (InstantiationException e) {
		} catch (InvocationTargetException e) {
		} catch (ClassNotFoundException e) {
		}
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
