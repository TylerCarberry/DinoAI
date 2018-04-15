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

    public void start(double mutateProb, Individual maxes, Individual mins, int size,
                      int iterations, int stopScore, double crossoverProb) {

        this.mutateProb = mutateProb;
        this.mins = mins;
        this.maxes = maxes;

        double[][] entities = new double[size][];

        for (int i = 0; i < size; i++) {
            entities[i] = seed(maxes, mins).toArray();
        }

        for (int i = 0; i < iterations; i++) {
            // get the fitness score for each entity
            ArrayList<Individual> fitnesses = new ArrayList<>();

            ArrayList<Callable<Void>> tasks = new ArrayList<>();
            for (int j = 0; j < size; j++) {
                double[] entity = entities[j];
                tasks.add(() -> fitness(new Individual(entity), fitnesses));
            }
            try {
                exec.invokeAll(tasks);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // sort the entities by fitness score
            fitnesses.sort((fitness1, fitness2) -> (int) (fitness2.getFitness() - fitness1.getFitness()));
            double minScore = fitnesses.get(size - 1).getFitness();

            if (!generation(stopScore, minScore))
                break;

            double[][] newPop = new double[size][];

            // add best one to new population
            newPop[0] = fitnesses.get(0).toArray();
            int numNewPop = 1;

            while (numNewPop < size) {
                if (random.nextDouble() < crossoverProb && numNewPop + 1 < size) {

                    Individual parent1 = getRandomElementInList(fitnesses);
                    Individual parent2 = getRandomElementInList(fitnesses);

                    Individual child1 = crossover(parent1, parent2);
                    Individual child2 = crossover(parent1, parent2);

                    newPop[numNewPop] = child1.toArray();
                    numNewPop++;
                    newPop[numNewPop] = child2.toArray();
                    numNewPop++;
                } else {
                    Individual randomElementInList = getRandomElementInList(fitnesses);
                    double[] ent = mutate(randomElementInList).toArray();
                    newPop[numNewPop] = ent;
                    numNewPop++;
                }
            }

            for (int j = 0; j < size; j++) {
                double[] entity = fitnesses.get(j).toArray();
                String params = entityToQueryParams(new Individual(entity)) +
                        " " + fitnesses.get(j).getFitness();
                //System.out.println(params);
            }
            //System.out.println(fitnesses.get(0).score);
            entities = newPop;
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

        child.setX(random.nextBoolean() ? mother.getX() : father.getX());
        child.setY(random.nextBoolean() ? mother.getY() : father.getY());
        child.setWidth(random.nextBoolean() ? mother.getWidth() : father.getWidth());
        child.setHeight(random.nextBoolean() ? mother.getHeight() : father.getHeight());
        child.setVelocity(random.nextBoolean() ? mother.getVelocity() : father.getVelocity());

        return child;
    }

    private Void fitness(Individual entity, List<Individual> fitnesses) {
        String params = entityToQueryParams(entity);
        // this is where the game is played
        WebDriver driver = new ChromeDriver();
        driver.get("file:///Users/Tyler/DinoAI/index.html?" + params);
        while (!driver.findElement(By.id("game-over")).getText().equals("game over")) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        int score = Integer.parseInt(driver.findElement(By.id("score")).getText());
        System.out.println(score);
        driver.quit();
        Individual individual = new Individual(entity.toArray()).setFitness(score);
        fitnesses.add(individual);
        return null;
    }

    private boolean generation(double stopScore, double minScore) {
        return minScore < stopScore;
    }

    private Individual mutate(Individual individual) {
        if (Math.random() < mutateProb) {
            individual.setX(randomDoubleInRange(mins.getX(), maxes.getX()));
        }
        if (Math.random() < mutateProb) {
            individual.setY(randomDoubleInRange(mins.getY(), maxes.getY()));
        }
        if (Math.random() < mutateProb) {
            individual.setWidth(randomDoubleInRange(mins.getWidth(), maxes.getWidth()));
        }
        if (Math.random() < mutateProb) {
            individual.setHeight(randomDoubleInRange(mins.getHeight(), maxes.getHeight()));
        }
        if (Math.random() < mutateProb) {
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
