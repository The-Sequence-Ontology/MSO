package com.so_refactored.java;

import org.obolibrary.robot.IOHelper;
import org.semanticweb.owlapi.model.OWLOntology;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {

        // Create an IOHelper to easily load and save ontologies to text files.
        IOHelper ioHelper = new IOHelper();

        // Use the IOHelper to conveniently load the master ontology from a file with a single line of code.
        OWLOntology master = ioHelper.loadOntology("master.owl");

        // Invoke generateMSO.
        MSOGenerator msoGenerator = new MSOGenerator();
        OWLOntology MSO = msoGenerator.generateMSO(master);

        // Save MSO to disk.
        ioHelper.saveOntology(MSO, "files/MSO.owl");

        // Reload the master ontology as it has been changed above due to passing by reference.
        OWLOntology master2 = ioHelper.loadOntology("master.owl");

        // Invoke generateSO.
        SOGenerator soGenerator = new SOGenerator();
        OWLOntology SO_refactored = soGenerator.generateSO(master2);

        // Save SO to disk.
        ioHelper.saveOntology(SO_refactored, "files/SO_refactored.owl");


    }
}