// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.ocil;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import ocil.schemas.core.ArtifactType;
import ocil.schemas.core.ChoiceType;
import ocil.schemas.core.ItemBaseType;
import ocil.schemas.core.OCILType;
import ocil.schemas.core.QuestionType;
import ocil.schemas.core.QuestionnaireType;
import ocil.schemas.core.QuestionTestActionType;
import ocil.schemas.core.TestActionConditionType;
import ocil.schemas.core.VariableType;

import org.joval.intf.xml.ITransformable;

/**
 * Representation of a OCIL checklist document.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IChecklist extends ITransformable {
    /**
     * Get the underlying JAXB data model checklist.
     */
    OCILType getOCILType();

    /**
     * List questionnaire IDs in document order.
     */
    List<String> listQuestionnaireIds();

    /**
     * Test to see if the checklist contains the specified questionnaire.
     */
    boolean containsQuestionnaire(String id);

    /**
     * Get the specified questionnaire.
     */
    QuestionnaireType getQuestionnaire(String id) throws NoSuchElementException;

    /**
     * Get all the question IDs defined in the checklist.
     */
    Collection<String> getQuestionIds();

    /**
     * Test to see if the checklist contains a question with the specified ID.
     */
    boolean containsQuestion(String id);

    /**
     * Get the question with the specified ID.
     */
    QuestionType getQuestion(String id) throws NoSuchElementException;

    /**
     * Test to see whether the checklist contains an artifact with the specified ID.
     */
    boolean containsArtifact(String id);

    /**
     * Get the artifact with the specified ID.
     */
    ArtifactType getArtifact(String id) throws NoSuchElementException;

    /**
     * Test to see whether the checklist contains a variable with the specified ID.
     */
    boolean containsVariable(String id);

    /**
     * Get the variable with the specified ID.
     */
    VariableType getVariable(String id) throws NoSuchElementException;

    /**
     * Get all of the question test actions defined in the checklist.
     */
    Collection<String> getQuestionTestActionIds();

    /**
     * Test to see whether the checklist contains a question test action with the specified ID.
     */
    boolean containsQuestionTestAction(String id);

    /**
     * Get the question test action with the specified ID.
     */
    QuestionTestActionType getQuestionTestAction(String id) throws NoSuchElementException;

    /**
     * Get all the questions that can be asked in the specified questionnaire.
     */
    Collection<String> getQuestionIdsRecursive(String questionnaireId) throws NoSuchElementException;

    /**
     * Given a list of questionnaire IDs, return a table of lists of all the test actions that refer to individual questions,
     * indexed by the question ID.
     */
    Map<String, Collection<String>> getQuestionActions(List<String> questionnaireIds) throws NoSuchElementException;

    /**
     * Returns a list (in order) of all the QuestionType and QuestionnaireType objects that can be considered immediate
     * children of the specified QuestionTestActionType or QuestionnaireType parent.
     */
    List<ItemBaseType> listChildren(ItemBaseType parent) throws NoSuchElementException;

    /**
     * Get the list (in order) of test action conditions for the specified action.
     */
    List<TestActionConditionType> getConditions(QuestionTestActionType action);

    /**
     * Return a list of choices, given a choice group ID.
     */
    List<ChoiceType> getChoices(String id) throws NoSuchElementException;

    /**
     * Test to see that the checklist contains the specified choice.
     */
    boolean containsChoice(String id);

    /**
     * Get the specified choice.
     */
    ChoiceType getChoice(String id) throws NoSuchElementException;

    /**
     * Test to see if the specified questionnaire contains a cycle. Cycles are bad.
     *
     * @throws NoSuchElementException if there is no questionnaire with the specified ID, or if an illegal reference
     *				is encountered during the evaluation process
     */
    boolean containsCycle(String id) throws NoSuchElementException;

    /**
     * Return a list of all the language values in TextType entities throughout the OCIL document.
     *
     * @see <a href="http://www.ietf.org/rfc/rfc4646.txt">RFC4646</a> for information about permissible values.
     */
    Collection<String> getLanguages();

    /**
     * Serialize the checklist to a file.
     */
    void writeXML(File f) throws IOException;
}
