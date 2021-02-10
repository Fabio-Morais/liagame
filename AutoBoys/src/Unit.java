import lia.api.UnitData;

public class Unit {
    public UnitData unitData;
    public boolean randomNavigation;

    public Unit(UnitData unitData) {
        this.unitData = unitData;
        this.randomNavigation = false;
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
