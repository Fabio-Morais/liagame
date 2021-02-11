import lia.api.*;
import lia.*;

import java.awt.*;

/**
 * Initial implementation keeps picking random locations on the map
 * and sending units there. Worker units collect resources if they
 * see them while warrior units shoot if they see opponents.
 */
public class MyBot implements Bot {

    // This method is called 10 times per game second and holds current
    // game state. Use Api object to call actions on your units.
    // - GameState reference: https://docs.liagame.com/api/#gamestate
    // - Api reference:       https://docs.liagame.com/api/#api-object
    @Override
    public void update(GameState state, Api api) {

        // If you have enough resources to spawn a new warrior unit then spawn it.
        if (state.resources >= Constants.WARRIOR_PRICE) {
            api.spawnUnit(UnitType.WARRIOR);
        }

        // We iterate through all of our units that are still alive.
        for (int i = 0; i < state.units.length; i++) {
            UnitData unit = state.units[i];

            // If the unit is not going anywhere, we send it
            // to a random valid location on the map.
            if (unit.navigationPath.length == 0) {

                // Generate new x and y until you get a position on the map
                // where there is no obstacle. Then move the unit there.
                while (true) {
                    int x = (int) (Math.random() * Constants.MAP_WIDTH);
                    int y = (int) (Math.random() * Constants.MAP_HEIGHT);

                    // Map is a 2D array of booleans. If map[x][y] equals false it means that
                    // at (x,y) there is no obstacle and we can safely move our unit there.
                    if (!Constants.MAP[x][y]) {
                        api.navigationStart(unit.id, x, y);
                        break;
                    }
                }
            }

            // If the unit is a worker and it sees at least one resource
            // then make it go to the first resource to collect it.
            if (unit.type == UnitType.WORKER && unit.resourcesInView.length > 0) {
                ResourceInView resource = unit.resourcesInView[0];
                api.navigationStart(unit.id, resource.x, resource.y);
            }

            // If the unit is a warrior and it sees an opponent then start shooting
            if (unit.type == UnitType.WARRIOR && unit.opponentsInView.length > 0) {

                if (unit.type == UnitType.WARRIOR && unit.opponentsInView.length > 0) {

                    OpponentInView enemy = ChooseTarget(unit);
                    float RotationAngle = PredictPosition(unit, enemy);
                    Shoot(api, unit, RotationAngle, enemy);

                }
            }
        }
    }

        public OpponentInView ChooseTarget(UnitData u){

            //If see more than 1 Choose the target with less health
            OpponentInView enemy = u.opponentsInView[0];

            if (u.opponentsInView.length > 1) {
                for (int j = 1; j < u.opponentsInView.length; j++) {
                    if (enemy.health > u.opponentsInView[j].health) {
                        enemy = u.opponentsInView[j];
                    }
                }
            }
            return enemy;

        }

        public float PredictPosition(UnitData u, OpponentInView enemy){

            double vX, vY;
            vX = vY = 1;

            float v ;
            float EnemyAngle = enemy.orientationAngle;
            float unitAngle = u.orientationAngle;

            if (enemy.speed == Speed.FORWARD){
                v = Constants.UNIT_FORWARD_VELOCITY;
            }else if (enemy.speed == Speed.BACKWARD){
                v = Constants.UNIT_BACKWARD_VELOCITY;
            }else{
                return MathUtil.angleBetweenUnitAndPoint(u, enemy.x, enemy.y);
            }

            vX *= Math.cos( Math.toRadians( EnemyAngle ) ) * v;
            vY *= Math.sin( Math.toRadians( EnemyAngle ) ) * v;

            double t;
            double t1 = (enemy.x - u.x) / (Constants.BULLET_VELOCITY * Math.cos(Math.toRadians( unitAngle ) ) - vX);
            double t2 = (enemy.y - u.y) / (Constants.BULLET_VELOCITY * Math.sin(Math.toRadians( unitAngle ) ) - vY);

            if(t1 == t2){
                t = t1;
            } else {
                t = t2;
            }

            float x = (float) (vX * t + enemy.x);
            float y = (float) (vY * t + enemy.y);

            return MathUtil.angleBetweenUnitAndPoint(u, x, y);
        }


        public void Shoot(Api api, UnitData u, float rotationAngle, OpponentInView enemy) {

            if (rotationAngle > 0 && rotationAngle < 2f) {
                api.setRotation(u.id, Rotation.SLOW_LEFT);
            }
            else if (rotationAngle < 0 && rotationAngle > -2f) {
                api.setRotation(u.id, Rotation.SLOW_RIGHT);
            }
            else if (rotationAngle > 0 ) {
                api.setRotation(u.id, Rotation.LEFT);
            }
            else {
                api.setRotation(u.id, Rotation.RIGHT);
            }

            float shootAngle;
            float enemyRadius = Constants.UNIT_DIAMETER / 2f;
            float enemyDistance = MathUtil.distance(u.x, u.y, enemy.x, enemy.y);
            double offset = Math.sqrt(Math.pow((double) enemyRadius, 2) + Math.pow((double) enemyDistance, 2));

            if (enemyDistance > 15){
                shootAngle = (float) Math.toDegrees(Math.asin(enemyRadius / (float) offset)) / 3f;
                api.setSpeed(u.id, Speed.FORWARD);
            }
            else {
                shootAngle = (float) Math.toDegrees(Math.asin(enemyRadius / (float) offset)) / 2f;
                api.setSpeed(u.id, Speed.NONE);
            }


            if ((rotationAngle < shootAngle && rotationAngle >= 0) || (rotationAngle > -shootAngle && rotationAngle <= 0)) {
                api.shoot(u.id);
            }

        }

    // Connects your bot to Lia game engine, don't change it.
    public static void main(String[] args) throws Exception {
        NetworkingClient.connectNew(args, new MyBot());
    }
}
