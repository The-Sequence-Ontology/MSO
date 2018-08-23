# Molecular Sequence Ontology (MSO)

This is the primary repository for the Molecular Sequence Ontology (MSO), an ontology in development for the description of the physical molecules that contain biologically relevant sequence, and their properties. MSO entities, being independent continuants, are the bearers of Sequence Ontology (SO) entities, which are generically dependent continuants.

Eventually we will automatically generate both the SO and the MSO from a single master file, with the SO taxonomy rebuilt to be as parallel as possible with the MSO. This is still a work in progress, and this repository also contains the OWL API project for writing the program for the auto-generate task. The output files (MSO.owl and SO_refactored.owl) are not yet ready for release.

In the meantime, the community is invited to explore and comment on the master file, "master.owl", located in the root of the repository. This ontology must be reasoned over with FaCT++ or JFact. The inferred ontology is essentially what is proposed for the final MSO, with a refactored, parallel SO to be derived from it. We welcome feedback on the consistency and appropriateness of this structure for the MSO and SO.

Comments on the code are also welcome. It is not completely cleaned up yet (for example, some duplicated code that should be resolved into separate functions). But more importantly, as we are new to the OWL API, and there are multiple ways to do things, any suggestions from those who are more proficient as to best practices or the most convenient methods to use would be greatly appreciated.

### MSO short paper

We presented a paper at the ISMB 2018 conference (Bio-ontologies community of interest) on the design
and programmatic algorithm for generating the MSO and SO from the master file.  It can be found in the
repository root directory, [here](https://github.com/The-Sequence-Ontology/MSO/blob/master/MSO_short_paper-ISMB_2018.pdf).

### Stable release versions

The latest version of the Sequence Ontology is still to be found at:

https://github.com/The-Sequence-Ontology/SO-Ontologies

## Contact
Please use the [Issues](https://github.com/The-Sequence-Ontology/MSO/issues) tab to provide feedback on the proposed ontology design in "master.owl" and its suitability for use as the MSO and for a refactored, parallel SO. Also please create issues to give feedback on the OWL API code.

