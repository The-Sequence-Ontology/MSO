package org.obolibrary.MSO;

import org.obolibrary.robot.*;
import org.obolibrary.robot.exceptions.InvalidReferenceException;
import org.obolibrary.robot.exceptions.OntologyLogicException;
import org.semanticweb.owlapi.formats.OBODocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.OWLObjectTransformer;
import org.semanticweb.owlapi.util.OWLOntologyMerger;
import uk.ac.manchester.cs.jfact.JFactFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

class ReasonerHelper {

    OWLOntology reasonMSO(IOHelper ioHelper) throws IOException, OWLOntologyCreationException, OntologyLogicException, InvalidReferenceException {

        // Load the saved MSO.
        OWLOntology MSO = ioHelper.loadOntology("MSO_unreasoned.owl");

        // Instantiate a JFact reasoner factory to reason over MSO. Reason, then save to disk.
        OWLReasonerFactory reasonerFactory = new JFactFactory();

        Map<String, String> options = new HashMap<>();

        options.put("remove-redundant-subclass-axioms", "true");

        ReasonOperation.reason(MSO, reasonerFactory, options);

        RepairOperation.repair(MSO, ioHelper);

        // Return the reasoned MSO.
        return MSO;

    }

    void reasonSO(IOHelper ioHelper) throws IOException, OWLOntologyCreationException, OntologyLogicException, InvalidReferenceException {

        // Retrieve the IRI for the only represented in MSO boolean annotation. This IRI should be static.
        IRI onlyInMSOIRI = IRI.create("http://purl.obolibrary.org/obo/SO_3100075");

        // Load the saved SO.
        OWLOntology SO = ioHelper.loadOntology("SO_unreasoned.owl");

        OWLOntologyManager m = SO.getOWLOntologyManager();

        OWLDataFactory df = m.getOWLDataFactory();

        OWLAnnotationProperty onlyInMSO = df.getOWLAnnotationProperty(onlyInMSOIRI);

        OWLImportsDeclaration importDeclaration = df.getOWLImportsDeclaration(IRI.create
                (new File("MSO_unreasoned.owl")));

        m.applyChange(new AddImport(SO, importDeclaration));

        m.loadOntologyFromOntologyDocument(new File("MSO_unreasoned.owl"));

        OWLReasonerFactory reasonerFactory = new JFactFactory();

        ReasonOperation.reason(SO, reasonerFactory);

        OWLOntologyMerger merger = new OWLOntologyMerger(m);

        OWLOntology merged = merger.createMergedOntology(m, IRI.create
                ("http://purl.obolibrary.org/obo/MSO-SO.owl"));

        m.applyChange(new RemoveImport(merged, importDeclaration));

        ioHelper.saveOntology(merged, "MSO-SO_merged.owl");

        /* Now we have to add the genus for all SO classes to the 'generically depends on' equivalent
        class axioms as a conjunction so that it gets translated to the OBO file properly. We will need to use the
        the immediate superclasses as the genera. */

        // The merged ontology may have different managers and data factories so retrieve them.
        OWLOntologyManager mergedManager = merged.getOWLOntologyManager();

        OWLDataFactory mergedDataFactory = mergedManager.getOWLDataFactory();

        // Retrieve the object property for 'generically depends on'.
        OWLObjectProperty GDO = mergedDataFactory.getOWLObjectProperty
                (IRI.create("http://purl.obolibrary.org/obo/SO_6000000"));

        // Get all the merged classes.
        Set<OWLClass> mergedClasses = merged.getClassesInSignature();

        // Run a loop and check if they have equivalent classes of type object some properties from.
        for (OWLClass cls : mergedClasses) {

            // Get all equivalent classes on this class.
            Collection<OWLClassExpression> equivs = EntitySearcher.getEquivalentClasses(cls, merged);

            // Loop through the collection and see if any are of type object some properties from.
            for (OWLClassExpression expression : equivs) {

                if (expression.getClassExpressionType() == ClassExpressionType.OBJECT_SOME_VALUES_FROM){

                    // Cast the expression as an object some values from expression.
                    OWLObjectSomeValuesFrom objectExpression = (OWLObjectSomeValuesFrom) expression;

                    // Is the object property generically depends on? (it should be in all cases really)
                    if (objectExpression.getProperty().equals(GDO)) {

                        // Create new expressions which are conjunctions of the genera and the object some
                        // values from expression.

                        // Get the superclasses.
                        Collection<OWLClassExpression> superclasses = EntitySearcher.getSuperClasses(cls, merged);

                        // Add the generically depends on expression itself to the collection.
                        superclasses.add(objectExpression);

                        // Now get the conjuncts as a set.
                        Set<OWLClassExpression> conjuncts = new HashSet<>(superclasses);

                        // Get the intersectionOf expression for all the conjuncts.
                        OWLObjectIntersectionOf intersectionOf = mergedDataFactory.
                                getOWLObjectIntersectionOf(conjuncts);

                        // Get the axiom asserting equivalence between the class and this intersection expression.
                        OWLEquivalentClassesAxiom axiom = mergedDataFactory.getOWLEquivalentClassesAxiom
                                (cls, intersectionOf);

                        // Add the axiom to the ontology.
                        mergedManager.applyChange(new AddAxiom(merged, axiom));

                        // Get the remove axiom for the original equivalent class.
                        RemoveAxiom removeAxiom = new RemoveAxiom
                                (merged, mergedDataFactory.getOWLEquivalentClassesAxiom(cls, objectExpression));

                        // Remove it.
                        mergedManager.applyChange(removeAxiom);

                    }
                }


            }
        }



        OBODocumentFormat format = new OBODocumentFormat();

        File soReasonedOBO = new File("MSO-SO_merged.obo");

        ioHelper.saveOntology(merged, format, soReasonedOBO, false);

        m.applyChange(new RemoveImport(SO, importDeclaration));

        m.removeOntology(Objects.requireNonNull(m.getOntology
                (IRI.create("http://purl.obolibrary.org/obo/MSO.owl"))));

        Set<OWLClass> soClasses = SO.getClassesInSignature();

        for (OWLClass cls : soClasses) {

            IRI iri = cls.getIRI();

            if (iri.toString().contains("MSO_")) {

                for (OWLAxiom MSOAxiom : EntitySearcher.getReferencingAxioms(cls, SO)) {

                    RemoveAxiom removeAxiom = new RemoveAxiom(SO, MSOAxiom);

                    m.applyChange(removeAxiom);
                }

            }

            for (OWLAnnotation annotation : EntitySearcher.getAnnotations(cls, SO)) {

                if (annotation.getProperty().equals(onlyInMSO)) {

                    for (OWLAxiom MSOAxiom : EntitySearcher.getReferencingAxioms(cls, SO)) {

                        RemoveAxiom removeAxiom = new RemoveAxiom(SO, MSOAxiom);

                        m.applyChange(removeAxiom);
                    }
                }
            }


        }

        ioHelper.saveOntology(SO, "SO_refactored.owl");

        File soSoloOBO = new File("SO_refactored.obo");

        ioHelper.saveOntology(SO, format, soSoloOBO, false);

    }

    void qualityControl(IOHelper ioHelper) throws IOException {

        // Create a file and file writer for the output.
        FileWriter writer = new FileWriter("quality_control.txt");

        // Retrieve the IRI for biological sequence entity in the SO.
        IRI biologicalSequenceEntityIRI = IRI.create("http://purl.obolibrary.org/obo/SO_3000265");

        // Load the saved MSO.
        OWLOntology MSO_reasoned = ioHelper.loadOntology("MSO.owl");

        // Load the reasoned SO.
        OWLOntology SO_reasoned = ioHelper.loadOntology("SO_refactored.owl");

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

        // Retrieve the node sets for all descendants of biological sequence entity for SO and MSO.
        NodeSet<OWLClass> MSOsubClassNodes = MSOreasoner.getSubClasses(biologicalSequenceEntity, false);

        NodeSet<OWLClass> SOsubClassNodes = SOreasoner.getSubClasses(biologicalSequenceEntity, false);

        // Retrieve the nodes as flattened sets as we will only be working with direct subclasses from now on.
        Set<OWLClass> MSOsubClasses = MSOsubClassNodes.getFlattened();

        Set<OWLClass> SOsubClasses = SOsubClassNodes.getFlattened();

        // Also retrieve the node sets for the direct descendents only of biological sequence entity for SO and MSO.
        NodeSet<OWLClass> MSOdirectBioSeqEntitySubclassNodes = MSOreasoner.getSubClasses(biologicalSequenceEntity, true);

        NodeSet<OWLClass> SOdirectBioSeqEntitySubclassNodes = SOreasoner.getSubClasses(biologicalSequenceEntity, true);

        // region This region reports subsumptions that exist only in the MSO

        // First write a header to the file to introduce the MSO section.
        writer.write("MSO-ONLY SUBSUMPTIONS\n\n");

        // Compare the direct subclasses of biological sequence entity in each ontology by retrieving the subclasses as a flattened set of all the entities in the NodeSet.
        Set<OWLClass> MSOdirectBioSeqEntitySubClasses1 = MSOdirectBioSeqEntitySubclassNodes.getFlattened();

        Set<OWLClass> SOdirectBioSeqEntitySubClasses1 = SOdirectBioSeqEntitySubclassNodes.getFlattened();

        // Now see if the sets are identical.  If not, we'll find out where the difference lies.
        if (!MSOdirectBioSeqEntitySubClasses1.equals(SOdirectBioSeqEntitySubClasses1)) {

            // Subtract from the MSO subclasses those that are in the SO subclasses to check for unalignment.
            MSOdirectBioSeqEntitySubClasses1.removeAll(SOdirectBioSeqEntitySubClasses1);

            // If it is the case that it is MSO that is the cause of unalignment, report the subsumption.
            if (!MSOdirectBioSeqEntitySubClasses1.isEmpty()) {

                for (OWLClass subClass : MSOdirectBioSeqEntitySubClasses1) {

                    // Get the label.
                    String subClassLabel = getLabel(subClass, MSO_reasoned);

                    // Write to file.
                    writer.write("biological sequence entity\t\t\t" + subClassLabel + "\n\n");

                }
            }

        }

        // Now the we've dealt with the top node itself, it's time to compare all other nodes. First we will check those
        // direct subsumptions that appear in MSO but not SO. Loop through the MSO classes first.
        for (OWLClass MSOClass : MSOsubClasses) {

            // Check if this class is also contained in SO.
            if (!SOsubClasses.contains(MSOClass)) continue;

            // Get all the direct subclasses of this class in the MSO.
            Set<OWLClass> MSOdirectSubClasses = MSOreasoner.getSubClasses(MSOClass, true).getFlattened();

            // Get all the direct subclasses of this class in the SO.
            Set<OWLClass> SOdirectSubClasses = SOreasoner.getSubClasses(MSOClass, true).getFlattened();

            // Get the difference between these subclasses.  Report only if the discrepancy is due to an MSO class that
            // is not a subclass of this class in the SO.
            MSOdirectSubClasses.removeAll(SOdirectSubClasses);

            // Write out to file all subsumptions only present in the MSO.
            if (!MSOdirectSubClasses.isEmpty()) {

                // Get the label for the current parent class.
                String MSOClassLabel = getLabel(MSOClass, MSO_reasoned);

                for (OWLClass subClass : MSOdirectSubClasses) {

                    // Get the label.
                    String subClassLabel = getLabel(subClass, MSO_reasoned);

                    // Write to file.
                    writer.write(MSOClassLabel + "\t\t\t" + subClassLabel + "\n\n");

                }
            }

        }

        // endregion

        // region This region reports subsumptions that are only in the SO.

        // First write a header to the file to introduce the SO section.
        writer.write("\n\nSO-ONLY SUBSUMPTIONS\n\n");

        // Compare the direct subclasses of biological sequence entity in each ontology by retrieving the subclasses as
        // a flattened set of all the entities in the NodeSet. We need to recreate the set since it has been changed in
        // the process of finding the MSO-only subsumptions.
        Set<OWLClass> MSOdirectBioSeqEntitySubClasses2 = MSOdirectBioSeqEntitySubclassNodes.getFlattened();

        Set<OWLClass> SOdirectBioSeqEntitySubClasses2 = SOdirectBioSeqEntitySubclassNodes.getFlattened();

        // Now see if the sets are identical.  If not, we'll find out where the difference lies.
        if (!SOdirectBioSeqEntitySubClasses2.equals(MSOdirectBioSeqEntitySubClasses2)) {

            // Subtract from the SO subclasses those that are in the MSO subclasses to check for unalignment.
            SOdirectBioSeqEntitySubClasses2.removeAll(MSOdirectBioSeqEntitySubClasses2);

            // If it is the case that it is SO that is the cause of unalignment, report the subsumption.
            if (!SOdirectBioSeqEntitySubClasses2.isEmpty()) {

                for (OWLClass subClass : SOdirectBioSeqEntitySubClasses2) {

                    // Get the label.
                    String subClassLabel = getLabel(subClass, SO_reasoned);

                    // Write to file.
                    writer.write("biological sequence entity\t\t\t" + subClassLabel + "\n\n");

                }
            }

        }

        // Now the we've dealt with the top node itself, it's time to compare all other nodes. First we will check those
        // direct subsumptions that appear in SO but not MSO. Loop through the SO classes first.
        for (OWLClass SOClass : SOsubClasses) {

            // Check if this class is also contained in MSO.
            if (!MSOsubClasses.contains(SOClass)) continue;

            // Get all the direct subclasses of this class in the SO.
            Set<OWLClass> SOdirectSubClasses = SOreasoner.getSubClasses(SOClass, true).getFlattened();

            // Get all the direct subclasses of this class in the MSO.
            Set<OWLClass> MSOdirectSubClasses = MSOreasoner.getSubClasses(SOClass, true).getFlattened();

            // Get the difference between these subclasses.  Report only if the discrepancy is due to an SO class that
            // is not a subclass of this class in the MSO.
            SOdirectSubClasses.removeAll(MSOdirectSubClasses);

            // Write out to file all subsumptions only present in the SO.
            if (!SOdirectSubClasses.isEmpty()) {

                // Get the label for the current parent class.
                String SOClassLabel = getLabel(SOClass, SO_reasoned);

                for (OWLClass subClass : SOdirectSubClasses) {

                    // Get the label.
                    String subClassLabel = getLabel(subClass, SO_reasoned);

                    // Write to file.
                    writer.write(SOClassLabel + "\t\t\t" + subClassLabel + "\n\n");

                }
            }

        }

        // endregion

        writer.close();
    }


    private String getLabel(OWLClass cls, OWLOntology ont) {

        // Initialize the string variable to be filled with the label.
        String label = "missing label";

        // We need a data factory to retrieve the RDFSlabel annotation property.
        OWLDataFactory df = ont.getOWLOntologyManager().getOWLDataFactory();

        // Retrieve all RDFSLabel annotations on the class and loop through them (we expect only one).
        for (OWLAnnotation annotation : EntitySearcher.getAnnotations(cls, ont, df.getRDFSLabel())) {

            // Make sure that the annotation value is a Literal.
            if (annotation.getValue() instanceof OWLLiteral) {

                // If so, cast the value as an OWLLiteral.
                OWLLiteral labelLiteral = (OWLLiteral) annotation.getValue();

                // Change the string label to the literal value.
                label = labelLiteral.getLiteral();

                // Return the label as a plain string.
                return label;

            }
        }

        return label;

    }

}





