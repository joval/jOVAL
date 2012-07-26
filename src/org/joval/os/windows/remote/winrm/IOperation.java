// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm;

import java.io.IOException;
import java.util.List;
import javax.xml.bind.JAXBException;

/**
 * Interface describing a SOAP operation, with generic input and output types.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IOperation<I, O> {
    /**
     * Add a mandatory DMTF WS-Management Resource URI header field for the operation.
     */
    void addResourceURI(String uri);

    /**
     * Get the output (i.e., SOAP response message body contents) for the operation, dispatched through the specified
     * port.
     */
    O dispatch(IPort port) throws IOException, JAXBException, WSMFault;
}
