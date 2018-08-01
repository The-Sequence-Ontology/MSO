package com.MSO.java;

import org.obolibrary.robot.IOHelper;
import org.obolibrary.robot.ReasonOperation;
import org.obolibrary.robot.exceptions.InvalidReferenceException;
import org.obolibrary.robot.exceptions.OntologyLogicException;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.util.OWLObjectTransformer;
import uk.ac.manchester.cs.jfact.JFactFactory;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

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

        // Return the reasoned MSO.
        return SO;
    }

    void qualityControl(IOHelper ioHelper) throws IOException {

        // Retrieve the IRI for biological sequence entity in the SO.
        IRI biologicalSequenceEntityIRI = IRI.create("http://purl.obolibrary.org/obo/SO_3000265");

        // Load the saved MSO.
        OWLOntology MSO_reasoned = ioHelper.loadOntology("files/MSO_reasoned.owl");

        // Load the reasoned SO.
        OWLOntology SO_reasoned = ioHelper.loadOntology("files/SO_reasoned.owl");

        // Change the IRIs in the MSO to be identical to those in SO.
        OWLOntologyManager MSOManager = MSO_reasoned.getOWLOntologyManager();

        OWLDataFactory MSODataFactory = MSOManager.getOWLDataFactory();

        OWLObjectTransformer<IRI> MSOreplacer = new OWLObjectTransformer<>((x) -> true,
                (input) -> {

                    if (input != null) {

                        String newIRI = input.toString().replaceAll("MSO_", "SO_");


                        return IRI.create(newIRI);
                    }

                    return null;
                },

                MSODataFactory,

                IRI.class);

        List<OWLOntologyChange> MSOchanges = MSOreplacer.change(MSO_reasoned);

        MSOManager.applyChanges(MSOchanges);

        // Create reasoners to get subclass nodes of biological sequence entity.
        OWLReasonerConfiguration config = new SimpleConfiguration();

        OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();

        OWLReasoner MSOreasoner = reasonerFactory.createReasoner(MSO_reasoned, config);

        OWLReasoner SOreasoner = reasonerFactory.createReasoner(SO_reasoned, config);

        MSOreasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

        SOreasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

        // Get biological sequence entity as a class.
        OWLClass biologicalSequenceEntity = MSODataFactory.getOWLClass(biologicalSequenceEntityIRI);

        // Retrieve the node sets for biological sequence entity for SO and MSO.
        NodeSet<OWLClass> MSOsubClassNodes = MSOreasoner.getSubClasses(biologicalSequenceEntity, false);

        NodeSet<OWLClass> SOsubClassNodes = SOreasoner.getSubClasses(biologicalSequenceEntity, false);

        // Compare the subclass nodes and report on the first discrepancy. Must use iterators since I can't index a
        // NodeSet directly.
        Iterator<Node<OWLClass>> MSOiterator = MSOsubClassNodes.iterator();

        Iterator<Node<OWLClass>> SOiterator = SOsubClassNodes.iterator();

        while (MSOiterator.hasNext() || SOiterator.hasNext()) {

            // Get the current nodes.
            Node<OWLClass> MSOnode = MSOiterator.next();

            Node<OWLClass> SOnode = SOiterator.next();

            // Compare the nodes.
            if (!MSOnode.equals(SOnode)) {

                System.out.println("MSO node: " + MSOnode.toString());

                System.out.println("SO node: " + SOnode.toString());

                break;

            }

            // We may have node sets of unequal size. Therefore we must check if we are at the end of either of them.
            if (!MSOiterator.hasNext() && SOiterator.hasNext()) {

                System.out.println("MSO hierarchy is smaller than SO hierarchy.");

                break;

            }

            if (!SOiterator.hasNext() && MSOiterator.hasNext()) {

                System.out.println("SO hierarchy is smaller than MSO hierarchy.");

                break;

            }

        }

    }

}

