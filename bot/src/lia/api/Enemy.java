package lia.api;

public class Enemy {
    public int id;
    public boolean worker;
    public int health;
    public float x;
    public float y;
    public float orientationAngle;
    public Speed speed;
    public Rotation rotation;

    public Enemy(int id, UnitType type, int health, float x, float y, float orientationAngle, Speed speed, Rotation rotation) {
        this.id = id;
        this.worker = type == UnitType.WORKER;
        this.health = health;
        this.x = x;
        this.y = y;
        this.orientationAngle = orientationAngle;
        this.speed = speed;
        this.rotation = rotation;
    }

    public Enemy(OpponentInView o) {
        this.id = o.id;
        this.worker = o.type == UnitType.WORKER;
        this.health = o.health;
        this.x = o.x;
        this.y = o.y;
        this.orientationAngle = o.orientationAngle;
        this.speed = o.speed;
        this.rotation = o.rotation;
    }

    public boolean equals(Object o) {
        
        if (this == o) return true;

        if (!(o instanceof Enemy)) return false;

        Enemy other = (Enemy) o;

        return other.id == this.id;
    }
}
