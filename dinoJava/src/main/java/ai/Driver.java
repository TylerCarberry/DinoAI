package ai;

/**
 * Grad AI
 * Chrome Dinosaur Game
 * @author Cole Robertson and Tyler Carberry
 */
public class Driver {

    public static void main(String[] args) {

        // Each attribute of the individuals will always be in this range.
        // Ensure that these value are set wide enough apart to contain the ideal solution
        Individual maxValues = new Individual(400, 400, 700, 700, 20);
        Individual minValues = new Individual(0, 0, 0, 0, 0);

        // Number of individuals in each population
        int populationSize = 25;
        // How long to run for
        int iterations = 1000;
        // The likelihood that 2 individuals will breed
        double crossoverProb = 0.8;
        // Likelihood of a mutation occurring
        double mutateProb = 0.3;
        // Used to speed up the training process by running multiple instances at once
        // Setting this value too high will cause your computer to lag and will cause the AI to randomly fail
        int numWindowsAtOnce = 5;
        // If true, resume the training with the last generation. If false, start the training from scratch
        boolean resumeTraining = true;

        Evolution evolution = new Evolution(numWindowsAtOnce);
        evolution.start(populationSize, iterations, crossoverProb, mutateProb, maxValues, minValues, resumeTraining);

        // Ensures all the Chrome tabs are closed out
        System.exit(0);
    }
}
