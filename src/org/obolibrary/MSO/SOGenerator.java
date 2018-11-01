package org.obolibrary.MSO;

import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.change.ConvertEquivalentClassesToSuperClasses;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.OWLObjectTransformer;
import org.semanticweb.owlapi.util.OWLOntologyIRIChanger;
import uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplString;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectSomeValuesFromImpl;

import java.util.*;

//import static java.util.Collections.singleton;

class SOGenerator {
    OWLOntology generateSO(OWLOntology master) {

        //region This region prepares all objects necessary for the generation of SO from the master file

        // Create an ontology manager for many ontology editing tasks in the OWL API.
        OWLOntologyManager manager = master.getOWLOntologyManager();

        // Create a data factory for many information retrieval and editing tasks in the OWL API.
        OWLDataFactory dataFactory = manager.getOWLDataFactory();

        // These IRIs have been already decided and should not change at all. The following IRIs all fall into that
        // category.  For example, onlyInMSOIRI will always have the IRI = MSO_3100075.

        // Retrieve the IRI for the only represented in MSO boolean annotation. This IRI should be static.
        IRI onlyInMSOIRI = IRI.create("http://purl.obolibrary.org/obo/MSO_3100075");

        // Retrieve the IRI for the only represented in SO boolean annotation. This IRI should be static.
        IRI onlyInSOIRI = IRI.create("http://purl.obolibrary.org/obo/MSO_3100074");

        // Get the id annotation property. The IRI used should be static.
        OWLAnnotationProperty idAnnotationProperty = dataFactory.getOWLAnnotationProperty(IRI.create
                ("http://www.geneontology.org/formats/oboInOwl#id"));

        // Retrieve the IRI for the generically depends on object property. This IRI should be static.
        // This property needs to now be imported from the Relations Ontology so this value will change.
        IRI genericallyDependsIRI = IRI.create("http://purl.obolibrary.org/obo/SO_6000000");

        // Retrieve the IRI for generically dependent continuant. This IRI should be static.
        IRI genericallyDependentContinuantIRI = IRI.create("http://purl.obolibrary.org/obo/BFO_0000031");

        // Get generically dependent continuant as a class.
        OWLClass genericallyDependentContinuant = dataFactory.getOWLClass(genericallyDependentContinuantIRI);

        // Retrieve the IRI for specifically dependent continuant. This IRI should be static.
        IRI specificallyDependentContinuantIRI = IRI.create("http://purl.obolibrary.org/obo/BFO_0000020");

        // Get specifically dependent continuant as a class.
        OWLClass specificallyDependentContinuant = dataFactory.getOWLClass(specificallyDependentContinuantIRI);

        // Retrieve the IRI for biological sequence entity in the SO.
        IRI biologicalSequenceEntityIRI = IRI.create("http://purl.obolibrary.org/obo/SO_3000265");

        // Get biological sequence entity as a class.
        OWLClass biologicalSequenceEntity = dataFactory.getOWLClass(biologicalSequenceEntityIRI);

        // Retrieve the AnnotationProperty object for the only represented in SO, MSO, and genericallyDepends properties.
        // Create empty objects to store the references later.
        // In a future iteration, change this process of retrieving annotation properties to use the PrefixManager.
        OWLAnnotationProperty onlyInSO = null;
        OWLAnnotationProperty onlyInMSO = null;
        OWLObjectProperty genericallyDepends = null;

        // Loop through all the annotation properties.
        for (OWLAnnotationProperty property : master.getAnnotationPropertiesInSignature()) {

            // If the IRI of the property is equal to onlyInSOIRI, store the reference.
            if (property.getIRI().equals(onlyInSOIRI)) {

                onlyInSO = property;
            }

            // If the IRI of the property is equal to onlyInMSOIRI, store the reference.
            if (property.getIRI().equals(onlyInMSOIRI)) {

                onlyInMSO = property;
            }
        }

        // Loop through all object properties to find generically depends on.
        for (OWLObjectProperty property : master.getObjectPropertiesInSignature()) {

            // If the IRI of the property is equal to genericallyDependsIRI, store the reference
            if (property.getIRI().equals(genericallyDependsIRI)) {

                genericallyDepends = property;
            }
        }

        // Create an IRI for the refactored sequence ontology that will be saved to disk as the output.
        IRI idSO = IRI.create("http://purl.obolibrary.org/obo/SO.owl");

        // Create a set that will store classes that have counterparts in both ontologies. We will need the later when
        // we add generic dependence relations.
        Set<OWLClass> overlappingClasses = new HashSet<>();

        // Retrieve all of the classes in the master ontology.
        Set<OWLClass> masterClasses = master.getClassesInSignature();

        // Create a set to store those classes only in SO.
        Set<OWLClass> onlyInSOClasses = new HashSet<>();

        //endregion


        //region This region generates the SO ontology from the objects prepared in the previous section.

        // Loop through all the classes in the master ontology and remove those that contain the onlyInMSO
        // annotation. As we proceed with transforming the master ontology into the SO, we'll refer to it as the
        // working ontology.
        for (OWLClass cls : masterClasses) {

            // Retrieve all the annotations on the current class. Examine them one by one.
            // We'll need to keep track of our place in the collection of annotations so that we can tell when
            // we're at the last item. So use an iterator in the loop.
            for (Iterator<OWLAnnotation> iterator = EntitySearcher.getAnnotations(cls, master).iterator();
                 iterator.hasNext(); ) {

                //Get the current annotation.
                OWLAnnotation ann = iterator.next();

                // Determine what type of annotation property is associated with the current annotation.
                OWLAnnotationProperty annProp = ann.getProperty();

                // If it is the id annotation, take the opportunity to change it if the class is a native MSO class
                // (e.g. its IRI starts with the 'MSO_' prefix.
                if (annProp.equals(idAnnotationProperty)) {

                    // Call the function to change the ID.
                    changeIDAnnotation(master, cls, ann);
                }

                // Determine if it is the onlyInMSO property.
                if (annProp.equals(onlyInMSO)) {

                    // To remove the class completely from the ontology, retrieve all axioms referencing it and remove
                    // them one by one.
                    for (OWLAxiom MSOAxiom : EntitySearcher.getReferencingAxioms(cls, master)) {

                        RemoveAxiom removeAxiom = new RemoveAxiom(master, MSOAxiom);

                        manager.applyChange(removeAxiom);
                    }

                    // We have removed the class so there's no need to continue checking annotations. Move to the next
                    // class.
                    break;

                    // If the class is only in MSO, it has no feature from SO that inheres in it, so we don't need to
                    // store its numerical ID for adding generic dependence relations to SO later.
                }

                // On the other hand, if the property is onlyInSO, we should break too, as it won't have the onlyInMSO
                // property (unless the curator seriously screwed up). We want to keep track of classes that have neither
                // annotation. That means they are classes to which generic dependence from the SO counterpart to the
                // MSO counterpart should be added. We will also add this class to a set of only in SO classes
                // because we need to prevent them from losing their equivalent to axioms later on.
                if (annProp.equals(onlyInSO)) {

                    onlyInSOClasses.add(cls);

                    break;
                }

                // If we've reached this point, it means the current annotation is not the onlyInMSOIRI annotation, nor
                // the onlyInSO annotation. If it is the last annotation in the collection, that means this class has
                // counterparts in both MSO and SO.  We need to check if we're at the end of the annotation collection,
                // and if we are, we need to store the class in our "overlapping" set.
                if (!iterator.hasNext()) {

                    // Before we store the class, we have to exclude classes that have been imported from other
                    // ontologies and don't belong strictly to MSO. So we'll include only those classes that have "MSO"
                    // in their IRIs.
                    String iri = cls.getIRI().toString();

                    if (iri.contains("MSO_")) {

                        // It's safe, we can now store the class.
                        overlappingClasses.add(cls);
                    }

                }
            }

        }

        /* Use an OWLObjectTransformer to replace every "MSO" in an IRI to "SO". We are currently working on the
           design principle that classes that are counterparts of each other in MSO and SO will have the same IRI number
           but differ in the namespace prefix, i.e. SO instead of MSO. */

        OWLObjectTransformer<IRI> replacer = new OWLObjectTransformer<>((x) -> true,
                (input) -> {

                    // Every object in the ontology is checked. If it is of type IRI, execute the code block.
                    if (input != null) {

                        // Replace "MSO" with "SO" in the IRI string.
                        String newIRI = input.toString().replaceAll("MSO_", "SO_");

                        // Avoid a null pointer exception.

                        return IRI.create(newIRI);
                    }

                    return null;
                },

                // Required argument for the constructor.
                dataFactory,

                // Required argument specifying the type of objects to transform.
                IRI.class);

        // The OWLObjectTransformer returns its replacements as list of OWLOntologyChange objects that we must apply to
        // the ontology.
        List<OWLOntologyChange> changes = replacer.change(master);

        // Apply the IRI replacements.
        manager.applyChanges(changes);

        /* We have to now retrieve the set of all specifically dependent continuants to ensure that they do not get a generic dependence axiom added to them nor have their equivalent to axioms removed. We will use a reasoner to obtain all subclasses of "specifically dependent continuant" and add them to our set. */
        OWLReasonerConfiguration config = new SimpleConfiguration();

        OWLReasonerFactory reasonerFactory = new ReasonerFactory();

        OWLReasoner reasoner = reasonerFactory.createReasoner(master, config);

        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

        NodeSet<OWLClass> subClassNodes = reasoner.getSubClasses(specificallyDependentContinuant, false);

        Set<OWLClass> specificallyDependentContinuants = subClassNodes.getFlattened();

        // To get rid of the reasoned ontology revert back to the root.
        master = reasoner.getRootOntology();

        // Dispose of the reasoner.
        reasoner.dispose();

        // Since we will use the generically depends on relation to create equivalentTo axioms and use those to
        // classify the SO, it's important to remove all other equivalentTo axioms that are carried over from the
        // master file to avoid logical conflicts.

        // If there are equivalentTo axioms, we will change them to SubClassOf axioms instead.

        // Update the set of classes in the signature of the working ontology since it has changed since it was first
        // loaded.
        for (OWLClass clsSO : master.getClassesInSignature()) {

            // If the class is a specifically dependent continuant, skip this step.
            if (specificallyDependentContinuants.contains(clsSO)) continue;

            // Global changes to the IRIs (MSO_ to SO_) hadn't yet been done when the onlyInSOClasses set was generated.
            // If the class is only in SO, skip it also.
            IRI clsMSOIRI = IRI.create(clsSO.getIRI().toString().replace("SO_", "MSO_"));

            OWLClass clsMSO = dataFactory.getOWLClass(clsMSOIRI);

            if (onlyInSOClasses.contains(clsMSO)) continue;

            // If this class is the biological sequence entity, assert that it is a subclass of generically dependent continuant for proper classification of all SO entities as generically dependent continuants under this class.
            if (clsSO.equals(biologicalSequenceEntity)) {

                OWLSubClassOfAxiom subClassOfAxiom = dataFactory.getOWLSubClassOfAxiom(clsSO,
                        genericallyDependentContinuant);

                AddAxiom addAxiom = new AddAxiom(master, subClassOfAxiom);

                manager.applyChange(addAxiom);

            }

            // Use the ConvertEquivalentClassesToSuperClasses object to change all Equivalent Class axioms to SubClassOf
            // axioms for this class.
            ConvertEquivalentClassesToSuperClasses converter = new ConvertEquivalentClassesToSuperClasses(manager
                    .getOWLDataFactory(), clsSO, manager.getOntologies(), master, false);

            // Apply the generated changes from this converter.
            manager.applyChanges(converter.getChanges());

        }

        /* Now we add generically_depends_on equivalent class axioms to each class in our working ontology.
           We have to build up the equivalent class axiom that will assert that each SO class, as subject, depends on
           some MSO class with the same ID number. We'll need to first create an OWLObjectSomeValuesFrom expression,
           containing a reference to the property and a filler with the target MSO class. */

        // Loop through the set of overlapping classes.
        for (OWLClass clsMSO : overlappingClasses) {

            // Avoid null pointer exception. Sigh.
            if (genericallyDepends != null) {
                OWLObjectSomeValuesFromImpl someValues = new OWLObjectSomeValuesFromImpl(genericallyDepends, clsMSO);

                // Retrieve the equivalent SO class.
                IRI iriSO = IRI.create(clsMSO.getIRI().toString().replace("MSO_", "SO_"));

                // Create the corresponding SO class out of the IRI above.
                OWLClass clsSO = dataFactory.getOWLClass(iriSO);

                // If this class exists as a specifically dependent continuant, continue.
                if (specificallyDependentContinuants.contains(clsSO)) continue;

                // Create the equivalent class axiom equating the SO class with the object property restriction.
                OWLEquivalentClassesAxiom axiom = dataFactory.getOWLEquivalentClassesAxiom(clsSO, someValues);

                // Create an add axiom change to add to the working ontology.
                AddAxiom addAxiom = new AddAxiom(master, axiom);

                // Add the axiom through an applied change.
                manager.applyChange(addAxiom);
            }

        }

        //endregion


        //region This section prepares the ontology for output.

        /* Now that all of our changes to the master ontology has given us the final form of SO, we must save this
        ontology not as the master, but as SO. To do this we change the ontology's IRI and label before returning it. */

        // Retrieve the working ontology's manager to change its IRI.
        OWLOntologyIRIChanger IRIchanger = new OWLOntologyIRIChanger(manager);

        // Get OWLOntologyChange object to effect a change in IRI.
        List<OWLOntologyChange> IRIchanges = IRIchanger.getChanges(master, idSO);

        // Apply the change. The working ontology's IRI is now the SO's IRI.
        manager.applyChanges(IRIchanges);

        // Return the complete SO to main.
        return master;

        //endregion

    }

    // This is a method to replace the ID annotation that contains a value of 'MSO:XXXXXXX' to one of 'SO:XXXXXXX'.
    private void changeIDAnnotation(OWLOntology ont, OWLClass cls, OWLAnnotation ann) {

        // If this is not a native MSO class (e.g. it is imported from another ontology), simply return without changes.
        if (!cls.getIRI().toString().contains("MSO_")) return;

        // Get the ontology manager.
        OWLOntologyManager manager = ont.getOWLOntologyManager();

        // Get the manager datafactory.
        OWLDataFactory factory = manager.getOWLDataFactory();

        // Get the id annotation property. The IRI used should be static.
        OWLAnnotationProperty idAnnotationProperty = factory.getOWLAnnotationProperty(IRI.create
                ("http://www.geneontology.org/formats/oboInOwl#id"));

        // Get the IRI of the class as a string.
        String iri = cls.getIRI().toString();

        // First remove the existing id annotation.  Get the assertion axiom.
        OWLAnnotationAssertionAxiom annAxiom = factory.getOWLAnnotationAssertionAxiom(cls.getIRI(), ann);

        // Create a remove axiom change and apply it.
        RemoveAxiom removeAxiom = new RemoveAxiom(ont, annAxiom);

        manager.applyChange(removeAxiom);

        // Split the iri (as a string) along the MSO_ part, so that the part to the right will be the numeric ID only.
        String[] split = iri.split("MSO_");

        // Retrieve the numeric portion only.
        String numericID = split[1];

        // Create a new string with the SO: prefix.
        String newID = "SO:" + numericID;

        // Create an OWLLiteral from this string of the new ID.
        OWLLiteral idLiteral = new OWLLiteralImplString(newID);

        // Create a new annotation from this literal that contains the string of the new ID.
        OWLAnnotation newIDAnnoation = factory.getOWLAnnotation(idAnnotationProperty, idLiteral);

        // Create the assertion axiom for the new annotation containing the correct ID string.
        OWLAnnotationAssertionAxiom newAnnAxiom = factory.getOWLAnnotationAssertionAxiom(cls.getIRI(), newIDAnnoation);

        manager.applyChange(new AddAxiom(ont, newAnnAxiom));

    }
}

