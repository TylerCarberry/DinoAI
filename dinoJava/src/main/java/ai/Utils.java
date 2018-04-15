package ai;

import java.util.List;

public class Utils {

    public static double randomDoubleInRange(double min, double max) {
        return Math.random() * (max - min) + min;
    }

    public static <T> T getRandomElementInList(List<T> list) {
        if (list.isEmpty()) {
            return null;
        }
        return list.get((int) (Math.random() * list.size()));
    }
}
