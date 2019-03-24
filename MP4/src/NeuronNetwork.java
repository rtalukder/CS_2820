//NeuronNetwork.java

/**
 * mp4, a program to create a neural network. 
 * Bug Notice: Only one unnamed synapse can be created. Also primary 
 * synapses are created as neurons. 
 * @author Raquib Talukder
 * @version 4/3/2016
 * @version mp4
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.regex.Pattern;
import java.util.Scanner;

/** Framework for discrete event simulation
 */
class Simulator {
    
    public interface Action {
        void fire(float time);
    }
    
    private static class Event {
        public float time;
        public Action act;
    }
    
    private static PriorityQueue <Event> eventSet 
            = new PriorityQueue <Event> ((Event e1, Event e2) -> Float.compare(e1.time, e2.time));
    
    static void schedule (float time, Action act){
        Event e = new Event ();
        e.time = time;
        e.act = act;
        eventSet.add(e);
    }
    
    /** Call run() after scheduling some initial events
     *  to run the simulation
     */
    static void run(){
        while(!eventSet.isEmpty()){
            Event e = eventSet.remove();
            e.act.fire(e.time);
        }
    }
}

// Utility classes

/** Error reporting methods
 */
class Errors {
    // counting errors in the code
    private static int errorCount = 0;
    
    static void fatal( String message ) {
        System.err.println( "Fatal error: " + message );
        System.exit( 1 );
    }
    
    static void warning( String message ) {
        System.err.println( "Error: " + message );
        errorCount = errorCount + 1;
    }
    
    static boolean anyErrors() {
        return errorCount > 0;
    }
}

/** Input scanning support methods
 */

class ScanSupport {
    /** Interface allowing error messages to be passed as 
     * lambda expressions
     */
    public interface ErrorMessage {
        abstract String myString();
    }
    
    /** Make sure there is a line end after the command,
     * complain if not
     */
    static void lineEnd( Scanner sc, ErrorMessage message){
        String skip = sc.nextLine();
        if (!"".equals(skip)){
            Errors.warning(message.myString() + " -- expected a newline");
        }
    }
    
    // Private pattern string to nextName
    private static final Pattern name = Pattern.compile("[A-Za-z]\\w*");
    
    /** Get next name, produce an error message if there isn't one
     */
    static String nextName(Scanner sc, ErrorMessage message){
        if (sc.hasNext(name)){
            return sc.next(name);
        }
        else{
            Errors.warning(message.myString() + " -- expected a name");
            return null;
        }
    }
    
    /** Get next float, produce an error message if there isn't one
     */
    static float nextFloat(Scanner sc, ErrorMessage message){
        if(sc.hasNextFloat()){
            return sc.nextFloat();
        }
        else{
            Errors.warning(message.myString() + " -- expected a number");
            return (float) 99.99;
        }
    }
        
}

// Simulation classes

abstract class NeuronOrSynapse {
    String name;    // name of neuron or synapse
    
    public class IllegalName extends Exception {}
    
    public LinkedList <Synapse> outgoing = new LinkedList <Synapse> ();
    public LinkedList <Synapse> incoming = new LinkedList <Synapse> ();
}

/** Neuron joins synapses together
 *  @see Synapse
 */
class Neuron extends NeuronOrSynapse {
    private float threshold;
    private float voltage;
    private float voltageDecay;
    private float time = (float) 0.0;
    
    // outputs of the neuron
    private LinkedList <Synapse> synapses;
       
    // initializer
    public Neuron(Scanner sc) throws IllegalName {
        String name = ScanSupport.nextName(sc, () -> "Neuron ???");
        
        // check if neuron exists
        // nextName already reported synatax error
        if (name == null){
            sc.nextLine();
            throw new IllegalName();
        }
        // checking if there is a duplicate definition of the neuron
        this.name = name;
        
        if (NeuronNetwork.findNeuronOrSynapse(name) != null){
            Errors.warning("Neuron "+ name +  " -- redefined");
            sc.nextLine();
            throw new IllegalName();
        }
        
        
        // get values of threshold and voltage
        // don't need to checkp them since they can be negative
        threshold = ScanSupport.nextFloat(sc, () -> Neuron.this.toString());
        voltage = ScanSupport.nextFloat(sc, () -> Neuron.this.toString());
        
        // don't need to worry about rest of information from line
        ScanSupport.lineEnd(sc,() -> Neuron.this.toString());
    }
    
    
    void voltageChange(float voltage, float strength){
        voltageDecay = (voltage * (float)Math.exp(time) + strength);
    }
    
    // other methods
    @Override
    public String toString() {
        return ("Neuron " + ( name != null ? name: "---" )
                + " " + threshold + " " + voltage);
        }
}

/** Synapse are one-way links to Neurons
 *  @see Neurons
 */
class Synapse extends NeuronOrSynapse {
    Neuron source;
    NeuronOrSynapse destination;
    float delay = (float) 99.99;
    float strength = (float) 99.99;
    
    
    
    // check if the synapse is named or unnamed
    private static final Pattern dash = Pattern.compile("-");
   
    public Synapse(Scanner sc) throws IllegalName {
        // scan and process one synapse
        // checking if is an unamed synapse
        if (sc.hasNext(dash)){
            sc.next(dash);
        }
        // is a named synapse
        else {
            name = ScanSupport.nextName(sc, () -> "Synapse ???");
            
            // checking name 
            if (name == null){
                // error has been reported
                sc.nextLine();
                throw new IllegalName();
            }
            
            // checking for duplicate names
            if (NeuronNetwork.findNeuronOrSynapse(name) != null){
                Errors.warning("Synapse " + name + " -- redefined");
                sc.nextLine();
                throw new IllegalName();
            }
        }
        
        // checking for source and destination names
        String srcName = ScanSupport.nextName(sc, () ->
                                ("Synapse " + (name != null ? name : "-")
                                + " ???"));
        
        String dstName = ScanSupport.nextName(sc, () ->
                                ("Synapse " + (name != null ? name : "-")
                                + " " + (srcName != null ? srcName : "---")
                                + " ???"));
    
        // checking if neurons have been intiazlized already
        source = NeuronNetwork.findNeuron(srcName);
        destination = NeuronNetwork.findNeuronOrSynapse(dstName);
    
        // reading in delay and strength values
        delay = ScanSupport.nextFloat(sc, () -> Synapse.this.toString());
        strength = ScanSupport.nextFloat(sc, () -> Synapse.this.toString());
        ScanSupport.lineEnd(sc, () -> Synapse.this.toString());
        // check if they values are correct

        if ((srcName != null) && (source == null)){
            Errors.warning("Synapse " + (name != null ? name : "-")
                           + " " + srcName 
                           + " " + (dstName != null ? dstName : "??")
                           + " --no such source");
        }

        if ((dstName != null) && (destination == null)){
            Errors.warning("Synapse " + (name != null ? name : "-")
                           + " " + (srcName != null ? srcName : "??")
                           + " " + dstName
                           + " -- no such destination");
        }

        if ((destination != null) &&
           (destination instanceof Synapse) &&
           ((Synapse)destination != null) &&
           (((Synapse)destination).destination instanceof Synapse)){

            Errors.warning(Synapse.this.toString() + " -- desination is a secondary synapse");
        }     

        if (delay < 0){
           Errors.warning(Synapse.this.toString() + " -- negative delay");
        }
        
        // synapse between neurons
        if (source != null){
            source.incoming.add(this);
        }
        
        if (destination != null){
            destination.outgoing.add(this);
        }
    }
    
    // Simulation methods
    
    // other methods
    @Override
    public String toString() {
        return ("Synapse " + (name != null ? name : "-")
                + " " + (source != null ? source.name : "---")
                + " " + (destination != null ? destination.name : "---")
                + " " + delay + " " + strength);
    }
}


/** NeuronNetwork is the main class that builds the whole model
 *  @see Synapse
 *  @see Neuron
 */
public class NeuronNetwork {

        // list of all neurons and synapses
    static LinkedList <Synapse> synapse
                        = new LinkedList <Synapse> ();
    static LinkedList <Neuron> neurons
                 = new LinkedList <Neuron> ();

    /** Looks up string in synapse. returns i if it exists
     *  return null if not.
     */
    public static Neuron findNeuron( String s ) {
        // ScanSupport can return null
        if (s == null){
            return null;
        }
        // search the neuron list
        for (Neuron n : neurons){
            if (n.name.equals(s)){
                return n;
            }
        }
        return null;
    }

    /** Looks up string in synapse and neurons. returns it if it exists
     *  return null if not.
     */

     public static NeuronOrSynapse findNeuronOrSynapse(String s) {
         // ScanSupport can return null
         if (s == null){
             return null;
         }
         
         // searching in neuron
         Neuron n = findNeuron(s);
         if (n != null){
             return n;
         }
         
         // searching in synapses
         for (Synapse sy : synapse){
             if((sy.name != null) && (sy.name.equals(s))){
                 return sy;
             }
         }
         return null;
     }
   

    // Private pattern string to initializeNetwork
    private static final Pattern primary = Pattern.compile("[A-Za-z]\\w*");
   
    
    /** initialize the NeuronNetwork based on the commands
     *  it is reading in from the scanner
     */
    static void initializeNetwork( Scanner sc ) {
        while (sc.hasNext()) {
            String command = sc.next();
            if (("neuron".equals( command )) ||  ("n".equals( command ))) {
                try{
                    neurons.add(new Neuron(sc));
                }
                catch (NeuronOrSynapse.IllegalName e) {}
            } 
            
            else if (("synapse".equals( command ))||("s".equals( command ))) {                
                try{
                    synapse.add(new Synapse(sc));
                    }
                    catch (NeuronOrSynapse.IllegalName e) {}
                    }
            else if (("quit".equals(command))){
                Simulator.run();
                System.out.println("### system quitting ###");
                printNetwork();
                System.exit(0);
            }
            else {
                Errors.warning( "unknown command" );
            }
        }
    }

    /** Print out the neuron network from the data structure
    */
    static void printNetwork() {
        for (Neuron n:neurons) {
            System.out.println( n.toString() );
            }
        for (Synapse s:synapse) {
            System.out.println( s.toString() );
            }
        }

    /** Main program
    * @see initializeNetwork
    */

    public static void main(String[] args){
        if (args.length < 1) {
            Errors.fatal( "missing file name" );
        }
	if (args.length > 1) {
            Errors.fatal( "too many arguments" );
        }
	try {
            initializeNetwork( new Scanner(new File(args[0])) );
	}
        catch (FileNotFoundException e) {
            Errors.fatal( "file not found: " + args[0] );
	}
	if (Errors.anyErrors()) {
            printNetwork();
	}
        else {
            Simulator.run();
        }
    }
}