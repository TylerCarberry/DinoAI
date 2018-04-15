package ai;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Evolution {

    private Random random = new Random();
    private ExecutorService exec;

    public Evolution(int numThreads) {
        Logger logger = Logger.getLogger("");
        logger.setLevel(Level.OFF);
        configureChromeWebDriver();
        exec = Executors.newFixedThreadPool(numThreads);
    }

    private void configureChromeWebDriver() {
        System.setProperty("webdriver.chrome.logfile", "\\path\\chromedriver.log");
        System.setProperty("webdriver.chrome.driver", "\\path\\chromedriver.exe");
        System.setProperty("webdriver.chrome.args", "--disable-logging");
        System.setProperty("webdriver.chrome.silentOutput", "true");
        System.setProperty("webdriver.chrome.driver", "/usr/local/Cellar/chromedriver/2.37/bin/chromedriver");
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

    private double randomDoubleInRange(double min, double max) {
        return random.nextDouble() * (max - min) + min;
    }

    private double[] mutate(double[] growthSpeed, Fitness fitness) {
        double[] result = new double[fitness.entity.length];
        for (int i = 0; i < fitness.entity.length; i++) {
            double temp = 10000 / fitness.score;
            double change = (random.nextDouble() * (growthSpeed[i] * 2.0) - growthSpeed[i]) * temp;
            result[i] = fitness.entity[i] + change;
        }
        return result;
    }

    private Pair<Configuration, Configuration> crossover(double[] mother, double[] father) {
        int numGenes = mother.length;

        double[] son = new double[numGenes];
        double[] daughter = new double[numGenes];
        int leftSlice = (int) (random.nextDouble() * numGenes);
        int rightSlice = (int) (random.nextDouble() * numGenes);

        if (leftSlice > rightSlice) {
            int tmp = rightSlice;
            rightSlice = leftSlice;
            leftSlice = tmp;
        }
        for (int i = 0; i < leftSlice; i++) {
            son[i] = father[i];
            daughter[i] = mother[i];
        }
        for (int i = leftSlice; i < rightSlice; i++) {
            son[i] = mother[i];
            daughter[i] = father[i];
        }
        for (int i = rightSlice; i < numGenes; i++) {
            son[i] = father[i];
            daughter[i] = mother[i];
        }
        return new Pair<>(new Configuration(son), new Configuration(daughter));
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

    private double[] mutateOrNot(double probability, double[] growthSpeed, Fitness fitness) {
        if (random.nextDouble() < probability) {
            return mutate(growthSpeed, fitness);
        }
        return fitness.entity;
    }

    private Fitness selectOne(ArrayList<Fitness> fitnesses) {
        double n = (double) fitnesses.size();
        Fitness a = fitnesses.get((int) (random.nextDouble() * n));
        Fitness b = fitnesses.get((int) (random.nextDouble() * n));
        if (a.score - b.score > 0) {
            return a;
        }
        return b;
    }

    private Pair<Configuration, Configuration> selectTwo(ArrayList<Fitness> fitnesses) {
        return new Pair<>(new Configuration(selectOne(fitnesses).entity), new Configuration(selectOne(fitnesses).entity));
    }

    public void start(double[] growthSpeed, double mutateProb, Configuration maxes, Configuration mins, int size,
                      int iterations, int stopScore, double crossoverProb) {
        double[][] entities = new double[size][];

        for (int i = 0; i < size; i++) {
            entities[i] = seed(maxes, mins).toArray();
        }

        for (int i = 0; i < iterations; i++) {
            // get the fitness score for each entity
            ArrayList<Fitness> fitnesses = new ArrayList<Fitness>();

            ArrayList<Callable<Void>> tasks = new ArrayList<Callable<Void>>();
            for (int j = 0; j < size; j++) {
                double[] entity = entities[j];
                tasks.add(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        return fitness(new Configuration(entity), fitnesses);
                    }
                });
            }
            try {
                exec.invokeAll(tasks);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // sort the entities by fitness score
            fitnesses.sort(new Comparator<Fitness>() {
                @Override
                public int compare(Fitness fitness1, Fitness fitness2) {
                    return fitness2.score - fitness1.score;
                }
            });
            int minScore = fitnesses.get(size - 1).score;

            if (!generation(stopScore, minScore))
                break;

            double[][] newPop = new double[size][];

            // add best one to new population
            newPop[0] = fitnesses.get(0).entity;
            int numNewPop = 1;

            while (numNewPop < size) {
                if (random.nextDouble() < crossoverProb && numNewPop + 1 < size) {
                    Pair<Configuration, Configuration> parents = selectTwo(fitnesses);
                    Pair<Configuration, Configuration> children = crossover(parents.first.toArray(), parents.second.toArray());
                    newPop[numNewPop] = children.first.toArray();
                    numNewPop++;
                    newPop[numNewPop] = children.second.toArray();
                    numNewPop++;
                } else {
                    double[] ent = mutateOrNot(mutateProb, growthSpeed, selectOne(fitnesses));
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

    public String entityToQueryParams(Configuration entity) {
        String result = "x=" + entity.getX() +
                "&y=" + entity.getY() +
                "&w=" + entity.getWidth() +
                "&h=" + entity.getHeight() +
                "&v=" + entity.getVelocity();
        return result;
    }
}
