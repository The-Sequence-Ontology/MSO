package com.so_refactored.java;

import org.obolibrary.obo2owl.OwlStringTools;
import org.semanticweb.owlapi.change.AddAxiomData;
import org.semanticweb.owlapi.change.AxiomChangeData;
import org.semanticweb.owlapi.expression.OWLClassExpressionParser;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxParserImpl;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxPrefixNameShortFormProvider;
import org.semanticweb.owlapi.manchestersyntax.renderer.ParserException;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.*;
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLDataPropertyImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyImpl;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.semanticweb.owlapi.model.ClassExpressionType.*;
import static org.semanticweb.owlapi.vocab.OWLXMLVocabulary.OBJECT_MIN_CARDINALITY;

public class IDChanger {
    public String changeIRIs(String expression) {
        String new_expression = expression.replaceAll("MSO_", "SO_");
        return new_expression;
    }
//    private IRI changeIRI(IRI iri) {
//        String ID = iri.toString();
//        if (iri.toString().contains("MSO_")) {
//            String[] IDs = ID.split("MSO_");
//            String new_ID = "http://purl.obolibrary.org/obo/SO_" + IDs[1];
//            return IRI.create(new_ID);
//        } else {
//            return iri;
//        }
//    }

    public OWLClass changeClassID(OWLClass cls) {
        String iri = cls.getIRI().toString();
//        IRI new_iri = this.changeIRI(iri);
        return new OWLClassImpl(IRI.create(this.changeIRIs(iri)));
    }

    public OWLObjectProperty changePropertyID(OWLObjectProperty objectProperty) {
        String iri = objectProperty.getIRI().toString();
//        IRI new_iri = this.changeIRI(iri);
        return new OWLObjectPropertyImpl(IRI.create(this.changeIRIs(iri)));
    }

    public OWLDataProperty changePropertyID(OWLDataProperty property) {
        String iri = property.getIRI().toString();
//        IRI new_iri = this.changeIRI(iri);
        return new OWLDataPropertyImpl(IRI.create(this.changeIRIs(iri)));
    }

    public OWLAnnotationProperty changePropertyID(OWLAnnotationProperty property, OWLDataFactory df) {
        String iri = property.getIRI().toString();
//        IRI new_iri = this.changeIRI(iri);
        return df.getOWLAnnotationProperty(IRI.create(this.changeIRIs(iri)));
    }

    public OWLAnnotationAssertionAxiom ChangeAnnotation(OWLAnnotationAssertionAxiom ann, OWLDataFactory df) {
        String iri = ann.getSubject().toString();
//        IRI new_iri = this.changeIRI(iri);
        OWLAnnotation annotation = ann.getAnnotation();
        OWLAnnotationProperty annotationProperty = annotation.getProperty();
        OWLAnnotationValue annotationValue = annotation.getValue();
        OWLAnnotationProperty annotationPropertyChanged = this.changePropertyID(annotationProperty, df);
        OWLAnnotation annotationChanged = df.getOWLAnnotation(annotationPropertyChanged, annotationValue);
        return df.getOWLAnnotationAssertionAxiom(IRI.create(this.changeIRIs(iri)), annotationChanged);
    }

    public Set<OWLAxiom> changeSubClassOf(OWLClass cls, OWLOntology o, OWLDataFactory df) {
        Set<OWLAxiom> newAxioms = new HashSet<>();
        // Create a set of OWLSubClassOfAxioms.
        Set<OWLSubClassOfAxiom> axioms = o.getSubClassAxiomsForSubClass(cls);
        // Change mso ID to so ID
        for (OWLSubClassOfAxiom axiom : axioms) {
            System.out.println(axiom.toString());
            List<AxiomChangeData> changes = this.parseClassExpression(axiom , o);
            if (changes.size() == 2) {
                System.out.println(changes.get(1).toString());
                newAxioms.add(changes.get(1).getAxiom());
            }
        }
        return newAxioms;
    }

//    public Set<OWLSubClassOfAxiom> changeSubClassOf(OWLClass cls, OWLOntology o, OWLDataFactory df) {
//        // Create a set of OWLSubClassOfAxioms.
//        Set<OWLSubClassOfAxiom> axioms = new HashSet<>();
//        // Change mso ID to so ID
//        OWLClass new_cls = this.changeClassID(cls);
//        Collection<OWLClassExpression> superclasses = EntitySearcher.getSuperClasses(cls, o);
//        for (OWLClassExpression superclass : superclasses) {
//            OWLClassExpression superclass_SO = this.classExpressionVisitor(superclass);
//            axioms.add(df.getOWLSubClassOfAxiom(new_cls, superclass_SO));
//        }
//        return axioms;
//    }

    public Set<OWLSubObjectPropertyOfAxiom> changeSubObjectPropertyOf(OWLObjectProperty property, OWLOntology o, OWLDataFactory df) {
        // Create a set of OWLSubPropertyOfAxioms.
        Set<OWLSubObjectPropertyOfAxiom> axioms = new HashSet<>();
        // Change mso ID to so ID
        OWLObjectProperty new_property = this.changePropertyID(property);
        Collection<OWLObjectPropertyExpression> superproperties = EntitySearcher.getSuperProperties(property, o);
        for (OWLObjectPropertyExpression superproperty : superproperties) {
            OWLObjectProperty super_asProperty = superproperty.asOWLObjectProperty();
            OWLObjectProperty superProperty_SO = this.changePropertyID(super_asProperty);
            axioms.add(df.getOWLSubObjectPropertyOfAxiom(new_property, superProperty_SO));
        }
        return axioms;
    }

    public List<AxiomChangeData> parseClassExpression(OWLSubClassOfAxiom axiom, OWLOntology o) {
        OWLOntologyManager manager = o.getOWLOntologyManager();
        OWLDataFactory df= manager.getOWLDataFactory();
        OWLObjectTransformer<IRI> replacer = new OWLObjectTransformer<>((x)-> true, (input) -> {
            String newExpression = this.changeIRIs(input.toString());
            return IRI.create(newExpression);
        }, df, IRI.class);
        return replacer.change(axiom);

    }

//    public OWLClassExpression classExpressionVisitor(OWLClassExpression expression) {
//        // Get the class expression type on which to switch.
//        ClassExpressionType type = expression.getClassExpressionType();
//        switch (type) {
//            case OWL_CLASS:
//                OWLClass exp_asClass = expression.asOWLClass();
//                OWLClass exp_SO = this.changeClassID(exp_asClass);
//                return exp_SO;
//
//            case OBJECT_SOME_VALUES_FROM:
//                OWLObjectSomeValuesFrom propExp = (OWLObjectSomeValuesFrom) expression;
//                OWLObjectProperty property = (OWLObjectProperty) propExp.getProperty();
//                OWLObjectProperty propertySO = this.changePropertyID(property);
//                break;
//
//            case OBJECT_ALL_VALUES_FROM:
//                break;
//
//            case OBJECT_MIN_CARDINALITY:
//                break;
//
//            case OBJECT_MAX_CARDINALITY:
//                break;
//
//            case OBJECT_EXACT_CARDINALITY:
//                break;
//
//            case OBJECT_HAS_VALUE:
//                break;
//
//            case OBJECT_HAS_SELF:
//                break;
//
//            case DATA_SOME_VALUES_FROM:
//                break;
//
//            case DATA_ALL_VALUES_FROM:
//                break;
//
//            case DATA_MIN_CARDINALITY:
//                break;
//
//            case DATA_MAX_CARDINALITY:
//                break;
//
//            case DATA_EXACT_CARDINALITY:
//                break;
//
//            case DATA_HAS_VALUE:
//                break;
//
//            case OBJECT_INTERSECTION_OF:
//                break;
//
//            case OBJECT_UNION_OF:
//                break;
//
//            case OBJECT_COMPLEMENT_OF:
//                break;
//
//            case OBJECT_ONE_OF:
//                break;
//        }
//        return expression;
//    }
}
