import lia.api.Point;
import lia.api.UnitData;

public class InitialPosition {
    public InitialAngle angle;
    public UnitData unit;
    public Point point;

    public InitialPosition(Point point, InitialAngle angle) {
        this.angle = angle;
        this.point = point;
    }

    public void setUnit(UnitData unit) {
        this.unit = unit;
    }
}
