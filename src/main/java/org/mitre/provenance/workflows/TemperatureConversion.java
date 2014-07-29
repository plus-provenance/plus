package org.mitre.provenance.workflows;

import java.util.Date;

import org.mitre.provenance.PLUSException;
import org.mitre.provenance.client.LocalProvenanceClient;
import org.mitre.provenance.plusobject.PLUSInvocation;
import org.mitre.provenance.plusobject.PLUSObject;
import org.mitre.provenance.plusobject.PLUSWorkflow;
import org.mitre.provenance.plusobject.ProvenanceCollection;
import org.mitre.provenance.plusobject.builder.ProvBuilder;
import org.mitre.provenance.user.User;

/**
 * A teaching example of a provenance workflow, as part of the default catalog.
 * This workflow converts the fahrenheit temperature of 100 degrees to Celsius. 
 * The formula for converting Fahrenheit to Celsius is subtract 32, multiply by 5, then divide by 9.
 * This simple example shows the difference between operations and the values they take as arguments, and
 * how the result of multiple operations becomes a provenance graph.
 * @author moxious
 *
 */
public class TemperatureConversion {
	public static ProvenanceCollection create() throws PLUSException {
		PLUSWorkflow wf = new PLUSWorkflow();
		wf.setName("Temperature Conversion: 100F to Celsius");
		wf.setWhenStart(new Date().toString());
		wf.setWhenEnd(new Date().toString()); 

		ProvBuilder b = new ProvBuilder(wf, User.PUBLIC);
		b = b.link(b.merge(b.newDataNamed("100 degrees Fahrenheit"),
				       b.newDataNamed(" 32 ")),
			   b.newInvocationNamed("Subtract"));
		
		b = b.merge(b.link(b.nodeNamed("Subtract"), b.newDataNamed("68")));
		
		b = b.merge(b.link(new ProvBuilder(b.nodeNamed("68"), b.newDataNamed("5")),
				   b.newInvocationNamed("Multiply")));
		b = b.merge(b.link(b.nodeNamed("Multiply"), b.newDataNamed("340")));
		b = b.merge(b.link(new ProvBuilder(b.nodeNamed("340"), b.newDataNamed("9")),
				   b.newInvocationNamed("Divide")));
		b = b.merge(b.link(b.nodeNamed("Divide"), b.newDataNamed("37.77")));
		
		System.out.println(b);
		return b;
	}
	
	public static void main(String [] args) throws Exception { 
		new LocalProvenanceClient().report(TemperatureConversion.create());
	}
}
