// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.scap.xccdf;

import java.util.Collection;
import java.util.NoSuchElementException;
import javax.xml.bind.JAXBElement;

import scap.xccdf.TailoringType;
import scap.xccdf.ProfileType;

import org.joval.intf.xml.ITransformable;

/**
 * A representation of an XCCDF 1.2 tailoring.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface ITailoring extends ITransformable<JAXBElement<TailoringType>> {
    /**
     * Set the originating document URI.
     */
    void setHref(String href);

    /**
     * Get the originating document URI.
     */
    String getHref();

    /**
     * Shortcut for getRootObject().getValue().getTailoringId().
     */
    String getId();

    /**
     * Shortcut for getRootObject().getValue().getBenchmark().getId().
     */
    String getBenchmarkId();

    /**
     * Enumerate all the profile IDs in the tailoring.
     */
    Collection<String> getProfileIds();

    /**
     * Retrieve a specific profile from the tailoring.
     */
    ProfileType getProfile(String id) throws NoSuchElementException;
}
