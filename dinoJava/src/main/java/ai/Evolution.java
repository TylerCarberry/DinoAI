package ai;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ai.Utils.*;
import static ai.Utils.randomDoubleInRange;

public class Evolution {

    private Random random = new Random();
    private ExecutorService exec;

    private double mutateProb;
    private Configuration mins;
    private Configuration maxes;

    public Evolution(int numThreads) {
        Logger logger = Logger.getLogger("");
        logger.setLevel(Level.OFF);
        configureChromeWebDriver();
        exec = Executors.newFixedThreadPool(numThreads);
    }

    public void start(double mutateProb, Configuration maxes, Configuration mins, int size,
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
            ArrayList<Fitness> fitnesses = new ArrayList<>();

            ArrayList<Callable<Void>> tasks = new ArrayList<>();
            for (int j = 0; j < size; j++) {
                double[] entity = entities[j];
                tasks.add(() -> fitness(new Configuration(entity), fitnesses));
            }
            try {
                exec.invokeAll(tasks);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // sort the entities by fitness score
            fitnesses.sort((fitness1, fitness2) -> fitness2.score - fitness1.score);
            int minScore = fitnesses.get(size - 1).score;

            if (!generation(stopScore, minScore))
                break;

            double[][] newPop = new double[size][];

            // add best one to new population
            newPop[0] = fitnesses.get(0).entity;
            int numNewPop = 1;

            while (numNewPop < size) {
                if (random.nextDouble() < crossoverProb && numNewPop + 1 < size) {

                    Configuration parent1 = new Configuration(getRandomElementInList(fitnesses).entity);
                    Configuration parent2 = new Configuration(getRandomElementInList(fitnesses).entity);

                    Configuration child1 = crossover(parent1, parent2);
                    Configuration child2 = crossover(parent1, parent2);

                    newPop[numNewPop] = child1.toArray();
                    numNewPop++;
                    newPop[numNewPop] = child2.toArray();
                    numNewPop++;
                } else {
                    Fitness randomElementInList = getRandomElementInList(fitnesses);
                    double[] ent = mutate(new Configuration(randomElementInList.entity)).toArray();
                    newPop[numNewPop] = ent;
                    numNewPop++;
                }
            }

            for (int j = 0; j < size; j++) {
                double[] entity = fitnesses.get(j).entity;
                String params = entityToQueryParams(new Configuration(entity)) +
                        " " + fitnesses.get(j).score;
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
    private Configuration seed(Configuration max, Configuration min) {
        Configuration initialSeed = new Configuration();
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
     * @param father Configuration of the other parent
     * @return The child configuration
     */
    private Configuration crossover(Configuration mother, Configuration father) {
        Configuration child = new Configuration();

        child.setX(random.nextBoolean() ? mother.getX() : father.getX());
        child.setY(random.nextBoolean() ? mother.getY() : father.getY());
        child.setWidth(random.nextBoolean() ? mother.getWidth() : father.getWidth());
        child.setHeight(random.nextBoolean() ? mother.getHeight() : father.getHeight());
        child.setVelocity(random.nextBoolean() ? mother.getVelocity() : father.getVelocity());

        return child;
    }

    private Void fitness(Configuration entity, ArrayList<Fitness> fitnesses) {
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
        fitnesses.add(new Fitness(entity.toArray(), score));
        return null;
    }

    private boolean generation(int stopScore, int minScore) {
        return minScore < stopScore;
    }

    private Configuration mutate(Configuration configuration) {
        if (Math.random() < mutateProb) {
            configuration.setX(randomDoubleInRange(mins.getX(), maxes.getX()));
        }
        if (Math.random() < mutateProb) {
            configuration.setY(randomDoubleInRange(mins.getY(), maxes.getY()));
        }
        if (Math.random() < mutateProb) {
            configuration.setWidth(randomDoubleInRange(mins.getWidth(), maxes.getWidth()));
        }
        if (Math.random() < mutateProb) {
            configuration.setHeight(randomDoubleInRange(mins.getHeight(), maxes.getHeight()));
        }
        if (Math.random() < mutateProb) {
            configuration.setVelocity(randomDoubleInRange(mins.getVelocity(), maxes.getVelocity()));
        }

        return configuration;
    }

    public String entityToQueryParams(Configuration entity) {
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
