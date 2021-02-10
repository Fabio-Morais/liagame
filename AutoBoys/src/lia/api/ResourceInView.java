package lia.api;

import java.util.Objects;

public class ResourceInView {
    public float x;
    public float y;

    public ResourceInView(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceInView that = (ResourceInView) o;
        return Float.compare(that.x, x) == 0 && Float.compare(that.y, y) == 0;
    }

    @Override
    public String toString() {
        return "ResourceInView{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
