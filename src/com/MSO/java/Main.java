package com.MSO.java;

import org.obolibrary.robot.IOHelper;
import org.obolibrary.robot.exceptions.InvalidReferenceException;
import org.obolibrary.robot.exceptions.OntologyLogicException;
import org.semanticweb.owlapi.model.*;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, OWLOntologyCreationException, OntologyLogicException, InvalidReferenceException {

        // Create an IOHelper to easily load and save ontologies to text files.
        IOHelper ioHelper = new IOHelper();

        // Create a ReasonerHelper to reason ontologies before saving them.
        ReasonerHelper reasonerHelper = new ReasonerHelper();

        // Use the IOHelper to conveniently load the master ontology from a file with a single line of code.
        OWLOntology master = ioHelper.loadOntology("master.owl");

        // Invoke generateMSO.
        MSOGenerator msoGenerator = new MSOGenerator();

        OWLOntology MSO = msoGenerator.generateMSO(master);

        // Save MSO to disk.
        ioHelper.saveOntology(MSO, "files/MSO.owl");

        // Invoke the reasoner helper to reason the MSO and save to disk.
        OWLOntology MSO_reasoned = reasonerHelper.reasonMSO(ioHelper);

        ioHelper.saveOntology(MSO_reasoned,"files/MSO_reasoned.owl");

        // Reload the master ontology as it has been changed above due to passing by reference.
        OWLOntology master2 = ioHelper.loadOntology("master.owl");

        // Invoke generateSO.
        SOGenerator soGenerator = new SOGenerator();

        OWLOntology SO = soGenerator.generateSO(master2);

        // Save SO to disk.
        ioHelper.saveOntology(SO, "files/SO.owl");

        // Invoke the reasoner helper to reason the SO and save to disk.
        OWLOntology SO_reasoned = reasonerHelper.reasonSO(ioHelper);

        ioHelper.saveOntology(SO_reasoned, "files/SO_reasoned.owl");

        // Compare hierarchies of MSO and SO to make sure they are parallel.
        reasonerHelper.qualityControl(ioHelper);

    }
}