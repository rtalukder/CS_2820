// NeuronNetwork.java
/** Java progra to read and process a neuron network
 * @author: Raquib Talukder
 * @version: March 7, 2016
 * 
 * This code is based on NeuronNetwork.java from March 7, 2016,
 * extended to solve MP3 using a superclass of Neuron and Synapse
 * (this is the second approach to solving the problem)
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.regex.Pattern;
import java.util.Scanner;

// Utility classes

/** Error reporting methods
 */
class Errors {
	static void fatal( String message ) {
		System.err.println( "Fatal error: " + message );
		System.exit( 1 );
	}
	static void warning( String message ) {
		System.err.println( "Error: " + message );
	}
}

/** Input scanning support methods
 */
class ScanSupport {
	/** Interface allowing error messages passed as lambda expressions
	 */
	public interface ErrorMessage {
		abstract String myString();
	}

	/** Force there to be a line end here, complain if not
	 */
	static void lineEnd( Scanner sc, ErrorMessage message ) {
		String skip = sc.nextLine();
		if (!"".equals( skip )) {
			// Bug:  do we want to allow comments here
			Errors.warning(
				message.myString() +
				" -- expected a newline"
			);
		}
		// Bug:  should we allow comments here?
		// Bug:  what if sc.nextLine() was illegal (illegal state)
	}

	/* really private to nextName */
	private static final Pattern name = Pattern.compile( "[A-Za-z]\\w*" );

	/** Get the next float, or complain if there isn't one
	 */
	final static String nextName( Scanner sc, ErrorMessage message ) {
		if (sc.hasNext( name )) {
			return sc.next( name );
		} else {
			Errors.warning(
				message.myString() +
				" -- expected a name"
			);
			return null;
		}
	}

	/** Get the next float, or complain if there isn't one
	 */
	final static float nextFloat( Scanner sc, ErrorMessage message ) {
		if (sc.hasNextFloat()) {
			return sc.nextFloat();
		} else {
			Errors.warning(
				message.myString() +
				" -- expected a number"
			);
			return 99.99f;
		}
	}
}

// Simulation classes

/** Superclass of Neurons and Synapses allowing naming of either
 *  @see Neuron
 *  @see Synapse
 */
abstract class NeuronOrSynapse {
	String name;			// name of this neuron or synapse

	public class IllegalNameEx extends Exception {}
}

/** Neurons are joined by synapses
 *  @see Synapse
 */
class Neuron extends NeuronOrSynapse {
	// default values below for errors with incompletely defined neurons
	private float threshold = 99.99f;// voltage at which the neuron fires
	private float voltage = 99.99f;	// voltage at the given time
	final private float time = 0.0f;	// (see above)

        private LinkedList <Synapse> synapses;	// the outputs of this neuron

	// initializer
	public Neuron( Scanner sc ) throws IllegalNameEx {
		// scan and process one neuron
		final String name = ScanSupport.nextName(
			sc,
			() -> "Neuron ???"
		);
		if (name == null) { // nextName() already reported syntax error
			sc.nextLine();
			throw new IllegalNameEx ();
		}
		this.name = name;
		if (NeuronNetwork.findNeuronOrSynapse( name ) != null) {
			Errors.warning(
				"Neuron " + name +
				" -- duplicate declaration"
			);
			sc.nextLine();
			throw new IllegalNameEx();
		}
		threshold = ScanSupport.nextFloat(
			sc,
			() -> Neuron.this.toString()
		);
		voltage = ScanSupport.nextFloat(
			sc,
			() -> Neuron.this.toString()
		);
		ScanSupport.lineEnd(
			sc,
			() -> Neuron.this.toString()
		);
        }

	// other methods
	public String toString() {
		return (
			"Neuron " +
			name +
			" " +
			threshold +
			" " +
			voltage
		);
	}
}

/** Synapses join neurons
 *  @see Neuron
 */
class Synapse extends NeuronOrSynapse {
	// default values below for errors with incompletely defined synapses
	final Neuron source;
	final NeuronOrSynapse destination;
	Float delay = 99.99f;
	Float strength = 99.99f;

	/* really private to Synapse initializer */
	private static final Pattern noName = Pattern.compile( "-" );

	public Synapse( Scanner sc) throws IllegalNameEx {
		// scan and process one synapse
		if (sc.hasNext( noName )) { // unnamed synapse
			sc.next( noName );
		} else { // named synapse, process the name
			name = ScanSupport.nextName(
				sc,
				() -> "Synapse ???"
			);
			if (name == null) {
				// nextName() already reported syntax error
				sc.nextLine();
				throw new IllegalNameEx ();
			}
			if (NeuronNetwork.findNeuronOrSynapse( name ) != null) {
				Errors.warning(
					"Synapse " + name +
					" -- duplicate declaration"
				);
				sc.nextLine();
				throw new IllegalNameEx();
			}
		}
		final String sourceName = ScanSupport.nextName(
			sc,
			() -> (
				"Synapse " +
				(name != null ? name : "-") +
				" ???"
			)
		);
		final String dstName = ScanSupport.nextName(
			sc,
			() -> (
				"Synapse " +
				(name != null ? name : "-") +
				" " +
				(sourceName != null ? sourceName : "---") +
				" ???"
			)
		);
		source = NeuronNetwork.findNeuron( sourceName );
		destination = NeuronNetwork.findNeuronOrSynapse( dstName );
		delay = ScanSupport.nextFloat(
			sc,
			() -> Synapse.this.toString()
		);
		strength = ScanSupport.nextFloat(
			sc,
			() -> Synapse.this.toString()
		);
		ScanSupport.lineEnd(
			sc,
			() -> Synapse.this.toString()
		);

		// check correctness of fields
		if ((sourceName != null) && (source == null)) {
			Errors.warning(
				"Synapse " +
				(name != null ? name : "-") +
				" " +
				sourceName +
				" " +
				( dstName != null ? dstName : "??" ) +
				" -- no such source"
			);
		}
		if ((dstName != null) && (destination == null)) {
			Errors.warning(
				"Synapse " +
				(name != null ? name : "-") +
				" " +
				( sourceName != null ? sourceName : "??" ) +
				" " +
				dstName +
				" -- no such destination"
			);
		}
		if (
			(destination != null) &&
			(destination instanceof Synapse) &&
			((Synapse)destination != null) &&
			(((Synapse)destination).destination instanceof Synapse)
		) {
			Errors.warning(
				Synapse.this.toString() +
				" -- destination is a secondary synapse"
			);
		}
		if (delay < 0.0f) {
			Errors.warning(
				Synapse.this.toString() +
				" -- illegal negative delay"
			);
			delay = 99.99f;
		}
	}

	// other methods
	public String toString() {
		return (
			"Synapse " +
			(name != null ? name : "-") +
			" " +
			( source != null ? source.name : "---" ) +
			" " +
			( destination != null ? destination.name : "---" ) +
			" " + delay + " " + strength
		);
	}
}

/** NeuronNetwork is the main class that builds the whole model
 *  @see Neuron
 *  @see Synapse
 */
public class NeuronNetwork {

	// the sets of all neurons and synapses
	static LinkedList <Neuron> neurons
		= new LinkedList <Neuron> ();
	static LinkedList <Synapse> synapses
		= new LinkedList <Synapse> ();

	/** Look up s in neurons, find that Neuron if it exists
	 *  return null if not.
	 */
	public static Neuron findNeuron( String s ) {
		/* special case added because scan-support can return null */
		if (s == null) return null;

		/* search the neuron list */
		for (Neuron n: neurons) {
			if (n.name.equals(s)) {
				return n;
			}
		}
		return null;
	}

	/** Look up s in neurons or synapses, find that NeuronOrSynapse
	 *  return null if not.
	 */
	public static NeuronOrSynapse findNeuronOrSynapse( String s ) {
		/* special case added because scan-support can return null */
		if (s == null) return null;

		/* search the neuron list first */
		Neuron n = findNeuron( s );
		if (n != null) return n;

		/* search the synapse list second */
		for (Synapse sy: synapses) {
			if ((sy.name != null) && (sy.name.equals(s))) {
				return sy;
			}
		}
		return null;
	}

	/** Initialize the neuron network by scanning its description
	 */
	static void initializeNetwork( Scanner sc ) {
		while (sc.hasNext()) {
			String command = sc.next();
			if ("neuron".equals( command )) {
				try {
					neurons.add( new Neuron( sc ) );
				} catch (NeuronOrSynapse.IllegalNameEx e) {}
			} else if ("synapse".equals( command )) {
				try {
					synapses.add( new Synapse( sc ) );
				} catch (NeuronOrSynapse.IllegalNameEx e) {}
			} else {
				Errors.warning( command + " -- what is that" );
				sc.nextLine();
			}
		}
	}

	/** Print out the neuron network from the data structure
	 */
	static void printNetwork() {
		for (Neuron n:neurons) {
			System.out.println( n.toString() );
		}
		for (Synapse s:synapses) {
			System.out.println( s.toString() );
		}
	}

	/** Main program
	 * @see initializeNetwork
	 * @see printNetwork
	 */
	public static void main(String[] args) {
		/*8if (args.length < 1) {
			Errors.fatal( "missing file name" );
		}
		if (args.length > 1) {
			Errors.fatal( "too many arguments" );
		}
		try {
			initializeNetwork( new Scanner(new File(args[0])) );
		} catch (FileNotFoundException e) {
			Errors.fatal( "file not found: " + args[0] );
		}
*/
                Scanner sc = new Scanner(System.in);
                initializeNetwork(sc); 
		printNetwork();
	}
}