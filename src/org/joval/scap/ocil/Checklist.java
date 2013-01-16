// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.ocil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import java.util.NoSuchElementException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.SAXParseException;

import ocil.schemas.core.ArtifactType;
import ocil.schemas.core.BooleanQuestionTestActionType;
import ocil.schemas.core.ChoiceType;
import ocil.schemas.core.ChoiceGroupType;
import ocil.schemas.core.ChoiceQuestionTestActionType;
import ocil.schemas.core.ChoiceQuestionType;
import ocil.schemas.core.ChoiceTestActionConditionType;
import ocil.schemas.core.CompoundTestActionType;
import ocil.schemas.core.EqualsTestActionConditionType;
import ocil.schemas.core.InstructionsType;
import ocil.schemas.core.ItemBaseType;
import ocil.schemas.core.NamedItemBaseType;
import ocil.schemas.core.NumericQuestionTestActionType;
import ocil.schemas.core.OCILType;
import ocil.schemas.core.PatternTestActionConditionType;
import ocil.schemas.core.QuestionnaireType;
import ocil.schemas.core.QuestionTestActionType;
import ocil.schemas.core.QuestionType;
import ocil.schemas.core.RangeTestActionConditionType;
import ocil.schemas.core.ReferenceType;
import ocil.schemas.core.ResultsType;
import ocil.schemas.core.StepType;
import ocil.schemas.core.StringQuestionTestActionType;
import ocil.schemas.core.SystemTargetType;
import ocil.schemas.core.TestActionConditionType;
import ocil.schemas.core.TestActionRefType;
import ocil.schemas.core.VariableType;

import org.joval.intf.ocil.IChecklist;
import org.joval.xml.SchemaRegistry;

/**
 * Representation of a OCIL checklist document.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Checklist implements IChecklist {
    /**
     * An empty IChecklist.
     */
    public static final IChecklist EMPTY;
    static {
	try {
	    OCILType root = Factories.core.createOCILType();
	    root.setQuestionnaires(Factories.core.createQuestionnairesType());
	    root.setQuestions(Factories.core.createQuestionsType());
	    root.setTestActions(Factories.core.createTestActionsType());
	    root.setVariables(Factories.core.createVariablesType());
	    EMPTY = new Checklist(root);
	} catch (OcilException e) {
	    throw new RuntimeException(e);
	}
    }

    public static final OCILType getOCILType(File f) throws OcilException {
	return getOCILType(new StreamSource(f));
    }

    public static final OCILType getOCILType(InputStream in) throws OcilException {
	return getOCILType(new StreamSource(in));
    }

    public static final OCILType getOCILType(Source source) throws OcilException {
	try {
	    String packages = SchemaRegistry.lookup(SchemaRegistry.OCIL);
	    JAXBContext ctx = JAXBContext.newInstance(packages);
	    Unmarshaller unmarshaller = ctx.createUnmarshaller();
	    Object rootObj = unmarshaller.unmarshal(source);
	    if (rootObj instanceof OCILType) {
		return (OCILType)rootObj;
	    } else if (rootObj instanceof JAXBElement) {
		JAXBElement root = (JAXBElement)rootObj;
		if (root.getValue() instanceof OCILType) {
		    return (OCILType)root.getValue();
		} else {
		    throw new OcilException("Bad OCIL source: " + source.getSystemId());
		}
	    } else {
		throw new OcilException("Bad OCIL source: " + source.getSystemId());
	    }
	} catch (JAXBException e) {
	    Throwable linkedEx = e.getLinkedException();
	    if (linkedEx instanceof SAXParseException) {
		SAXParseException spe = (SAXParseException)linkedEx;
		String s = "Error encountered while parsing OCIL document; " +
			   "Entity: " + spe.getPublicId() +
			   ", Line: " + spe.getLineNumber() +
			   ", Col: " + spe.getColumnNumber();
		throw new OcilException(s);
	    } else {
		throw new OcilException(e);
	    }
	}
    }

    private JAXBContext ctx;
    private OCILType ocil;
    private Map<String, QuestionnaireType> questionnaires;
    private Map<String, QuestionType> questions;
    private Map<String, VariableType> variables;
    private Map<String, ArtifactType> artifacts;
    private Map<String, QuestionTestActionType> questionTestActions;
    private Map<String, List<ChoiceType>> choiceGroups;
    private Map<String, ChoiceType> choices;

    /**
     * Create a Checklist based on the contents of a checklist file.
     */
    public Checklist(File f) throws OcilException {
	this(getOCILType(f));
    }

    public Checklist(InputStream in) throws OcilException {
	this(getOCILType(in));
    }

    /**
     * Create a Checklist from unmarshalled XML.
     */
    public Checklist(OCILType ocil) throws OcilException {
	this();
	this.ocil = ocil;
	for (QuestionnaireType q : ocil.getQuestionnaires().getQuestionnaire()) {
	    questionnaires.put(q.getId(), q);
	}
	for (JAXBElement<? extends QuestionType> elt : ocil.getQuestions().getQuestion()) {
	    if (!elt.isNil()) {
		QuestionType q = elt.getValue();
		questions.put(q.getId(), q);
		if (q instanceof ChoiceQuestionType) {
		    for (JAXBElement elt2 : ((ChoiceQuestionType)q).getChoiceOrChoiceGroupRef()) {
			if (!elt2.isNil()) {
			    Object obj = elt2.getValue();
			    if (obj instanceof ChoiceType) {
				choices.put(((ChoiceType)obj).getId(), ((ChoiceType)obj));
			    }
			}
		    }
		}
	    }
	}
	for (JAXBElement<? extends ItemBaseType> elt : ocil.getTestActions().getTestAction()) {
	    if (!elt.isNil()) {
		ItemBaseType item = elt.getValue();
		//
		// DAS: Technically any ItemBaseType is valid, but only QuestionTestActions make any sense here.
		//
		if (item instanceof QuestionTestActionType) {
		    QuestionTestActionType action = (QuestionTestActionType)item;
		    questionTestActions.put(action.getId(), action);
		}
	    }
	}
	if (ocil.isSetVariables() && ocil.getVariables().isSetVariable()) {
	    for (JAXBElement<? extends VariableType> elt : ocil.getVariables().getVariable()) {
		if (!elt.isNil()) {
		    VariableType variable = elt.getValue();
		    variables.put(variable.getId(), variable);
		}
	    }
	}
	if (ocil.getQuestions().isSetChoiceGroup()) {
	    for (ChoiceGroupType cg : ocil.getQuestions().getChoiceGroup()) {
		choiceGroups.put(cg.getId(), cg.getChoice());
		for (ChoiceType choice : cg.getChoice()) {
		    choices.put(choice.getId(), choice);
		}
	    }
	}
	if (ocil.isSetArtifacts() && ocil.getArtifacts().isSetArtifact()) {
	    for (ArtifactType artifact: ocil.getArtifacts().getArtifact()) {
		artifacts.put(artifact.getId(), artifact);
	    }
	}
    }

    // Implement IChecklist

    public OCILType getOCILType() {
	return ocil;
    }

    public void setResults(ResultsType results) {
	ocil.setResults(results);
    }

    /**
     * List questionnaire IDs in document order.
     */
    public List<String> listQuestionnaireIds() {
	List<String> list = new Vector<String>();
	for (QuestionnaireType q : ocil.getQuestionnaires().getQuestionnaire()) {
	    list.add(q.getId());
	}
	return list;
    }

    public boolean containsQuestionnaire(String id) {
	return questionnaires.containsKey(id);
    }

    public QuestionnaireType getQuestionnaire(String id) throws NoSuchElementException {
	if (questionnaires.containsKey(id)) {
	    return questionnaires.get(id);
	} else {
	    throw new NoSuchElementException(id);
	}
    }

    public Collection<String> getQuestionIds() {
	return questions.keySet();
    }

    public boolean containsQuestion(String id) {
	return questions.containsKey(id);
    }

    public QuestionType getQuestion(String id) throws NoSuchElementException {
	if (questions.containsKey(id)) {
	    return questions.get(id);
	} else {
	    throw new NoSuchElementException(id);
	}
    }

    public boolean containsArtifact(String id) {
	return artifacts.containsKey(id);
    }

    public ArtifactType getArtifact(String id) throws NoSuchElementException {
	if (artifacts.containsKey(id)) {
	    return artifacts.get(id);
	} else {
	    throw new NoSuchElementException(id);
	}
    }

    public boolean containsVariable(String id) {
	return variables.containsKey(id);
    }

    public VariableType getVariable(String id) throws NoSuchElementException {
	if (variables.containsKey(id)) {
	    return variables.get(id);
	} else {
	    throw new NoSuchElementException(id);
	}
    }

    public Collection<String> getQuestionTestActionIds() {
	return questionTestActions.keySet();
    }

    public boolean containsQuestionTestAction(String id) {
	return questionTestActions.containsKey(id);
    }

    public QuestionTestActionType getQuestionTestAction(String id) throws NoSuchElementException {
	if (questionTestActions.containsKey(id)) {
	    return questionTestActions.get(id);
	} else {
	    throw new NoSuchElementException(id);
	}
    }

    public Collection<String> getQuestionIdsRecursive(String questionnaireId) throws NoSuchElementException {
	HashSet<String> questionIds = new HashSet<String>();
	crawlQuestions(getQuestionnaire(questionnaireId), questionIds);
	return questionIds;
    }

    /**
     * Given a list of questionnaire IDs, return a table of lists of all the test actions that refer to individual questions,
     * indexed by the question ID.
     */
    public Map<String, Collection<String>> getQuestionActions(List<String> questionnaireIds)
		throws NoSuchElementException {

	HashSet<String> expandedQuestionnaireIds = new HashSet<String>();
	for (String id : questionnaireIds) {
	    crawlQuestionnaires(getQuestionnaire(id), expandedQuestionnaireIds);
	}
	Map<String, Collection<String>> questionActions = new HashMap<String, Collection<String>>();
	for (String id : expandedQuestionnaireIds) {
	    crawlQuestionActions(getQuestionnaire(id), questionActions);
	}
	return questionActions;
    }

    /**
     * Returns a list of all the QuestionType and QuestionnaireType objects that can be considered immediate children of
     * the specified QuestionTestActionType or QuestionnaireType parent.
     */
    public List<ItemBaseType> listChildren(ItemBaseType parent) throws NoSuchElementException {
	List<ItemBaseType> children = new Vector<ItemBaseType>();
	if (parent instanceof QuestionTestActionType) {
	    QuestionTestActionType action = (QuestionTestActionType)parent;
	    QuestionType question = getQuestion(action.getQuestionRef());
	    if (!children.contains(question)) {
		children.add(question);
	    }
	    for (TestActionConditionType condition : getConditions(action)) {
		if (condition.isSetTestActionRef()) {
		    String refId = condition.getTestActionRef().getValue();
		    if (containsQuestionnaire(refId)) {
			QuestionnaireType child = getQuestionnaire(refId);
			if (!children.contains(child)) {
			    children.add(child);
			}
		    } else if (containsQuestionTestAction(refId)) {
			for (ItemBaseType child : listChildren(getQuestionTestAction(refId))) {
			    if (!children.contains(child)) {
				children.add(child);
			    }
			}
		    } else {
			throw new NoSuchElementException(refId);
		    }
		}
	    }
	} else if (parent instanceof QuestionnaireType) {
	    QuestionnaireType questionnaire = (QuestionnaireType)parent;
	    for (TestActionRefType ref : questionnaire.getActions().getTestActionRef()) {
		String refId = ref.getValue();
		if (containsQuestionnaire(refId)) {
		    QuestionnaireType child = getQuestionnaire(refId);
		    if (!children.contains(child)) {
			children.add(child);
		    }
		} else if (containsQuestionTestAction(refId)) {
		    children.addAll(listChildren(getQuestionTestAction(refId)));
		} else {
		    throw new NoSuchElementException(refId);
		}
	    }
	} else {
	    throw new IllegalArgumentException(parent.getClass().getName());
	}
	return children;
    }

    public List<TestActionConditionType> getConditions(QuestionTestActionType action) {
	List<TestActionConditionType> conditions = new Vector<TestActionConditionType>();
	if (action instanceof BooleanQuestionTestActionType) {
	    BooleanQuestionTestActionType qt = (BooleanQuestionTestActionType)action;
	    if (qt.isSetWhenTrue()) {
		conditions.add(new BooleanTestActionConditionType(true, qt.getWhenTrue()));
	    }
	    if (qt.isSetWhenFalse()) {
		conditions.add(new BooleanTestActionConditionType(false, qt.getWhenFalse()));
	    }
	} else if (action instanceof ChoiceQuestionTestActionType) {
	    ChoiceQuestionTestActionType qt = (ChoiceQuestionTestActionType)action;
	    if (qt.isSetWhenChoice()) {
		conditions.addAll(qt.getWhenChoice());
	    }
	} else if (action instanceof NumericQuestionTestActionType) {
	    NumericQuestionTestActionType qt = (NumericQuestionTestActionType)action;
	    for (JAXBElement<? extends TestActionConditionType> elt : qt.getRest()) {
		if (!elt.isNil()) {
		    conditions.add(elt.getValue());
		}
	    }
	} else if (action instanceof StringQuestionTestActionType) {
	    StringQuestionTestActionType qt = (StringQuestionTestActionType)action;
	    conditions.addAll(qt.getWhenPattern());
	}
	if (action.isSetWhenUnknown()) {
	    conditions.add(action.getWhenUnknown());
	}
	if (action.isSetWhenNotTested()) {
	    conditions.add(action.getWhenNotTested());
	}
	if (action.isSetWhenNotApplicable()) {
	    conditions.add(action.getWhenNotApplicable());
	}
	if (action.isSetWhenError()) {
	    conditions.add(action.getWhenError());
	}
	return conditions;
    }

    /**
     * Return a list of choices, given a choice group ID.
     */
    public List<ChoiceType> getChoices(String id) throws NoSuchElementException {
	if (choiceGroups.containsKey(id)) {
	    return choiceGroups.get(id);
	} else {
	    throw new NoSuchElementException(id);
	}
    }

    public boolean containsChoice(String id) {
	return choices.containsKey(id);
    }

    public ChoiceType getChoice(String id) throws NoSuchElementException {
	if (choices.containsKey(id)) {
	    return choices.get(id);
	} else {
	    throw new NoSuchElementException(id);
	}
    }

    /**
     * Test to see if the specified questionnaire contains a cycle. Cycles are bad.
     *
     * @throws NoSuchElementException if there is no questionnaire with the specified ID, or if an illegal reference
     *				is encountered during the evaluation process
     */
    public boolean containsCycle(String id) throws NoSuchElementException {
	return cycleCheck(id, new Stack<String>());
    }

    /**
     * Return a list of all the language values in TextType entities throughout the OCIL document.
     *
     * @see http://www.ietf.org/rfc/rfc4646.txt
     */
    public Collection<String> getLanguages() {
	HashSet<String> locales = new HashSet<String>();
	for (QuestionType question : questions.values()) {
	    if (question.isSetInstructions()) {
		InstructionsType instructions = question.getInstructions();
		if (instructions.isSetTitle() && instructions.getTitle().isSetLang()) {
		    locales.add(instructions.getTitle().getLang());
		}
		if (instructions.isSetStep()) {
		    for (StepType step : instructions.getStep()) {
			if (step.isSetDescription() && step.getDescription().isSetLang()) {
			    locales.add(step.getDescription().getLang());
			}
			if (step.isSetReference()) {
			    for (ReferenceType reference : step.getReference()) {
				if (reference.isSetLang()) {
				    locales.add(reference.getLang());
				}
			    }
			}
		    }
		}
	    }
	}
	for (QuestionnaireType questionnaire : questionnaires.values()) {
	    if (questionnaire.isSetTitle() && questionnaire.getTitle().isSetLang()) {
		locales.add(questionnaire.getTitle().getLang());
	    }
	    if (questionnaire.isSetDescription() && questionnaire.getDescription().isSetLang()) {
		locales.add(questionnaire.getDescription().getLang());
	    }
	    if (questionnaire.isSetReferences() && questionnaire.getReferences().isSetReference()) {
		for (ReferenceType reference : questionnaire.getReferences().getReference()) {
		    if (reference.isSetLang()) {
			locales.add(reference.getLang());
		    }
		}
	    }
	}
	for (VariableType variable : variables.values()) {
	    if (variable.isSetDescription() && variable.getDescription().isSetLang()) {
		locales.add(variable.getDescription().getLang());
	    }
	}
	for (ArtifactType artifact: artifacts.values()) {
	    if (artifact.isSetTitle() && artifact.getTitle().isSetLang()) {
		locales.add(artifact.getTitle().getLang());
	    }
	    if (artifact.isSetDescription() && artifact.getDescription().isSetLang()) {
		locales.add(artifact.getDescription().getLang());
	    }
	}
	for (QuestionTestActionType action : questionTestActions.values()) {
	    if (action.isSetTitle() && action.getTitle().isSetLang()) {
		locales.add(action.getTitle().getLang());
	    }
	}
	if (ocil != null && ocil.isSetResults()) {
	    ResultsType results = ocil.getResults();
	    if (results.isSetTitle() && results.getTitle().isSetLang()) {
		locales.add(results.getTitle().getLang());
	    }
	    if (results.isSetTargets() && results.getTargets().isSetTarget()) {
		for (JAXBElement<? extends NamedItemBaseType> elt : results.getTargets().getTarget()) {
		    if (elt.isNil()) {
			continue;
		    }
		    NamedItemBaseType item = elt.getValue();
		    if (item instanceof SystemTargetType) {
			SystemTargetType system = (SystemTargetType)item;
			if (system.isSetDescription() && system.getDescription().isSetLang()) {
			    locales.add(system.getDescription().getLang());
			}
		    }
		}
	    }
	}
	return locales;
    }

    public void writeXML(File f) throws IOException {
	OutputStream out = null;
	try {
	    Marshaller marshaller = ctx.createMarshaller();
	    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
	    out = new FileOutputStream(f);
	    marshaller.marshal(Factories.core.createOcil(getOCILType()), out);
	} catch (JAXBException e) {
	    throw new IOException(e);
	} catch (FactoryConfigurationError e) {
	    throw new IOException(e);
	} finally {
	    if (out != null) {
		try {
		    out.close();
		} catch (IOException e) {
		}
	    }
	}
    }

    // Implement ITransformable

    public Source getSource() throws JAXBException {
	return new JAXBSource(ctx, Factories.core.createOcil(getOCILType()));
    }

    // Private

    /**
     * Create an empty Checklist.
     */
    private Checklist() throws OcilException {
	try {
	    ctx = JAXBContext.newInstance(SchemaRegistry.lookup(SchemaRegistry.OCIL));
	} catch (JAXBException e) {
	    throw new OcilException(e);
	}
	artifacts = new HashMap<String, ArtifactType>();
	questionnaires = new HashMap<String, QuestionnaireType>();
	questions = new HashMap<String, QuestionType>();
	variables = new HashMap<String, VariableType>();
	questionTestActions = new HashMap<String, QuestionTestActionType>();
	choiceGroups = new HashMap<String, List<ChoiceType>>();
	choices = new HashMap<String, ChoiceType>();
    }

    private void crawlQuestions(QuestionnaireType questionnaire, HashSet<String> questionIds) throws NoSuchElementException {
	for (ItemBaseType item : listChildren(questionnaire)) {
	    if (item instanceof QuestionnaireType) {
		crawlQuestions((QuestionnaireType)item, questionIds);
	    } else if (item instanceof QuestionType) {
		questionIds.add(((QuestionType)item).getId());
	    }
	}
    }

    private void crawlQuestionnaires(QuestionnaireType questionnaire, HashSet<String> questionnaireIds)
		throws NoSuchElementException {

	String id = questionnaire.getId();
	if (!questionnaireIds.contains(id)) {
	    questionnaireIds.add(id);
	    for (ItemBaseType item : listChildren(questionnaire)) {
		if (item instanceof QuestionnaireType) {
		    crawlQuestionnaires((QuestionnaireType)item, questionnaireIds);
		}
	    }
	}
    }

    private void crawlQuestionActions(ItemBaseType item, Map<String, Collection<String>> questionActions)
		throws NoSuchElementException {

	if (item instanceof QuestionnaireType) {
	    QuestionnaireType questionnaire = (QuestionnaireType)item;
	    for (TestActionRefType ref  : questionnaire.getActions().getTestActionRef()) {
		String refId = ref.getValue();
		if (containsQuestionTestAction(refId)) {
		    crawlQuestionActions(getQuestionTestAction(refId), questionActions);
		} else if (containsQuestionnaire(refId)) {
		    crawlQuestionActions(getQuestionnaire(refId), questionActions);
		} else {
		    throw new NoSuchElementException(refId);
		}
	    }
	} else if (item instanceof QuestionTestActionType) {
	    QuestionTestActionType action = (QuestionTestActionType)item;
	    String refId = action.getQuestionRef();
	    if (containsQuestion(refId)) {
		if (!questionActions.containsKey(refId)) {
		    questionActions.put(refId, new HashSet<String>());
		}
		questionActions.get(refId).add(action.getId());
	    } else if (containsQuestionnaire(refId)) {
		crawlQuestionActions(getQuestionnaire(refId), questionActions);
	    } else {
		throw new NoSuchElementException(refId);
	    }
	    for (TestActionConditionType condition : getConditions(action)) {
		if (condition.isSetTestActionRef()) {
		    String actionRef = condition.getTestActionRef().getValue();
		    if (containsQuestionTestAction(actionRef)) {
			crawlQuestionActions(getQuestionTestAction(actionRef), questionActions);
		    } else if (containsQuestionnaire(actionRef)) {
			crawlQuestionActions(getQuestionnaire(actionRef), questionActions);
		    } else {
			throw new NoSuchElementException(actionRef);
		    }
		}
	    }
	} else {
	    throw new IllegalArgumentException(item.getClass().getName());
	}
    }

    /**
     * Returns true if the questionnaire with the specified ID contains a cycle.
     */
    private boolean cycleCheck(String id, Stack<String> stack) throws NoSuchElementException {
	if (stack.contains(id)) {
	    return true;
	} else {
	    stack.push(id);
	    QuestionnaireType questionnaire = getQuestionnaire(id);
	    if (questionnaire.isSetActions() && questionnaire.getActions().isSetTestActionRef()) {
		for (TestActionRefType ref : questionnaire.getActions().getTestActionRef()) {
		    String refId = ref.getValue();
		    if (containsQuestionTestAction(refId)) {
			for (TestActionConditionType condition : getConditions(getQuestionTestAction(refId))) {
			    if (condition.isSetTestActionRef()) {
				String actionRef = condition.getTestActionRef().getValue();
				if (containsQuestionnaire(actionRef)) {
				    if (cycleCheck(actionRef, stack)) {
					return true;
				    }
				}
			    }
			}
		    } else if (containsQuestionnaire(refId)) {
			if (cycleCheck(refId, stack)) {
			    return true;
			}
		    } else {
			throw new NoSuchElementException(refId);
		    }
		}
	    }
	    stack.pop();
	    return false;
	}
    }
}
