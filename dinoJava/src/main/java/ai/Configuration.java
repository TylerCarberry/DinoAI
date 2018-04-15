package ai;

import java.util.Objects;

public class Configuration {

    private double x, y, width, height, velocity;

    public Configuration() {
    }

    public Configuration(double x, double y, double width, double height, double velocity) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.velocity = velocity;
    }

    public Configuration(double[] attributes) {
        this.x = attributes[0];
        this.y = attributes[1];
        this.width = attributes[2];
        this.height = attributes[3];
        this.velocity = attributes[4];

    }

    public double[] toArray() {
        return new double[]{x, y, width, height, velocity};
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Configuration that = (Configuration) o;
        return Double.compare(that.x, x) == 0 &&
                Double.compare(that.y, y) == 0 &&
                Double.compare(that.width, width) == 0 &&
                Double.compare(that.height, height) == 0 &&
                Double.compare(that.velocity, velocity) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, width, height, velocity);
    }

    @Override
    public String toString() {
        return "Configuration{" +
                "x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                ", velocity=" + velocity +
                '}';
    }
}
