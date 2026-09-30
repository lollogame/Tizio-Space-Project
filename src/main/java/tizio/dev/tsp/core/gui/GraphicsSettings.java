package tizio.dev.tsp.core.gui;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;
import tizio.dev.tsp.config.ConfigManager;
import tizio.dev.tsp.core.gui.widgets.AhhhWidget;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class GraphicsSettings extends Screen {

    private static final int LOGO_DRAW_WIDTH = 96;
    private static final int LOGO_DRAW_HEIGHT = 96;
    private static final int LOGO_MARGIN = 16;

    private OptionsList optionsList;
    private AhhhWidget ahhhWidget;

    public GraphicsSettings() {
        super(Component.literal("§6T.S.P Graphics Settings§r"));
    }

    @Override
    protected void init() {
        this.optionsList = new OptionsList(this.minecraft, this.width, this.height, 32, this.height - 32, 25);
        this.optionsList.setRenderBackground(false);
        this.optionsList.setRenderTopAndBottom(true);

        List<OptionInstance<?>> options = buildConfigWidgets();

        for (int i = 0; i < options.size(); i += 2) {
            OptionInstance<?> left = options.get(i);
            OptionInstance<?> right = (i + 1 < options.size()) ? options.get(i + 1) : null;
            this.optionsList.addSmall(left, right);
        }

        this.addRenderableWidget(this.optionsList);

        int logoX = this.width - LOGO_DRAW_WIDTH - LOGO_MARGIN;
        int logoY = (this.height - LOGO_DRAW_HEIGHT) / 2;

        if (logoX >= this.width / 2 + 160) {
            this.ahhhWidget = new AhhhWidget(logoX, logoY, LOGO_DRAW_WIDTH, LOGO_DRAW_HEIGHT);
            this.addRenderableWidget(this.ahhhWidget);
        }

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
            ConfigManager.getClientSpec().save();
            this.onClose();
        }).bounds(this.width / 2 - 100, this.height - 27, 200, 20).build());
    }

    private List<OptionInstance<?>> buildConfigWidgets() {
        List<OptionInstance<?>> options = new ArrayList<>();
        Field[] fields = ConfigManager.class.getFields();

        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) {
                try {
                    Object objectValue = field.get(null);

                    if (objectValue instanceof ForgeConfigSpec.ConfigValue<?> configValue) {
                        if (isClientConfig(configValue)) {
                            OptionInstance<?> option = createWidgetForConfig(field.getName(), configValue);
                            if (option != null) {
                                options.add(option);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return options;
    }

    private boolean isClientConfig(ForgeConfigSpec.ConfigValue<?> configValue) {
        return ConfigManager.getClientSpec().getValues().contains(configValue.getPath());
    }

    private OptionInstance<?> createWidgetForConfig(String fieldName, ForgeConfigSpec.ConfigValue<?> configValue) {
        String displayName = formatOptionName(fieldName);

        if (configValue instanceof ForgeConfigSpec.BooleanValue boolValue) {
            return OptionInstance.createBoolean(displayName, boolValue.get(), newValue -> boolValue.set(newValue));
        }

        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.ahhhWidget != null && this.ahhhWidget.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private String formatOptionName(String fieldName) {
        String cleanName = fieldName.replaceFirst("^(HAS_|ENABLE_|IS_)", "").replace("_", " ");
        StringBuilder sb = new StringBuilder();

        for (String word : cleanName.split(" ")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase()).append(" ");
            }
        }

        return sb.toString().trim();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.optionsList.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        ConfigManager.getClientSpec().save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}