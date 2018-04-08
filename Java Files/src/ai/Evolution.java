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
    System.setProperty("webdriver.chrome.logfile", "\\path\\chromedriver.log");
    System.setProperty("webdriver.chrome.driver", "\\path\\chromedriver.exe");
    System.setProperty("webdriver.chrome.args", "--disable-logging");
    System.setProperty("webdriver.chrome.silentOutput", "true");
    System.setProperty("webdriver.chrome.driver", "/usr/local/Cellar/chromedriver/2.36/bin/chromedriver");
    exec = Executors.newFixedThreadPool(numThreads);
  }

  private class Parents {
    double[] father;
    double[] mother;
    Parents(double[] father, double[] mother) {
      this.father = father;
      this.mother = mother;
    }
  }
  private class Children {
    double[] son;
    double[] daughter;
    Children(double[] son, double[] daughter) {
      this.son = son;
      this.daughter = daughter;
    }
  }
  private class Fitness {
    double[] entity;
    int score;
    Fitness(double[] entity, int score) {
      this.entity = entity;
      this.score = score;
    }
  }

  // maxes and mins need to be the same length
  private double[] seed(int[] maxes, int[] mins) {
    double[] result = new double[maxes.length];
    for(int i = 0; i < maxes.length; i++) {
      result[i] = random.nextDouble() * (maxes[i] - mins[i]) + mins[i];
    }
    return result;
  }

  private double[] mutate(double[] growthSpeed, Fitness fitness) {
    double[] result = new double[fitness.entity.length];
    for(int i = 0; i < fitness.entity.length; i++) {
      double temp = 10000 / fitness.score;
      double change = (random.nextDouble() * (growthSpeed[i] * 2.0) - growthSpeed[i]) * temp;
      result[i] = fitness.entity[i] + change;
    }
    return result;
  }

  private Children crossover(double[] mother, double[] father) {
    double[] son = new double[mother.length];
    double[] daughter = new double[mother.length];
    int ca = (int) (random.nextDouble() * mother.length);
    int cb = (int) (random.nextDouble() * mother.length);
    if (ca > cb) {
      int tmp = cb;
      cb = ca;
      ca = tmp;
    }
    for(int i = 0; i < ca; i++) {
      son[i] = father[i];
      daughter[i] = mother[i];
    }
    for(int i = ca; i < cb; i++) {
      son[i] = mother[i];
      daughter[i] = father[i];
    }
    for(int i = cb; i < mother.length; i++) {
      son[i] = father[i];
      daughter[i] = mother[i];
    }
    return new Children(son, daughter);
  }

  private Void fitness(double[] entity, ArrayList<Fitness> fitnesses) {
    String params = entityToString(entity);
    // this is where the game is played
    WebDriver driver = new ChromeDriver();
    driver.get("file:///Users/ctr/DinoAI/index.html?" + params);
    while(!driver.findElement(By.id("game-over")).getText().equals("game over")) {
      try {
        Thread.sleep(1000);
      } catch(InterruptedException e) {
        e.printStackTrace();
      }
    }
    int score = Integer.parseInt(driver.findElement(By.id("score")).getText());
    driver.quit();
    fitnesses.add(new Fitness(entity, score));
    return null;
  }

  private boolean generation(int stopScore, int minScore) {
    return minScore < stopScore;
  }

  private double[] mutateOrNot(double probability, double[] growthSpeed, Fitness fitness) {
    if(random.nextDouble() < probability) {
      return mutate(growthSpeed, fitness);
    }
    return fitness.entity;
  }

  private Fitness selectOne(ArrayList<Fitness> fitnesses) {
    double n = (double) fitnesses.size();
    Fitness a = fitnesses.get((int) (random.nextDouble() * n));
    Fitness b = fitnesses.get((int) (random.nextDouble() * n));
    if(a.score - b.score > 0) {
      return a;
    }
    return b;
  }

  private Parents selectTwo(ArrayList<Fitness> fitnesses) {
    return new Parents(selectOne(fitnesses).entity, selectOne(fitnesses).entity);
  }

  public void start(double[] growthSpeed, double mutateProb, int[] maxes, int[] mins, int size,
      int iterations, int stopScore, double crossoverProb) {
    double[][] entities = new double[size][];

    for(int i = 0; i < size; i++) {
      entities[i] = seed(maxes, mins);
    }

    for(int i = 0; i < iterations; i++) {
      // get the fitness score for each entity
      ArrayList<Fitness> fitnesses = new ArrayList<Fitness>();

      ArrayList<Callable<Void>> tasks = new ArrayList<Callable<Void>>();
      for(int j = 0; j < size; j++) {
        double[] entity = entities[j];
        tasks.add(new Callable<Void>() {
          @Override
          public Void call() throws Exception {
            return fitness(entity, fitnesses);
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
        public int compare(Fitness o1, Fitness o2) {
          return  o2.score - o1.score;
        }
      });
      int minScore = fitnesses.get(size - 1).score;

      if(!generation(stopScore, minScore))
        break;

      double[][] newPop = new double[size][];

      // add best one to new population
      newPop[0] = fitnesses.get(0).entity;
      int numNewPop = 1;

      while(numNewPop < size) {
        if(random.nextDouble() < crossoverProb && numNewPop + 1 < size) {
          Parents parents = selectTwo(fitnesses);
          Children children = crossover(parents.mother, parents.father);
          newPop[numNewPop] = children.daughter;
          numNewPop++;
          newPop[numNewPop] = children.son;
          numNewPop++;
        }
        else {
          double[] ent = mutateOrNot(mutateProb, growthSpeed, selectOne(fitnesses));
          newPop[numNewPop] = ent;
          numNewPop++;
        }
      }

      for(int j = 0; j < size; j++) {
        double[] entity = fitnesses.get(j).entity;
        String params = entityToString(entity) +
            " " + fitnesses.get(j).score;
        System.out.println(params);
      }
      System.out.println(fitnesses.get(0).score);
      entities = newPop;
    }
  }

  public String entityToString(double[] entity) {
    String result = "x=" + entity[0] +
        "&y=" + entity[1] +
        "&w=" + entity[2] +
        "&h=" + entity[3] +
        "&v=" + entity[4];
    return result;
  }
}
