import lia.api.Point;
import lia.api.ResourceInView;
import lia.api.UnitData;

public class Unit {
    public UnitData unitData;
    public boolean randomNavigation;
    public boolean initialPosition;
    public int lastHealth;
    public int numberOfPaths;//numero de trajetorias que o worker ainda vai fazer
    public ResourceInView goToResource;

    public ResourceInView goTo;
    public float nextTime;

    public boolean inFire;
    public boolean helping;


    public Unit(UnitData unitData) {
        this.unitData = unitData;
        this.randomNavigation = false;
        this.initialPosition = false;
        this.lastHealth = unitData.health;
        this.numberOfPaths = 0;
        this.goToResource = null;
        this.inFire = false;
    }

    public void update(UnitData unit){
        this.unitData = unit;
    }

    @Override
    public String toString() {
        return "Unit{" +
                "unitData=" + unitData +
                ", randomNavigation=" + randomNavigation +
                '}';
    }

}
