package ai;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ai.Utils.getRandomElementInList;
import static ai.Utils.randomDoubleInRange;

public class Evolution {

    private Random random = new Random();
    private ExecutorService exec;

    private double mutateProb;
    private Individual mins;
    private Individual maxes;

    public Evolution(int numThreads) {
        Logger logger = Logger.getLogger("");
        logger.setLevel(Level.OFF);
        configureChromeWebDriver();
        exec = Executors.newFixedThreadPool(numThreads);
    }

    public void start(int populationSize, int iterations, double crossoverProb, double mutateProb, Individual maxes, Individual mins) {

        this.mutateProb = mutateProb;
        this.mins = mins;
        this.maxes = maxes;

        List<Individual> population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            population.add(seed(maxes, mins));
        }

        System.out.println("n\tMin \tQ1  \tMean\tMed  \tQ3  \tMax");
        for (int iteration = 0; iteration < iterations; iteration++) {

            ArrayList<Callable<Void>> tasks = new ArrayList<>();
            for (Individual individual : population) {
                tasks.add(() -> calculateFitness(individual));
            }

            try {
                exec.invokeAll(tasks);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // sort the entities by calculateFitness score
            population.sort((individual1, individual2) -> (int) (individual2.getFitness() - individual1.getFitness()));

            double mean = population.parallelStream().mapToDouble(Individual::getFitness).sum() / population.size();
            double median = population.get((int) (population.size() * 0.5)).getFitness();
            double q1 = population.get((int) (population.size() * 0.75)).getFitness();
            double q3 = population.get((int) (population.size() * 0.25)).getFitness();
            double min = population.get(population.size() - 1).getFitness();
            double max = population.get(0).getFitness();

            System.out.println(iteration + "\t" + min + "\t" + q1 + "\t" + mean + "\t" + median + "\t" + q3 + "\t" + max);

            List<Individual> newPopulation = new ArrayList<>();

            // Copy the best 25% over directly, with no crossover or mutations
            for (int i = 0; i < populationSize * 0.25; i ++) {
                newPopulation.add(population.get(i));
            }

            List<Individual> bestIndividuals = population.subList(0, (int) (population.size() * 0.25));

            while (newPopulation.size() < populationSize) {

                Individual individual;

                if (random.nextDouble() < crossoverProb) {
                    Individual parent1 = getRandomElementInList(bestIndividuals);
                    Individual parent2 = getRandomElementInList(bestIndividuals);
                    assert parent1 != null && parent2 != null;
                    individual = crossover(parent1, parent2);
                } else {
                    individual = getRandomElementInList(bestIndividuals);
                }

                assert individual != null;
                individual.setFitness(0);
                individual = potentiallyMutate(individual);
                newPopulation.add(individual);
            }

            population = newPopulation;
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

    private Void calculateFitness(Individual individual) {
        String params = entityToQueryParams(individual);
        // this is where the game is played
        WebDriver driver = new ChromeDriver();
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
        //System.out.println(score);
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
        // Since there are different attributes that could be mutated, divide the overall likelihood
        double probability = mutateProb / 5;

        if (Math.random() < probability) {
            individual.setX(randomDoubleInRange(mins.getX(), maxes.getX()));
        }
        if (Math.random() < probability) {
            individual.setY(randomDoubleInRange(mins.getY(), maxes.getY()));
        }
        if (Math.random() < probability) {
            individual.setWidth(randomDoubleInRange(mins.getWidth(), maxes.getWidth()));
        }
        if (Math.random() < probability) {
            individual.setHeight(randomDoubleInRange(mins.getHeight(), maxes.getHeight()));
        }
        if (Math.random() < probability) {
            individual.setVelocity(randomDoubleInRange(mins.getVelocity(), maxes.getVelocity()));
        }

        return individual;
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
}
