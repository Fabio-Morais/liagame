import lia.api.*;
import lia.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
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
            if (currentDistance < 75 && currentDistance < minDistance && resourcesOccupied.get(resources.get(i)) == -1 ) {
                minDist = resources.get(i);
                minDistance = currentDistance;
            }
        }
        /*Execute when it find some resource available*/
        if (minDistance != 500) {
            api.navigationStart(unit.id, minDist.x, minDist.y);
            units.get(unit.id).randomNavigation = false;
            units.get(unit.id).goToResource = minDist;
            resourcesOccupied.put(minDist, unit.id);
        }
    }
    /*returns false if doesn't have reserved resources*/
    private boolean goToReservedResources(GameState state, Api api, UnitData unit){
        for (int i = 0; i < resources.size(); i++) {
            if(resourcesOccupied.get(resources.get(i)) == unit.id){
                //System.out.println(state.time+" -> "+resourcesOccupied.get(resources.get(i)) + " | "+ unit.id+" go to: "+ resources.get(i).x+ "  "+ resources.get(i).y);
                api.navigationStart(unit.id, resources.get(i).x, resources.get(i).y);
                units.get(unit.id).randomNavigation = false;
                resourcesOccupied.put(resources.get(i), -2);// -2 = ja foi visitado
                units.get(unit.id).numberOfPaths--;
                /*desbloqueia para onde ia para os outros*/
                if(units.get(unit.id).goToResource != null){
                    resourcesOccupied.put(units.get(unit.id).goToResource, unit.id);
                    units.get(unit.id).goToResource = null;// desbloqueia
                }
                return true;
            }
        }
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
        if (unit.navigationPath.length == 0 && !units.get(unit.id).initialPosition) {
            if(unit.type == UnitType.WORKER && units.get(unit.id).numberOfPaths > 0){
                return;
            }else if(unit.type == UnitType.WORKER && units.get(unit.id).goToResource != null){
                resourcesOccupied.put(units.get(unit.id).goToResource, unit.id);
                units.get(unit.id).goToResource = null;// desbloqueia
            }


            while (true) {
                int x = (int) (Math.random() * Constants.MAP_WIDTH);
                int y = (int) (Math.random() * Constants.MAP_HEIGHT);

                if(mapSide == 1 && state.time < 30){
                    x = (int) (Math.random() * (Constants.MAP_WIDTH/2));
                }else if(mapSide == 2 && state.time < 30){
                    x = (int) ((Math.random() * (Constants.MAP_WIDTH - Constants.MAP_WIDTH/2)) + Constants.MAP_WIDTH/2);
                }

                // Map is a 2D array of booleans. If map[x][y] equals false it means that
                // at (x,y) there is no obstacle and we can safely move our unit there.
                if (!Constants.MAP[x][y]) {
                    api.navigationStart(unit.id, x, y);
                    //System.out.println(state.time+" ->random-> "+ unit.id+" go to: "+ x+ "  "+ y);

                    units.get(unit.id).randomNavigation = true;
                    break;
                }
            }
        }
    }

    private void searchResources(GameState state, Api api, UnitData unit) {
        if(unit.resourcesInView.length == 0 ){
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
        }else{
            for (int i = 0; i < unit.resourcesInView.length; i++) {
                if (!resources.contains(unit.resourcesInView[i])) {
                    resources.add(unit.resourcesInView[i]);
                    if(!resourcesOccupied.containsKey(unit.resourcesInView[i]))
                        resourcesOccupied.put(unit.resourcesInView[i], -1);
                }
            }
        }

        // se for um worker reserva logo todos os que ve
    }

    private void goToResources(GameState state, Api api, UnitData unit) {
        if (unit.type == UnitType.WORKER && (unit.navigationPath.length == 0 || units.get(unit.id).randomNavigation)) {//entra se estiver parado ou a ir random
            //chama uma funçao que vai ver ao array se tem algum reservado para ele, caso nao tenha executa a funçao de baixo
            if(!goToReservedResources(state,api,unit))
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
        if (!(state.time < 30) || unit.opponentsInView.length != 0) {
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
    private void checkHealth(GameState state, Api api, UnitData unit){
        if(units.get(unit.id).lastHealth != unit.health){
            units.get(unit.id).lastHealth = unit.health;
        }
    }
    @Override
    public void update(GameState state, Api api) {
        smartSpawnUnits(state, api);

        // We iterate through all of our units that are still alive.
        for (int i = 0; i < state.units.length; i++) {
            UnitData unit = state.units[i];
            updateUnits(state, api, unit);
            if (state.time == 0 && !initialize) {
                initializeFunction(unit);
            }
            checkHealth(state, api, unit);

            initialPosition(state, api, unit);
            randomNavigation(state, api, unit);// navigation randomly if doens't have a path
            searchResources(state, api, unit);//first search and then goToResources
            goToResources(state, api, unit);

            // If the unit is a warrior and it sees an opponent then start shooting
            if (unit.type == UnitType.WARRIOR && unit.opponentsInView.length > 0) {
                api.shoot(unit.id);
                api.saySomething(unit.id, "I see you!");
            }
        }
    }


    // Connects your bot to Lia game engine, don't change it.
    public static void main(String[] args) throws Exception {
        NetworkingClient.connectNew(args, new MyBot());
    }
}
