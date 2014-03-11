## Synopsis

PLUS is a system for capturing and managing provenance information, originally created at the MITRE Corporation.

Data provenance is “information that helps determine the derivation history of a data product...[It includes] the ancestral data product(s) from which this data product evolved, and the process of transformation of these ancestral data product(s).  Essentially, provenance provides a data family tree,‖ helping consumers of data to interpret it properly and determine how much they should trust it.

## Installation & Quick Start:  Running the Provenance Server

PLUS comes bundled with a web application that can be deployed to a container, which permits visualizing and querying provenance.

	git clone https://github.com/plus-provenance/plus.git
	mvn jetty:run

Wait until jetty finishes starting up, and then go to [http://localhost:8080/plus/](http://localhost:8080/plus/).  The first time PLUS starts up, it may take a bit of extra time as it pre-populates your provenance store with some example workflows and data.  In general, your provenance database will be stored in your user's home directory under the name `provenance.db`.

## Components of the PLUS System:  What Can You Do With This?

The PLUS system has four main components: capture, storage, usage and admin.

*Provenance Capture* refers to observing or deriving provenance information about what occured in a particular program. Capture Agents report provenance information from applications to PLUS.  For instance, if a user edits a document in Word, a Capture Agent would report to PLUS the file(s) modified and the user.   PLUS provides an API for creating, managing, and reporting provenance objects to a database.  To create your own provenance capture agent, or to "instrument" a piece of software to collect provenance, it's as simple as creating objects that correspond to graph nodes and edges, and then saving them to a database.  

*Provenance Storage* refers to where PLUS stores the reported provenance information. PLUS currently uses a [Neo4J database](http://www.neo4j.org/) for its storage needs, providing the ability to perform arbitrary graph queries.  The web application that you'd start via `mvn jetty:run` is the primary front-end interface to this database.

*Provenance Usage* refers to how PLUS can be used to answer questions using the provenance data.  Provenance is a data stream that can be used for many purposes. Examples of usage currently implemented in PLUS include:    
- ROI Analysis on Data Sources (Determine which sources are used most frequently)
- Unique Source Counts (I.e. how many distinct sources of information went into creating a report)
- Custom Data Fitness Assessments (is this data source good for my purpose?)

*Provenance Admin* refers to how PLUS manages and protects the provenance information. This includes topics such as:
- Access Control Model
- Surrogates 

## Code Examples

Code tutorials can be found in the org.mitre.provenance.tutorialcode package, covering elements of provenance creation, fetching from databases, associating access control information with provenance, and query.

## API Reference

(LINK TO JAVADOCS HERE)

## Tests

JUnit test code is provided in the org.mitre.provenance.test package.  Test coverage is evolving.

## License

[Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0.html)

## Provenance Basics:  Nodes and Edges

There are several kinds of nodes that you can use in provenance graphs, to express what kind of computation happened:

- **Data** nodes refer to some piece of information, of any type.
- **Invocation** nodes refer to a process that was actually executed at a particular time. An example of an invocation would be "Algorithm 27 run on Tuesday at 2:00PM".
- **Activity** nodes refer to abstract processes, not necessarily executed. An example of an activity is "Algorithm 27". Notice the difference between the algorithm (an activity) and a particular run of the algorithm (which is an invocation)
- **Workflow** nodes are ways of grouping lots of provenance nodes into a cohesive graph. So you might create a special workflow node, then associate it with many different invocations, to illustrate that they all happened as part of one cohesive workflow. 

There are different types of edges as well, that correspond to different semantics:

- **Input to** indicates that the head of the edge is data, that was input to the tail of the edge (an invocation)
- **Generated** indicates that the head of the edge is an invocation, which produced as an output the tail of the edge (data).
- **Triggered** indicates that both the head and the tail of the edge are invocations, one of which triggered the other.
 
## Non-Provenance Edges and Node Metadata

Provenance itself is very limiting; many times you'll want to capture and express extra information that doesn't fall into the category of a provenance edge or node type. For that reason, Non-Provenance Edges (NPEs) and node metadata are provided.

All provenance nodes can contain arbitrarily many key/value pairs of node metadata. So for example, you can assign latitude and longitude information to any data node, if that's meaningful for your application. There is no schema or data constraint available for node metadata, but it provides a way of storing just about anything along with a node.

NPEs permit linking two provenance nodes via non-provenance semantics. For example, some users might create links saying that file A is "similar to" file B. That isn't a provenance edge, but it is something that you can assert using PLUS. Note that because NPEs don't follow provenance semantics, it is possible to introduce cycles into the graph via NPEs - and that's perfectly OK. 

## Selected Academic Bibliography Relevant to PLUS

PLUS was created within the MITRE Corporation as part of sponsored research into data provenance.  The articles below are a selected list of academic reading on the concepts and motivation behind this code base:

- [Provenance Capture and Use: A Practical Guide](http://www.mitre.org/publications/technical-papers/provenance-capture-and-use-a-practical-guide)
- [PLUS: A Provenance Manager for Integrated Information](http://ieeexplore.ieee.org/xpls/abs_all.jsp?arnumber=6009558&tag=1) Information Reuse and Integration (IRI), 2011 IEEE International Conference on. IEEE, 2011.
- [Surrogate Parenthood: Protected and Informative Graphs](http://dl.acm.org/citation.cfm?id=2002979) Proceedings of the VLDB Endowment 4.8 (2011): 518-525.
- [Capturing Provenance in the Wild](http://rd.springer.com/chapter/10.1007/978-3-642-17819-1_12#page-1) Provenance and Annotation of Data and Processes. Springer Berlin Heidelberg, 2010. 98-101.
- [It’s about the data: Provenance as a tool for assessing data fitness](https://www.usenix.org/system/files/conference/tapp12/tapp12-final8.pdf) Proceedings of the 4th USENIX Workshop on the Theory and Practice of Provenance (TAPP’12). 2012.
- [Getting it Together: Enabling Multi-Organization Provenance Exchange](http://static.usenix.org/legacy/events/tapp11/tech/final_files/Allen.pdf) TaPP 2011. 
- [Provenance for collaboration: Detecting suspicious behaviors and assessing trust in information](http://ieeexplore.ieee.org/xpls/abs_all.jsp?arnumber=6144820) Collaborative Computing: Networking, Applications and Worksharing (CollaborateCom), 2011 7th International Conference on. IEEE, 2011.
