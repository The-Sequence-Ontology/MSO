# MSO

This is the primary repository for the Molecular Sequence Ontology (MSO), an ontology in development for the description of the physical molecules that contain biologically relevant sequence, and their properties. Eventually we hope to automatically generated a refactored version of the Sequence Ontology (SO) alongside the MSO from a single master file. This is still a work in progress, and the repository also contains the OWL API project for writing the program for the auto-generate task. The output files (MSO.owl and SO_refactored.owl) are not yet ready for release.

In the meantime, the community is invited to explore and comment on the master file, "master.owl", located in the root of the repository. This ontology must be reasoned over with FaCT++ or JFact. The inferred ontology is essentially what is proposed for the final MSO, with a refactored, parallel SO to be derived from it. We welcome feedback on the consistency and appropriateness of this structure for the MSO and SO.

Comments on the code are also welcome. It is not completely cleaned up yet (for example, some duplicated code that should be resolved into separate functions). But more importantly, as we are new to the OWL API, and there are multiple ways to do things, any suggestions from those who are more proficient as to best practices or the most convenient methods to use would be greatly appreciated.

### Stable release versions

The latest version of the Sequence Ontology is still to be found at:

https://github.com/The-Sequence-Ontology/SO-Ontologies

## Contact
Please use the [Issues](https://github.com/The-Sequence-Ontology/MSO/issues) tab to provide feedback on the proposed ontology design in "master.owl" and its suitability for use as the MSO and for a refactored, parallel SO. Also please create issues to give feedback on the OWL API code.

