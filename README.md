# Molecular Sequence Ontology (MSO)

This is the primary repository for the Molecular Sequence Ontology (MSO), an ontology in development for the description of the physical molecules that contain the biologically relevant sequence represented in the Sequence Ontology (SO), and their properties.

*MSO* entities, being *independent continuants*, are the bearers of *SO* entities, which are *generically dependent continuants*.

The taxonomy of the SO has been significantly refactored and designed to be as parallel as possible to that of the MSO.

Both ontologies are available in OWL and OBO format (MSO.owl, MSO.obo, SO.owl, SO.obo). The OBO files can be opened in OBO-Edit (if one really must do so), but make sure that "Allow dangling references" is checked in the Advanced settings when loading and ignore non-critical warnings. Also, cross-products will not display properly.

The ontology is also available as a merge of MSO and SO. Here, the relation of "generically depends on" for each applicable SO entity on its corresponding MSO bearer is explicitly asserted.

The ontologies are fully reasoned with all axioms asserted. However, we also provide both ontologies unreasoned (MSO_unreasoned.owl and SO_unreasoned.owl) in case users wish to import them into their own ontologies for reasoning.

We consider the ontologies to be in the beta-testing phase. We are in the process of integrating terms from the SO currently in use that were added since it branched from our working copy, as well as a number of other tasks. But the main structure of both ontologies is in place.

### Guidelines for users

If you are annotating sequences in a database, abstracted from a molecular context and likely represented as a string of characters, use the SO.

If you are describing DNA, RNA, proteins etc. as molecules engaged in chemical events, use the MSO.

### MSO short paper

We presented a paper at the ISMB 2018 conference (Bio-ontologies community of interest) on the design
and programmatic algorithm for generating the MSO and SO from the master file.  It can be found in the
repository root directory, [here](https://github.com/The-Sequence-Ontology/MSO/blob/master/MSO_short_paper-ISMB_2018.pdf).

## Contact

Please use the [Issues](https://github.com/The-Sequence-Ontology/MSO/issues) to contact the curators with questions, concerns, and anything else of relevance.

