package ai;

public class Driver {

    public static void main(String[] args) {

        int size = 25;

        Individual max = new Individual(400, 400, 700, 700, 2);
        Individual min = new Individual(0, 0, 0, 0, 0);

        int iterations = 3000;
        int stopScore = 1_000_000;
        double crossoverProb = 0.4;
        double mutateProb = 0.4;
        int numWindowsAtOnce = 5;

        Evolution evolution = new Evolution(numWindowsAtOnce);
        evolution.start(mutateProb, max, min, size, iterations, stopScore, crossoverProb);

        // Best I have found so far letting it run
        //file:///Users/ctr/DinoAI/index.html?x=115.74168058318503&y=133.12269758628344&w=111.86195667466018&h=138.81277715347295&v=0.26803062540966427

        // Ensures all the Chrome tabs are closed out
        System.exit(0);
    }
}
