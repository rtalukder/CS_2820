// RoadNetwork.java
/** Read and process a road network
 * @author: Raquib Talukder
 * @version: March 7, 2016
 * 
 * This version of the code makes extensive use of lambda expressions
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
		// Bug:  what if sc.nextLine() was illegal (illegal state)
	}

	/* really private to nextName */
	private static final Pattern name = Pattern.compile( "[A-Za-z]\\w*" );

	/** Get the next float, or complain if there isn't one
	 */
	static String nextName( Scanner sc, ErrorMessage message ) {
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
	static float nextFloat( Scanner sc, ErrorMessage message ) {
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

/** Roads are one-way streets linking intersections
 *  @see Intersection
 */
class Road {
	// default values below for errors involving incompletely defined roads
	private float travelTime = 99.99f;	// measured in seconds
	private Intersection destination = null;// where road goes
	private Intersection source = null;	// where road comes from
	// name of road is source-destination

	// initializer
	public Road( Scanner sc, LinkedList <Intersection> inters ) {
		// scan and process one road
		String sourceName = ScanSupport.nextName(
			sc,
			new ScanSupport.ErrorMessage () {
				public String myString() {
					return Road.this.toString();
				}
			}
		);
		String dstName = ScanSupport.nextName(
			sc,
			() -> Road.this.toString()
		);
		source = RoadNetwork.findIntersection( sourceName );
		destination = RoadNetwork.findIntersection( dstName );
		if (source == null) {
			Errors.warning(
				Road.this.toString() +
				" -- source undefined"
			);
			// Note:  Object is created with a null source
		}
		if (destination == null) {
			Errors.warning(
				Road.this.toString() +
				" -- destination undefined"
			);
			// Note:  Object is created with a null destination
		}
		travelTime = ScanSupport.nextFloat(
			sc,
			() -> Road.this.toString()
		);
		ScanSupport.lineEnd(
			sc,
			() -> Road.this.toString()
		);
	}

	// other methods
	public String toString() {
		return (
			"Road " +
			( source != null ? source.name : "---" ) +
			" " +
			( destination != null ? destination.name : "---" ) +
			" " +
			travelTime
		);
	}
}

/** Intersections join roads
 *  @see Road
 *  @see NoStop
 *  @see StopLight
 */
abstract class Intersection {

	/* throw this if an attempt is made to redefine an intersection */
	public class IllegalName extends Exception {}

	String name;
	private LinkedList <Road> outgoing = new LinkedList <Road> ();
	private LinkedList <Road> incoming = new LinkedList <Road> ();
	// Bug: do I ever need to know about incoming roads?

	// other methods
	public abstract String toString();
}

// Subclasses of Intersection
class NoStop extends Intersection {
	// initializer
	public NoStop( Scanner sc, String name ) throws IllegalName {
		this.name = name;

		// scan and process one intersection
		if (RoadNetwork.findIntersection( name ) != null) {
			Errors.warning( toString() + " -- redefined." );
			throw new IllegalName();
		}
		ScanSupport.lineEnd(
			sc,
			() -> NoStop.this.toString()
		);
	}

	// other methods
	public String toString() {
		return (
			"Intersection " +
			( name != null ? name : "---" )
		);
	}
}

class StopLight extends Intersection {
	// initializer
	public StopLight( Scanner sc, String name ) throws IllegalName {
		this.name = name;

		// scan and process one intersection
		if (RoadNetwork.findIntersection( name ) != null) {
			Errors.warning( toString() + " -- redefined." );
			throw new IllegalName();
		}
		ScanSupport.lineEnd(
			sc,
			() -> StopLight.this.toString()
		);
	}

	// other methods
	public String toString() {
		return (
			"Intersection " +
			name +
			" Stoplight"
		);
	}
}

/** RoadNetwork is the main class that builds the whole model
 *  @see Road
 *  @see Intersection
 */
public class RoadNetwork {

	// the sets of all roads and all intersections 
	static LinkedList <Road> roads
		= new LinkedList <Road> ();
	static LinkedList <Intersection> inters
		= new LinkedList <Intersection> ();

	/** Look up s in inters, find that Intersection if it exists
	 *  return null if not.
	 */
	public static Intersection findIntersection( String s ) {
		/* special case added because scan-support can return null */
		if (s == null) return null;

		/* search the intersection list */
		for (Intersection i: RoadNetwork.inters) {
			if (i.name.equals(s)) {
				return i;
			}
		}
		return null;
	}

	/* really private to initializeNetwork() */
	private static final Pattern stoplight = Pattern.compile( "stoplight" );

	/** Initialize the road network by scanning its description
	 */
	static void initializeNetwork( Scanner sc ) {
		while (sc.hasNext()) {
			String command = sc.next();
			if (("intersection".equals( command ))
			||  ("i".equals( command ))) {
				String name = ScanSupport.nextName(
					sc,
					() -> "Intersection"
				);

				// What kind of intersection is this?
				if (sc.hasNext( stoplight )) {
					sc.next( stoplight ); // skip keyword
					try {
						inters.add(
						        new StopLight(sc,name)
						);
					} catch (Intersection.IllegalName e){}
				} else {
					try {
						inters.add(
							new NoStop(sc,name)
						);
					} catch (Intersection.IllegalName e){}
				}
			} else if (("road".equals( command ))
			||         ("r".equals( command ))) {
				roads.add( new Road( sc, inters ) );
			}
                        else if (("quit".equals( command ))){
                            System.out.println("###System Quitting###");
                            printNetwork();
                            System.exit(0);
                        }
                        else {
				Errors.warning( "unknown command" );
				// Bug:  should we allow comments?
			}
		}
	}

	/** Print out the road network from the data structure
	 */
	static void printNetwork() {
		for (Intersection i:inters) {
			System.out.println( i.toString() );
		}
		for (Road r:roads) {
			System.out.println( r.toString() );
		}
	}

	/** Main program
	 * @see initializeNetwork
	 */
	public static void main(String[] args) {
		/*if (args.length < 1) {
			Errors.fatal( "missing file name" );
		}
		if (args.length > 1) {
			Errors.fatal( "too many arguments" );
		}
		try {
			initializeNetwork( new Scanner(new File(args[0])) );
		} catch (FileNotFoundException e) {
			Errors.fatal( "file not found: " + args[0] );
		}*/
                Scanner sc = new Scanner(System.in);
                initializeNetwork(sc);
	}
}