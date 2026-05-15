package tizio.dev.engine.gui.widgets;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import tizio.dev.engine.utils.UniformOverrides;

public class UniformSlider extends AbstractSliderButton {

    private final String name;
    private final float min;
    private final float max;

    public UniformSlider(int x, int y, int width, String name, float min, float max) {
        super(x, y, width, 20, Component.empty(), (UniformOverrides.get(name) - min) / (max - min));
        this.name = name;
        this.min = min;
        this.max = max;
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.literal(String.format("%s: %.4f", name, currentValue())));
    }

    @Override
    protected void applyValue() {
        UniformOverrides.set(name, currentValue());
    }

    private float currentValue() {
        return min + (float) this.value * (max - min);
    }
}
