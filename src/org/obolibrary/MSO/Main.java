package org.obolibrary.MSO;

import org.obolibrary.robot.IOHelper;
import org.obolibrary.robot.exceptions.InvalidReferenceException;
import org.obolibrary.robot.exceptions.OntologyLogicException;
import org.semanticweb.owlapi.formats.OBODocumentFormat;
import org.semanticweb.owlapi.model.*;

import java.io.File;
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
        ioHelper.saveOntology(MSO, "MSO_unreasoned.owl");

        // Invoke the reasoner helper to reason the MSO and save to disk.
        OWLOntology MSO_reasoned = reasonerHelper.reasonMSO(ioHelper);

        ioHelper.saveOntology(MSO_reasoned, "MSO.owl");

        // Reload the master ontology as it has been changed above due to passing by reference.
        OWLOntology master2 = ioHelper.loadOntology("master.owl");

        // Invoke generateSO.
        SOGenerator soGenerator = new SOGenerator();

        OWLOntology SO = soGenerator.generateSO(master2);

        // Save SO to disk.
        ioHelper.saveOntology(SO, "SO_unreasoned.owl");

//      Invoke the reasoner helper to reason the SO and save to disk.
        reasonerHelper.reasonSO(ioHelper);

//      Compare hierarchies of MSO and SO to make sure they are parallel.
        reasonerHelper.qualityControl(ioHelper);

        // In preparation for converting to OBO, create a DocumentFormat.
        OBODocumentFormat oboFormat = new OBODocumentFormat();

        // Create a File object for converting to OBO for the two reasoned ontologies.
        File msoOBO = new File("MSO.obo");

//      Convert the MSO to OBO format and save.
        ioHelper.saveOntology(MSO_reasoned, oboFormat, msoOBO, false);

    }
}