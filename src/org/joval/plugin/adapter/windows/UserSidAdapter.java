// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.windows;

import java.util.List;
import java.util.NoSuchElementException;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.windows.UserSid55Object;
import oval.schemas.definitions.windows.UserSidObject;
import oval.schemas.systemcharacteristics.core.ItemType;

import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.windows.wmi.IWmiProvider;
import org.joval.oval.OvalException;
import org.joval.oval.ResolveException;
import org.joval.util.JOVALSystem;

/**
 * Provides items for UserSid OVAL objects.  This implementation leverages data model annotations that correct the problem
 * of the original user_sid_test/object/state triplet, and uses the UserSid55Adapter to do the work of getting the items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UserSidAdapter extends UserSid55Adapter {
    public UserSidAdapter(String hostname, IWmiProvider wmi) {
	super(hostname, wmi);
    }

    /**
     * @override
     */
    public Class getObjectClass() {
	return UserSidObject.class;
    }

    /**
     * @override
     */
    public List<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws OvalException {
	return super.getItems(new USRequestContext(rc));
    }

    // Private

    class USRequestContext implements IRequestContext {
	IRequestContext base;
	UserSid55Object object;

	USRequestContext(IRequestContext base) {
	    UserSidObject uso = (UserSidObject)base.getObject();
	    object = JOVALSystem.factories.definitions.windows.createUserSid55Object();
	    object.setUserSid(uso.getUserSid());
	}

	// Implement IRequestContext
	public ObjectType getObject() {
	    return object;
	}

	public void addMessage(MessageType msg) {
	    base.addMessage(msg);
	}

	public List<String> resolve(String variableId) throws NoSuchElementException, ResolveException, OvalException {
	    return base.resolve(variableId);
	}
    }
}
