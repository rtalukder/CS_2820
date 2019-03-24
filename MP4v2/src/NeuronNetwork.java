// NeuronNetwork.java
/** Java program to read and process a neuron network
 * @author Raquib Talukder
 * @version: April 6, 2016
 * 
 * This code is based on NeuronNetwork.java, the non-alternative
 * solution to MP3, as well as a simulation framework from the
 * March 11 lecture notes. These were extended to solve MP4. 
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.regex.Pattern;
import java.util.Scanner;
import java.lang.Math;

// Utility classes

/** Error reporting methods
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

/** Framework for discrete event simulation.
 */
class Simulator {

        public interface Action {
                // actions contain the specific code of each event
                void trigger( float time );
        }

        private static class Event {
                public float time; // the time of this event
                public Action act; // what to do at that time
        }

        private static PriorityQueue <Event> eventSet
        = new PriorityQueue <Event> (
                (Event e1, Event e2) -> Float.compare( e1.time, e2.time )
        );

        /** Call schedule to make act happen at time.
         *  Users typically pass the action as a lambda expression:
         *  <PRE>
         *  Simulator.schedule(t,(float time)->method(params,time))
         *  </PRE>
         */
        static void schedule( float time, Action act ) {
                Event e = new Event();
                e.time = time;
                e.act = act;
                eventSet.add( e );
        }

        /** Call run() after scheduling some initial events
         *  to run the simulation.
         */
        static void run() {
                while (!eventSet.isEmpty()) {
                        Event e = eventSet.remove();
                        e.act.trigger( e.time );
                }
        }
}


/** Neurons are joined by synapses
 *  @see Synapse
 */
class Neuron {
    String name;            // name of this neuron

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

        //  voltage must exceed threshold (non-inclusive)
        if (voltage > threshold){
            Simulator.schedule( 0.0f, (float t) -> this.fire(t) );
        }

        ScanSupport.lineEnd(
            sc,
            () -> Neuron.this.toString()
        );
    }

    // simulation methods
    void fire(float time) {
        System.out.println( time + " neuron " + name + " fired");
        this.voltage = 0.0f;
        for(Synapse s: synapses) {
            Simulator.schedule( time+s.delay, (float t) -> s.fire(t) );
        }
    }

    /** This method is called by incoming synapses. It returns the string
     *  describing how this neuron's voltage changed.
     */
    String kick(float time, float strength) {
        float v1 = voltage;
        // v2 = v1 e^(t1–t2) + s
        voltage = (v1 * (float)Math.exp( this.time - time )) + strength;
        this.time = time;
        if( voltage > threshold){
            // schedule this so we can return, and print the synapse firing
            // before we print the neuron firing in fire().
            Simulator.schedule( time, (float t) -> this.fire(t) );
        }
        return v1 +" "+ voltage;
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

/** Synapses come in several flavors
 *  @see Neuron
 *  @see PrimarySynapse
 *  @see SecondarySynapse
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

/** Primary Synapses join neurons to neurons
 *  @see Neuron
 *  @see Synapse
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
        // the voltage of the destination before and after
        String change_str = destination.kick(time, this.strength);
        System.out.println(
            time +
            " synapse " +
            ( name != null ? name : "-" ) +
            " " +
            source.name + // note: source is never null during simulation
            " " +
            destination.name + // note: destination is never null as above
            " " +
            change_str
        );
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

/** Secondary synapses join neurons to primary synapses
 *  @see Neuron
 *  @see Synapse
 *  @see PrimarySynapse
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
        System.out.println(
            time +
            " synapse " +
            (name != null ? name : "-") +
            " " +
            source.name + // note: source is never null during simulation
            " " +
            destination.name + // note: destination is never null as above
            " " +
            destination.strength +
            " " +
            (destination.strength + this.strength)
        );
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
                } catch (Neuron.IllegalNameEx e) {}
            } else if ("synapse".equals( command )) {
                try {
                    synapses.add( Synapse.newSynapse(sc) );
                } catch (Synapse.IllegalNameEx e) {}
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
        if (args.length < 1) {
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
        }
    }
}