package ai;

public class Driver {

    public static void main(String[] args) {

        // Each attribute of the individuals will always been in this range.
        // Ensure that these value are set wide enough apart
        Individual maxValues = new Individual(400, 400, 700, 700, 20);
        Individual minValues = new Individual(0, 0, 0, 0, 0);

        int populationSize = 25;
        int iterations = 1000;
        double crossoverProb = 0.8;
        double mutateProb = 0.2;
        int numWindowsAtOnce = 5;

        Evolution evolution = new Evolution(numWindowsAtOnce);
        evolution.start(populationSize, iterations, crossoverProb, mutateProb, maxValues, minValues);

        // Best I have found so far letting it run
        //file:///Users/ctr/DinoAI/index.html?x=115.74168058318503&y=133.12269758628344&w=111.86195667466018&h=138.81277715347295&v=0.26803062540966427

        // Ensures all the Chrome tabs are closed out
        System.exit(0);
    }
}
