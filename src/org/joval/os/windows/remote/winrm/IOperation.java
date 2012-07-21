// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.remote.winrm;

import java.util.List;

public interface IOperation <T> {
    void setOutput(T output);
    IMessage getInput();
    List<Object> getHeaders();
    T getOutput();
    String getSOAPAction();
}
