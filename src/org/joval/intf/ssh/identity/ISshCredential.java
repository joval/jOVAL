// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.ssh.identity;

import java.io.File;

import org.joval.intf.identity.ICredential;

/**
 * A representation of an SSH credential.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface ISshCredential extends ICredential {
    String getRootPassword();
    String getPassphrase();
    File getPrivateKey();
}
