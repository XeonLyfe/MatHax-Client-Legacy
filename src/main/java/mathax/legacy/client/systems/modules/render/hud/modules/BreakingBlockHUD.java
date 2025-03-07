package mathax.legacy.client.systems.modules.render.hud.modules;

import mathax.legacy.client.mixin.ClientPlayerInteractionManagerAccessor;
import mathax.legacy.client.systems.modules.render.hud.HUD;
import mathax.legacy.client.systems.modules.render.hud.TripleTextHUDElement;

public class BreakingBlockHUD extends TripleTextHUDElement {
    public BreakingBlockHUD(HUD hud) {
        super(hud, "breaking-block", "Displays percentage of the block you are breaking", true);
    }

    @Override
    protected String getLeft() {
        return "Breaking Block: ";
    }

    @Override
    protected String getRight() {
        if (isInEditor()) return "0%";

        return String.format("%.0f%%", ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).getBreakingProgress() * 100);
    }

    @Override
    public String getEnd() {
        return "";
    }
}
