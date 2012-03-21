// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.windows;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Vector;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.windows.UserSid55Object;
import oval.schemas.definitions.windows.UserSidObject;
import oval.schemas.systemcharacteristics.core.ItemType;

import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.oval.CollectException;
import org.joval.oval.Factories;
import org.joval.oval.OvalException;

/**
 * Provides items for UserSid OVAL objects.  This implementation leverages data model annotations that correct the problem
 * of the original user_sid_test/object/state triplet, and uses the UserSid55Adapter to do the work of getting the items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UserSidAdapter extends UserSid55Adapter {
    // Implement IAdapter

    @Override
    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(UserSidObject.class);
	}
	return classes;
    }

    @Override
    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws CollectException, OvalException {
	return super.getItems(new USRequestContext(rc));
    }

    // Private

    class USRequestContext implements IRequestContext {
	IRequestContext base;
	UserSid55Object object;

	USRequestContext(IRequestContext base) {
	    UserSidObject uso = (UserSidObject)base.getObject();
	    object = Factories.definitions.windows.createUserSid55Object();
	    object.setUserSid(uso.getUserSid());
	}

	// Implement IRequestContext
	public ObjectType getObject() {
	    return object;
	}

	public void addMessage(MessageType msg) {
	    base.addMessage(msg);
	}

	public Collection<String> resolve(String variableId) throws NoSuchElementException, OvalException {
	    return base.resolve(variableId);
	}
    }
}
