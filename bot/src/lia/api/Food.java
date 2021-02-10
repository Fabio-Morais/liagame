package lia.api;

public class Food {
    float x, y;

    public Food(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Food)) {
            return false;
        }

        Food other = (Food) o;
        
        return other.x == this.x && other.y == this.y;
    }
}