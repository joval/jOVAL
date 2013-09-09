// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.scap.ocil;

import java.util.List;

/**
 * Specification for an OCIL document required to evaluate an IScapContext.
 */
public interface IExport {
   /**
    * Get the href pertaining to this export.
    */
   String getHref();

   /**
    * Get the OCIL checklist corresponding to the href.
    */
   IChecklist getChecklist();

   /**
    * Get the IDs of the questionnaires in the checklist for which results are required. Results are unique, but
     * listed in the order in which they are first referenced by the XCCDF document.
    */
   List<String> getQuestionnaireIds();

   /**
    * Get the variable value exports for the checklist.
    */
   IVariables getVariables();
}
