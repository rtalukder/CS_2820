//NeuronNetwork.java

/**
 * mp3, a program to create a neural network. 
 * @author Raquib Talukder
 * @version 3/9/2016
 * @version mp3
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.regex.Pattern;
import java.util.Scanner;

// Utility classes

class Errors{
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
    private static final Pattern dash = Pattern.compile("-");
    
    /** Get next name, produce an error message if there isn't one
     */
    static String nextName(Scanner sc, ErrorMessage message){
        if (sc.hasNext(name)){
            return sc.next(name);
        }
        else if (sc.hasNext(dash)){
            return sc.next(dash);
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

/** Synapse are one-way links to Neurons
 *  @see Neurons
 */
abstract class Synapse {
    
    public class IllegalName extends Exception {}
    
    String incoming;    // name of the incomin synapse
    String outgoing;    // name of the outgoing synapse
    float delay;        // threshold of the neuron, has to be nonnegative
    float strength;     // strength of the synapse, can be negative
    String synName;
    
    @Override
    public abstract String toString();

}

class Primary extends Synapse {
    public Primary (Scanner sc, String name) throws IllegalName {
        synName = name;
        
        //scan and process one synapse
        if (NeuronNetwork.findNeuron(name) != null){
            Errors.warning(Primary.this.toString() + " -- redefined");
            sc.nextLine();
            throw new IllegalName();
        }

        String srcName = ScanSupport.nextName(sc, () -> Primary.this.toString());
        String dstName = ScanSupport.nextName(sc, () -> Primary.this.toString());
        
        incoming = (NeuronNetwork.findNeuron(srcName)).name;
        outgoing = (NeuronNetwork.findNeuron(dstName)).name;
        
        if(incoming == null){
            Errors.warning(Primary.this.toString() + " -- source undefined");
            sc.nextLine();
            throw new IllegalName();
        }
        
        if (outgoing == null){
            Errors.warning(Primary.this.toString() + " -- destination undefined");
            sc.nextLine();
            throw new IllegalName();
        }
        
        // check value of delay
        delay = ScanSupport.nextFloat(sc, () -> Primary.this.toString());
        if(delay < 0){
            Errors.warning(Primary.this.toString() + " negative delay");
            sc.nextLine();
            throw new IllegalName();
        }
        // don't need to check value of strength since can be negative
        strength = ScanSupport.nextFloat(sc, () -> Primary.this.toString());        
        
        ScanSupport.lineEnd(sc, () -> Primary.this.toString());
    }
    //other methods
    @Override
    public String toString() {
        return ("Synapse " + synName + " " + incoming + " " 
                + outgoing + " " + delay + " " + strength );
    }

}

class Secondary extends Synapse {
    public Secondary (Scanner sc, String name) throws IllegalName {
        synName = name;
        
        //scan and process one synapse
        String srcName = ScanSupport.nextName(sc, () -> Secondary.this.toString());
        String dstName = ScanSupport.nextName(sc, () -> Secondary.this.toString());

        // check and see if neuron exists
        System.out.println(srcName);
        incoming = (NeuronNetwork.findNeuron(srcName)).name;
        outgoing = (NeuronNetwork.findNeuron(dstName)).name;
        
        // undefined source
        if(incoming == null){
            Errors.warning(Secondary.this.toString() + " -- source undefined");
            sc.nextLine();
            throw new IllegalName();
        }
        
        // undefined destination
        if (outgoing == null){
            Errors.warning(Secondary.this.toString() + " -- destination undefined");
            sc.nextLine();
            throw new IllegalName();
        }
        
        // check value of delay
        delay = ScanSupport.nextFloat(sc, () -> Secondary.this.toString());
        if(delay < 0){
            Errors.warning(Secondary.this.toString() + " negative delay");
            sc.nextLine();
            throw new IllegalName();
        }
        // don't need to check value of strength since can be negative
        strength = ScanSupport.nextFloat(sc, () -> Secondary.this.toString());        
        
        ScanSupport.lineEnd(sc, () -> Secondary.this.toString());
        
    }    
    
    //other methods
    @Override
    public String toString() {
        return ("Synapse " + synName + " " + incoming + " " 
                + outgoing + " " + delay + " " + strength);
    
    }
}

/** Neuron joins synapses together
 *  @see Synapse
 */
class Neuron {
    String name;
    private float threshold;
    private float voltage;
    private float time = (float) 0.0;
    
    private LinkedList <Synapse> outgoing = new LinkedList <Synapse> ();
    private LinkedList <Synapse> incoming = new LinkedList <Synapse> ();
    
    public class IllegalNameException extends Exception {}
    
    // initializer
    public Neuron(Scanner sc, LinkedList <Neuron> neurons) throws IllegalNameException {
        String srcName = ScanSupport.nextName(sc, () -> Neuron.this.toString());
        name = srcName;
        
        // check if neuron exists
        if (NeuronNetwork.findNeuron( name ) != null){
            Errors.warning(Neuron.this.toString() + " -- name redefined");
            sc.nextLine();
            throw new IllegalNameException();
        }
        
        // check values of threshold 
        threshold = ScanSupport.nextFloat(sc, () -> Neuron.this.toString());
        if (threshold < 0){
            Errors.warning(Neuron.this.toString() + " -- negative threshold");
            sc.nextLine();
            throw new IllegalNameException();
        }
        
        // check values of voltage
        voltage = ScanSupport.nextFloat(sc, () -> Neuron.this.toString());
        if (voltage < 0){
            Errors.warning(Neuron.this.toString() + " -- negative voltage");
            sc.nextLine();
            throw new IllegalNameException();
        }
        ScanSupport.lineEnd(sc,() -> Neuron.this.toString());
    }
    
    public Neuron(String primaryS){
        name = primaryS;
    }

    // other methods
    @Override
    public String toString() {
        return ("Neuron " + ( name != null ? name: "---" )
                + " " + threshold + " " + voltage);
	}
}

/** NeuronNetwork is the main class that builds the whole model
 *  @see Synapse
 *  @see Neuron
 */
public class NeuronNetwork {

	// list of all neurons and synapses
    static LinkedList <Synapse> syn
                        = new LinkedList <Synapse> ();
    static LinkedList <Neuron> neurons
        		= new LinkedList <Neuron> ();

    /** Looks up string in syn. returns i if it exists
     *  return null if not.
     */
    public static Neuron findNeuron( String s ) {
        // ScanSupport can return null
        if (s == null){
            return null;
        }
        // search the neuron list
        for (Neuron i : NeuronNetwork.neurons){
            if (i.name.equals(s)){
                return i;
            }
        }
        return null;
    }

    // Private pattern string to initializeNetwork
    private static final Pattern primary = Pattern.compile("[A-Za-z]\\w*");
    private static final Pattern secondary = Pattern.compile("-");
    
    
    /** initialize the NeuronNetwork based on the commands
     *  it is reading in from the scanner
     */
    static void initializeNetwork( Scanner sc ) {
        while (sc.hasNext()) {
            String command = sc.next();
            if (("neuron".equals( command )) ||  ("n".equals( command ))) {
                try{
                    neurons.add(new Neuron(sc, neurons));
                }
                catch (Neuron.IllegalNameException e) {}
            } 
            else if (("synapse".equals( command ))||("s".equals( command ))) {
		String name = ScanSupport.nextName(sc, () -> "Synapse");
                
                // What kind of synapse?
                if (sc.hasNext(primary)){
                    try{
                        syn.add(new Primary(sc, name));
                        neurons.add(new Neuron(name));
                    }
                    catch (Synapse.IllegalName e) {}
                    }
                else if (sc.hasNext(secondary)){
                    sc.next(secondary);
                    try{
                        syn.add(new Secondary(sc, "-"));
                    }
                    catch (Synapse.IllegalName e) {}
                }
            }
            else {
                Errors.warning( "unknown command" );
            }
	}
    }

    /** Print out the neuron network from the data structure
    */
    static void printNetwork() {
        for (Neuron i:neurons) {
            System.out.println( i.toString() );
            }
        for (Synapse r:syn) {
            System.out.println( r.toString() );
            }
        }

    /** Main program
    * @see initializeNetwork
    */

public static void main(String[] args){
    try {
        if (args.length < 1) {
            Errors.fatal( "Missing file name" );
            }
        if (args.length > 1) {
            Errors.fatal( "Too many arguments" );
            }

        initializeNetwork( new Scanner(new File(args[0])) );
        }

        catch (FileNotFoundException e) {
			Errors.fatal( "File not found: " + args[0] );
            }
    }   

}