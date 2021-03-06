package ai;

import java.io.Serializable;
import java.util.Objects;

/**
 * An instance of the AI with different parameters that could be modified
 */
public class Individual implements Serializable {

    // Parameters
    private double x, y, width, height, velocity;

    // The final score of the game after running
    private double fitness;

    public Individual() {
    }

    public Individual(double x, double y, double width, double height, double velocity) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.velocity = velocity;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public double getVelocity() {
        return velocity;
    }

    public void setVelocity(double velocity) {
        this.velocity = velocity;
    }

    public double getFitness() {
        return fitness;
    }

    public Individual setFitness(double fitness) {
        this.fitness = fitness;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Individual that = (Individual) o;
        return Double.compare(that.x, x) == 0 &&
                Double.compare(that.y, y) == 0 &&
                Double.compare(that.width, width) == 0 &&
                Double.compare(that.height, height) == 0 &&
                Double.compare(that.velocity, velocity) == 0 &&
                Double.compare(that.fitness, fitness) == 0;
    }

    @Override
    public int hashCode() {

        return Objects.hash(x, y, width, height, velocity, fitness);
    }

    @Override
    public String toString() {
        return "Individual{" +
                "x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                ", velocity=" + velocity +
                ", fitness=" + fitness +
                '}';
    }

}
