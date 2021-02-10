package lia.api;

public class ResourceInView {
    public float x;
    public float y;

    public ResourceInView(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public boolean compare(Object o) {

        if (this == o) return true;

        if (!(o instanceof ResourceInView)) return false;

        ResourceInView other = (ResourceInView) o;

        return other.x == this.x && other.y == this.y;
    }
}
