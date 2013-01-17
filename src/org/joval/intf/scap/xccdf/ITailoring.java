// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.scap.xccdf;

import java.util.Collection;
import java.util.NoSuchElementException;

import scap.xccdf.TailoringType;
import scap.xccdf.ProfileType;

/**
 * A representation of an XCCDF 1.2 tailoring.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface ITailoring {
    String getBenchmarkId();

    TailoringType getTailoring();

    Collection<String> getProfileIds();

    ProfileType getProfile(String id) throws NoSuchElementException;
}
