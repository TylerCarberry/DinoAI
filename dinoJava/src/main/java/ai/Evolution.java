package ai;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ai.Utils.*;

/**
 * Runs the genetic algorithm on the Chrome Dinosaur Game
 */
public class Evolution {

    private Random random = new Random();
    private ExecutorService exec;

    private double mutateProb;
    private Individual mins;
    private Individual maxes;

    // The current best score of the population
    private double bestScore = 0;

    // Prevents overlapping of windows if multiple instances are running at once
    private int windowPosition = 0;

    /**
     * Create the genetic algorithm
     * @param numThreads The number of instances to be tested at once
     */
    public Evolution(int numThreads) {
        Logger logger = Logger.getLogger("");
        logger.setLevel(Level.OFF);
        configureChromeWebDriver();
        exec = Executors.newFixedThreadPool(numThreads);
    }

    /**
     * Start creating the genetic algorithm
     * @param populationSize Number of individuals in each population
     * @param iterations Number of populations to run before stopping
     * @param crossoverProb Likelyhood of breeding to occur. Between 0 and 1
     * @param mutateProb Likelihood of mutations to occur. Between 0 and 1
     * @param maxes An individual with the highest value of fields as allowed by the game
     * @param mins An individual with the lowest value of fields as allowed by the game
     * @param resumeLastTraining If true, load the last population from a file
     */
    public void start(int populationSize, int iterations, double crossoverProb, double mutateProb,
                      Individual maxes, Individual mins, boolean resumeLastTraining) {
        this.mutateProb = mutateProb;
        this.mins = mins;
        this.maxes = maxes;

        List<Individual> population;
        if (resumeLastTraining) {
            population = getPopulationFromFile();
        } else {
            population = new ArrayList<>();
        }

        if (population.isEmpty()) {
            for (int i = 0; i < populationSize; i++) {
                population.add(seed(maxes, mins));
            }
        }

        System.out.println("n\tMin \tQ1  \tMean\tMed  \tQ3  \tMax");

        for (int iteration = 0; iteration < iterations; iteration++) {

            // Assign each individual to a different thread
            ArrayList<Callable<Void>> tasks = new ArrayList<>();
            for (Individual individual : population) {
                tasks.add(() -> calculateFitness(individual));
            }

            try {
                exec.invokeAll(tasks);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Sort the entities by their final score
            population.sort((individual1, individual2) -> (int) (individual2.getFitness() - individual1.getFitness()));

            // Print the statistics to the console for debugging
            double mean = population.parallelStream().mapToDouble(Individual::getFitness).sum() / population.size();
            double median = population.get((int) (population.size() * 0.5)).getFitness();
            double q1 = population.get((int) (population.size() * 0.75)).getFitness();
            double q3 = population.get((int) (population.size() * 0.25)).getFitness();
            double min = population.get(population.size() - 1).getFitness();
            bestScore = population.get(0).getFitness();
            System.out.println(iteration + "\t" + min + "\t" + q1 + "\t" + mean + "\t" + median + "\t" + q3 + "\t" + bestScore);

            // Perform variations on the population before creating the next generation
            List<Individual> newPopulation = new ArrayList<>();

            // Copy the best individuals over directly, with no crossover or mutations
            for (int i = 0; i < populationSize * 0.10; i ++) {
                newPopulation.add(population.get(i));
            }

            // Only the top half of individuals are available to breed
            List<Individual> bestIndividuals = population.subList(0, (int) (population.size() * 0.5));

            while (newPopulation.size() < populationSize) {
                Individual individual;

                // Crossover
                if (random.nextDouble() < crossoverProb) {
                    Individual parent1 = getRandomElementInList(bestIndividuals);
                    Individual parent2 = getRandomElementInList(bestIndividuals);
                    assert parent1 != null && parent2 != null;
                    individual = crossover(parent1, parent2);
                } else {
                    individual = getRandomElementInList(bestIndividuals);
                }

                // Mutations
                assert individual != null;
                individual = potentiallyMutate(individual);

                individual.setFitness(0);
                newPopulation.add(individual);
            }

            population = newPopulation;

            if (!resumeLastTraining) {
                savePopulationToFile(population);
            }
        }
    }

    /**
     * Create the initial seed were each of the values are between max and min
     * Precondition: max and min are the same length
     *
     * @param max The upper bound of values
     * @param min Lower bound of values
     * @return The initial seed
     */
    private Individual seed(Individual max, Individual min) {
        Individual initialSeed = new Individual();
        initialSeed.setX(randomDoubleInRange(min.getX(), max.getX()));
        initialSeed.setY(randomDoubleInRange(min.getY(), max.getY()));
        initialSeed.setWidth(randomDoubleInRange(min.getX(), max.getWidth()));
        initialSeed.setHeight(randomDoubleInRange(min.getX(), max.getHeight()));
        initialSeed.setVelocity(randomDoubleInRange(min.getX(), max.getVelocity()));

        return initialSeed;
    }

    /**
     * Create a child with each of the attributes randomly being chosen from the mother or father with no mutations
     * @param mother The configuration from one of the parents
     * @param father Individual of the other parent
     * @return The child configuration
     */
    private Individual crossover(Individual mother, Individual father) {
        Individual child = new Individual();

        child.setX(randomDoubleInRange(mother.getX(), father.getX()));
        child.setY(randomDoubleInRange(mother.getY(), father.getY()));
        child.setWidth(randomDoubleInRange(mother.getWidth(), father.getWidth()));
        child.setHeight(randomDoubleInRange(mother.getHeight(), father.getHeight()));
        child.setVelocity(randomDoubleInRange(mother.getVelocity(), father.getVelocity()));

        return child;
    }

    /**
     * Determine the fitness of an individual as calculated by the final score of the game
     * This is where the game is played
     * @param individual The individual to determine the fitness of
     */
    private Void calculateFitness(Individual individual) {
        String params = entityToQueryParams(individual);

        // Move each window slightly to prevent overlapping
        windowPosition = (windowPosition + 200) % 800;

        // Shrink the window slightly
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--window-size=800,500", "--window-position="+windowPosition+",0");

        WebDriver driver = new ChromeDriver(chromeOptions);
        driver.get("file:///Users/Tyler/DinoAI/index.html?" + params);

        // Wait until the game has completed
        while (!driver.findElement(By.id("game-over")).getText().equals("game over")) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // Read the final score from a textbox on the screen
        int score = Integer.parseInt(driver.findElement(By.id("score")).getText());
        driver.quit();

        individual.setFitness(score);

        return null;
    }

    /**
     * Randomly mutate the attributes of the individual
     * @param individual The individual to change the attributes of
     * @return The individual with 0 or more of its attributes changed
     */
    private Individual potentiallyMutate(Individual individual) {
        // Decrease the chance of mutations as the AI improves
        double temperature = 1/Math.log(Math.max(bestScore, 10));

        // Since there are 5 different variables, divide the overall chance to find the
        // probability of an individual mutation
        double probability = mutateProb * 0.20;

        if (Math.random() < probability) {
            individual.setX(mutateWithTemperature(individual.getX(), maxes.getX(), mins.getX(), temperature));
        }
        if (Math.random() < probability) {
            individual.setY(mutateWithTemperature(individual.getY(), maxes.getY(), mins.getY(), temperature));
        }
        if (Math.random() < probability) {
            individual.setWidth(mutateWithTemperature(individual.getWidth(), maxes.getWidth(), mins.getWidth(), temperature));
        }
        if (Math.random() < probability) {
            individual.setHeight(mutateWithTemperature(individual.getHeight(), maxes.getHeight(), mins.getHeight(), temperature));
        }
        if (Math.random() < probability) {
            individual.setVelocity(mutateWithTemperature(individual.getVelocity(), maxes.getVelocity(), mins.getVelocity(), temperature));
        }

        return individual;
    }

    /**
     * Perform a mutation where the amount of mutation is dependent on the current temperature of the game
     * @return the mutated value
     */
    private double mutateWithTemperature(double current, double max, double min, double temperature) {
        double maximumChange = Math.min(current - min, max - current);
        double possibleChange = maximumChange * temperature;

        return randomDoubleNormalDistribution(current - possibleChange, current + possibleChange);
    }

    public String entityToQueryParams(Individual entity) {
        String result = "x=" + entity.getX() +
                "&y=" + entity.getY() +
                "&w=" + entity.getWidth() +
                "&h=" + entity.getHeight() +
                "&v=" + entity.getVelocity();
        return result;
    }

    private void configureChromeWebDriver() {
        System.setProperty("webdriver.chrome.logfile", "\\path\\chromedriver.log");
        System.setProperty("webdriver.chrome.driver", "\\path\\chromedriver.exe");
        System.setProperty("webdriver.chrome.args", "--disable-logging");
        System.setProperty("webdriver.chrome.silentOutput", "true");
        System.setProperty("webdriver.chrome.driver", "/usr/local/Cellar/chromedriver/2.37/bin/chromedriver");
    }

    private void savePopulationToFile(List<Individual> population) {
        try {
            FileOutputStream f = new FileOutputStream(new File("storedPopulation.txt"));
            ObjectOutputStream o = new ObjectOutputStream(f);

            // Write objects to file
            o.writeObject(population);

            o.close();
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<Individual> getPopulationFromFile() {
        List<Individual> population = new ArrayList<>();
        try {
            FileInputStream fi = new FileInputStream(new File("storedPopulation.txt"));

            ObjectInputStream oi = new ObjectInputStream(fi);

            // Read objects
            population = (List<Individual>) oi.readObject();

            oi.close();
            fi.close();
        } catch (IOException | ClassNotFoundException e) {
            // No previous population found
        }

        return population;
    }
}
