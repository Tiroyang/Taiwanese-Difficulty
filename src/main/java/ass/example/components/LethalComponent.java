package ass.example.components;

import ass.example.core.DeathReason;
import com.almasb.fxgl.entity.component.Component;

public class LethalComponent extends Component {

    private final DeathReason deathReason;

    public LethalComponent(DeathReason deathReason) {
        this.deathReason = deathReason;
    }

    public DeathReason getDeathReason() {
        return deathReason;
    }

    public String getDeathId() {
        return deathReason.getId();
    }
}