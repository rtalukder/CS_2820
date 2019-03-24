import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.regex.Pattern;
import java.util.Scanner;
import java.lang.Math;
import java.util.List;

/** mp6, SimulationOutput
 *  This program will print out reports of 
 *  neurons if they fired during the current interval
 * @author: Raquib Talukder
 * @version: May 1, 2016
 */


// Errors.java
/** Error reporting methods.
 *  @author: Douglas W. Jones
 *  @author: Andy W.M. Arthur
 *  @version: April 9, 2016
 * 
 *  This code is extracted from the April 6, 2016 version of NeuronNetwork.java,
 *  which, in turn was based on the non-alternative solution to MP3,
 *  as well as a simulation framework from the March 11 lecture notes.
 */

class Errors {
    public static int errCount = 0;
    public static void fatal( String message ) {
        errCount++;
        System.err.println( "Fatal error: " + message );
        System.exit( 1 );
    }
    public static void warning( String message ) {
        errCount++;
        System.err.println( "Error: " + message );
    }
}

// ScanSupport.java
/** Input scanning support methods
 *  This bundle of static methods provides services that read input using
 *  a Java {@link Scanner}, and if the input does not meet the stated
 *  requirement, report that using a call to the {@code warning} method
 *  in {@link Errors}.
 *  @author Douglas W. Jones 
 *  @author: Andy W.M. Arthur
 *  @version 4/11/2016
 *  @see java.util.Scanner
 *  @see Errors
 *
 *  This code is extracted from the April 6, 2016 version of NeuronNetwork.java,
 *  which, in turn was based on the non-alternative solution to MP3,
 *  as well as a simulation framework from the March 11 lecture notes.
 *  This code is further modified to meet the requirements of MP5.
 */
class ScanSupport {
    /** Interface allowing error messages passed as lambda expressions
     *  The {@code ErrorMessage} interface is used implicitly in the
     *  parameter lists to methods of class {@code ScanSupport},
     *  but is rarely explicitly mentioned.
     *  The implicit mechanism for passing error message strings creates
     *  a subclass of {@code ErrorMessage} for each message.
     */
    public interface ErrorMessage {
        /** Return the part of the error message text giving the context
         *  each call to a {@code ScanSupport} method will typically
         *  provide a different implementation of this function.
         *  @return string
         */
        abstract String myString();
    }

    /** Force there to be a line end here, complain if not
     *  @param sc  the {@link Scanner} from which the input text is
     *             being scanned
     *  @param message  the {@link ErrorMessage} to use if the input text
     *             is not currently positioned at a line end.
     *  @return void
     *  Typically, the {@code message} parameter is given as a lambda
     *  expression, for example: {@code LineEnd(sc,()->"Line:"+n);}
     *  The use of a lambda expression here means that the computations
     *  (for example, string concatenations) are not done unless there
     *  is not a line end where one was expected.
     */
    public static void lineEnd( Scanner sc, ErrorMessage message ) {
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

    /** Get the next name, or complain if there isn't one
     *  @param sc  the {@link Scanner} from which the input text is
     *             being scanned
     *  @param message  the {@link ErrorMessage} to use if the input text
     *             is not currently positioned at a line end.
     *  @return String
     *  Typically, the {@code message} parameter is given as a lambda
     *  expression, for example: {@code NextName(sc,()->"Line:"+n);}
     *  See {@link lineEnd} for the reason a lambda expression is used here.
     *  Names are defined as a letter followed by any number of letters
     *  or digits using Java's {@link Pattern} recognition mechanisms.
     *  @see java.util.regex.Pattern
     */
    public static String nextName( Scanner sc, ErrorMessage message ) {
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

    /** Get the next int, or complain if there isn't one
     *  @param sc  the {@link Scanner} from which the input text is
     *             being scanned
     *  @param message  the {@link ErrorMessage} to use if the input text
     *             is not currently positioned at a line end.
     *  @return int
     *  Typically, the {@code message} parameter is given as a lambda
     *  expression, for example: {@code NextName(sc,()->"Line:"+n);}
     *  See {@link lineEnd} for the reason a lambda expression is used here.
     */
    public static int nextInt( Scanner sc, ErrorMessage message ) {
        if (sc.hasNextInt()) {
            return sc.nextInt();
        } else {
            Errors.warning(
                message.myString() +
                " -- expected an integer"
            );
            return 99;
        }
    }

    /** Get the next float, or complain if there isn't one
     *  @param sc  the {@link Scanner} from which the input text is
     *             being scanned
     *  @param message  the {@link ErrorMessage} to use if the input text
     *             is not currently positioned at a line end.
     *  @return float
     *  Typically, the {@code message} parameter is given as a lambda
     *  expression, for example: {@code NextName(sc,()->"Line:"+n);}
     *  See {@link lineEnd} for the reason a lambda expression is used here.
     */
    public static float nextFloat( Scanner sc, ErrorMessage message ) {
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

// Simulator.java
/** Framework for discrete event simulation.
 *  @author: Douglas W. Jones
 *  @version: April 21, 2016
 * 
 *  This code is based on the April 20, 2016 lecture notes.
 */
class Simulator {

    /** Users create new subclasses of event for each thing that happens
     */
    public static abstract class Event {

        /** The time of this event, read only within subclasses
         */
        protected final float time; // the time of this event

        /** The only way to create a new event
         *  @param t, the time at which the new event will be triggered
         *  This may only be called to initialize a subclass of Event,
         *  typically, it will be an anonymous subclass.
         */
        public Event( float t ) {
            time = t;               // initializer (the only way to set time)
        }

	/** Each subclass must give a specific trigger method
	 *  This method will be called when the event is triggered;
	 *  the code of Trigger may refer to {@code time}, the event time.
	 */
        abstract void trigger();    // what to do at that time
    }

    private static PriorityQueue <Event> eventSet
    = new PriorityQueue <Event> (
        (Event e1, Event e2) -> Float.compare( e1.time, e2.time )
    );

    /** Called to trigger the event at the given time
     *  @param e, the event to be triggered, with its time.
     */
    public static void schedule( Event e ) {
        eventSet.add( e );
    }

    /** Run the discrete event simulation
     *  Prior to calling {@code run}, the user should {@code schedule}
     *  some initial {@code Event}s.  The simulation will run until either
     *  no events remain or until some event terminates the program.
     */
    static void run() {
        while (!eventSet.isEmpty()) {
            Event e = eventSet.remove();
            e.trigger();
        }
    }
}

// Neuron.java
/** Neurons, joined by synapses, are the active components of a neuron network.
 *  @author: Douglas W. Jones
 *  @author: Andy W.M. Arthur
 *  @version: April 21, 2016
 *  @see Synapse
 *  @see ScanSupport
 *  @see Errors 
 *  @see Simulator
 * 
 *  This code is a modified version of the code from April 13, distributed
 *  as part of the framework for MP5; the modifications incorporate the
 *  alternative simulation framework discussed on April 20.
 */

class Neuron {
    String name;                // name of this neuron
    private int fireCount = 0;  // number of times this neuron fired

    public static class IllegalNameEx extends Exception {}

    // default values below for errors with incompletely defined neurons
    private float threshold = 99.99f;// voltage at which the neuron fires
    private float voltage = 99.99f; // voltage at the given time
    private float time = 0.0f;  // (see above)

    // the outputs of this neuron
    public LinkedList <Synapse> synapses = new LinkedList<Synapse>();

    // initializer
    public Neuron( Scanner sc ) throws IllegalNameEx {
        // scan and process one neuron
        String name = ScanSupport.nextName(
            sc,
            () -> "Neuron ???"
        );
        if (name == null) { // nextName() already reported syntax error
            sc.nextLine();
            throw new IllegalNameEx ();
        }
        this.name = name;
        if ( (NeuronNetwork.findNeuron( name ) != null)
        ||   (NeuronNetwork.findSynapse( name ) != null) ) {
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

        //  if voltage exceeds threshold (non-inclusive) the neuron fires
        if (voltage > threshold){
            Simulator.schedule(
                new Simulator.Event( 0.0f ) {
                    void trigger() {
                        Neuron.this.fire( time );
                    }
                }
            );
        }

        ScanSupport.lineEnd(
            sc,
            () -> Neuron.this.toString()
        );
    }

    // simulation methods
    void fire(float time) {
        fireCount = fireCount + 1;
        this.voltage = 0.0f;
        for(Synapse s: synapses) {
            Simulator.schedule(
                new Simulator.Event( time + s.delay ) {
                    void trigger() {
                        s.fire( time );
                    }
                }
            );
        }
    }

    /** This method is called by incoming synapses.
     */
    void kick(float time, float strength) {
        float v1 = voltage;
        // v2 = v1 e^(t1–t2) + s
        voltage = (v1 * (float)Math.exp( this.time - time )) + strength;
        this.time = time;
        if( voltage > threshold) this.fire( time );
    }

    /** Get the current count and reset the count
     */
    int getCount() {
        int r = fireCount;
        fireCount = 0;
        return r;
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

// Synapse.java
/** Synapses join neurons and come in several flavors
 *  @author: Douglas W. Jones
 *  @author: Andy W.M. Arthur
 *  @version: April 9, 2016
 *  @see Neuron
 *  @see Synapse
 *  @see PrimarySynapse
 *  @see SecondarySynapse
 *  @see ScanSupport
 *  @see Errors 
 *  @see Simulator
 * 
 *  This code is extracted from the April 6, 2016 version of NeuronNetwork.java,
 *  which, in turn was based on the non-alternative solution to MP3,
 *  as well as a simulation framework from the March 11 lecture notes.
 */
abstract class Synapse {
    // default values below for errors with incompletely defined synapses
    Neuron source;          // source for this synapse
    Float delay = 99.99f;
    Float strength = 99.99f;
    String name = null;     // name of this synapse, if it has one

    public static class IllegalNameEx extends Exception {}

    // really private to Synapse initializer
    private static final Pattern noName = Pattern.compile( "-" );

    // generic initializer
    static Synapse newSynapse( Scanner sc ) throws IllegalNameEx {
        // proxies for fields until we know the type of this synapse
        String myName = null;
        Neuron mySource = null;

        // only one of the following proxies will be non-null
        Neuron myPrimaryDest = null;
        Synapse mySecondaryDest = null;

        // the Synapse we're allocating
        Synapse mySynapse = null;
        
        // scan and process one synapse
        if (sc.hasNext( noName )) { // unnamed synapse
            sc.next( noName );
        } else { // named synapse, process the name
            myName = ScanSupport.nextName(
                sc,
                () -> "Synapse ???"
            );
            if (myName == null) {
                // nextName() already reported syntax error
                sc.nextLine();
                throw new IllegalNameEx ();
            }
            if ((NeuronNetwork.findNeuron( myName ) != null)
            ||  (NeuronNetwork.findSynapse( myName ) != null)) {
                Errors.warning(
                    "Synapse " + myName +
                    " -- duplicate declaration"
                );
                sc.nextLine();
                throw new IllegalNameEx();
            }
        }

        // the following is needed because of limits of java lambda
        final String finalName = myName;

        String sourceName = ScanSupport.nextName(
            sc,
            () -> (
                "Synapse " +
                (finalName != null ? finalName : "-") +
                " ???"
            )
        );
        String dstName = ScanSupport.nextName(
            sc,
            () -> (
                "Synapse " +
                (finalName != null ? finalName : "-") +
                " " +
                (sourceName != null ? sourceName : "---") +
                " ???"
            )
        );
        mySource = NeuronNetwork.findNeuron( sourceName );
        myPrimaryDest = NeuronNetwork.findNeuron( dstName );
        if (myPrimaryDest == null) {
            mySecondaryDest = NeuronNetwork.findSynapse( dstName );
            mySynapse = new SecondarySynapse( mySecondaryDest );
        } else {
            mySynapse = new PrimarySynapse( myPrimaryDest );
        }

        // the following is needed because of limits of java lambda
        final Synapse finalSynapse = mySynapse;

        finalSynapse.name = finalName;
        finalSynapse.source = mySource;
    
        finalSynapse.delay = ScanSupport.nextFloat(
            sc,
            () -> finalSynapse.toString()
        );
        finalSynapse.strength = ScanSupport.nextFloat(
            sc,
            () -> finalSynapse.toString()
        );
        ScanSupport.lineEnd(
            sc,
            () -> finalSynapse.toString()
        );

        // check correctness of fields
        if ((sourceName != null) && (mySource == null)) {
            Errors.warning(
                finalSynapse.toString() +
                " -- no such source"
            );
        }
        if ( (dstName != null)
        &&   (myPrimaryDest == null)
        &&   (mySecondaryDest == null) ) {
            Errors.warning(
                finalSynapse.toString() +
                " -- no such destination"
            );
        }
        if (finalSynapse.delay < 0.0f) {
            Errors.warning(
                finalSynapse.toString() +
                " -- illegal negative delay"
            );
            finalSynapse.delay = 99.99f;
        }
        if (finalSynapse != null && finalSynapse.source != null) {
            finalSynapse.source.synapses.add(finalSynapse);
        }
        return finalSynapse;
    }

    // simulation methods
    abstract void fire(float time);

    // other methods
    public abstract String toString();
}

// PrimarySynapse.java
/** Primary Synapses join neurons to neurons
 *  @author: Douglas W. Jones
 *  @author: Andy W.M. Arthur
 *  @version: April 9, 2016
 *  @see Neuron
 *  @see Synapse
 * 
 *  This code is extracted from the April 6, 2016 version of NeuronNetwork.java,
 *  which, in turn was based on the non-alternative solution to MP3,
 *  as well as a simulation framework from the March 11 lecture notes.
 *  This code is further modified to meet the requirements of MP5.
 */
class PrimarySynapse extends Synapse {
    Neuron destination;

    public PrimarySynapse( Neuron dst ) {
        // Called from Synapse.newSynapse() and nowhere else
        // All the field initialization and checking is done there,
        // except the following:
        destination = dst;
    }

    // simulation methods
    void fire(float time) {
        // adjust the voltage of the destination
        destination.kick(time, this.strength);
    }

    // other methods
    public String toString() {
        return (
            "Synapse " +
            ( name != null ? name : "-" ) +
            " " +
            ( source != null ? source.name : "---" ) +
            " " +
            ( destination != null ? destination.name : "---" ) +
            " " + delay + " " + strength
        );
    }
}

// SecondarySynapse.java
/** Secondary synapses join neurons to primary synapses
 *  @author: Douglas W. Jones
 *  @author: Andy W.M. Arthur
 *  @version: April 9, 2016
 *  @see Neuron
 *  @see Synapse
 *  @see PrimarySynapse
 *  @see ScanSupport
 *  @see Errors 
 *  @see Simulator
 * 
 *  This code is extracted from the April 6, 2016 version of NeuronNetwork.java,
 *  which, in turn was based on the non-alternative solution to MP3,
 *  as well as a simulation framework from the March 11 lecture notes.
 *  This code is further modified to meet the requirements of MP5.
 */
class SecondarySynapse extends Synapse {
    PrimarySynapse destination;

    public SecondarySynapse( Synapse dst ) {
        // Called from Synapse.newSynapse() and nowhere else
        // All the field initialization and checking is done there,
        // except the following:

        if ( (dst != null)
        &&   (dst instanceof SecondarySynapse) ) {
            Errors.warning(
                this.toString() +
                " -- destination is a secondary synapse"
            );
            destination = null;
        } else {
            destination = (PrimarySynapse) dst;
        }
    }

    // simulation methods
    void fire(float time) {
        destination.strength += this.strength;
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

// NeuronNetwork.java
/** NeuronNetwork is the main class that builds the whole model
 *  @author: Douglas W. Jones
 *  @author: Andy W.M. Arthur
 *  @version: April 13, 2016
 *  @see Neuron
 *  @see Synapse
 *  @see Errors
 *  @see Simulator
 * 
 *  This code is extracted from the April 6, 2016 version of NeuronNetwork.java,
 *  which, in turn was based on the non-alternative solution to MP3,
 *  as well as a simulation framework from the March 11 lecture notes.
 *  This code is further modified to meet the requirements of MP5.
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

    /** Look up s in synapses, find that Synapse if it exists
     *  return null if not.
     */
    public static Synapse findSynapse( String s ) {
        /* special case added because scan-support can return null */
        if (s == null) return null;

        /* search the synapse list */
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
                } 
                catch (Neuron.IllegalNameEx e) {}
            } 
            else if ("synapse".equals( command )) {
                try {
                    synapses.add( Synapse.newSynapse(sc) );
                } 
                catch (Synapse.IllegalNameEx e) {}
            } 
            else if ("output".equals( command )) {
                SimulationOutput.setOutput( sc );
            }
            else if ("run".equals(command)) {
                System.out.println("--- running simulation ---");
                Simulator.run();
            }
            else if("quit".equals(command)) {
                System.out.println("--- system quitting ---");
                System.exit(0);
            }
            else {
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
        }
        if (Errors.errCount == 0){
            Simulator.run();
        } else {
            printNetwork();
        }*/
        Scanner sc = new Scanner(System.in);
        initializeNetwork(sc);
    }
}