// SimulationOutput.java

import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/** SimulationOutput creates an appropriate  output for the neuron network, MP6
 *  @author: Raquib Talukder
 *  @version: May 1, 2016
 *  @see Neuron
 *  @see NeuronNetwork
 *  @see Simulator
 *  @see ScanSupport
 * 
 *  This code is an updated version of MP5 to fit the requirements of the 
 *  new simulation framework from lecture on April 20th. 
 *  This code is modified to meet the requirements of MP6.
 */

class SimulationOutput extends NeuronNetwork {
    
    static float interval = (float) 0.0;
    static float length = (float) 0.0;
    // list of all neurons 
    private static List <Neuron> outputList 
                        = new LinkedList <Neuron> ();
    // different signs that can be printed out
    private static final String [] outputValues = {"| ", "|-", "|="};
    
    private SimulationOutput() {}
    
    public static void setOutput(Scanner sc) {
        
        // get the interval and length of the simulation run
        interval = ScanSupport.nextFloat(sc, () -> "- not a float");
        length = ScanSupport.nextFloat(sc, () -> "- not a float");
        
        // since all the neurons have been scheduled
        // we're trying to put them in the outputlist to be able
        // to search for infomation about individual neurons
        for (Neuron n : SimulationOutput.neurons){
            outputList.add(n);
        }
        // start the simulation with time 1
        //Simulator.schedule(1, (float time) -> displayHeaders(time));
        Simulator.schedule(
            new Simulator.Event(1) {
                void trigger() {
                    displayHeaders(1);
                }
            }
        );
    }
    
    
    private static void displayHeaders (float t){
        for (Neuron o: outputList){
            String objName = o.name;
            
            // if length of neuron is >4 char
            // shorten it
            if (objName.length() >  4) {
                objName = objName.substring(0, 4);
            }
            // name is of acceptable length
            System.out.append(' ');
            System.out.append(objName);
            
            // name is too short. make sure spacing is still kept standard
            if (objName.length() < 4){
                System.out.append("\t".substring(0, 4 - objName.length()));
            }
            
            System.out.append(' ');
        }
        System.out.println();
        
        displayOutput(t);
    }
    
    // calculate what symbol the neuron will get
    private static void displayOutput(float t){
        // find out how many times the neuron fired
        // and then pass it to outputSymbol
        for (Neuron o : outputList ){
            int count = o.getCount();
            outputSymbol(count);
        }
        System.out.println();

        // keep simulator running until time == length
        if (t < length) {
            Simulator.schedule(
                    new Simulator.Event(t + interval) {
                        @Override
                        void trigger() {
                            displayOutput(time);
                        }
                    }
            );
            //Simulator.schedule(t + interval, (float time) -> displayOutput(time));
        }
    }
    
    // figure out the output for the neuron by counting the 
    // times it fired
    private static void outputSymbol(int count){
        // output : |=
        // neuron fired >2 times
        if (count >= 2){
            System.out.append(outputValues[2]);
        }
        // output: |-
        // neuron only fired once
        else if(count == 1){
            System.out.append(outputValues[1]);
        }
        // output: | 
        // neuron didn't fire
        else {
            System.out.append(outputValues[0]);
        }
        
        System.out.append( '\t' );
    }
}
