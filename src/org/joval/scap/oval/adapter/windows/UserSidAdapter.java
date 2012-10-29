// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Vector;

import oval.schemas.common.MessageType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.windows.UserSid55Object;
import oval.schemas.definitions.windows.UserSidObject;
import oval.schemas.systemcharacteristics.core.ItemType;

import org.joval.intf.system.IBaseSession;
import org.joval.intf.windows.system.IWindowsSession;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;

/**
 * Provides items for UserSid OVAL objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UserSidAdapter extends UserSid55Adapter {
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
    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	UserSidObject uso = (UserSidObject)obj;
	UserSid55Object object = Factories.definitions.windows.createUserSid55Object();
	object.setUserSid(uso.getUserSid());
	return super.getItems(object, rc);
    }
}
