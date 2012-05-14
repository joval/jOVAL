// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.ocil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Stack;
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

import ocil.schemas.core.ArtifactType;
import ocil.schemas.core.ChoiceType;
import ocil.schemas.core.ChoiceGroupType;
import ocil.schemas.core.CompoundTestActionType;
import ocil.schemas.core.InstructionsType;
import ocil.schemas.core.ItemBaseType;
import ocil.schemas.core.NamedItemBaseType;
import ocil.schemas.core.ObjectFactory;
import ocil.schemas.core.OCILType;
import ocil.schemas.core.QuestionnaireType;
import ocil.schemas.core.QuestionTestActionType;
import ocil.schemas.core.QuestionType;
import ocil.schemas.core.ReferenceType;
import ocil.schemas.core.ResultsType;
import ocil.schemas.core.StepType;
import ocil.schemas.core.SystemTargetType;
import ocil.schemas.core.TestActionRefType;
import ocil.schemas.core.VariableType;

import org.joval.intf.xml.ITransformable;
import org.joval.xml.SchemaRegistry;

/**
 * Representation of a OCIL checklist document.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Checklist implements ITransformable {
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
	    throw new OcilException(e);
	}
    }

    private JAXBContext ctx;
    private OCILType ocil;
    private Hashtable<String, QuestionnaireType> questionnaires;
    private Hashtable<String, QuestionType> questions;
    private Hashtable<String, QuestionTestActionType> testActions;
    private Hashtable<String, List<ChoiceType>> choiceGroups;

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
	    }
	}
	for (JAXBElement<? extends ItemBaseType> elt : ocil.getTestActions().getTestAction()) {
	    if (!elt.isNil()) {
		ItemBaseType item = elt.getValue();
		//
		// DAS: Technically, any ItemBaseType is valid, but reality, only QuestionTestActionTypes make sense here.
		//
		if (item instanceof QuestionTestActionType) {
		    QuestionTestActionType action = (QuestionTestActionType)item;
		    testActions.put(action.getId(), action);
		}
	    }
	}
	if (ocil.getQuestions().isSetChoiceGroup()) {
	    for (ChoiceGroupType cg : ocil.getQuestions().getChoiceGroup()) {
		choiceGroups.put(cg.getId(), cg.getChoice());
	    }
	}
    }

    /**
     * Create an empty Checklist.
     */
    private Checklist() throws OcilException {
	try {
	    ctx = JAXBContext.newInstance(SchemaRegistry.lookup(SchemaRegistry.OCIL));
	} catch (JAXBException e) {
	    throw new OcilException(e);
	}
	questionnaires = new Hashtable<String, QuestionnaireType>();
	questions = new Hashtable<String, QuestionType>();
	testActions = new Hashtable<String, QuestionTestActionType>();
	choiceGroups = new Hashtable<String, List<ChoiceType>>();
    }

    public OCILType getOCILType() {
	return ocil;
    }

    public void setResults(ResultsType results) {
	ocil.setResults(results);
    }

    public boolean containsQuestionnaire(String id) {
	return questionnaires.contains(id);
    }

    public QuestionnaireType getQuestionnaire(String id) throws NoSuchElementException {
	if (questionnaires.containsKey(id)) {
	    return questionnaires.get(id);
	} else {
	    throw new NoSuchElementException(id);
	}
    }

    public Collection<QuestionnaireType> getQuestionnaires() {
	return questionnaires.values();
    }

    public boolean containsQuestion(String id) {
	return questions.contains(id);
    }

    public QuestionType getQuestion(String id) throws NoSuchElementException {
	if (questions.containsKey(id)) {
	    return questions.get(id);
	} else {
	    throw new NoSuchElementException(id);
	}
    }

    public boolean containsTestAction(String id) {
	return testActions.containsKey(id);
    }

    public QuestionTestActionType getTestAction(String id) throws NoSuchElementException {
	if (testActions.containsKey(id)) {
	    return testActions.get(id);
	} else {
	    throw new NoSuchElementException(id);
	}
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
	if (ocil != null) {
	    if (ocil.getQuestions().isSetQuestion()) {
		for (JAXBElement<? extends QuestionType> elt : ocil.getQuestions().getQuestion()) {
		    if (elt.isNil()) {
			continue;
		    }
		    QuestionType question = elt.getValue();
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
	    }
	    if (ocil.getTestActions().isSetTestAction()) {
		for (JAXBElement<? extends ItemBaseType> elt : ocil.getTestActions().getTestAction()) {
		    if (elt.isNil()) {
			continue;
		    }
		    ItemBaseType item = elt.getValue();
		    if (item instanceof CompoundTestActionType) {
			CompoundTestActionType action = (CompoundTestActionType)item;
			if (action.isSetTitle() && action.getTitle().isSetLang()) {
			    locales.add(action.getTitle().getLang());
			}
			if (action.isSetDescription() && action.getDescription().isSetLang()) {
			    locales.add(action.getDescription().getLang());
			}
			if (action.isSetReferences() && action.getReferences().isSetReference()) {
			    for (ReferenceType reference : action.getReferences().getReference()) {
				if (reference.isSetLang()) {
				    locales.add(reference.getLang());
				}
			    }
			}
		    } else if (item instanceof QuestionTestActionType) {
			QuestionTestActionType action = (QuestionTestActionType)item;
			if (action.isSetTitle() && action.getTitle().isSetLang()) {
			    locales.add(action.getTitle().getLang());
			}
		    }
		}
	    }
	    if (ocil.getQuestionnaires().isSetQuestionnaire()) {
		for (QuestionnaireType questionnaire : ocil.getQuestionnaires().getQuestionnaire()) {
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
	    }
	    if (ocil.isSetArtifacts() && ocil.getArtifacts().isSetArtifact()) {
		for (ArtifactType artifact: ocil.getArtifacts().getArtifact()) {
		    if (artifact.isSetTitle() && artifact.getTitle().isSetLang()) {
			locales.add(artifact.getTitle().getLang());
		    }
		    if (artifact.isSetDescription() && artifact.getDescription().isSetLang()) {
			locales.add(artifact.getDescription().getLang());
		    }
		}
	    }
	    if (ocil.isSetVariables() && ocil.getVariables().isSetVariable()) {
		for (JAXBElement<? extends VariableType> elt : ocil.getVariables().getVariable()) {
		    if (elt.isNil()) {
			continue;
		    }
		    VariableType variable = elt.getValue();
		    if (variable.isSetDescription() && variable.getDescription().isSetLang()) {
			locales.add(variable.getDescription().getLang());
		    }
		}
	    }
	    if (ocil.isSetResults()) {
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
	}
	return locales;
    }

    public void writeXML(File f) throws IOException {
	OutputStream out = null;
	try {
	    Marshaller marshaller = ctx.createMarshaller();
	    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
	    out = new FileOutputStream(f);
	    marshaller.marshal(new ObjectFactory().createOcil(getOCILType()), out);
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
	return new JAXBSource(ctx, new ObjectFactory().createOcil(getOCILType()));
    }

    // Private

    private boolean cycleCheck(String id, Stack<String> stack) throws NoSuchElementException {
	if (stack.contains(id)) {
	    return true;
	}

	QuestionnaireType questionnaire = getQuestionnaire(id);
	stack.push(id);
	if (questionnaire.isSetActions() && questionnaire.getActions().isSetTestActionRef()) {
	    for (TestActionRefType ref : questionnaire.getActions().getTestActionRef()) {
		String refId = ref.getValue();
		if (containsTestAction(refId)) {
		    // continue
		} else if (containsQuestionnaire(refId)) {
		    return cycleCheck(refId, stack);
		} else {
		    throw new NoSuchElementException(refId);
		}
	    }
	}
	stack.pop();
	return false;
    }
}
