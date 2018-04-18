package ai;

import java.util.List;
import java.util.Random;

/**
 * Collection of mathematical functions
 */
public class Utils {

    /**
     * @return a random double between min and max
     */
    public static double randomDoubleInRange(double min, double max) {
        return Math.random() * (max - min) + min;
    }

    /**
     * @return a random double between min and max that occurs on a normal distribution.
     * Values near the mean are more likely to occur than values closer to min and max
     */
    public static double randomDoubleNormalDistribution(double min, double max) {
        double prob = new Random().nextGaussian();
        return (max - min) * prob + min;
    }

    public static <T> T getRandomElementInList(List<T> list) {
        if (list.isEmpty()) {
            return null;
        }
        return list.get((int) (Math.random() * list.size()));
    }
}
