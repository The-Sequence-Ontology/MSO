package org.obolibrary.MSO;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.OWLOntologyIRIChanger;

import java.util.List;
import java.util.Set;

class MSOGenerator {
    OWLOntology generateMSO(OWLOntology master) {

        //region This region prepares all objects necessary for the generation of MSO from the master file

        // Create an ontology manager for many ontology editing tasks in the OWL API.
        OWLOntologyManager manager = master.getOWLOntologyManager();

        // Retrieve the IRI for the only represented in MSO boolean annotation. This IRI should be static.
//        IRI onlyInMSOIRI = IRI.create("http://purl.obolibrary.org/obo/MSO_3100075");

        // Retrieve the IRI for the only represented in SO boolean annotation. This IRI should be static.
        IRI onlyInSOIRI = IRI.create("http://purl.obolibrary.org/obo/MSO_3100074");

        // Retrieve the AnnotationProperty object for the only represented in SO, MSO, and genericallyDepends properties.
        // Create empty objects to store the references later.
        OWLAnnotationProperty onlyInSO = null;
//        OWLAnnotationProperty onlyInMSO = null;

        // Loop through all the annotation properties.
        for (OWLAnnotationProperty property : master.getAnnotationPropertiesInSignature()) {

            // If the IRI of the property is equal to onlyInSOIRI, store the reference.
            if (property.getIRI().equals(onlyInSOIRI)) {

                onlyInSO = property;
            }

            // If the IRI of the property is equal to onlyInMSOIRI, store the reference.
//            if (property.getIRI().equals(onlyInMSOIRI)) {
//
//                onlyInMSO = property;
//            }
        }

        // Create an IRI for the refactored sequence ontology that will be saved to disk as the output.
        IRI idMSO = IRI.create("http://purl.obolibrary.org/obo/MSO.owl");

        // Retrieve all of the classes in the master ontology.
        Set<OWLClass> masterClasses = master.getClassesInSignature();

        //endregion

        //region This region generates the MSO ontology from the objects prepared in the previous section.

        // Loop through all the classes in the master ontology and remove those that contain the onlyInSOIRI
        // annotation. As we proceed with transforming the master ontology into the MSO, we'll refer to it as the
        // working ontology.
        for (OWLClass cls : masterClasses) {

            // Retrieve all the annotations on the current class. Examine them one by one.
            for (OWLAnnotation ann : EntitySearcher.getAnnotations(cls, master)) {

                // Determine what type of annotation property is associated with the current annotation.
                OWLAnnotationProperty annProp = ann.getProperty();

                // Determine if it is the onlyInSO property.
                if (annProp.equals(onlyInSO)) {

                    // To remove the class completely from the ontology, retrieve all axioms referencing it and remove
                    // them one by one.
                    for (OWLAxiom SOAxiom : EntitySearcher.getReferencingAxioms(cls, master)) {

                        RemoveAxiom removeAxiom = new RemoveAxiom(master, SOAxiom);

                        master.getOWLOntologyManager().applyChange(removeAxiom);
                    }

                    // We have removed the class so there's no need to continue checking annotations. Move to the next
                    // class.
                    break;
                }
            }
        }

//        // The remaining classes that are not flagged as onlyInSOIRI are retained to continue the generation process.
//        // Thus, we do not need the "only in" annotations anymore and should remove them.
//
//        // Avoid a null reference.
//        if (onlyInSO != null) {
//
//            // Loop through all axioms that reference the only in SO annotation property and remove them.
//            for (OWLAxiom onlyInSOAxiom : EntitySearcher.getReferencingAxioms(onlyInSO, master)) {
//
//                RemoveAxiom removeAxiom = new RemoveAxiom(master, onlyInSOAxiom);
//
//                master.getOWLOntologyManager().applyChange(removeAxiom);
//            }
//        }
//
//        // Avoid a null reference.
//        if (onlyInMSO != null) {
//
//            // Loop through all axioms that reference the only in MSO annotation property and remove them.
//            for (OWLAxiom onlyInMSOAxiom : EntitySearcher.getReferencingAxioms(onlyInMSO, master)) {
//
//                RemoveAxiom removeAxiom = new RemoveAxiom(master, onlyInMSOAxiom);
//
//                master.getOWLOntologyManager().applyChange(removeAxiom);
//            }
//        }

        //endregion

        //region This section prepares the ontology for output and saves it to a file.

        /* Now that all of our changes to the master ontology has given us the final form of MSO, we must save this
        ontology not as the master, but as MSO. To do this we change the ontology's IRI and label and save it as a
        separate file. */

        // Retrieve the working ontology's manager to change its IRI.
        OWLOntologyIRIChanger IRIchanger = new OWLOntologyIRIChanger(master.getOWLOntologyManager());

        // Get OWLOntologyChange object to effect a change in IRI.
        List<OWLOntologyChange> IRIchanges = IRIchanger.getChanges(master, idMSO);

        // Apply the change. The working ontology's IRI is now the SO's IRI.
        manager.applyChanges(IRIchanges);

        // Return the complete SO to main.
        return master;

        //endregion

    }
}
