package lia.api;

public class Unit {
    public int id;
    public boolean worker;
    public int health;
    public float x;
    public float y;
    public float orientationAngle;
    public Speed speed;
    public Rotation rotation;
    public boolean canShoot;
    public int nBullets;
    public OpponentInView[] opponentsInView;
    public BulletInView[] opponentBulletsInView;
    public ResourceInView[] resourcesInView;
    public Point[] navigationPath;

    public boolean updated;

    public Unit(int id,
                    UnitType type,
                    int health,
                    float x, float y,
                    float orientationAngle,
                    Speed speed,
                    Rotation rotation,
                    boolean canShoot,
                    int nBullets,
                    OpponentInView[] opponentsInView,
                    BulletInView[] opponentBulletsInView,
                    ResourceInView[] resourcesInView,
                    Point[] navigationPath) {
        this.id = id;
        this.worker = type == UnitType.WORKER;
        this.health = health;
        this.x = x;
        this.y = y;
        this.orientationAngle = orientationAngle;
        this.speed = speed;
        this.rotation = rotation;
        this.canShoot = canShoot;
        this.nBullets = nBullets;
        this.opponentsInView = opponentsInView;
        this.opponentBulletsInView = opponentBulletsInView;
        this.resourcesInView = resourcesInView;
        this.navigationPath = navigationPath;

        this.updated = true;
    }

    public Unit(UnitData d) {
        this.id                 = d.id;
        this.worker             = d.type == UnitType.WORKER;
        this.health             = d.health;
        this.x                  = d.x;
        this.y                  = d.y;
        this.orientationAngle   = d.orientationAngle;
        this.speed              = d.speed;
        this.rotation           = d.rotation;
        this.canShoot           = d.canShoot;
        this.nBullets           = d.nBullets;
        this.opponentsInView    = d.opponentsInView;
        this.opponentBulletsInView = d.opponentBulletsInView;
        this.resourcesInView    = d.resourcesInView;
        this.navigationPath     = d.navigationPath;

        this.updated = true;
    }

    public Unit(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        
        if (o == this) {
            return true;
        }

        if (!(o instanceof Unit)) {
            return false;
        }

        Unit other = (Unit) o;

        return other.id == this.id;
    }
}
