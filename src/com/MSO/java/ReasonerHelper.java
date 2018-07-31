package com.MSO.java;

import org.obolibrary.robot.IOHelper;
import org.obolibrary.robot.ReasonOperation;
import org.obolibrary.robot.exceptions.InvalidReferenceException;
import org.obolibrary.robot.exceptions.OntologyLogicException;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;
import uk.ac.manchester.cs.jfact.JFactFactory;

import java.io.File;
import java.io.IOException;

class ReasonerHelper {

    OWLOntology reasonMSO(IOHelper ioHelper) throws IOException, OWLOntologyCreationException, OntologyLogicException, InvalidReferenceException {

        // Load the saved MSO.
        OWLOntology MSO = ioHelper.loadOntology("files/MSO.owl");

        // Instantiate a JFact reasoner factory to reason over MSO. Reason, then save to disk.
        OWLReasonerFactory reasonerFactory = new JFactFactory();

        ReasonOperation.reason(MSO, reasonerFactory);

        // Return the reasoned MSO.
        return MSO;

    }

    OWLOntology reasonSO(IOHelper ioHelper) throws IOException, OWLOntologyCreationException, OntologyLogicException, InvalidReferenceException {

        // Load the saved SO.
        OWLOntology SO = ioHelper.loadOntology("files/SO.owl");

        // Create an import statement for the MSO.
        File MSO_file = new File("files/MSO.owl");

        OWLImportsDeclaration MSO_import = SO.getOWLOntologyManager().getOWLDataFactory().getOWLImportsDeclaration(
                IRI.create(MSO_file));

        AddImport SO_import = new AddImport(SO, MSO_import);

        // Add import directive to the SO.
        SO.getOWLOntologyManager().applyChange(SO_import);

        // Add the MSO to SO's ontology manager.

        SO.getOWLOntologyManager().loadOntology(IRI.create(MSO_file));

        // Instantiate a JFact reasoner factory and reason over SO with the MSO imported.
        OWLReasonerFactory reasonerFactory = new JFactFactory();

        ReasonOperation.reason(SO, reasonerFactory);

//      Return the reasoned MSO.
        return SO;

    }
}
