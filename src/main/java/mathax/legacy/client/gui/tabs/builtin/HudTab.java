package mathax.legacy.client.gui.tabs.builtin;

import mathax.legacy.client.events.render.Render2DEvent;
import mathax.legacy.client.gui.GuiTheme;
import mathax.legacy.client.gui.WidgetScreen;
import mathax.legacy.client.gui.screens.HudElementScreen;
import mathax.legacy.client.gui.tabs.Tab;
import mathax.legacy.client.gui.tabs.TabScreen;
import mathax.legacy.client.gui.tabs.WindowTabScreen;
import mathax.legacy.client.renderer.Renderer2D;
import mathax.legacy.client.systems.modules.Modules;
import mathax.legacy.client.systems.modules.render.hud.HUD;
import mathax.legacy.client.systems.modules.render.hud.HUDElement;
import mathax.legacy.client.utils.Utils;
import mathax.legacy.client.utils.misc.NbtUtils;
import mathax.legacy.client.utils.render.color.Color;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.NbtCompound;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class HudTab extends Tab {
    public static HudTab INSTANCE;

    public HudTab() {
        super("HUD");

        INSTANCE = this;
    }

    @Override
    public void openScreen(GuiTheme theme) {
        Utils.mc.setScreen(createScreen(theme));
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new HudScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof HudScreen;
    }

    public static class HudScreen extends WindowTabScreen {
        private final Color HOVER_BG_COLOR = new Color(200, 200, 200, 75);
        private final Color HOVER_OL_COLOR = new Color(200, 200, 200, 200);

        private final Color INACTIVE_BG_COLOR = new Color(200, 25, 25, 75);
        private final Color INACTIVE_OL_COLOR = new Color(200, 25, 25, 200);

        private final HUD hud;

        private boolean selecting;
        private double mouseStartX, mouseStartY;

        private boolean dragging, dragged;
        private double lastMouseX, lastMouseY;
        private HUDElement hoveredModule;
        private final List<HUDElement> selectedElements = new ArrayList<>();

        public HudScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);

            hud = Modules.get().get(HUD.class);
        }

        @Override
        protected void init() {
            super.init();

            Utils.mc.options.hudHidden = false;
        }

        @Override
        public void initWidgets() {} // Unused

        @Override
        public boolean toClipboard() {
            return NbtUtils.toClipboard(hud.title, hud.toTag());
        }

        @Override
        public boolean fromClipboard() {
            NbtCompound clipboard = NbtUtils.fromClipboard(hud.toTag());

            if (clipboard != null) {
                hud.fromTag(clipboard);
                return true;
            }

            return false;
        }

        @Override
        public void onClose() {
            super.onClose();

            if (theme.hideHUD() && parent instanceof WidgetScreen) Utils.mc.options.hudHidden = true;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (hoveredModule != null) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    if (!selectedElements.isEmpty()) selectedElements.clear();
                    Utils.mc.setScreen(new HudElementScreen(theme, hoveredModule));
                }
                else {
                    dragging = true;
                    dragged = false;

                    if (!selectedElements.contains(hoveredModule)) {
                        selectedElements.clear();
                        selectedElements.add(hoveredModule);
                    }

                }

                return true;
            }

            double s = Utils.mc.getWindow().getScaleFactor();

            selecting = true;
            mouseStartX = mouseX * s;
            mouseStartY = mouseY * s;

            if (!selectedElements.isEmpty()) {
                selectedElements.clear();
                return true;
            }

            return false;
        }

        private boolean isInSelection(double mouseX, double mouseY, double x, double y) {
            double sx, sy;
            double sw, sh;

            if (mouseX >= mouseStartX) {
                sx = mouseStartX;
                sw = mouseX - mouseStartX;
            } else {
                sx = mouseX;
                sw = mouseStartX - mouseX;
            }

            if (mouseY >= mouseStartY) {
                sy = mouseStartY;
                sh = mouseY - mouseStartY;
            } else {
                sy = mouseY;
                sh = mouseStartY - mouseY;
            }

            return x >= sx && x <= sx + sw && y >= sy && y <= sy + sh;
        }

        @Override
        public void mouseMoved(double mouseX, double mouseY) {
            double s = Utils.mc.getWindow().getScaleFactor();

            mouseX *= s;
            mouseY *= s;

            if (selecting) {
                selectedElements.clear();

                for (HUDElement module : hud.elements) {
                    double mX = module.box.getX();
                    double mY = module.box.getY();
                    double mW = module.box.width;
                    double mH = module.box.height;

                    if (isInSelection(mouseX, mouseY, mX, mY) || isInSelection(mouseX, mouseY, mX + mW, mY) || (isInSelection(mouseX, mouseY, mX, mY + mH) || isInSelection(mouseX, mouseY, mX + mW, mY + mH))) {
                        selectedElements.add(module);
                    }
                }
            } else if (dragging) {
                for (HUDElement element : selectedElements) {
                    element.box.addPos(mouseX - lastMouseX, mouseY - lastMouseY);
                }

                double r = hud.snappingRange.get();

                if (r > 0) {
                    double x = Double.MAX_VALUE;
                    double y = Double.MAX_VALUE;
                    double w = 0;
                    double h = 0;

                    for (HUDElement element : selectedElements) {
                        x = Math.min(x, element.box.getX());
                        y = Math.min(y, element.box.getY());
                    }

                    for (HUDElement element : selectedElements) {
                        w = Math.max(w, element.box.getX() - x + element.box.width);
                        h = Math.max(h, element.box.getY() - y + element.box.height);
                    }

                    boolean movedX = false;
                    boolean movedY = false;

                    for (HUDElement element : hud.elements) {
                        if (selectedElements.contains(element)) continue;

                        double eX = element.box.getX();
                        double eY = element.box.getY();
                        double eW = element.box.width;
                        double eH = element.box.height;

                        boolean isHorizontallyIn = isPointBetween(x, w, eX) || isPointBetween(x, w, eX + eW) || isPointBetween(eX, eW, x) || isPointBetween(eX, eW, x + w);
                        boolean isVerticallyIn = isPointBetween(y, h, eY) || isPointBetween(y, h, eY + eH) || isPointBetween(eY, eH, y) || isPointBetween(eY, eH, y + h);

                        double moveX = 0;
                        double moveY = 0;

                        if (!movedX && isVerticallyIn) {
                            double x2 = x + w;
                            double eX2 = eX + eW;

                            if (Math.abs(eX - x) < r) moveX = eX - x;
                            else if (Math.abs(eX2 - x2) <= r) moveX = eX2 - x2;
                            else if (Math.abs(eX2 - x) <= r) moveX = eX2 - x;
                            else if (Math.abs(eX - x2) <= r) moveX = eX - x2;
                        }

                        if (!movedY && isHorizontallyIn) {
                            double y2 = y + h;
                            double eY2 = eY + eH;

                            if (Math.abs(eY - y) <= r) moveY = eY - y;
                            else if (Math.abs(eY2 - y2) <= r) moveY = eY2 - y2;
                            else if (Math.abs(eY2 - y) <= r) moveY = eY2 - y;
                            else if (Math.abs(eY - y2) <= r) moveY = eY - y2;
                        }

                        if (moveX != 0 || moveY != 0) {
                            for (HUDElement e : selectedElements) e.box.addPos(moveX, moveY);

                            if (moveX != 0) movedX = true;
                            if (moveY != 0) movedY = true;
                        }

                        if (movedX && movedY) break;
                    }

                    dragged = true;
                }
            }

            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }

        private boolean isPointBetween(double start, double size, double point) {
            return point >= start && point <= start + size;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (dragging) {
                dragging = false;

                if (!dragged && !selectedElements.isEmpty()) {
                    selectedElements.forEach(HUDElement::toggle);
                    selectedElements.clear();
                }

                if (selectedElements.size() <= 1) selectedElements.clear();
                return true;
            }

            if (selecting) {
                selecting = false;
                return true;
            }

            return false;
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            double s = Utils.mc.getWindow().getScaleFactor();

            mouseX *= s;
            mouseY *= s;

            if (!Utils.canUpdate()) {
                renderBackground(matrices);

                Utils.unscaledProjection();
                hud.onRender(Render2DEvent.get(0, 0, delta));
            } else {
                Utils.unscaledProjection();
                if (!hud.isActive()) hud.onRender(Render2DEvent.get(0, 0, delta));
            }

            Renderer2D.COLOR.begin();

            for (HUDElement element : hud.elements) {
                if (element.active) continue;

                renderElement(element, INACTIVE_BG_COLOR, INACTIVE_OL_COLOR);
            }

            for (HUDElement element : selectedElements) {
                renderElement(element, HOVER_BG_COLOR, HOVER_OL_COLOR);
            }

            if (!dragging) {
                hoveredModule = null;

                for (HUDElement module : hud.elements) {
                    if (module.box.isOver(mouseX, mouseY)) {
                        if (!selectedElements.contains(module)) renderElement(module, HOVER_BG_COLOR, HOVER_OL_COLOR);
                        hoveredModule = module;

                        break;
                    }
                }

                if (selecting) {
                    renderQuad(mouseStartX, mouseStartY, mouseX - mouseStartX, mouseY - mouseStartY, HOVER_BG_COLOR, HOVER_OL_COLOR);
                }
            }

            Renderer2D.COLOR.render(new MatrixStack());
            Utils.scaledProjection();

            runAfterRenderTasks();
        }

        private void renderElement(HUDElement module, Color bgColor, Color olColor) {
            renderQuad(module.box.getX(), module.box.getY(), module.box.width, module.box.height, bgColor, olColor);
        }

        private void renderQuad(double x, double y, double w, double h, Color bgColor, Color olColor) {
            Renderer2D.COLOR.quad(x, y, w, h, bgColor);
            Renderer2D.COLOR.quad(x - 1, y - 1, w + 2, 1, olColor);
            Renderer2D.COLOR.quad(x - 1, y + h - 1, w + 2, 1, olColor);
            Renderer2D.COLOR.quad(x - 1, y, 1, h, olColor);
            Renderer2D.COLOR.quad(x + w, y, 1, h, olColor);
        }
    }
}
