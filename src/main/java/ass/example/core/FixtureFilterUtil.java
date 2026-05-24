package ass.example.core;

import com.almasb.fxgl.physics.box2d.dynamics.FixtureDef;
import com.almasb.fxgl.physics.box2d.dynamics.Filter;

public final class FixtureFilterUtil {

    private FixtureFilterUtil() {
    }

    public static FixtureDef applyFilter(
            FixtureDef fixtureDef,
            short categoryBits,
            short maskBits
    ) {
        Filter filter = new Filter();
        filter.categoryBits = categoryBits;
        filter.maskBits = maskBits;

        fixtureDef.setFilter(filter);

        return fixtureDef;
    }
}