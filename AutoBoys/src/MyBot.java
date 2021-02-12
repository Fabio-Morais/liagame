import lia.api.*;
import lia.*;
import lia.api.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Initial implementation keeps picking random locations on the map
 * and sending units there. Worker units collect resources if they
 * see them while warrior units shoot if they see opponents.
 */
public class MyBot implements Bot {

    List<InitialPosition> initialPosition = new ArrayList<InitialPosition>();


    private boolean initialize = false;
    private int mapSide = 0;//1->left; 2->right
    /*se aparecer uma resource mais perto entao inverter*/
    HashMap<Integer, Unit> units = new HashMap<Integer, Unit>();//Serve para colocarmos nesta classe coisas especificas de cada unit
    List<ResourceInView> resources = new ArrayList<ResourceInView>();
    List<OpponentInView> visibleOpponents = new ArrayList<OpponentInView>();
    HashMap<ResourceInView, Integer> resourcesOccupied = new HashMap<ResourceInView, Integer>();// integer é o unit.id | -2= ja foi visitado | -1 = sem reserva | >=0 unit.id reservado
    List<Point> initialPositionOccupied = new ArrayList<Point>();


    /*Auxiliar function to determine the shortest path between the WORKER and all the resources*/
    private void shortestPathToResource(UnitData unit, Api api) {
        if (resources.size() <= 0)
            return;

        ResourceInView minDist = new ResourceInView(200, 200);//high value just to simplify
        float minDistance = 500;//high value just to simplify

        for (int i = 0; i < resources.size(); i++) {
            float currentDistance = MathUtil.distance(unit.x, unit.y, resources.get(i).x, resources.get(i).y);// calculates the distance between the robot and the resource coords
            if (currentDistance < 75 && currentDistance < minDistance && resourcesOccupied.get(resources.get(i)) == -1) {
                minDist = resources.get(i);
                minDistance = currentDistance;
            }
        }
        /*Execute when it find some resource available*/
        if (minDistance != 500) {
            api.navigationStart(unit.id, minDist.x, minDist.y);
            units.get(unit.id).goTo = minDist;
            units.get(unit.id).randomNavigation = false;
            units.get(unit.id).goToResource = minDist;
            resourcesOccupied.put(minDist, unit.id);
        }
    }

    /*returns false if doesn't have reserved resources*/
    private boolean goToReservedResources(GameState state, Api api, UnitData unit) {
        ResourceInView minDist = new ResourceInView(200, 200);//high value just to simplify
        float minDistance = 500;//high value just to simplify

        for (int i = 0; i < resources.size(); i++) {
            if (resourcesOccupied.get(resources.get(i)) == unit.id) {
                float currentDistance = MathUtil.distance(unit.x, unit.y, resources.get(i).x, resources.get(i).y);// calculates the distance between the robot and the resource coords
                if (currentDistance < minDistance) {
                    minDist = resources.get(i);
                    minDistance = currentDistance;
                }
                //System.out.println(state.time+" -> "+resourcesOccupied.get(resources.get(i)) + " | "+ unit.id+" go to: "+ resources.get(i).x+ "  "+ resources.get(i).y);
            }
        }
        if (minDistance != 500) {
            api.navigationStart(unit.id, minDist.x, minDist.y);
            units.get(unit.id).goTo = minDist;
            units.get(unit.id).randomNavigation = false;
            resourcesOccupied.put(minDist, -2);// -2 = ja foi visitado
            units.get(unit.id).numberOfPaths--;
            /*desbloqueia para onde ia para os outros*/
            if (units.get(unit.id).goToResource != null) {
                resourcesOccupied.put(units.get(unit.id).goToResource, unit.id);
                units.get(unit.id).goToResource = null;// desbloqueia
            }
            return true;
        } else
            return false;
    }

    /*Returns true if it needs more workers*/
    private boolean workersResources(GameState state, int numberOfWorkers) {

        if (state.time <= 110) {
            return true;
        } else if (state.time > 110 && numberOfWorkers > 10) {
            return true;
        } else if (state.time > 120 && numberOfWorkers > 6) {
            return true;
        } else if (state.time > 130 && numberOfWorkers > 2) {
            return true;
        }
        return false;
    }

    private void randomNavigation(GameState state, Api api, UnitData unit) {
        // If the unit is not going anywhere, we send it
        // to a random valid location on the map.
        if (unit.navigationPath.length == 0 && !units.get(unit.id).initialPosition && unit.opponentsInView.length == 0) {
            if (unit.type == UnitType.WORKER && units.get(unit.id).numberOfPaths > 0) {
                return;
            } else if (unit.type == UnitType.WORKER && units.get(unit.id).goToResource != null) {
                resourcesOccupied.put(units.get(unit.id).goToResource, unit.id);
                units.get(unit.id).goToResource = null;// desbloqueia
            }


            while (true) {
                int x = (int) (Math.random() * Constants.MAP_WIDTH);
                int y = (int) (Math.random() * Constants.MAP_HEIGHT);

                if (mapSide == 1 && state.time < 30) {
                    x = (int) (Math.random() * (Constants.MAP_WIDTH / 2));
                } else if (mapSide == 2 && state.time < 30) {
                    x = (int) ((Math.random() * (Constants.MAP_WIDTH - Constants.MAP_WIDTH / 2)) + Constants.MAP_WIDTH / 2);
                }

                // Map is a 2D array of booleans. If map[x][y] equals false it means that
                // at (x,y) there is no obstacle and we can safely move our unit there.
                if (!Constants.MAP[x][y]) {
                    api.navigationStart(unit.id, x, y);
                    //System.out.println(state.time+" ->random-> "+ unit.id+" go to: "+ x+ "  "+ y);
                    units.get(unit.id).helping = false;
                    units.get(unit.id).goTo = null;// desbloqueia
                    units.get(unit.id).randomNavigation = true;
                    //api.saySomething(unit.id, "randommm");
                    break;
                }
            }
        }
    }

    private void searchResources(GameState state, Api api, UnitData unit) {
        if (unit.resourcesInView.length == 0) {
            return;
        }
        //if > 0 then put all the resources in view in array
        if (unit.type == UnitType.WORKER) {
            for (int i = 0; i < unit.resourcesInView.length; i++) {
                if (!resources.contains(unit.resourcesInView[i])) {
                    resources.add(unit.resourcesInView[i]);
                    resourcesOccupied.put(unit.resourcesInView[i], unit.id);
                    units.get(unit.id).numberOfPaths++;
                }
                //System.out.println(state.time+" -> "+ unit.resourcesInView[i].x+" : "+ unit.resourcesInView[i].y + " - "+resourcesOccupied.get(unit.resourcesInView[i]) + " | "+ unit.id);

            }
        } else {
            for (int i = 0; i < unit.resourcesInView.length; i++) {
                if (!resources.contains(unit.resourcesInView[i])) {
                    resources.add(unit.resourcesInView[i]);
                    if (!resourcesOccupied.containsKey(unit.resourcesInView[i]))
                        resourcesOccupied.put(unit.resourcesInView[i], -1);
                }
            }
        }

        // se for um worker reserva logo todos os que ve
    }

    private void goToResources(GameState state, Api api, UnitData unit) {
        if (unit.type == UnitType.WORKER && (unit.navigationPath.length == 0 || units.get(unit.id).randomNavigation)) {//entra se estiver parado ou a ir random
            //chama uma funçao que vai ver ao array se tem algum reservado para ele, caso nao tenha executa a funçao de baixo
            if (!goToReservedResources(state, api, unit))
                shortestPathToResource(unit, api);

            /*
             * Ele está a ir com a funçao de cima e de repente encontra resources mais perto para ir -> libertar o ponto em que ia
             * */
        }
    }

    private void updateUnits(GameState state, Api api, UnitData unit) {
        if (!units.containsKey(unit.id)) {
            units.put(unit.id, new Unit(unit));
        } else {
            units.get(unit.id).update(unit);
        }
    }

    private void smartSpawnUnits(GameState state, Api api) {
        int numberOfWorkers = 0;
        int numberOfWarrior = 0;
        for (UnitData unit : state.units) {
            if (unit.type == UnitType.WORKER)
                numberOfWorkers++;
            else
                numberOfWarrior++;
        }
        /*workers < 60% e unidades < 20 e (tempo < 120 e workers < 5)*/
        if ((numberOfWorkers / (float) state.units.length) < 0.6f && (state.units.length <= 20) && workersResources(state, numberOfWorkers)) {
            if (state.resources >= Constants.WORKER_PRICE) {
                api.spawnUnit(UnitType.WORKER);
            }
        } else if (state.resources >= Constants.WARRIOR_PRICE) {
            api.spawnUnit(UnitType.WARRIOR);
        }

    }

    private void initializeFunction(UnitData unit) {
        initialize = true;
        /*LEFT SIDE*/
        if (unit.x < 20) {
            mapSide = 1;
            initialPosition.add(new InitialPosition(new Point(48, 70), new InitialAngle(180, 100, 50)));
            initialPosition.add(new InitialPosition(new Point(48, 50), new InitialAngle(180, 57, 18)));
            initialPosition.add(new InitialPosition(new Point(59, 33), new InitialAngle(90, 350, 280)));
        }/*RIGHT SIDE*/ else {
            mapSide = 2;
            initialPosition.add(new InitialPosition(new Point(116, 62), new InitialAngle(180, 165, 100)));
            initialPosition.add(new InitialPosition(new Point(125, 45), new InitialAngle(180, 220, 190)));
            initialPosition.add(new InitialPosition(new Point(129, 30), new InitialAngle(180, 280, 245)));
        }

    }

    private void initialPosition(GameState state, Api api, UnitData unit) {
        if (!(state.time < 30) || unit.opponentsInView.length != 0 || units.get(unit.id).helping) {
            units.get(unit.id).initialPosition = false;
            return;
        } else if (!(unit.type == UnitType.WARRIOR && state.time < 30)) {
            return;
        }
        Point point = null;

        for (int i = 0; i < initialPosition.size(); i++) {
            if (!initialPositionOccupied.contains(initialPosition.get(i).point)) {
                point = initialPosition.get(i).point;
                initialPosition.get(i).setUnit(unit);
                break;
            }
        }


        if (unit.speed == Speed.NONE && state.time > 2 && units.get(unit.id).initialPosition) {
            smalRotations(unit, api);
        }

        if (point != null) {
            api.navigationStart(unit.id, point.x, point.y);
            initialPositionOccupied.add(point);
            units.get(unit.id).initialPosition = true;
        }

    }

    /*Auxiliar function -> rotates to try seeing enemies*/
    private void smalRotations(UnitData unit, Api api) {
        for (int i = 0; i < initialPosition.size(); i++) {
            if (unit.id == initialPosition.get(i).unit.id) {
                if (unit.orientationAngle > initialPosition.get(i).angle.maxAngle || (unit.orientationAngle > initialPosition.get(i).angle.minAngle && unit.rotation == Rotation.NONE)) {
                    api.setRotation(unit.id, Rotation.RIGHT);
                } else if (unit.orientationAngle < initialPosition.get(i).angle.minAngle) {
                    api.setRotation(unit.id, Rotation.LEFT);
                }
            }
        }
    }

    private void checkHealth(GameState state, Api api, UnitData unit) {

        /*Recebeu dano aqui*/
        if (units.get(unit.id).lastHealth != unit.health) {
            units.get(unit.id).lastHealth = unit.health;
            units.get(unit.id).inFire = true;
        }
        if (unit.health == 100) {
            units.get(unit.id).inFire = false;
        }
    }


    @Override
    public void update(GameState state, Api api) {

        visibleOpponents.clear();
        smartSpawnUnits(state, api);
        // We iterate through all of our units that are still alive.
        for (int i = 0; i < state.units.length; i++) {
            UnitData unit = state.units[i];
            updateUnits(state, api, unit);
            if (state.time == 0 && !initialize) {
                initializeFunction(unit);
            }
            initialPosition(state, api, unit);
            randomNavigation(state, api, unit);// navigation randomly if doens't have a path
            searchResources(state, api, unit);//first search and then goToResources
            goToResources(state, api, unit);

            // If the unit is a warrior and it sees an opponent then start shooting
            if (unit.type == UnitType.WARRIOR && unit.opponentsInView.length > 0) {
                OpponentInView enemy = ChooseTarget(unit);
                float RotationAngle = PredictPosition(unit, enemy);
                Shoot(api, unit, RotationAngle, enemy, state);

            }
            else  if (unit.type == UnitType.WARRIOR && unit.opponentsInView.length == 0 && !units.get(unit.id).helping){
                goToEnemy(api,unit);
            }

            // If the unit sees an opponent
            if (unit.opponentsInView.length > 0) {

                for (OpponentInView opponent : unit.opponentsInView) {
                    visibleOpponents.add(opponent);

                }

            }

        }
    }


        public void goToEnemy(Api api, UnitData unit){
            OpponentInView minDist = null;//high value just to simplify
            float minDistance = 500;//high value just to simplify
            for (OpponentInView enemy : visibleOpponents) {
                float currentDistance = MathUtil.distance(unit.x, unit.y, enemy.x, enemy.y);// calculates the distance between the robot and the resource coords
                if (currentDistance < minDistance) {
                    minDist = enemy;
                    minDistance = currentDistance;
                }
            }
            if(minDistance != 500){
                api.navigationStart(unit.id, minDist.x, minDist.y);
                units.get(unit.id).helping = true;
                api.saySomething(unit.id, "A ir "+minDist.x+" "+minDist.y);
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

            double vX = 1, vY = 1;
            float v ;

            float EnemyAngle = enemy.orientationAngle;

            if (enemy.speed == Speed.FORWARD){
                v = Constants.UNIT_FORWARD_VELOCITY;
            }else if (enemy.speed == Speed.BACKWARD){
                v = Constants.UNIT_BACKWARD_VELOCITY;
            }else{
                return MathUtil.angleBetweenUnitAndPoint(u, enemy.x, enemy.y);
            }

            //Calculate velocity vX and vY
            vX *= Math.cos( Math.toRadians( EnemyAngle ) ) * v;
            vY *= Math.sin( Math.toRadians( EnemyAngle ) ) * v;


            //Resolve equation (BULLET_VELOCITY*t)^2 = (enemy.x + vX*t - u.x)^2 + (enemy.y + vy*t -u.y)^2
            double a = Math.pow(vX, 2) + Math.pow(vY, 2) - Math.pow(Constants.BULLET_VELOCITY, 2);
            double b = 2 * (vX * (enemy.x - u.x) + vY * (enemy.y - u.y));
            double c = Math.pow(enemy.x - u.x, 2) + Math.pow(enemy.y - u.y, 2);
            double result = Math.pow(b, 2) - 4 * a * c;

            double t1 = 0, t2 = 0, t;
            if (result > 0.0) {
                t1 = (-b + Math.sqrt(result)) / (2 * a);
                t2 = (-b - Math.sqrt(result)) / (2 * a);
            } else if (result == 0.0) {
                t1 = -b / (2.0 * a);
            } else {
                System.out.println("The equation has no real roots.");
            }

            //Choose t
            if (t1 < t2 && t1 > 0) {
                t = t1;
            }
            else {
                t = t2;
            }

            //Calculate point to shoot
            float x = (float) (vX * t + enemy.x);
            float y = (float) (vY * t + enemy.y);

            return MathUtil.angleBetweenUnitAndPoint(u, x, y);
        }

        public void Shoot(Api api, UnitData u, float rotationAngle, OpponentInView enemy, GameState state) {

            Rotate(api, u, rotationAngle);

            float shootAngle;
            float enemyRadius = Constants.UNIT_DIAMETER / 2f;
            float enemyDistance = MathUtil.distance(u.x, u.y, enemy.x, enemy.y);
            double offset = Math.sqrt( Math.pow(enemyRadius, 2) + Math.pow(enemyDistance, 2) );

            if (enemyDistance > 15){
                shootAngle = (float) Math.toDegrees(Math.asin(enemyRadius / (float) offset)) / 3f;
                if (u.rotation == Rotation.NONE) {
                    float enemyRotation = MathUtil.angleBetweenUnitAndPoint(enemy.x, enemy.y, enemy.orientationAngle, u.x, u.y);
                    Rotate(api, u, enemyRotation);
                }
                api.navigationStart(u.id, enemy.x, enemy.y);
            }
            else {
                shootAngle = (float) Math.toDegrees(Math.asin(enemyRadius / (float) offset)) / 2f;
                api.setSpeed(u.id, Speed.NONE);
            }

            if ((rotationAngle < shootAngle && rotationAngle >= 0) || (rotationAngle > -shootAngle && rotationAngle <= 0)) {

                //No ally ahead
                int i;
                for (i = 0; i < state.units.length; i++) {
                    UnitData ally = state.units[i];

                    if (ally == u)
                        continue;

                    float allyAngle = MathUtil.angleBetweenUnitAndPoint(u,ally.x,ally.y);

                    if( MathUtil.distance(u.x,u.y,enemy.x,enemy.y) > MathUtil.distance(u.x,u.y,ally.x,ally.y) ) {
                        if ( Math.abs(allyAngle) < 3f ) {
                            api.saySomething(u.id, "Hello Friend");
                            break;
                        }
                    }
                }

                if (i>=state.units.length){
                    api.shoot(u.id);
                }

            }

        }

        public void Rotate(Api api, UnitData u, float rotationAngle){
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
        }

    // Connects your bot to Lia game engine, don't change it.
    public static void main(String[] args) throws Exception {
        NetworkingClient.connectNew(args, new MyBot());
    }
}
