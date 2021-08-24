package mathax.client.legacy.gui.widgets.pressable;

import mathax.client.legacy.gui.widgets.WWidget;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public abstract class WPressable extends WWidget {
    public Runnable action;

    protected boolean pressed;

    @Override
    public boolean onMouseClicked(double mouseX, double mouseY, int button, boolean used) {
        if (mouseOver && (button == GLFW_MOUSE_BUTTON_LEFT || button == GLFW_MOUSE_BUTTON_RIGHT) && !used) pressed = true;
        return pressed;
    }

    @Override
    public boolean onMouseReleased(double mouseX, double mouseY, int button) {
        if (pressed) {
            onPressed(button);
            if (action != null) action.run();

            pressed = false;
        }

        return false;
    }

    protected void onPressed(int button) {}
}
