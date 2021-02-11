import lia.api.*;
import lia.*;

import java.lang.Math;
import java.util.*;

public class MyBot implements Bot {

    GameState state;
    Api api;

    List<Unit> units = new ArrayList<Unit>();

    List<Enemy> visibleEnemies = new ArrayList<Enemy>();

    List<ResourceInView> food = new ArrayList<ResourceInView>();
    List<ResourceInView> foodToPick = new ArrayList<ResourceInView>();

    // - GameState reference: https://docs.liagame.com/api/#gamestate
    // - Api reference:       https://docs.liagame.com/api/#api-object
    @Override
    public void update(GameState gameState, Api gameApi) {

        updateState(gameState, gameApi);
        spawnUnits();

        roam();
        checkForFood();
        checkForEnemies();
        collectResource();
        attack();
    }

    //region RandomStuff
    public void updateState(GameState gameState, Api gameApi) {
        this.state = gameState;
        this.api = gameApi;

        if (units.size() == 0) {
            for (UnitData data : state.units) {
                units.add(new Unit(data));
            }
        }
        else {
            updateUnits();
        }
    }
    //endregion

    //region UnitMethods
    public void updateUnits() {
        for (int i = 0; i < state.units.length; i++) {
            Unit unit = unitsContainsUnitWithId(state.units[i].id);
            if (unit != null) {
                updateUnit(unit, state.units[i]);
            }
            else if (unit == null) {
                units.add(new Unit(state.units[i]));
            }
        }

        // we add all the units that we want to remove from <units> list to <toRemove> list
        List<Unit> toRemove = new LinkedList<Unit>();

        for (Unit unit : units) {
            if (!unit.updated) {
                toRemove.add(unit);
            }
            else {
                unit.updated = false;
            }
        }

        // now we remove the units from the <units> list
        for (Unit unit : toRemove) {
            units.remove(unit);
        }
    }

    public void updateUnit(Unit u, UnitData d) {
        u.health             = d.health;
        u.x                  = d.x;
        u.y                  = d.y;
        u.orientationAngle   = d.orientationAngle;
        u.speed              = d.speed;
        u.rotation           = d.rotation;
        u.canShoot           = d.canShoot;
        u.nBullets           = d.nBullets;
        u.opponentsInView    = d.opponentsInView;
        u.opponentBulletsInView = d.opponentBulletsInView;
        u.resourcesInView    = d.resourcesInView;
        u.navigationPath     = d.navigationPath;
        u.updated = true;
    }

    public Unit unitsContainsUnitWithId(int id) {
        for (Unit u : units) {
            if (u.id == id) {
                return u;
            }
        }
        return null;
    }

    public void checkForEnemies() {

        visibleEnemies.clear();

        for (Unit unit : units) {

            if (unit.opponentsInView.length > 0) {
                for (OpponentInView opponent : unit.opponentsInView) {

                    visibleEnemies.add(new Enemy(opponent));
                }
            }
        }
    }

    public boolean enemiesContainEnemyWithId(int id) {
        return false;
    }

    public void checkForFood() {
        for (Unit unit : units) {
            if (unit.resourcesInView.length > 0) {

                for (ResourceInView res : unit.resourcesInView) {
                    if (!(food.contains(res) || foodToPick.contains(res))) {
                        food.add(res);
                    }
                }
            }
        }

        // odstranimo najkasneje dodane
        if (food.size() > 20) {
            for (int i = 0; i < food.size() - 20; i++) {
                food.remove(i);
            }
        }
    }

    public void spawnUnits() {
        int workers = 0;
        for (UnitData unit : state.units) {
            if (unit.type == UnitType.WORKER) workers++;
        }

        if (state.time < 120f && workers / (float) state.units.length < 0.6f && workers < 10 && state.resources >= Constants.WORKER_PRICE) {
            api.spawnUnit(UnitType.WORKER);
        }
        else if (state.resources >= Constants.WARRIOR_PRICE) {
            api.spawnUnit(UnitType.WARRIOR);
        }
    }

    public  void roam() {
        for (UnitData unit : state.units) {
            if (unit.navigationPath.length == 0) {
                while (true) {
                    int x = (int) (Math.random() * Constants.MAP_WIDTH);
                    int y = (int) (Math.random() * Constants.MAP_HEIGHT);

                    if (!Constants.MAP[x][y]) {
                        api.navigationStart(unit.id, x, y);
                        break;
                    }
                }
            }
        }
    }

    public void collectResource() {
        for (UnitData unit : state.units) {
            if (unit.type == UnitType.WORKER && unit.resourcesInView.length > 0) {
                ResourceInView resource = unit.resourcesInView[0];
                api.navigationStart(unit.id, resource.x, resource.y);
            }
        }
    }

    public void attack() {
        for (UnitData unit : state.units) {

            if (unit.type == UnitType.WARRIOR && unit.opponentsInView.length > 0) {
                OpponentInView enemy = unit.opponentsInView[0];

                float aimAngle;

                aimAngle = calculateAimAngle(unit, enemy);

                if (aimAngle > 0 && aimAngle < 3f) {
                    api.setRotation(unit.id, Rotation.SLOW_LEFT);
                }
                else if (aimAngle < 0f && aimAngle > -3f) {
                    api.setRotation(unit.id, Rotation.SLOW_RIGHT);
                }
                else if (aimAngle < 0f) {
                    api.setRotation(unit.id, Rotation.RIGHT);
                }
                else {
                    api.setRotation(unit.id, Rotation.LEFT);
                }

                float epsilonAngle = epsilonAngle(unit, enemy);
                if ((aimAngle < epsilonAngle && aimAngle >= 0) || (aimAngle > -epsilonAngle && aimAngle <= 0)) {
                    api.shoot(unit.id);
                }

                float enemyRotationRelativeToUnit = MathUtil.angleBetweenUnitAndPoint
                    (enemy.x, enemy.y, enemy.orientationAngle, unit.x, unit.y);

                if ((enemyRotationRelativeToUnit > 110f || enemyRotationRelativeToUnit < -110f) && enemy.speed == Speed.FORWARD &&
                    MathUtil.distance(unit.x, unit.y, enemy.x, enemy.y) > 10f) {
                    api.setSpeed(unit.id, Speed.FORWARD);
                }
                else {
                    api.setSpeed(unit.id, Speed.NONE);
                }
            }
            else if (unit.type == UnitType.WARRIOR) {
                goToClosestTarget(unit);
            }
        }
    }

    public void goToClosestTarget(UnitData unit) {
        Enemy closest = null;
        float minDist = Constants.VIEWING_AREA_WIDTH + Constants.MAP_HEIGHT;
        float dist = 0;

        for (Enemy enemy : visibleEnemies) {

            dist = MathUtil.distance(unit.x, unit.y, enemy.x, enemy.y);
            if (dist < minDist) {
                minDist = dist;
                closest = enemy;
            }
        }

        if (closest != null) {
            api.navigationStart(unit.id, closest.x, closest.y);
        }
    }
    //endregion

    public float epsilonAngle(UnitData unit, OpponentInView enemy) {
        float unitRadius = Constants.UNIT_DIAMETER / 2f;
        float distance = MathUtil.distance(unit.x, unit.y, enemy.x, enemy.y);
        double hipo = Math.sqrt(Math.pow((double) unitRadius, 2) + Math.pow((double) distance, 2));

        return (float) Math.toDegrees(Math.asin(unitRadius / (float) hipo)) / 2f;
    }

    public float calculateAimAngle(UnitData unit, OpponentInView enemy) {

        float velocity = 0;
        if (enemy.speed == Speed.FORWARD) velocity = Constants.UNIT_FORWARD_VELOCITY;
        else if (enemy.speed == Speed.BACKWARD) velocity = Constants.UNIT_BACKWARD_VELOCITY;

        if (velocity != 0) {
            float angle = enemy.orientationAngle;

            double velocityX = 1;
            double velocityY = 1;

            if (angle <= 90f) {
                angle = angle;
            }
            else if (angle <= 180f) {
                angle = 180f - angle;
                velocityX = -1;
            }
            else if (angle <= 270f) {
                angle = angle - 180f;
                velocityX = -1;
                velocityY = -1;
            }
            else {
                angle = -angle;
                velocityY = -1;
            }

            velocityX *= Math.cos(Math.toRadians(angle)) * velocity;
            velocityY *= Math.sin(Math.toRadians(angle)) * velocity;

            double a = Math.pow(velocityX, 2) + Math.pow(velocityY, 2) - Math.pow(Constants.BULLET_VELOCITY, 2);
            double b = 2 * (velocityX * (enemy.x - unit.x) + velocityY * (enemy.y - unit.y));
            double c = Math.pow(enemy.x - unit.x, 2) + Math.pow(enemy.y - unit.y, 2);

            double disc = Math.pow(b, 2) - 4 * a * c;

            double t1 = (-b + Math.sqrt(disc)) / (2 * a);
            double t2 = (-b - Math.sqrt(disc)) / (2 * a);
            double t;

            if (t1 < t2 && t1 > 0) {
                t = t1;
            }
            else {
                t = t2;
            }

            double aimX = t * velocityX + enemy.x;
            double aimY = t * velocityY + enemy.y;

            return (float) MathUtil.angleBetweenUnitAndPoint(unit, (float) aimX, (float) aimY);
        }

        return MathUtil.angleBetweenUnitAndPoint(unit, enemy.x, enemy.y);
    }


    public static void main(String[] args) throws Exception {
        NetworkingClient.connectNew(args, new MyBot());
    }
}
