// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.aix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.aix.FilesetObject;
import oval.schemas.results.core.ResultEnumeration;
import oval.schemas.systemcharacteristics.aix.EntityItemFilesetStateType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.EntityItemVersionType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.aix.FilesetItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;
import org.joval.util.StringTools;

/**
 * Retrieves items for AIX fileset_objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class FilesetAdapter implements IAdapter {
    private IUnixSession session;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IUnixSession) {
	    this.session = (IUnixSession)session;
	    classes.add(FilesetObject.class);
	}
	return classes;
    }

    public Collection<FilesetItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	FilesetObject fObj = (FilesetObject)obj;
	Collection<FilesetItem> items = new Vector<FilesetItem>();
	switch(fObj.getFlstinst().getOperation()) {
	  case EQUALS:
	    try {
		String fileset = (String)fObj.getFlstinst().getValue();
		session.getLogger().trace(JOVALMsg.STATUS_AIX_FILESET, fileset);
		for (String line : SafeCLI.multiLine("lslpp -lac " + fileset, session, IUnixSession.Timeout.M)) {
		    if (!line.startsWith("#")) {
			List<String> info = StringTools.toList(StringTools.tokenize(line, ":"));
			if (info.get(0).equals("/etc/objrepos")) {
			    FilesetItem item = Factories.sc.aix.createFilesetItem();

			    EntityItemStringType flstinst = Factories.sc.core.createEntityItemStringType();
			    flstinst.setValue(info.get(1));
			    item.setFlstinst(flstinst);

			    EntityItemVersionType level = Factories.sc.core.createEntityItemVersionType();
			    level.setValue(info.get(2));
			    level.setDatatype(SimpleDatatypeEnumeration.VERSION.value());
			    item.setLevel(level);

			    EntityItemFilesetStateType state = Factories.sc.aix.createEntityItemFilesetStateType();
			    state.setValue(info.get(4));
			    item.setState(state);

			    EntityItemStringType description = Factories.sc.core.createEntityItemStringType();
			    description.setValue(info.get(6));
			    item.setDescription(description);

			    items.add(item);
			}
		    }
		}
	    } catch (Exception e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		rc.addMessage(msg);
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    break;

	  default: {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, fObj.getFlstinst().getOperation());
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	  }
	}

	return items;
    }
}
