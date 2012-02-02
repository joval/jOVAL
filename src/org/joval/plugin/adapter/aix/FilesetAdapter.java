// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.aix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
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
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.oval.NotCollectableException;
import org.joval.oval.TestException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
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

    public FilesetAdapter(IUnixSession session) {
	this.session = session;
    }

    // Implement IAdapter

    private static Class[] objectClasses = {FilesetObject.class};

    public Class[] getObjectClasses() {
	return objectClasses;
    }

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws NotCollectableException {
	FilesetObject fObj = (FilesetObject)rc.getObject();
	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	switch(fObj.getFlstinst().getOperation()) {
	  case EQUALS:
	    try {
		for (FilesetItem item : getItems((String)fObj.getFlstinst().getValue())) {
		    items.add(JOVALSystem.factories.sc.aix.createFilesetItem(item));
		}
	    } catch (Exception e) {
		MessageType msg = JOVALSystem.factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		rc.addMessage(msg);
		session.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    break;

	  default: {
	    String s = JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, fObj.getFlstinst().getOperation());
	    throw new NotCollectableException(s);
	  }
	}

	return items;
    }

    // Private

    private Collection<FilesetItem> getItems(String fileset) throws Exception {
	session.getLogger().trace(JOVALMsg.STATUS_AIX_FILESET, fileset);
	Collection<FilesetItem> items = new Vector<FilesetItem>();

	for (String line : SafeCLI.multiLine("lslpp -lac " + fileset, session, IUnixSession.Timeout.M)) {
	    if (!line.startsWith("#")) {
		List<String> info = StringTools.toList(StringTools.tokenize(line, ":"));
		if (info.get(0).equals("/etc/objrepos")) {
		    FilesetItem item = JOVALSystem.factories.sc.aix.createFilesetItem();

		    EntityItemStringType flstinst = JOVALSystem.factories.sc.core.createEntityItemStringType();
		    flstinst.setValue(info.get(1));
		    item.setFlstinst(flstinst);

		    EntityItemVersionType level = JOVALSystem.factories.sc.core.createEntityItemVersionType();
		    level.setValue(info.get(2));
		    level.setDatatype(SimpleDatatypeEnumeration.VERSION.value());
		    item.setLevel(level);

		    EntityItemFilesetStateType state = JOVALSystem.factories.sc.aix.createEntityItemFilesetStateType();
		    state.setValue(info.get(4));
		    item.setState(state);

		    EntityItemStringType description = JOVALSystem.factories.sc.core.createEntityItemStringType();
		    description.setValue(info.get(6));
		    item.setDescription(description);

		    items.add(item);
		}
	    }
	}

	return items;
    }
}
