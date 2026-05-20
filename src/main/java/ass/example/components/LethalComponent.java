package ass.example.components;

import ass.example.core.DeathReasons;
import com.almasb.fxgl.entity.component.Component;

public class LethalComponent extends Component {

    private final DeathReasons deathReason;

    public LethalComponent(DeathReasons deathReason) {
        this.deathReason = deathReason;
    }

    public DeathReasons getDeathReason() {
        return deathReason;
    }

    public String getDeathId() {
        return deathReason.getId();
    }
}