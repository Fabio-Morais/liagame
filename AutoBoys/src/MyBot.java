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

    /*se aparecer uma resource mais perto entao inverter*/
    HashMap<Integer, Unit> units = new HashMap<Integer, Unit>();//Serve para colocarmos nesta classe coisas especificas de cada unit
    List<ResourceInView> resources = new ArrayList<ResourceInView>();
    HashMap<ResourceInView, Boolean> resourcesOccupied = new HashMap<ResourceInView, Boolean>();

    /*Auxiliar function to determine the shortest path between the WORKER and all the resources*/
    private void shortestPathToResource(UnitData unit, Api api){
        if(resources.size() <= 0)
            return;

        ResourceInView minDist = new ResourceInView(200,200);//high value just to simplify
        float minDistance = 500;//high value just to simplify

        for(int i = 0; i < resources.size(); i++ ){
            float currentDistance = MathUtil.distance(unit.x, unit.y, resources.get(i).x, resources.get(i).y);// calculates the distance between the robot and the resource coords
            if(currentDistance < 75 && currentDistance < minDistance && !resourcesOccupied.containsKey(resources.get(i)) ){
                minDist = resources.get(i);
                minDistance = currentDistance;
            }
        }
        /*Execute when it find some resource available*/
        if(minDistance != 500){
            api.navigationStart(unit.id, minDist.x, minDist.y);
            units.get(unit.id).randomNavigation = false;
            resourcesOccupied.put(minDist, true);
        }
    }
    /*Returns true if it needs more workers*/
    private boolean workersResources(GameState state, int numberOfWorkers){

        if(state.time <= 110){
            return true;
        }else if(state.time > 110 && numberOfWorkers > 10){
            return true;
        }else if(state.time > 120 && numberOfWorkers > 6) {
            return true;
        }else if(state.time > 130 && numberOfWorkers > 2) {
            return true;
        }
        return false;
    }

    private void randomNavigation(GameState state, Api api, UnitData unit){
        // If the unit is not going anywhere, we send it
        // to a random valid location on the map.
        if (unit.navigationPath.length == 0) {
            while (true) {
                int x = (int) (Math.random() * Constants.MAP_WIDTH);
                int y = (int) (Math.random() * Constants.MAP_HEIGHT);

                // Map is a 2D array of booleans. If map[x][y] equals false it means that
                // at (x,y) there is no obstacle and we can safely move our unit there.
                if (!Constants.MAP[x][y]) {
                    api.navigationStart(unit.id, x, y);
                    units.get(unit.id).randomNavigation = true;
                    break;
                }
            }
        }
    }
    private void searchResources(GameState state, Api api, UnitData unit){
        //if > 0 then put all the resources in view in array
        if(unit.resourcesInView.length > 0){
            for(int i = 0; i < unit.resourcesInView.length; i++){
                if(!resources.contains(unit.resourcesInView[i])){
                    resources.add(unit.resourcesInView[i]);
                }
            }
        }
    }
    private void goToResources(GameState state, Api api, UnitData unit){
       //System.out.println(state.time+"  " + unit.id+"->"+ unit.randomNavigation +"   "+ unit.x+" "+unit.y);
        if(unit.type == UnitType.WORKER && (unit.navigationPath.length == 0  || units.get(unit.id).randomNavigation) ){//entra se estiver parado ou a ir random
            shortestPathToResource(unit, api);
        }
    }
    private void updateUnits(GameState state, Api api, UnitData unit){
        if(!units.containsKey(unit.id)){
            units.put(unit.id, new Unit(unit));
        }else{
            units.get(unit.id).update(unit);
        }
    }

    private void smartSpawnUnits(GameState state, Api api){
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

    @Override
    public void update(GameState state, Api api) {

        smartSpawnUnits(state, api);

        // We iterate through all of our units that are still alive.
        for (int i = 0; i < state.units.length; i++) {
            UnitData unit = state.units[i];
            updateUnits(state, api, unit);
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
