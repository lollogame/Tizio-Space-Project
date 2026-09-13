package tizio.dev.tsp.core.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import tizio.dev.tsp.config.DataConfig;
import tizio.dev.tsp.config.ConfigManager;
import tizio.dev.tsp.core.celestial.camera.CameraPlanetOrbit;
import tizio.dev.tsp.core.celestial.instance.elements.SolarSystemData;
import tizio.dev.tsp.core.celestial.instance.elements.planet.PlanetInstance;
import tizio.dev.tsp.core.celestial.instance.elements.planet.OrbitCollisionUtil;
import tizio.dev.tsp.core.celestial.instance.elements.sun.SunInstance;
import tizio.dev.tsp.core.data.CelestialJsonLoader;
import tizio.dev.tsp.core.gui.theme.SystemEditorTheme;
import tizio.dev.tsp.core.gui.widgets.*;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class SystemEditor extends Screen {

    private static final int ROW_H    = 18;
    private static final int HEADER_H = 24;

    private enum NodeType { SYSTEM, STAR, BODY }

    private enum Tab {
        SYSTEM, STAR, GENERAL, ORBIT, SURFACE, ATMOSPHERE, RINGS, SKY
    }

    private static class InspectorEntry {
        AbstractWidget widget;
        int rawY;
        int height;
        String label;

        InspectorEntry(AbstractWidget widget, int rawY, int height, String label) {
            this.widget = widget;
            this.rawY = rawY;
            this.height = height;
            this.label = label;
        }
    }

    private SolarSystemData activeSystem  = null;
    private NodeType           selectedNode  = NodeType.SYSTEM;
    private PlanetInstance.Config selectedBody = null;
    private Tab                activeTab     = Tab.SYSTEM;

    private boolean hideUI              = false;
    private boolean leftPanelCollapsed  = false;
    private boolean rightPanelCollapsed = false;
    private boolean systemDropdownOpen  = false;

    private String searchQuery      = "";
    private EditBox searchEditBox   = null;

    private EditorTreeWidget treeWidget = null;
    private PlanetInstance.Config copiedBodyConfig = null;

    private final List<InspectorEntry> inspectorEntries = new ArrayList<>();
    private int rightScrollOffset = 0;
    private int totalInspectorHeight = 0;

    private String  statusMessage     = "";
    private long    statusMessageTime = 0;
    private boolean statusIsError     = false;

    private EditBox sysIdEditBox;
    private EditBox sysDimEditBox;
    private EditBox starIdEditBox;
    private EditBox starColorEditBox;
    private EditBox bodyIdEditBox;
    private EditBox bodyParentEditBox;
    private EditBox bodyDimEditBox;
    private EditBox dayTextureEditBox;
    private EditBox nightTextureEditBox;
    private EditBox bodyColorEditBox;
    private EditBox epochUtcEditBox;
    private EditBox atmosColorEditBox;
    private EditBox ringTextureEditBox;
    private EditBox rockTextureEditBox;
    private EditBox ringColorEditBox;
    private EditBox cloudTextureEditBox;
    private EditBox cloudColorEditBox;
    private EditBox skyTextureEditBox;
    private EditBox starsColorEditBox;
    private EditBox fogColorEditBox;

    private ColorPreviewWidget starColorPicker;
    private ColorPreviewWidget bodyColorPicker;
    private ColorPreviewWidget atmosColorPicker;
    private ColorPreviewWidget ringColorPicker;
    private ColorPreviewWidget cloudColorPicker;
    private ColorPreviewWidget starsColorPicker;
    private ColorPreviewWidget fogColorPicker;
    private boolean isDraggingCamera = false;

    public SystemEditor() {
        super(Component.literal("Development System Editor"));
    }
    private int getPanelLeftWidth() {
        return Math.min(190, Math.max(140, (this.width / 2) - 95));
    }
    private int getPanelRightWidth() {
        return Math.min(195, Math.max(140, (this.width / 2) - 95));
    }

    @Override
    protected void init() {

        if(!ConfigManager.isDevelopmentMode()) return;
        CelestialJsonLoader.ensureLoaded();

        if (activeSystem == null) {
            var systems = CelestialJsonLoader.getActiveSystems();
            String lastId = CelestialJsonLoader.getActiveSelectedSystemId();
            if (lastId != null && systems.containsKey(lastId)) {
                activeSystem = systems.get(lastId);
            } else if (!systems.isEmpty()) {
                activeSystem = systems.values().iterator().next();
            } else {
                activeSystem = createDefaultSystem();
                CelestialJsonLoader.updateSystemInMemory(activeSystem, true);
            }
        }

        if (activeSystem != null) {
            CelestialJsonLoader.setActiveSelectedSystemId(activeSystem.id);
        }

        buildLayout();
    }

    @Override
    public void tick() {
        super.tick();
        if (!CameraPlanetOrbit.isActive()) {
            return;
        }
        if (selectedNode == NodeType.BODY && selectedBody != null) {
            getSpatialInfoForSelectedBody().ifPresent(CameraPlanetOrbit::updateFocus);
        } else if (selectedNode == NodeType.STAR && activeSystem != null && activeSystem.star != null) {
            SunInstance.Config star = activeSystem.star;
            Vec3 pos = new Vec3(activeSystem.originX, activeSystem.originY, activeSystem.originZ);
            float globalScale = activeSystem.globalScale;
            double visualRadius = Math.max((double) DataConfig.Body.MIN_PHYSICAL_RADIUS, Math.min((double) DataConfig.Star.MAX_RADIUS, DataConfig.Star.toBlocks(star.radius) * globalScale));
            CameraPlanetOrbit.updateFocus(star.id, pos, visualRadius);
        }
    }

    private Optional<CelestialJsonLoader.BodySpatialInfo> getSpatialInfoForSelectedBody() {
        if (selectedBody == null) {
            return Optional.empty();
        }

        Optional<CelestialJsonLoader.BodySpatialInfo> baseInfo = CelestialJsonLoader.getDimensionBodiesInSpace(Instant.now()).stream()
                .filter(info -> selectedBody.id.equals(info.body().id))
                .findFirst()
                .or(this::buildFallbackSpatialInfo);

        if (baseInfo.isEmpty()) {
            return Optional.empty();
        }

        CelestialJsonLoader.BodySpatialInfo existing = baseInfo.get();

        double scaleRatio = (existing.physicalRadius() > 0) ? (existing.visualRadius() / existing.physicalRadius()) : 1.0D;
        float globalScale = activeSystem != null ? activeSystem.globalScale : 1.0F;
        double livePhysRadius = Math.max((double) DataConfig.Body.MIN_PHYSICAL_RADIUS, DataConfig.Body.toBlocks(selectedBody.radius) * globalScale);
        double liveVisualRadius = Math.max(existing.visualRadius(), livePhysRadius * scaleRatio);

        return Optional.of(new CelestialJsonLoader.BodySpatialInfo(
                existing.system(),
                selectedBody,
                existing.spacePosition(),
                livePhysRadius,
                liveVisualRadius,
                existing.dimension()
        ));
    }

    private SolarSystemData createDefaultSystem() {
        SolarSystemData system = new SolarSystemData(DataConfig.System.DEFAULT_SYSTEM_ID, DataConfig.System.DEFAULT_SPACE_DIMENSION);
        system.star = new SunInstance.Config(DataConfig.Star.ID_DEF, DataConfig.Star.RADIUS.defF(), DataConfig.Star.COLOR_HEX_DEF);
        PlanetInstance.Config earth = new PlanetInstance.Config(DataConfig.Body.NEW_BODY_ID, DataConfig.Body.TYPE_DEF, DataConfig.Body.PARENT_ID_DEF, DataConfig.Body.DEFAULT_SYSTEM_BODY_RADIUS, DataConfig.Body.TEXTURE_DEF, DataConfig.Body.COLOR_HEX_DEF);
        earth.orbit.radius     = DataConfig.Orbit.RADIUS.def();
        earth.orbit.periodDays = DataConfig.Orbit.PERIOD_DAYS.def();
        earth.atmosphere.enabled = true;
        system.bodies.add(earth);
        return system;
    }

    private void focusCameraOnSelectedBody() {
        getSpatialInfoForSelectedBody().ifPresent(CameraPlanetOrbit::activate);
    }

    private void focusCameraOnSelectedStar() {
        if (activeSystem == null || activeSystem.star == null) {
            return;
        }
        SunInstance.Config star = activeSystem.star;
        Vec3 pos = new Vec3(activeSystem.originX, activeSystem.originY, activeSystem.originZ);
        float globalScale = Math.max(0.0001F, activeSystem.globalScale <= 0.0F ? 1.0F : activeSystem.globalScale);
        double visualRadius = Math.max((double) DataConfig.Body.MIN_PHYSICAL_RADIUS, Math.min((double) DataConfig.Star.MAX_RADIUS, DataConfig.Star.toBlocks(star.radius) * globalScale));
        CameraPlanetOrbit.activate(star.id, pos, visualRadius);
    }

    private Optional<CelestialJsonLoader.BodySpatialInfo> buildFallbackSpatialInfo() {
        if (activeSystem == null || selectedBody == null) {
            return Optional.empty();
        }
        Map<String, Vec3> positions = CelestialJsonLoader.calculateBodyPositions(activeSystem, Instant.now());
        Vec3 pos = positions.get(selectedBody.id);
        if (pos == null) {
            return Optional.empty();
        }
        float globalScale = Math.max(0.0001F, activeSystem.globalScale <= 0.0F ? 1.0F : activeSystem.globalScale);
        double physRadius = Math.max((double) DataConfig.Body.MIN_PHYSICAL_RADIUS, DataConfig.Body.toBlocks(selectedBody.radius) * globalScale);
        double visualRadius = CelestialJsonLoader.resolveAtmosphereRadiusConfig(selectedBody.atmosphere, (float) physRadius);
        return Optional.of(new CelestialJsonLoader.BodySpatialInfo(
                activeSystem, selectedBody, pos, physRadius, visualRadius, selectedBody.dimension));
    }

    private boolean isInViewport(double mouseX, double mouseY) {
        if (hideUI) {
            return mouseY >= 0;
        }
        int leftBound = leftPanelCollapsed ? 0 : getPanelLeftWidth();
        int rightBound = rightPanelCollapsed ? this.width : this.width - getPanelRightWidth();
        return mouseX >= leftBound && mouseX <= rightBound && mouseY >= HEADER_H && mouseY <= this.height;
    }

    private void showStatus(String msg, boolean error) {
        this.statusMessage = msg;
        this.statusIsError = error;
        this.statusMessageTime = System.currentTimeMillis() + 2200;
    }

    private void buildLayout() {
        clearWidgets();
        inspectorEntries.clear();
        if (hideUI) return;

        buildHeaderToolbar();

        if (!leftPanelCollapsed) {
            buildLeftPanel();
        }

        if (!rightPanelCollapsed) {
            buildRightPanel();
        }
    }

    private void buildHeaderToolbar() {
        int x = 4;
        int y = 2;

        String sysLabel = "Sys: [" + (activeSystem != null ? activeSystem.id : "None") + "]";
        addRenderableWidget(new Button(x, y, 120, 20, Component.literal(sysLabel), b -> {
            systemDropdownOpen = !systemDropdownOpen;
        }).accentColor(SystemEditorTheme.ACCENT));
        x += 123;

        addRenderableWidget(new Button(x, y, 45, 20, Component.literal("+Sys"), b -> addNewSystem()));
        x += 47;

        addRenderableWidget(new Button(x, y, 45, 20, Component.literal("-Sys"), b -> promptRemoveCurrentSystem()).accentColor(SystemEditorTheme.RED));
        x += 47;

        addRenderableWidget(new Button(x, y, 60, 20, Component.literal("Export"), b -> exportCurrentSystem()));
        x += 62;

        addRenderableWidget(new Button(this.width - 75, y, 71, 20, Component.literal("Hide UI"), b -> toggleHideUI()).accentColor(SystemEditorTheme.AMBER));

        int leftW = getPanelLeftWidth();
        String leftToggleLabel = leftPanelCollapsed ? " Tree > " : " < ";
        addRenderableWidget(new Button(leftPanelCollapsed ? 4 : leftW - 24, HEADER_H + 2, leftPanelCollapsed ? 55 : 20, 16, Component.literal(leftToggleLabel), b -> {
            leftPanelCollapsed = !leftPanelCollapsed;
            buildLayout();
        }).compact(true));

        int rightW = getPanelRightWidth();
        int p2X = this.width - rightW;
        String rightToggleLabel = rightPanelCollapsed ? " < Insp. " : " > ";
        addRenderableWidget(new Button(rightPanelCollapsed ? this.width - 60 : p2X + 4, HEADER_H + 2, rightPanelCollapsed ? 56 : 20, 16, Component.literal(rightToggleLabel), b -> {
            rightPanelCollapsed = !rightPanelCollapsed;
            buildLayout();
        }).compact(true));
    }

    private void buildLeftPanel() {
        int panelX = 4;
        int startY = HEADER_H + 20;
        int width  = getPanelLeftWidth() - 8;

        searchEditBox = new EditBox(this.font, panelX, startY, width, ROW_H, Component.literal("Search"));
        searchEditBox.setValue(searchQuery);
        searchEditBox.setHint(Component.literal("Search bodies..."));
        searchEditBox.setResponder(val -> {
            this.searchQuery = val;
            if (treeWidget != null) {
                treeWidget.setFilterQuery(val);
            }
        });
        addRenderableWidget(searchEditBox);

        int treeY = startY + ROW_H + 4;
        int treeH = this.height - treeY - 52;

        String currentSelId = (selectedNode == NodeType.SYSTEM && activeSystem != null) ? activeSystem.id
                : ((selectedNode == NodeType.STAR && activeSystem != null && activeSystem.star != null) ? activeSystem.star.id
                : (selectedBody != null ? selectedBody.id : null));

        treeWidget = new EditorTreeWidget(panelX, treeY, width, treeH, item -> {
            if (item == null) return;
            if ("system".equalsIgnoreCase(item.type)) {
                selectedNode = NodeType.SYSTEM;
                activeTab    = Tab.SYSTEM;
            } else if (item.bodyConfig == null && ("star".equalsIgnoreCase(item.type) || "blackhole".equalsIgnoreCase(item.type))) {
                selectedNode = NodeType.STAR;
                activeTab    = Tab.STAR;
                focusCameraOnSelectedStar();
            } else if (item.bodyConfig != null) {
                selectedNode = NodeType.BODY;
                selectedBody = item.bodyConfig;
                boolean isBH = selectedBody.isBlackHole();
                boolean isStar = "star".equalsIgnoreCase(selectedBody.type);
                if ((isBH || isStar) && (activeTab == Tab.ATMOSPHERE || activeTab == Tab.RINGS)) {
                    activeTab = Tab.GENERAL;
                } else if (activeTab == Tab.SYSTEM || activeTab == Tab.STAR) {
                    activeTab = Tab.GENERAL;
                }
                focusCameraOnSelectedBody();
            }
            buildLayout();
        });

        treeWidget.rebuildFromSystem(activeSystem, currentSelId);
        if (!searchQuery.isEmpty()) {
            treeWidget.setFilterQuery(searchQuery);
        }
        addRenderableWidget(treeWidget);

        int botY1 = this.height - 46;
        int btnW4 = (width - 6) / 4;

        addRenderableWidget(new Button(panelX, botY1, btnW4, ROW_H, Component.literal("+Planet"), button -> addBody("planet")).compact(true));
        addRenderableWidget(new Button(panelX + btnW4 + 2, botY1, btnW4, ROW_H, Component.literal("+Moon"), button -> addBody("moon")).compact(true));
        addRenderableWidget(new Button(panelX + (btnW4 + 2) * 2, botY1, btnW4, ROW_H, Component.literal("+ BH"), button -> addBody("blackhole")).compact(true));

        addRenderableWidget(new Button(panelX + (btnW4 + 2) * 3, botY1, btnW4, ROW_H, Component.literal("Delete"), button -> promptRemoveSelectedBody()).compact(true).accentColor(SystemEditorTheme.RED));

        int botY2 = this.height - 24;
        int btnW3 = (width - 4) / 3;
        addRenderableWidget(new Button(panelX, botY2, btnW3, ROW_H, Component.literal("Copy"), button -> copySelectedBodyConfig()).compact(true));
        addRenderableWidget(new Button(panelX + btnW3 + 2, botY2, btnW3, ROW_H, Component.literal("Paste"), button -> pasteBodyConfig()).compact(true));
        addRenderableWidget(new Button(panelX + (btnW3 + 2) * 2, botY2, btnW3, ROW_H, Component.literal("Duplicate"), button -> duplicateSelectedBody()).compact(true));
    }

    private void buildRightPanel() {
        int rightW = getPanelRightWidth();
        int panelX = this.width - rightW + 4;
        int startY = HEADER_H + 20;
        int width  = rightW - 8;

        buildTabHeader(panelX, startY, width);

        int formY = startY + ROW_H + 4;

        switch (activeTab) {
            case SYSTEM     -> buildSystemTab(panelX, formY, width);
            case STAR       -> buildStarTab(panelX, formY, width);
            case GENERAL    -> { if (selectedBody != null) buildGeneralTab(panelX, formY, width); }
            case ORBIT      -> { if (selectedBody != null) buildOrbitTab(panelX, formY, width); }
            case SURFACE    -> { if (selectedBody != null) buildSurfaceTab(panelX, formY, width); }
            case ATMOSPHERE -> { if (selectedBody != null && !selectedBody.isBlackHole() && !"star".equalsIgnoreCase(selectedBody.type)) buildAtmosphereTab(panelX, formY, width); }
            case RINGS      -> { if (selectedBody != null && !selectedBody.isBlackHole() && !"star".equalsIgnoreCase(selectedBody.type)) buildRingsTab(panelX, formY, width); }
            case SKY        -> { if (selectedBody != null) buildSkyTab(panelX, formY, width); }
        }

        if (selectedNode == NodeType.BODY && selectedBody != null) {
            addRenderableWidget(new Button(panelX, this.height - 22, width, ROW_H,
                    Component.literal("Reset Body"), b -> promptResetBody())
                    .compact(true)
                    .accentColor(SystemEditorTheme.RED));
        }

        updateInspectorScrollPositions();
    }

    private void buildTabHeader(int x, int y, int width) {
        if (selectedNode == NodeType.SYSTEM) {
            addTabBtn(x, y, width, "System Settings", Tab.SYSTEM);
        } else if (selectedNode == NodeType.STAR) {
            addTabBtn(x, y, width, "Central Object", Tab.STAR);
        } else {
            boolean isBH = selectedBody != null && selectedBody.isBlackHole();
            boolean isStar = selectedBody != null && "star".equalsIgnoreCase(selectedBody.type);
            if ((isBH || isStar) && (activeTab == Tab.ATMOSPHERE || activeTab == Tab.RINGS)) {
                activeTab = Tab.GENERAL;
            }

            if (isBH || isStar) {
                int tw = (width - 6) / 4;
                addTabBtn(x,            y, tw, "Gen",  Tab.GENERAL);
                addTabBtn(x + (tw+2),   y, tw, "Orb",  Tab.ORBIT);
                addTabBtn(x + (tw+2)*2, y, tw, "Surf", Tab.SURFACE);
                addTabBtn(x + (tw+2)*3, y, tw, "Sky",  Tab.SKY);
            } else {
                int tw = (width - 12) / 7;
                addTabBtn(x,            y, tw, "Gen",  Tab.GENERAL);
                addTabBtn(x + (tw+2),   y, tw, "Orb",  Tab.ORBIT);
                addTabBtn(x + (tw+2)*2, y, tw, "Surf", Tab.SURFACE);
                addTabBtn(x + (tw+2)*3, y, tw, "Atm",  Tab.ATMOSPHERE);
                addTabBtn(x + (tw+2)*4, y, tw, "Ring", Tab.RINGS);
                addTabBtn(x + (tw+2)*5, y, tw, "Sky",  Tab.SKY);
                addTabBtn(x + (tw+2)*6, y, tw, "Sys",  Tab.SYSTEM);
            }
        }
    }

    private void addTabBtn(int x, int y, int width, String label, Tab tab) {
        boolean isCurrent = (activeTab == tab);
        addRenderableWidget(new Button(x, y, width, ROW_H, Component.literal(label), b -> {
            activeTab = tab;
            rightScrollOffset = 0;
            buildLayout();
        }).activeTab(isCurrent).compact(true));
    }

    private void addInspectorWidget(AbstractWidget widget, int rawY, int height, String label) {
        addWidget(widget);
        inspectorEntries.add(new InspectorEntry(widget, rawY, height, label));
        totalInspectorHeight = Math.max(totalInspectorHeight, rawY + height);
    }

    private void updateInspectorScrollPositions() {
        int formY = HEADER_H + 44;
        int visibleTop = formY;
        int visibleBottom = this.height - 24;
        int maxScroll = getMaxInspectorScroll();
        rightScrollOffset = Math.max(0, Math.min(maxScroll, rightScrollOffset));

        for (InspectorEntry entry : inspectorEntries) {
            int widgetY = formY + entry.rawY - rightScrollOffset;
            entry.widget.setY(widgetY);
            entry.widget.visible = (widgetY + entry.height >= visibleTop && widgetY <= visibleBottom);
        }
    }

    private int getMaxInspectorScroll() {
        int formY = HEADER_H + 44;
        int visibleH = this.height - formY - 26;
        int maxScroll = Math.max(0, totalInspectorHeight - visibleH);

        ColorPreviewWidget openPicker = findOpenColorPicker();
        if (openPicker != null) {
            InspectorEntry entry = findEntryForWidget(openPicker);
            if (entry != null) {
                int popupContentBottom = entry.rawY + openPicker.getPopupBottomOffset();
                int neededScroll = popupContentBottom - visibleH;
                maxScroll = Math.max(maxScroll, neededScroll);
            }
        }

        return maxScroll;
    }

    private InspectorEntry findEntryForWidget(AbstractWidget widget) {
        for (InspectorEntry entry : inspectorEntries) {
            if (entry.widget == widget) return entry;
        }
        return null;
    }

    private EditBox addLabeledEditBox(int x, int relativeY, int width, String label, String value, Consumer<String> responder) {
        int formY = HEADER_H + 44;
        int labelWidth = 42;
        int fieldX = x + labelWidth;
        int fieldW = width - labelWidth;

        EditBox box = new EditBox(this.font, fieldX, formY + relativeY, fieldW, ROW_H, Component.literal(label));
        box.setValue(value != null ? value : "");
        box.setResponder(responder);
        addInspectorWidget(box, relativeY, ROW_H, label);
        return box;
    }

    private LabeledSlider addCustomSlider(int x, int relativeY, int width, String label, double current, double def, double min, double max, Consumer<Double> onChange) {
        int formY = HEADER_H + 44;
        int sliderW = width - 24;
        LabeledSlider slider = new LabeledSlider(x, formY + relativeY, sliderW, ROW_H, label, current, def, min, max, onChange);
        addInspectorWidget(slider, relativeY, ROW_H, null);

        Button resetBtn = new Button(x + sliderW + 2, formY + relativeY, 22, ROW_H, Component.literal("R"), b -> slider.resetToDefault()).compact(true).accentColor(SystemEditorTheme.RED);
        addInspectorWidget(resetBtn, relativeY, ROW_H, null);
        return slider;
    }

    private LabeledSlider addCustomSlider(int x, int relativeY, int width, String label, double current, DataConfig.Slider slider, Consumer<Double> onChange) {
        return addCustomSlider(x, relativeY, width, label, current, slider.def(), slider.min(), slider.max(), onChange);
    }

    private void buildSystemTab(int x, int y, int width) {
        if (activeSystem == null) return;
        totalInspectorHeight = 0;
        int relY = 0;

        sysIdEditBox = addLabeledEditBox(x, relY, width, "ID:", activeSystem.id, val -> {
            if (val != null && !val.isBlank() && !val.equalsIgnoreCase(activeSystem.id)) {
                String oldId = activeSystem.id;
                CelestialJsonLoader.removeSystemInMemory(oldId);
                activeSystem.id = val;
                CelestialJsonLoader.setActiveSelectedSystemId(val);
                CelestialJsonLoader.updateSystemInMemory(activeSystem, true);
                if (treeWidget != null) treeWidget.rebuildFromSystem(activeSystem, val);
            }
        });
        relY += 22;

        {
            int formY = HEADER_H + 44;
            int labelWidth = 42;
            String dimInitial = activeSystem.dimension != null ? activeSystem.dimension : SolarSystemData.DEFAULT_SPACE_DIMENSION;
            sysDimEditBox = new EditBox(this.font, x + labelWidth, formY + relY, width - labelWidth, ROW_H, Component.literal("Dim:")) {
                @Override
                public void setFocused(boolean focused) {
                    if (!focused && isFocused()) {
                        String val = getValue();
                        net.minecraft.resources.ResourceLocation parsed = net.minecraft.resources.ResourceLocation.tryParse(val);
                        net.minecraft.client.multiplayer.ClientPacketListener conn = Minecraft.getInstance().getConnection();
                        boolean exists = parsed != null && conn != null && conn.levels().contains(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, parsed));
                        String resolved = exists ? val : SolarSystemData.DEFAULT_SPACE_DIMENSION;
                        if (!resolved.equals(val)) setValue(resolved);
                        if (!CelestialJsonLoader.isBlacklistedForSpaceDimension(resolved)) {
                            activeSystem.dimension = resolved;
                            notifyChanged();
                        }
                    }
                    super.setFocused(focused);
                }
            };
            sysDimEditBox.setValue(dimInitial);
            addInspectorWidget(sysDimEditBox, relY, ROW_H, "Dim:");
        }
        relY += 22;

        addCustomSlider(x, relY, width, "Origin X", activeSystem.originX, DataConfig.System.ORIGIN_X, val -> { activeSystem.originX = val; notifyChanged(); }); relY += 22;
        addCustomSlider(x, relY, width, "Origin Y", activeSystem.originY, DataConfig.System.ORIGIN_Y, val -> { activeSystem.originY = val; notifyChanged(); }); relY += 22;
        addCustomSlider(x, relY, width, "Origin Z", activeSystem.originZ, DataConfig.System.ORIGIN_Z, val -> { activeSystem.originZ = val; notifyChanged(); }); relY += 22;
        addCustomSlider(x, relY, width, "Scale", activeSystem.globalScale, DataConfig.System.SCALE, val -> { activeSystem.globalScale = val.floatValue(); notifyChanged(); });
    }

    private void buildStarTab(int x, int y, int width) {
        if (activeSystem == null || activeSystem.star == null) return;
        SunInstance.Config star = activeSystem.star;
        totalInspectorHeight = 0;
        int relY = 0;
        int formY = HEADER_H + 44;

        starIdEditBox = addLabeledEditBox(x, relY, width, "Star ID:", star.id, val -> {
            if (val == null || val.isBlank()) return;
            val = val.trim();
            if (val.equalsIgnoreCase(star.id)) return;
            if (activeSystem.findBody(val) != null) {
                showStatus("ID in use: " + val, true);
                return;
            }
            String oldStarId = star.id;
            star.id = val;
            for (PlanetInstance.Config b : activeSystem.bodies) {
                if (oldStarId.equalsIgnoreCase(b.parentId) || "sun".equalsIgnoreCase(b.parentId)) {
                    b.parentId = val;
                }
            }
            notifyChanged();
            if (treeWidget != null) treeWidget.rebuildFromSystem(activeSystem, val);
        }); relY += 22;

        Button typeBtn = new Button(x, formY + relY, width, ROW_H, Component.literal("Type: " + (star.isBlackHole() ? "Black Hole" : "Star")), b -> { star.type = star.isBlackHole() ? "star" : "blackhole"; notifyChanged(); buildLayout(); });
        addInspectorWidget(typeBtn, relY, ROW_H, null); relY += 22;

        Button enableBtn = new Button(x, formY + relY, width, ROW_H, Component.literal("Center Object Enabled: " + star.enabled), b -> { star.enabled = !star.enabled; notifyChanged(); buildLayout(); });
        addInspectorWidget(enableBtn, relY, ROW_H, null); relY += 22;

        starColorEditBox = new EditBox(this.font, x + 42, formY + relY, width - 70, ROW_H, Component.literal("Color"));
        starColorEditBox.setValue(star.colorHex != null ? star.colorHex : DataConfig.Star.COLOR_HEX_DEF);
        starColorEditBox.setResponder(val -> {
            if (val.startsWith("#") && val.length() == 7) {
                star.colorHex = val;
                if (starColorPicker != null) starColorPicker.setHexColor(val);
                notifyChanged();
            }
        });
        addInspectorWidget(starColorEditBox, relY, ROW_H, "Color:");

        starColorPicker = new ColorPreviewWidget(x + width - 26, formY + relY, 26, ROW_H, star.colorHex != null ? star.colorHex : DataConfig.Star.COLOR_HEX_DEF, hex -> {
            star.colorHex = hex;
            if (starColorEditBox != null) starColorEditBox.setValue(hex);
            notifyChanged();
        });
        addInspectorWidget(starColorPicker, relY, ROW_H, null); relY += 22;

        addCustomSlider(x, relY, width, "Radius", star.radius, DataConfig.Star.RADIUS, val -> {
            star.radius = val.floatValue();
            for (PlanetInstance.Config body : activeSystem.bodies) {
                if (body.parentId == null || body.parentId.isBlank() || body.parentId.equalsIgnoreCase(star.id) || "sun".equalsIgnoreCase(body.parentId)) {
                    if (body.orbit != null) {
                        double minSafe = OrbitCollisionUtil.getMinimumSafeOrbitRadius(activeSystem, body);
                        if (body.orbit.radius < minSafe) {
                            body.orbit.radius = minSafe;
                        }
                    }
                }
            }
            notifyChanged();
        }); relY += 22;

        if (star.isBlackHole()) {
            addCustomSlider(x, relY, width, "Disk Speed", star.diskRotationSpeed, DataConfig.Star.DISK_ROTATION_SPEED, val -> { star.diskRotationSpeed = val.floatValue(); notifyChanged(); }); relY += 22;
            addCustomSlider(x, relY, width, "Intensity", star.intensity, DataConfig.Star.INTENSITY, val -> { star.intensity = val.floatValue(); notifyChanged(); }); relY += 22;
        }

        addCustomSlider(x, relY, width, "Yaw (°)", star.yaw, DataConfig.Star.YAW, val -> { star.yaw = val.floatValue(); notifyChanged(); }); relY += 22;
        addCustomSlider(x, relY, width, "Pitch (°)", star.pitch, DataConfig.Star.PITCH, val -> { star.pitch = val.floatValue(); notifyChanged(); }); relY += 22;
        addCustomSlider(x, relY, width, "Roll (°)", star.roll, DataConfig.Star.ROLL, val -> { star.roll = val.floatValue(); notifyChanged(); });
    }

    private void buildGeneralTab(int x, int y, int width) {
        totalInspectorHeight = 0;
        int relY = 0;
        int formY = HEADER_H + 44;

        Button typeBtn = new Button(x, formY + relY, width, ROW_H, Component.literal("Type: " + selectedBody.type), b -> {
            String currType = selectedBody.type != null ? selectedBody.type.toLowerCase(Locale.ROOT) : "planet";
            String nextType;
            if ("planet".equals(currType)) {
                List<PlanetInstance.Config> childMoons = activeSystem.getMoonsOf(selectedBody.id);
                if (!childMoons.isEmpty()) {
                    showStatus("Cannot convert: has moons", true);
                    nextType = "star";
                } else {
                    PlanetInstance.Config availablePlanet = activeSystem.bodies.stream()
                            .filter(other -> other != selectedBody && other.isPlanet())
                            .findFirst().orElse(null);
                    if (availablePlanet == null) {
                        showStatus("No parent planet available", true);
                        nextType = "star";
                    } else {
                        nextType = "moon";
                        selectedBody.parentId = availablePlanet.id;
                    }
                }
            } else if ("moon".equals(currType)) {
                nextType = "star";
                selectedBody.parentId = (activeSystem.star != null && activeSystem.star.id != null && !activeSystem.star.id.isBlank())
                        ? activeSystem.star.id : DataConfig.Body.PARENT_ID_DEF;
            } else if ("star".equals(currType)) {
                nextType = "blackhole";
            } else {
                nextType = "planet";
                selectedBody.parentId = (activeSystem.star != null && activeSystem.star.id != null && !activeSystem.star.id.isBlank())
                        ? activeSystem.star.id : DataConfig.Body.PARENT_ID_DEF;
            }

            selectedBody.type = nextType;
            if (selectedBody.isBlackHole()) {
                selectedBody.atmosphere.enabled = false;
                selectedBody.ring.enabled = false;
            }
            notifyChanged();
            buildLayout();
        });
        addInspectorWidget(typeBtn, relY, ROW_H, null); relY += 22;

        bodyIdEditBox = addLabeledEditBox(x, relY, width, "ID:", selectedBody.id, val -> {
            if (val == null || val.isBlank()) return;
            val = val.trim();
            if (val.equalsIgnoreCase(selectedBody.id)) return;
            String starId = activeSystem.star != null ? activeSystem.star.id : "sun";
            if (val.equalsIgnoreCase(starId) || activeSystem.findBody(val) != null) {
                showStatus("ID in use: " + val, true);
                return;
            }
            String oldId = selectedBody.id;
            selectedBody.id = val;
            for (PlanetInstance.Config body : activeSystem.bodies) {
                if (oldId.equalsIgnoreCase(body.parentId)) {
                    body.parentId = val;
                }
            }
            notifyChanged();
            if (treeWidget != null) treeWidget.rebuildFromSystem(activeSystem, val);
        }); relY += 22;

        bodyParentEditBox = addLabeledEditBox(x, relY, width, "Parent:", selectedBody.parentId != null ? selectedBody.parentId : DataConfig.Body.PARENT_ID_DEF, val -> {
            if (val == null) return;
            val = val.trim();
            if (val.equalsIgnoreCase(selectedBody.parentId)) return;

            if (val.equalsIgnoreCase(selectedBody.id)) {
                showStatus("Self-parenting invalid", true);
                return;
            }

            String starId = activeSystem.star != null ? activeSystem.star.id : "sun";

            if (selectedBody.isMoon()) {
                if (val.isBlank() || val.equalsIgnoreCase(starId) || "sun".equalsIgnoreCase(val)) {
                    showStatus("Moons must orbit planets", true);
                    return;
                }
                PlanetInstance.Config targetParent = activeSystem.findBody(val);
                if (targetParent == null || !targetParent.isPlanet()) {
                    showStatus("Invalid parent planet", true);
                    return;
                }
                if (activeSystem.isDescendant(val, selectedBody.id)) {
                    showStatus("Circular orbit detected", true);
                    return;
                }
                selectedBody.parentId = val;
                selectedBody.orbit.radius = OrbitCollisionUtil.resolveSafeOrbitRadius(activeSystem, selectedBody, selectedBody.orbit.radius, 0.0D);
                notifyChanged();
                if (treeWidget != null) treeWidget.rebuildFromSystem(activeSystem, selectedBody.id);
                return;
            }

            if (val.isBlank() || val.equalsIgnoreCase(starId) || "sun".equalsIgnoreCase(val)) {
                selectedBody.parentId = starId;
                selectedBody.orbit.radius = OrbitCollisionUtil.resolveSafeOrbitRadius(activeSystem, selectedBody, selectedBody.orbit.radius, 0.0D);
                notifyChanged();
                if (treeWidget != null) treeWidget.rebuildFromSystem(activeSystem, selectedBody.id);
                return;
            }

            PlanetInstance.Config targetParent = activeSystem.findBody(val);
            if (targetParent != null && targetParent.isMoon()) {
                showStatus("Moons cannot have moons", true);
                return;
            }
            if (activeSystem.isDescendant(val, selectedBody.id)) {
                showStatus("Circular orbit detected", true);
                return;
            }

            selectedBody.parentId = val;
            selectedBody.orbit.radius = OrbitCollisionUtil.resolveSafeOrbitRadius(activeSystem, selectedBody, selectedBody.orbit.radius, 0.0D);
            notifyChanged();
            if (treeWidget != null) treeWidget.rebuildFromSystem(activeSystem, selectedBody.id);
        }); relY += 22;

        bodyDimEditBox = addLabeledEditBox(x, relY, width, "Dim:", selectedBody.dimension != null ? selectedBody.dimension : DataConfig.Body.DIMENSION_DEF, val -> { selectedBody.dimension = val; notifyChanged(); }); relY += 24;

        Button dupBtn = new Button(x, formY + relY, width, ROW_H, Component.literal("Duplicate Body Config"), b -> duplicateSelectedBody());
        addInspectorWidget(dupBtn, relY, ROW_H, null);
    }

    private void buildOrbitTab(int x, int y, int width) {

        if (selectedBody.orbit == null) selectedBody.orbit = new PlanetInstance.Orbit();
        totalInspectorHeight = 0;
        int relY = 0;
        int formY = HEADER_H + 44;

        Button orbBtn = new Button(x, formY + relY, width, ROW_H, Component.literal("Orbit Enabled: " + selectedBody.orbit.enabled), b -> {
            selectedBody.orbit.enabled = !selectedBody.orbit.enabled;
            notifyChanged(); buildLayout();
        });

        addInspectorWidget(orbBtn, relY, ROW_H, null); relY += 22;

        double minSafeRadius = OrbitCollisionUtil.getMinimumSafeOrbitRadius(activeSystem, selectedBody);
        double maxSafeRadius = OrbitCollisionUtil.getMaximumSafeOrbitRadius(activeSystem, selectedBody);
        if (selectedBody.orbit.radius < minSafeRadius) {
            selectedBody.orbit.radius = minSafeRadius;
            notifyChanged();
        } else if (selectedBody.orbit.radius > maxSafeRadius) {
            selectedBody.orbit.radius = maxSafeRadius;
            notifyChanged();
        }

        double defRadius = Math.max(minSafeRadius, Math.min(maxSafeRadius, selectedBody.isMoon() ? DataConfig.Orbit.MOON_RADIUS : DataConfig.Orbit.RADIUS.def()));
        final LabeledSlider[] orbitSliderHolder = new LabeledSlider[1];
        orbitSliderHolder[0] = addCustomSlider(x, relY, width, "Orbit Radius", selectedBody.orbit.radius,
                defRadius, minSafeRadius, maxSafeRadius, val -> {
                    double safeVal = OrbitCollisionUtil.resolveSafeOrbitRadius(activeSystem, selectedBody, val, selectedBody.orbit.radius);
                    selectedBody.orbit.radius = safeVal;
                    if (orbitSliderHolder[0] != null) {
                        orbitSliderHolder[0].setValueQuiet(safeVal);
                    }
                    notifyChanged();
                });
        relY += 22;
        addCustomSlider(x, relY, width, "Period (Days)", selectedBody.orbit.periodDays,    DataConfig.Orbit.PERIOD_DAYS, val -> { selectedBody.orbit.periodDays    = val; notifyChanged(); }); relY += 22;
        addCustomSlider(x, relY, width, "Inclination°",  selectedBody.orbit.inclination,   DataConfig.Orbit.INCLINATION, val -> { selectedBody.orbit.inclination   = val; notifyChanged(); }); relY += 22;
        addCustomSlider(x, relY, width, "Asc Node°",     selectedBody.orbit.ascendingNode, DataConfig.Orbit.ASCENDING_NODE, val -> { selectedBody.orbit.ascendingNode = val; notifyChanged(); }); relY += 22;
        addCustomSlider(x, relY, width, "Epoch Angle°",  selectedBody.orbit.epochAngle,    DataConfig.Orbit.EPOCH_ANGLE, val -> { selectedBody.orbit.epochAngle    = val; notifyChanged(); }); relY += 22;
        addCustomSlider(x, relY, width, "Vert Offset",   selectedBody.orbit.verticalOffset, DataConfig.Orbit.VERTICAL_OFFSET, val -> { selectedBody.orbit.verticalOffset= val; notifyChanged(); }); relY += 22;
        addCustomSlider(x, relY, width, "Spin (Hours)", selectedBody.spinHours, DataConfig.Body.SPIN_HOURS, val -> { selectedBody.spinHours = val; notifyChanged(); }); relY += 22;

        epochUtcEditBox = addLabeledEditBox(x, relY, width, "Epoch:", selectedBody.orbit.epochUtc != null ? selectedBody.orbit.epochUtc : DataConfig.Orbit.EPOCH_UTC_DEF, val -> { selectedBody.orbit.epochUtc = val; notifyChanged(); });
    }

    private void buildSurfaceTab(int x, int y, int width) {
        totalInspectorHeight = 0;
        int relY = 0;
        int formY = HEADER_H + 44;

        addCustomSlider(x, relY, width, "Body Radius", selectedBody.radius, DataConfig.Body.RADIUS, val -> {
            selectedBody.radius = val.floatValue();
            if (selectedBody.orbit != null) {
                double minSafe = OrbitCollisionUtil.getMinimumSafeOrbitRadius(activeSystem, selectedBody);
                if (selectedBody.orbit.radius < minSafe) {
                    selectedBody.orbit.radius = minSafe;
                }
            }
            for (PlanetInstance.Config moon : activeSystem.getMoonsOf(selectedBody.id)) {
                if (moon.orbit != null) {
                    double moonMinSafe = OrbitCollisionUtil.getMinimumSafeOrbitRadius(activeSystem, moon);
                    if (moon.orbit.radius < moonMinSafe) {
                        moon.orbit.radius = moonMinSafe;
                    }
                }
            }
            notifyChanged();
        }); relY += 22;
        addCustomSlider(x, relY, width, "Gravity", selectedBody.gravity, DataConfig.Body.GRAVITY, val -> {selectedBody.gravity = val.floatValue();notifyChanged();});relY += 22;

        Button oxygenBtn = new Button(x, formY + relY, width, ROW_H, Component.literal("Oxygen: " + (selectedBody.oxygen ? "True" : "False")), b -> {
            selectedBody.oxygen = !selectedBody.oxygen;
            b.setMessage(Component.literal("Oxygen: " + (selectedBody.oxygen ? "True" : "False")));
            notifyChanged();
        });
        addInspectorWidget(oxygenBtn, relY, ROW_H, null); relY += 22;

        addCustomSlider(x, relY, width, "Temperature", selectedBody.temperature, DataConfig.Body.TEMPERATURE, val -> { selectedBody.temperature = val.floatValue(); notifyChanged(); }); relY += 22;

        if (selectedBody.isBlackHole()) {
            addCustomSlider(x, relY, width, "Disk Speed", selectedBody.diskRotationSpeed, DataConfig.Body.DISK_ROTATION_SPEED, val -> { selectedBody.diskRotationSpeed = val.floatValue(); notifyChanged(); }); relY += 22;
            addCustomSlider(x, relY, width, "Intensity", selectedBody.intensity, DataConfig.Body.INTENSITY, val -> { selectedBody.intensity = val.floatValue(); notifyChanged(); }); relY += 22;
        } else {
            dayTextureEditBox = addLabeledEditBox(x, relY, width, "Day:", selectedBody.texture != null ? selectedBody.texture : DataConfig.Body.TEXTURE_DEF, val -> { selectedBody.texture = val; notifyChanged(); }); relY += 22;
            nightTextureEditBox = addLabeledEditBox(x, relY, width, "Night:", selectedBody.nightTexture != null ? selectedBody.nightTexture : DataConfig.Body.NIGHT_TEXTURE_DEF, val -> { selectedBody.nightTexture = val; notifyChanged(); }); relY += 22;
        }

        bodyColorEditBox = new EditBox(this.font, x + 42, formY + relY, width - 70, ROW_H, Component.literal("Color Hex"));
        bodyColorEditBox.setValue(selectedBody.colorHex != null ? selectedBody.colorHex : DataConfig.Body.COLOR_HEX_DEF);
        bodyColorEditBox.setResponder(val -> {
            if (val.startsWith("#") && val.length() == 7) {
                selectedBody.colorHex = val;
                if (bodyColorPicker != null) bodyColorPicker.setHexColor(val);
                notifyChanged();
            }
        });
        addInspectorWidget(bodyColorEditBox, relY, ROW_H, "Color:");

        bodyColorPicker = new ColorPreviewWidget(x + width - 26, formY + relY, 26, ROW_H, selectedBody.colorHex, hex -> {
            selectedBody.colorHex = hex;
            if (bodyColorEditBox != null) bodyColorEditBox.setValue(hex);
            notifyChanged();
        });
        addInspectorWidget(bodyColorPicker, relY, ROW_H, null); relY += 22;

        addCustomSlider(x, relY, width, "Rot Yaw°",   selectedBody.yaw,   DataConfig.Body.ROT_YAW, val -> { selectedBody.yaw   = val.floatValue(); notifyChanged(); }); relY += 22;
        addCustomSlider(x, relY, width, "Rot Pitch°", selectedBody.pitch, DataConfig.Body.ROT_PITCH, val -> { selectedBody.pitch = val.floatValue(); notifyChanged(); }); relY += 22;
        addCustomSlider(x, relY, width, "Rot Roll°",  selectedBody.roll,  DataConfig.Body.ROT_ROLL, val -> { selectedBody.roll  = val.floatValue(); notifyChanged(); });

        if (!selectedBody.isBlackHole() && !"star".equalsIgnoreCase(selectedBody.type)) {
            if (selectedBody.clouds == null) selectedBody.clouds = new PlanetInstance.Clouds();
            relY += 22;

            Button cldBtn = new Button(x, formY + relY, width, ROW_H, Component.literal("Clouds Enabled: " + selectedBody.clouds.enabled), b -> {
                selectedBody.clouds.enabled = !selectedBody.clouds.enabled;
                b.setMessage(Component.literal("Clouds Enabled: " + selectedBody.clouds.enabled));
                notifyChanged();
            });
            addInspectorWidget(cldBtn, relY, ROW_H, null); relY += 22;

            cloudTextureEditBox = addLabeledEditBox(x, relY, width, "Noise:", selectedBody.clouds.texture != null ? selectedBody.clouds.texture : DataConfig.Clouds.TEXTURE_DEF, val -> { selectedBody.clouds.texture = val; notifyChanged(); }); relY += 22;

            cloudColorEditBox = new EditBox(this.font, x + 42, formY + relY, width - 70, ROW_H, Component.literal("Color Hex"));
            cloudColorEditBox.setValue(selectedBody.clouds.colorHex != null ? selectedBody.clouds.colorHex : DataConfig.Clouds.COLOR_HEX_DEF);
            cloudColorEditBox.setResponder(val -> {
                if (val.startsWith("#") && val.length() == 7) {
                    selectedBody.clouds.colorHex = val;
                    if (cloudColorPicker != null) cloudColorPicker.setHexColor(val);
                    notifyChanged();
                }
            });
            addInspectorWidget(cloudColorEditBox, relY, ROW_H, "Color:");

            cloudColorPicker = new ColorPreviewWidget(x + width - 26, formY + relY, 26, ROW_H, selectedBody.clouds.colorHex != null ? selectedBody.clouds.colorHex : DataConfig.Clouds.COLOR_HEX_DEF, hex -> {
                selectedBody.clouds.colorHex = hex;
                if (cloudColorEditBox != null) cloudColorEditBox.setValue(hex);
                notifyChanged();
            });
            addInspectorWidget(cloudColorPicker, relY, ROW_H, null); relY += 22;

            addCustomSlider(x, relY, width, "Cloud Height", selectedBody.clouds.height, DataConfig.Clouds.HEIGHT, val -> { selectedBody.clouds.height = val.floatValue(); notifyChanged(); }); relY += 22;
            addCustomSlider(x, relY, width, "Cloud Density", selectedBody.clouds.density, DataConfig.Clouds.DENSITY, val -> { selectedBody.clouds.density = val.floatValue(); notifyChanged(); }); relY += 22;
            addCustomSlider(x, relY, width, "Noise Scale", selectedBody.clouds.noiseScale, DataConfig.Clouds.NOISE_SCALE, val -> { selectedBody.clouds.noiseScale = val.floatValue(); notifyChanged(); }); relY += 22;
            addCustomSlider(x, relY, width, "Wind Speed", selectedBody.clouds.windSpeed, DataConfig.Clouds.WIND_SPEED, val -> { selectedBody.clouds.windSpeed = val.floatValue(); notifyChanged(); });
        }
    }

    private void buildAtmosphereTab(int x, int y, int width) {
        if (selectedBody.atmosphere == null) selectedBody.atmosphere = new PlanetInstance.Atmosphere();
        totalInspectorHeight = 0;
        int relY = 0;
        int formY = HEADER_H + 44;

        Button atmBtn = new Button(x, formY + relY, width, ROW_H, Component.literal("Atmosphere Enabled: " + selectedBody.atmosphere.enabled), b -> {
            selectedBody.atmosphere.enabled = !selectedBody.atmosphere.enabled;
            notifyChanged(); buildLayout();
        });
        addInspectorWidget(atmBtn, relY, ROW_H, null); relY += 22;

        addCustomSlider(x, relY, width, "Thickness",    selectedBody.atmosphere.thickness,         DataConfig.Atmosphere.THICKNESS, val -> { selectedBody.atmosphere.thickness         = val.floatValue(); notifyChanged(); }); relY += 22;
        addCustomSlider(x, relY, width, "Exposure",     selectedBody.atmosphere.exposure,           DataConfig.Atmosphere.EXPOSURE, val -> { selectedBody.atmosphere.exposure          = val.floatValue(); notifyChanged(); }); relY += 22;
        addCustomSlider(x, relY, width, "Intensity",    selectedBody.atmosphere.intensity,          DataConfig.Atmosphere.INTENSITY, val -> { selectedBody.atmosphere.intensity         = val.floatValue(); notifyChanged(); }); relY += 22;
        addCustomSlider(x, relY, width, "Rayleigh H",   selectedBody.atmosphere.rayleighScaleHeight, DataConfig.Atmosphere.RAYLEIGH_SCALE_HEIGHT, val -> { selectedBody.atmosphere.rayleighScaleHeight= val.floatValue(); notifyChanged(); }); relY += 22;
        addCustomSlider(x, relY, width, "Rayleigh Str", selectedBody.atmosphere.rayleighStrength,   DataConfig.Atmosphere.RAYLEIGH_STRENGTH, val -> { selectedBody.atmosphere.rayleighStrength  = val.floatValue(); notifyChanged(); }); relY += 22;
        addCustomSlider(x, relY, width, "λ Red",        selectedBody.atmosphere.wavelengthR,        DataConfig.Atmosphere.WAVELENGTH_R, val -> { selectedBody.atmosphere.wavelengthR       = val.floatValue(); notifyChanged(); }); relY += 22;
        addCustomSlider(x, relY, width, "λ Green",      selectedBody.atmosphere.wavelengthG,        DataConfig.Atmosphere.WAVELENGTH_G, val -> { selectedBody.atmosphere.wavelengthG       = val.floatValue(); notifyChanged(); }); relY += 22;
        addCustomSlider(x, relY, width, "λ Blue",       selectedBody.atmosphere.wavelengthB,        DataConfig.Atmosphere.WAVELENGTH_B, val -> { selectedBody.atmosphere.wavelengthB       = val.floatValue(); notifyChanged(); }); relY += 22;

        atmosColorEditBox = new EditBox(this.font, x + 42, formY + relY, width - 70, ROW_H, Component.literal("Color Hex"));
        atmosColorEditBox.setValue(selectedBody.atmosphere.colorHex != null ? selectedBody.atmosphere.colorHex : DataConfig.Atmosphere.COLOR_HEX_DEF);
        atmosColorEditBox.setResponder(val -> {
            if (val.startsWith("#") && val.length() == 7) {
                selectedBody.atmosphere.colorHex = val;
                if (atmosColorPicker != null) atmosColorPicker.setHexColor(val);
                notifyChanged();
            }
        });
        addInspectorWidget(atmosColorEditBox, relY, ROW_H, "Color:");

        atmosColorPicker = new ColorPreviewWidget(x + width - 26, formY + relY, 26, ROW_H, selectedBody.atmosphere.colorHex != null ? selectedBody.atmosphere.colorHex : DataConfig.Atmosphere.COLOR_HEX_DEF, hex -> {
            selectedBody.atmosphere.colorHex = hex;
            if (atmosColorEditBox != null) atmosColorEditBox.setValue(hex);
            notifyChanged();
        });
        addInspectorWidget(atmosColorPicker, relY, ROW_H, null);
    }

    private void buildRingsTab(int x, int y, int width) {
        if (selectedBody.ring == null) selectedBody.ring = new PlanetInstance.Ring();
        totalInspectorHeight = 0;
        int relY = 0;
        int formY = HEADER_H + 44;

        Button rngBtn = new Button(x, formY + relY, width, ROW_H, Component.literal("Ring Enabled: " + selectedBody.ring.enabled), b -> {
            selectedBody.ring.enabled = !selectedBody.ring.enabled;
            notifyChanged(); buildLayout();
        });
        addInspectorWidget(rngBtn, relY, ROW_H, null); relY += 22;

        addCustomSlider(x, relY, width, "Inner Radius", selectedBody.ring.innerRadius, DataConfig.Ring.INNER_RADIUS, val -> {float newInner = val.floatValue();selectedBody.ring.innerRadius = newInner;if (selectedBody.ring.outerRadius <= newInner) {selectedBody.ring.outerRadius = newInner + 0.1F;}notifyChanged();});relY += 22;
        addCustomSlider(x, relY, width, "Outer Radius", selectedBody.ring.outerRadius, DataConfig.Ring.OUTER_RADIUS, val -> {
            float newOuter = val.floatValue();
            selectedBody.ring.outerRadius = newOuter;
            if (selectedBody.ring.innerRadius >= newOuter) {
                selectedBody.ring.innerRadius = Math.max(DataConfig.Ring.INNER_RADIUS.minF(), newOuter);
            }
            for (PlanetInstance.Config moon : activeSystem.getMoonsOf(selectedBody.id)) {
                if (moon.orbit != null) {
                    double moonMinSafe = OrbitCollisionUtil.getMinimumSafeOrbitRadius(activeSystem, moon);
                    if (moon.orbit.radius < moonMinSafe) {
                        moon.orbit.radius = moonMinSafe;
                    }
                }
            }
            notifyChanged();
        }); relY += 22;

        ringTextureEditBox = addLabeledEditBox(x, relY, width, "Ring:", selectedBody.ring.texture != null ? selectedBody.ring.texture : DataConfig.Ring.TEXTURE_DEF, val -> { selectedBody.ring.texture = val; notifyChanged(); }); relY += 22;
        rockTextureEditBox = addLabeledEditBox(x, relY, width, "Rock:", selectedBody.ring.rockTexture != null ? selectedBody.ring.rockTexture : DataConfig.Ring.ROCK_TEXTURE_DEF, val -> { selectedBody.ring.rockTexture = val; notifyChanged(); }); relY += 22;

        ringColorEditBox = new EditBox(this.font, x + 42, formY + relY, width - 70, ROW_H, Component.literal("Color Hex"));

        addCustomSlider(x, relY, width, "Ring Yaw°",   selectedBody.ring.yaw,   DataConfig.Ring.YAW, val -> { selectedBody.ring.yaw   = val.floatValue(); notifyChanged(); }); relY += 22;
        addCustomSlider(x, relY, width, "Ring Pitch°",  selectedBody.ring.pitch, DataConfig.Ring.PITCH, val -> { selectedBody.ring.pitch = val.floatValue(); notifyChanged(); }); relY += 22;
        addCustomSlider(x, relY, width, "Ring Roll°",   selectedBody.ring.roll,  DataConfig.Ring.ROLL, val -> { selectedBody.ring.roll  = val.floatValue(); notifyChanged(); }); relY += 22;

        Button rkBtn = new Button(x, formY + relY, width, ROW_H, Component.literal("Rocks Enabled: " + selectedBody.ring.rocksEnabled), b -> {
            selectedBody.ring.rocksEnabled = !selectedBody.ring.rocksEnabled;
            notifyChanged(); buildLayout();
        });
        addInspectorWidget(rkBtn, relY, ROW_H, null); relY += 22;

        addCustomSlider(x, relY, width, "Rock Count",    selectedBody.ring.rockCount,   DataConfig.Ring.ROCK_COUNT, val -> { selectedBody.ring.rockCount   = val.intValue();   notifyChanged(); }); relY += 22;
        addCustomSlider(x, relY, width, "Rock Min Size", selectedBody.ring.rockMinSize,  DataConfig.Ring.ROCK_MIN_SIZE, val -> { selectedBody.ring.rockMinSize  = val.floatValue(); notifyChanged(); }); relY += 22;
        addCustomSlider(x, relY, width, "Rock Max Size", selectedBody.ring.rockMaxSize,  DataConfig.Ring.ROCK_MAX_SIZE, val -> { selectedBody.ring.rockMaxSize  = val.floatValue(); notifyChanged(); }); relY += 22;
        addCustomSlider(x, relY, width, "Rock Height",   selectedBody.ring.rockHeight,   DataConfig.Ring.ROCK_HEIGHT, val -> { selectedBody.ring.rockHeight   = val.floatValue(); notifyChanged(); }); relY += 22;
        addCustomSlider(x, relY, width, "Orbit Speed",   selectedBody.ring.rockOrbitSpeed, DataConfig.Ring.ROCK_ORBIT_SPEED, val -> { selectedBody.ring.rockOrbitSpeed= val.floatValue(); notifyChanged(); });
    }

    private void buildSkyTab(int x, int y, int width) {
        if (selectedBody.sky == null) selectedBody.sky = new PlanetInstance.Sky();
        if (selectedBody.fog == null) selectedBody.fog = new PlanetInstance.SurfaceInstance.SurfaceFog();
        totalInspectorHeight = 0;
        int relY = 0;
        int formY = HEADER_H + 44;

        Button groundBtn = new Button(x, formY + relY, width, ROW_H, Component.literal("Ground Mode: " + selectedBody.sky.groundMode), b -> {
            selectedBody.sky.groundMode = !selectedBody.sky.groundMode;
            notifyChanged(); buildLayout();
        });
        addInspectorWidget(groundBtn, relY, ROW_H, null); relY += 22;

        Button skyBtn = new Button(x, formY + relY, width, ROW_H, Component.literal("Skybox Rotation: " + selectedBody.sky.skyboxRotation), b -> {
            selectedBody.sky.skyboxRotation = !selectedBody.sky.skyboxRotation;
            notifyChanged(); buildLayout();
        });
        addInspectorWidget(skyBtn, relY, ROW_H, null); relY += 22;

        Button skyConstBtn = new Button(x, formY + relY, width, ROW_H, Component.literal("Skybox Constant: " + selectedBody.sky.skyboxConstant), b -> {
            selectedBody.sky.skyboxConstant = !selectedBody.sky.skyboxConstant;
            notifyChanged(); buildLayout();
        });
        addInspectorWidget(skyConstBtn, relY, ROW_H, null); relY += 22;

        skyTextureEditBox = addLabeledEditBox(x, relY, width, "Tex:", selectedBody.sky.skyboxTexture != null ? selectedBody.sky.skyboxTexture : DataConfig.Sky.SKYBOX_TEXTURE_DEF, val -> { selectedBody.sky.skyboxTexture = val; notifyChanged(); }); relY += 22;

        Button starsBtn = new Button(x, formY + relY, width, ROW_H, Component.literal("Stars Enabled: " + selectedBody.sky.starsEnabled), b -> {
            selectedBody.sky.starsEnabled = !selectedBody.sky.starsEnabled;
            notifyChanged(); buildLayout();
        });
        addInspectorWidget(starsBtn, relY, ROW_H, null); relY += 22;

        addCustomSlider(x, relY, width, "Stars Amount", selectedBody.sky.starsAmount, DataConfig.Sky.STARS_AMOUNT, val -> { selectedBody.sky.starsAmount = val.intValue(); notifyChanged(); }); relY += 22;
        addCustomSlider(x, relY, width, "Stars Seed", selectedBody.sky.starsSeed, DataConfig.Sky.STARS_SEED, val -> { selectedBody.sky.starsSeed = val.intValue(); notifyChanged(); }); relY += 22;

        starsColorEditBox = new EditBox(this.font, x + 42, formY + relY, width - 70, ROW_H, Component.literal("Color Hex"));
        starsColorEditBox.setValue(selectedBody.sky.starsColorHex != null ? selectedBody.sky.starsColorHex : DataConfig.Sky.STARS_COLOR_HEX_DEF);
        starsColorEditBox.setResponder(val -> {
            if (val.startsWith("#") && val.length() == 7) {
                selectedBody.sky.starsColorHex = val;
                if (starsColorPicker != null) starsColorPicker.setHexColor(val);
                notifyChanged();
            }
        });
        addInspectorWidget(starsColorEditBox, relY, ROW_H, "Color:");

        starsColorPicker = new ColorPreviewWidget(x + width - 26, formY + relY, 26, ROW_H, selectedBody.sky.starsColorHex != null ? selectedBody.sky.starsColorHex : DataConfig.Sky.STARS_COLOR_HEX_DEF, hex -> {
            selectedBody.sky.starsColorHex = hex;
            if (starsColorEditBox != null) starsColorEditBox.setValue(hex);
            notifyChanged();
        });
        addInspectorWidget(starsColorPicker, relY, ROW_H, null); relY += 24;

        Button fogBtn = new Button(x, formY + relY, width, ROW_H, Component.literal("Fog Enabled: " + selectedBody.fog.enabled), b -> {
            selectedBody.fog.enabled = !selectedBody.fog.enabled;
            notifyChanged(); buildLayout();
        });
        addInspectorWidget(fogBtn, relY, ROW_H, null); relY += 22;

        if (selectedBody.fog.enabled) {
            Button fogShapeBtn = new Button(x, formY + relY, width, ROW_H, Component.literal("Fog Shape: " + selectedBody.fog.shape), b -> {
                selectedBody.fog.shape = "CYLINDER".equalsIgnoreCase(selectedBody.fog.shape) ? "SPHERE" : "CYLINDER";
                notifyChanged(); buildLayout();
            });
            addInspectorWidget(fogShapeBtn, relY, ROW_H, null); relY += 22;

            fogColorEditBox = new EditBox(this.font, x + 42, formY + relY, width - 70, ROW_H, Component.literal("Color Hex"));
            fogColorEditBox.setValue(selectedBody.fog.colorHex != null ? selectedBody.fog.colorHex : DataConfig.Fog.COLOR_HEX_DEF);
            fogColorEditBox.setResponder(val -> {
                if (val.startsWith("#") && val.length() == 7) {
                    selectedBody.fog.colorHex = val;
                    if (fogColorPicker != null) fogColorPicker.setHexColor(val);
                    notifyChanged();
                }
            });
            addInspectorWidget(fogColorEditBox, relY, ROW_H, "Fog Clr:");

            fogColorPicker = new ColorPreviewWidget(x + width - 26, formY + relY, 26, ROW_H, selectedBody.fog.colorHex != null ? selectedBody.fog.colorHex : DataConfig.Fog.COLOR_HEX_DEF, hex -> {
                selectedBody.fog.colorHex = hex;
                if (fogColorEditBox != null) fogColorEditBox.setValue(hex);
                notifyChanged();
            });
            addInspectorWidget(fogColorPicker, relY, ROW_H, null); relY += 22;

            Button useRdBtn = new Button(x, formY + relY, width, ROW_H, Component.literal("Use RenderDist: " + selectedBody.fog.useRenderDistance), b -> {
                selectedBody.fog.useRenderDistance = !selectedBody.fog.useRenderDistance;
                notifyChanged(); buildLayout();
            });
            addInspectorWidget(useRdBtn, relY, ROW_H, null); relY += 22;

            if (selectedBody.fog.useRenderDistance) {
                addCustomSlider(x, relY, width, "Fog Start %", selectedBody.fog.startDistance, DataConfig.Fog.START_PERCENT, val -> { selectedBody.fog.startDistance = val.floatValue(); notifyChanged(); }); relY += 22;
                addCustomSlider(x, relY, width, "Fog End %", selectedBody.fog.endDistance, DataConfig.Fog.END_PERCENT, val -> { selectedBody.fog.endDistance = val.floatValue(); notifyChanged(); });
            } else {
                addCustomSlider(x, relY, width, "Fog Start", selectedBody.fog.startDistance, DataConfig.Fog.START_DISTANCE, val -> { selectedBody.fog.startDistance = val.floatValue(); notifyChanged(); }); relY += 22;
                addCustomSlider(x, relY, width, "Fog End", selectedBody.fog.endDistance, DataConfig.Fog.END_DISTANCE, val -> { selectedBody.fog.endDistance = val.floatValue(); notifyChanged(); });
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int rightW = getPanelRightWidth();
        int p2X = this.width - rightW;

        if (!rightPanelCollapsed && mouseX >= p2X && mouseX <= this.width && mouseY >= HEADER_H + 40 && mouseY <= this.height - 24) {
            int maxScroll = getMaxInspectorScroll();
            this.rightScrollOffset = Math.max(0, Math.min(maxScroll, this.rightScrollOffset - (int) (amount * 18)));
            updateInspectorScrollPositions();
            return true;
        }

        if (CameraPlanetOrbit.isActive() && isInViewport(mouseX, mouseY)) {
            CameraPlanetOrbit.handleScroll(amount);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (hideUI) {
            g.fill(this.width - 200, 4, this.width - 4, 20, SystemEditorTheme.PREVIEW_BANNER_BG);
            g.drawString(this.font, "Preview Mode [ TAB/H to toggle ]", this.width - 194, 8, SystemEditorTheme.PREVIEW_BANNER_TEXT, SystemEditorTheme.TEXT_SHADOW);
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }

        int leftW = getPanelLeftWidth();
        int rightW = getPanelRightWidth();

        g.fill(0, 0, this.width, HEADER_H, SystemEditorTheme.HEADER_BG);
        g.fill(0, HEADER_H - 1, this.width, HEADER_H, SystemEditorTheme.HEADER_BORDER_BOT);

        if (!leftPanelCollapsed) {
            g.fill(0, HEADER_H, leftW, this.height, SystemEditorTheme.PANEL_BG);
            g.fill(leftW - 1, HEADER_H, leftW, this.height, SystemEditorTheme.PANEL_BORDER);
            if (!systemDropdownOpen) {
                int bodyCount = activeSystem != null ? activeSystem.bodies.size() : 0;
                String treeTitle = "SYSTEM TREE (" + bodyCount + "/" + DataConfig.System.MAX_BODIES_LIMIT + ")";
                g.drawString(this.font, treeTitle, 6, HEADER_H + 5, SystemEditorTheme.PANEL_TITLE_TEXT, SystemEditorTheme.TEXT_SHADOW);
            }
        }

        if (!rightPanelCollapsed) {
            int p2X = this.width - rightW;
            g.fill(p2X, HEADER_H, this.width, this.height, SystemEditorTheme.PANEL_BG);
            g.fill(p2X, HEADER_H, p2X + 1, this.height, SystemEditorTheme.PANEL_BORDER);

            String rightHeader = switch (selectedNode) {
                case SYSTEM -> "SYSTEM [" + (activeSystem != null ? activeSystem.id : "?") + "]";
                case STAR   -> "STAR [" + (activeSystem != null && activeSystem.star != null ? activeSystem.star.id : "?") + "]";
                case BODY   -> "BODY [" + (selectedBody != null ? selectedBody.id : "?") + "]";
            };
            g.drawString(this.font, rightHeader, p2X + 30, HEADER_H + 5, SystemEditorTheme.PANEL_TITLE_TEXT, SystemEditorTheme.TEXT_SHADOW);
        }

        int p2X = this.width - rightW;
        int formY = HEADER_H + 42;
        int visibleH = this.height - formY - 26;
        int scissorBottom = this.height - 24;

        ColorPreviewWidget openPicker = findOpenColorPicker();
        int[] popupBounds = openPicker != null ? openPicker.getPopupBounds() : null;

        if (!rightPanelCollapsed) {
            g.enableScissor(p2X, formY, this.width, scissorBottom);

            for (InspectorEntry entry : inspectorEntries) {
                if (!entry.widget.visible) continue;
                if (popupBounds != null && entry.widget != openPicker && rectsOverlap(
                        entry.widget.getX(), entry.widget.getY(), entry.widget.getWidth(), entry.widget.getHeight(),
                        popupBounds[0], popupBounds[1], popupBounds[2], popupBounds[3])) {
                    continue;
                }

                if (entry.label != null) {
                    int labelY = formY + entry.rawY - rightScrollOffset + (ROW_H - 8) / 2;
                    g.drawString(this.font, entry.label, p2X + 6, labelY, SystemEditorTheme.INSPECTOR_LABEL_TEXT, SystemEditorTheme.TEXT_SHADOW);
                }
                entry.widget.render(g, mouseX, mouseY, partialTick);
            }

            g.disableScissor();
        }

        g.flush();

        boolean hideSearchAndTree = systemDropdownOpen && searchEditBox != null;
        if (hideSearchAndTree) {
            searchEditBox.visible = false;
            if (treeWidget != null) treeWidget.visible = false;
        }

        super.render(g, mouseX, mouseY, partialTick);

        if (hideSearchAndTree) {
            searchEditBox.visible = true;
            if (treeWidget != null) treeWidget.visible = true;
        }

        g.flush();

        if (!rightPanelCollapsed) {

            if (totalInspectorHeight > visibleH && visibleH > 0) {
                int maxScroll = getMaxInspectorScroll();
                int scrollbarH = Math.max(15, (visibleH * visibleH) / totalInspectorHeight);
                int scrollbarY = formY + (rightScrollOffset * (visibleH - scrollbarH)) / Math.max(1, maxScroll);
                int sbX = this.width - 4;

                g.fill(sbX, formY, sbX + 3, formY + visibleH, SystemEditorTheme.INSPECTOR_SCROLLBAR_BG);
                g.fill(sbX, scrollbarY, sbX + 3, scrollbarY + scrollbarH, SystemEditorTheme.INSPECTOR_SCROLLBAR_THUMB);
            }
        }

        if (!rightPanelCollapsed) {
            g.enableScissor(p2X, formY, this.width, scissorBottom);

            renderColorPickerOverlay(starColorPicker, g, mouseX, mouseY);
            renderColorPickerOverlay(bodyColorPicker, g, mouseX, mouseY);
            renderColorPickerOverlay(atmosColorPicker, g, mouseX, mouseY);
            renderColorPickerOverlay(ringColorPicker, g, mouseX, mouseY);
            renderColorPickerOverlay(cloudColorPicker, g, mouseX, mouseY);
            renderColorPickerOverlay(starsColorPicker, g, mouseX, mouseY);
            renderColorPickerOverlay(fogColorPicker, g, mouseX, mouseY);

            g.disableScissor();
        }

        if (systemDropdownOpen) {
            renderSystemDropdown(g, mouseX, mouseY);
        }

        if (System.currentTimeMillis() < statusMessageTime) {
            int textWidth   = this.font.width(statusMessage);
            int toastWidth  = textWidth + 16;
            int toastHeight = 15;

            int x1 = (this.width - toastWidth) / 2;
            int y1 = HEADER_H + 4;
            int x2 = x1 + toastWidth;
            int y2 = y1 + toastHeight;

            int bgColor     = 0xE6141414;
            int accentColor = statusIsError ? SystemEditorTheme.TOAST_ERROR_TEXT : SystemEditorTheme.TOAST_SUCCESS_TEXT;
            int textColor   = statusIsError ? SystemEditorTheme.TOAST_ERROR_TEXT : SystemEditorTheme.TEXT_HI;

            g.fill(x1, y1, x2, y2, bgColor);
            g.renderOutline(x1, y1, toastWidth, toastHeight, accentColor);
            g.drawString(this.font, statusMessage, x1 + 8, y1 + 4, textColor, SystemEditorTheme.TEXT_SHADOW);
        }
    }

    private ColorPreviewWidget findOpenColorPicker() {
        if (starColorPicker != null && starColorPicker.isPaletteOpen()) return starColorPicker;
        if (bodyColorPicker != null && bodyColorPicker.isPaletteOpen()) return bodyColorPicker;
        if (atmosColorPicker != null && atmosColorPicker.isPaletteOpen()) return atmosColorPicker;
        if (ringColorPicker != null && ringColorPicker.isPaletteOpen()) return ringColorPicker;
        if (cloudColorPicker != null && cloudColorPicker.isPaletteOpen()) return cloudColorPicker;
        if (starsColorPicker != null && starsColorPicker.isPaletteOpen()) return starsColorPicker;
        if (fogColorPicker != null && fogColorPicker.isPaletteOpen()) return fogColorPicker;
        return null;
    }

    private boolean rectsOverlap(int ax, int ay, int aw, int ah, int bx, int by, int bw, int bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    private void renderColorPickerOverlay(ColorPreviewWidget picker, GuiGraphics g, int mouseX, int mouseY) {
        if (picker != null && picker.visible && picker.isPaletteOpen()) {
            picker.renderPalettePopupOverlay(g, mouseX, mouseY);
        }
    }

    private void renderSystemDropdown(GuiGraphics g, int mouseX, int mouseY) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 400.0F);
        var systems = new ArrayList<>(CelestialJsonLoader.getActiveSystems().values());
        int dropX = 4;
        int dropY = 24;
        int dropW = 150;
        int itemH = 18;
        int totalH = (systems.size() + 1) * itemH + 4;

        g.fill(dropX, dropY, dropX + dropW, dropY + totalH, SystemEditorTheme.DROPDOWN_BG);
        g.fill(dropX, dropY, dropX + dropW, dropY + 1, SystemEditorTheme.DROPDOWN_BORDER_TOP);
        g.fill(dropX, dropY + totalH - 1, dropX + dropW, dropY + totalH, SystemEditorTheme.DROPDOWN_BORDER_SIDE);
        g.fill(dropX, dropY, dropX + 1, dropY + totalH, SystemEditorTheme.DROPDOWN_BORDER_SIDE);
        g.fill(dropX + dropW - 1, dropY, dropX + dropW, dropY + totalH, SystemEditorTheme.DROPDOWN_BORDER_SIDE);

        for (int i = 0; i < systems.size(); i++) {
            SolarSystemData sys = systems.get(i);
            int iy = dropY + 2 + i * itemH;
            boolean hovered = mouseX >= dropX && mouseX <= dropX + dropW && mouseY >= iy && mouseY < iy + itemH;
            boolean isCurrent = activeSystem != null && activeSystem.id.equalsIgnoreCase(sys.id);

            if (isCurrent) {
                g.fill(dropX + 2, iy, dropX + dropW - 2, iy + itemH, SystemEditorTheme.DROPDOWN_ITEM_SEL_BG);
                g.fill(dropX + 2, iy, dropX + 5, iy + itemH, SystemEditorTheme.DROPDOWN_ITEM_SEL_STRIP);
            } else if (hovered) {
                g.fill(dropX + 2, iy, dropX + dropW - 2, iy + itemH, SystemEditorTheme.DROPDOWN_ITEM_HOVER);
            }

            int textCol = isCurrent ? SystemEditorTheme.DROPDOWN_TEXT_ACTIVE : (hovered ? SystemEditorTheme.DROPDOWN_TEXT_HOVER : SystemEditorTheme.DROPDOWN_TEXT_NORMAL);
            g.drawString(this.font, "[s] " + sys.id, dropX + 8, iy + 4, textCol, SystemEditorTheme.TEXT_SHADOW);
        }

        int iy = dropY + 2 + systems.size() * itemH;
        boolean hoveredNew = mouseX >= dropX && mouseX <= dropX + dropW && mouseY >= iy && mouseY < iy + itemH;
        if (hoveredNew) {
            g.fill(dropX + 2, iy, dropX + dropW - 2, iy + itemH, SystemEditorTheme.DROPDOWN_ITEM_HOVER);
        }
        g.drawString(this.font, "+ Create New System", dropX + 8, iy + 4, hoveredNew ? SystemEditorTheme.DROPDOWN_TEXT_ACTIVE : SystemEditorTheme.DROPDOWN_TEXT_NEW, SystemEditorTheme.TEXT_SHADOW);
        g.pose().popPose();
    }

    private void closeOtherColorPickers(ColorPreviewWidget current) {
        if (starColorPicker != null && starColorPicker != current) starColorPicker.closePalette();
        if (bodyColorPicker != null && bodyColorPicker != current) bodyColorPicker.closePalette();
        if (atmosColorPicker != null && atmosColorPicker != current) atmosColorPicker.closePalette();
        if (ringColorPicker != null && ringColorPicker != current) ringColorPicker.closePalette();
        if (cloudColorPicker != null && cloudColorPicker != current) cloudColorPicker.closePalette();
        if (starsColorPicker != null && starsColorPicker != current) starsColorPicker.closePalette();
        if (fogColorPicker != null && fogColorPicker != current) fogColorPicker.closePalette();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (systemDropdownOpen) {
            if (handleSystemDropdownClick(mouseX, mouseY)) return true;
        }

        ColorPreviewWidget openPickerBefore = findOpenColorPicker();

        if (starColorPicker != null && starColorPicker.isPaletteOpen()) {
            if (starColorPicker.handlePaletteClick(mouseX, mouseY)) return true;
        }
        if (bodyColorPicker != null && bodyColorPicker.isPaletteOpen()) {
            if (bodyColorPicker.handlePaletteClick(mouseX, mouseY)) return true;
        }
        if (atmosColorPicker != null && atmosColorPicker.isPaletteOpen()) {
            if (atmosColorPicker.handlePaletteClick(mouseX, mouseY)) return true;
        }
        if (ringColorPicker != null && ringColorPicker.isPaletteOpen()) {
            if (ringColorPicker.handlePaletteClick(mouseX, mouseY)) return true;
        }
        if (cloudColorPicker != null && cloudColorPicker.isPaletteOpen()) {
            if (cloudColorPicker.handlePaletteClick(mouseX, mouseY)) return true;
        }
        if (starsColorPicker != null && starsColorPicker.isPaletteOpen()) {
            if (starsColorPicker.handlePaletteClick(mouseX, mouseY)) return true;
        }
        if (fogColorPicker != null && fogColorPicker.isPaletteOpen()) {
            if (fogColorPicker.handlePaletteClick(mouseX, mouseY)) return true;
        }

        int formY = HEADER_H + 44;
        int scissorBottom = this.height - 24;
        List<AbstractWidget> disabledForClick = new ArrayList<>();
        for (InspectorEntry entry : inspectorEntries) {
            if (entry.widget.visible && (entry.widget.getY() < formY || entry.widget.getY() + entry.height > scissorBottom)) {
                entry.widget.active = false;
                disabledForClick.add(entry.widget);
            }
        }

        boolean result = super.mouseClicked(mouseX, mouseY, button);

        for (AbstractWidget w : disabledForClick) {
            w.active = true;
        }

        if (!result && button == 0 && CameraPlanetOrbit.isActive() && isInViewport(mouseX, mouseY)) {
            setFocused(null);
            isDraggingCamera = true;
        }

        ColorPreviewWidget openPickerAfter = findOpenColorPicker();
        if (openPickerAfter != null && openPickerAfter != openPickerBefore) {
            closeOtherColorPickers(openPickerAfter);
        }

        return result;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDraggingCamera = false;
        }
        ColorPreviewWidget openPicker = findOpenColorPicker();
        if (openPicker != null) {
            openPicker.handleMouseReleased();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        ColorPreviewWidget openPicker = findOpenColorPicker();
        if (openPicker != null && openPicker.handlePaletteDrag(mouseX, mouseY)) {
            return true;
        }
        if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        if (button == 0 && CameraPlanetOrbit.isActive() && (isDraggingCamera || isInViewport(mouseX, mouseY))) {
            CameraPlanetOrbit.handleDrag(dragX, dragY);
            return true;
        }
        return false;
    }

    private boolean handleSystemDropdownClick(double mouseX, double mouseY) {
        var systems = new ArrayList<>(CelestialJsonLoader.getActiveSystems().values());
        int dropX = 4;
        int dropY = 24;
        int dropW = 150;
        int itemH = 18;

        if (mouseX >= dropX && mouseX <= dropX + dropW) {
            for (int i = 0; i < systems.size(); i++) {
                int iy = dropY + 2 + i * itemH;
                if (mouseY >= iy && mouseY < iy + itemH) {
                    activeSystem = systems.get(i);
                    selectedNode = NodeType.SYSTEM;
                    activeTab    = Tab.SYSTEM;
                    selectedBody = null;
                    notifyChanged();
                    systemDropdownOpen = false;
                    showStatus("Selected: " + activeSystem.id, false);
                    buildLayout();
                    return true;
                }
            }
            int iy = dropY + 2 + systems.size() * itemH;
            if (mouseY >= iy && mouseY < iy + itemH) {
                systemDropdownOpen = false;
                addNewSystem();
                return true;
            }
        }

        systemDropdownOpen = false;
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == 72 || keyCode == 258) && !isAnyTextFieldFocused()) {
            toggleHideUI();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean isAnyTextFieldFocused() {
        return (searchEditBox != null && searchEditBox.isFocused()) ||
                (sysIdEditBox != null && sysIdEditBox.isFocused()) ||
                (sysDimEditBox != null && sysDimEditBox.isFocused()) ||
                (starIdEditBox != null && starIdEditBox.isFocused()) ||
                (starColorEditBox != null && starColorEditBox.isFocused()) ||
                (bodyIdEditBox != null && bodyIdEditBox.isFocused()) ||
                (bodyParentEditBox != null && bodyParentEditBox.isFocused()) ||
                (bodyDimEditBox != null && bodyDimEditBox.isFocused()) ||
                (dayTextureEditBox != null && dayTextureEditBox.isFocused()) ||
                (nightTextureEditBox != null && nightTextureEditBox.isFocused()) ||
                (bodyColorEditBox != null && bodyColorEditBox.isFocused()) ||
                (epochUtcEditBox != null && epochUtcEditBox.isFocused()) ||
                (atmosColorEditBox != null && atmosColorEditBox.isFocused()) ||
                (ringTextureEditBox != null && ringTextureEditBox.isFocused()) ||
                (rockTextureEditBox != null && rockTextureEditBox.isFocused()) ||
                (ringColorEditBox != null && ringColorEditBox.isFocused()) ||
                (cloudTextureEditBox != null && cloudTextureEditBox.isFocused()) ||
                (cloudColorEditBox != null && cloudColorEditBox.isFocused()) ||
                (skyTextureEditBox != null && skyTextureEditBox.isFocused()) ||
                (starsColorEditBox != null && starsColorEditBox.isFocused()) ||
                (fogColorEditBox != null && fogColorEditBox.isFocused());
    }

    private void toggleHideUI() {
        this.hideUI = !this.hideUI;
        buildLayout();
    }

    private void switchNextSystem() {
        systemDropdownOpen = !systemDropdownOpen;
    }

    private void addNewSystem() {
        String newId = DataConfig.System.NEW_SYSTEM_ID_PREFIX + (CelestialJsonLoader.getActiveSystems().size() + 1);
        SolarSystemData sys = new SolarSystemData(newId, SolarSystemData.DEFAULT_SPACE_DIMENSION);
        sys.star = new SunInstance.Config(DataConfig.Star.ID_DEF, DataConfig.Star.RADIUS.defF(), DataConfig.Star.COLOR_HEX_DEF);
        PlanetInstance.Config body = new PlanetInstance.Config(DataConfig.Body.NEW_BODY_ID, DataConfig.Body.TYPE_DEF, DataConfig.Body.PARENT_ID_DEF, DataConfig.Body.NEW_SYSTEM_BODY_RADIUS, DataConfig.Body.TEXTURE_DEF, DataConfig.Body.COLOR_HEX_DEF);
        body.orbit.radius = DataConfig.Orbit.NEW_SYSTEM_RADIUS;
        sys.bodies.add(body);
        activeSystem = sys;
        selectedNode = NodeType.SYSTEM;
        activeTab    = Tab.SYSTEM;
        selectedBody = null;
        notifyChanged();
        showStatus("Created: " + newId, false);
        buildLayout();
    }

    private void promptRemoveCurrentSystem() {
        if (activeSystem != null) {
            Minecraft.getInstance().setScreen(new ConfirmationPopup(this, activeSystem.id, this::removeCurrentSystem));
        }
    }

    private void promptRemoveSelectedBody() {
        if (selectedBody != null) {
            Minecraft.getInstance().setScreen(new ConfirmationPopup(this, selectedBody.id, this::removeSelectedBody));
        }
    }

    private void promptResetBody() {
        if (selectedBody != null) {
            Minecraft.getInstance().setScreen(new ConfirmationPopup(this, selectedBody.id, this::resetSelectedBodyToDefaults));
        }
    }

    private void removeCurrentSystem() {
        if (activeSystem == null) return;
        String removedId = activeSystem.id;
        CelestialJsonLoader.removeSystemInMemory(activeSystem.id);
        var systems = CelestialJsonLoader.getActiveSystems();
        activeSystem = systems.isEmpty() ? null : systems.values().iterator().next();
        selectedNode = NodeType.SYSTEM;
        activeTab    = Tab.SYSTEM;
        selectedBody = null;
        showStatus("Removed: " + removedId, false);
        buildLayout();
    }

    private void exportCurrentSystem() {
        if (activeSystem == null) return;
        try {
            File exported = CelestialJsonLoader.exportActiveSystem(activeSystem.id);
            showStatus("Exported: " + exported.getName(), false);
        } catch (Exception e) {
            showStatus("Export failed: " + e.getMessage(), true);
        }
    }

    private void addBody(String type) {
        if (activeSystem == null) return;

        if (activeSystem.bodies.size() >= DataConfig.System.MAX_BODIES_LIMIT) {
            showStatus("Body limit reached (" + DataConfig.System.MAX_BODIES_LIMIT + ")", true);
            return;
        }

        String parentId;
        double orbitRadius;
        double orbitPeriodDays;

        if ("moon".equalsIgnoreCase(type)) {
            if (selectedNode != NodeType.BODY || selectedBody == null) {
                showStatus("Select a planet first", true);
                return;
            }
            if (selectedBody.isMoon()) {
                showStatus("Moons cannot have moons", true);
                return;
            }
            if (!selectedBody.isPlanet()) {
                showStatus("Moons must orbit planets", true);
                return;
            }

            parentId = selectedBody.id;
            long moonCount = activeSystem.getMoonsOf(parentId).size();
            orbitRadius = DataConfig.Orbit.MOON_RADIUS + (moonCount * DataConfig.Orbit.MOON_STEP_RADIUS);
            orbitPeriodDays = DataConfig.Orbit.MOON_PERIOD_DAYS + (moonCount * DataConfig.Orbit.MOON_STEP_PERIOD_DAYS);
        } else {
            parentId = (activeSystem.star != null && activeSystem.star.id != null && !activeSystem.star.id.isBlank())
                    ? activeSystem.star.id : DataConfig.Body.PARENT_ID_DEF;
            orbitRadius = (activeSystem.bodies.size() + 1) * DataConfig.Orbit.BODY_STEP_RADIUS;
            orbitPeriodDays = (activeSystem.bodies.size() + 1) * DataConfig.Orbit.BODY_STEP_PERIOD_DAYS;
        }

        String baseId = type + "_";
        int counter = activeSystem.bodies.size() + 1;
        String newId = baseId + counter;
        while (activeSystem.findBody(newId) != null || (activeSystem.star != null && newId.equalsIgnoreCase(activeSystem.star.id))) {
            counter++;
            newId = baseId + counter;
        }

        float radius = "moon".equalsIgnoreCase(type) ? DataConfig.Body.MOON_RADIUS : ("blackhole".equalsIgnoreCase(type) ? DataConfig.Body.BLACKHOLE_RADIUS : DataConfig.Body.PLANET_RADIUS);
        PlanetInstance.Config newBody = new PlanetInstance.Config(newId, type, parentId, radius, DataConfig.Body.TEXTURE_DEF, DataConfig.Body.COLOR_HEX_DEF);

        if (newBody.isBlackHole()) {
            newBody.atmosphere.enabled = false;
            newBody.ring.enabled = false;
        }

        newBody.orbit.radius = OrbitCollisionUtil.resolveSafeOrbitRadius(activeSystem, newBody, orbitRadius, 0.0D);
        newBody.orbit.periodDays = orbitPeriodDays;
        activeSystem.bodies.add(newBody);
        selectedBody = newBody;
        selectedNode = NodeType.BODY;
        activeTab = Tab.GENERAL;
        notifyChanged();
        showStatus("Added: " + newId, false);
        buildLayout();
    }

    private void duplicateSelectedBody() {
        if (activeSystem == null || selectedBody == null) return;

        if (activeSystem.bodies.size() >= DataConfig.System.MAX_BODIES_LIMIT) {
            showStatus("Body limit reached (" + DataConfig.System.MAX_BODIES_LIMIT + ")", true);
            return;
        }

        PlanetInstance.Config copy = selectedBody.copy();

        String baseCopyId = selectedBody.id + DataConfig.Body.COPY_SUFFIX;
        String uniqueId = baseCopyId;
        int copyIndex = 1;
        while (activeSystem.findBody(uniqueId) != null || (activeSystem.star != null && uniqueId.equalsIgnoreCase(activeSystem.star.id))) {
            uniqueId = baseCopyId + "_" + copyIndex;
            copyIndex++;
        }
        copy.id = uniqueId;

        if (copy.isMoon()) {
            copy.orbit.radius += DataConfig.Orbit.MOON_STEP_RADIUS;
            copy.orbit.periodDays += DataConfig.Orbit.MOON_STEP_PERIOD_DAYS;
        } else {
            copy.orbit.radius += DataConfig.Body.COPY_ORBIT_OFFSET;
        }
        copy.orbit.radius = OrbitCollisionUtil.resolveSafeOrbitRadius(activeSystem, copy, copy.orbit.radius, selectedBody.orbit.radius);

        activeSystem.bodies.add(copy);
        selectedBody = copy;
        selectedNode = NodeType.BODY;
        notifyChanged();
        showStatus("Duplicated: " + copy.id, false);
        buildLayout();
    }

    private void removeSelectedBody() {
        if (activeSystem == null || selectedBody == null) return;
        String removedId = selectedBody.id;

        List<PlanetInstance.Config> childMoons = activeSystem.getMoonsOf(removedId);
        int childCount = childMoons.size();

        activeSystem.bodies.removeIf(b -> b.id.equalsIgnoreCase(removedId) || removedId.equalsIgnoreCase(b.parentId));

        selectedBody = activeSystem.bodies.isEmpty() ? null : activeSystem.bodies.get(0);
        selectedNode = selectedBody != null ? NodeType.BODY : NodeType.SYSTEM;
        if (selectedNode == NodeType.SYSTEM) activeTab = Tab.SYSTEM;
        notifyChanged();

        String statusMsg = childCount > 0
                ? "Deleted: " + removedId + " (+" + childCount + ")"
                : "Deleted: " + removedId;
        showStatus(statusMsg, false);
        buildLayout();
    }

    private void copySelectedBodyConfig() {
        if (selectedBody != null) {
            copiedBodyConfig = selectedBody.copy();
            showStatus("Copied: " + selectedBody.id, false);
        }
    }

    private void pasteBodyConfig() {
        if (selectedBody != null && copiedBodyConfig != null && activeSystem != null) {
            String targetId = selectedBody.id;
            String originalType = selectedBody.type;
            String originalParent = selectedBody.parentId;

            int targetIndex = -1;
            for (int i = 0; i < activeSystem.bodies.size(); i++) {
                if (targetId.equalsIgnoreCase(activeSystem.bodies.get(i).id)) {
                    targetIndex = i;
                    break;
                }
            }

            if (targetIndex < 0) {
                showStatus("Target not found", true);
                return;
            }

            PlanetInstance.Config pasted = copiedBodyConfig.copy();
            pasted.id = targetId;

            if ("moon".equalsIgnoreCase(originalType)) {
                pasted.type = "moon";
                pasted.parentId = originalParent;
            } else {
                List<PlanetInstance.Config> children = activeSystem.getMoonsOf(targetId);
                if (!children.isEmpty() && "moon".equalsIgnoreCase(pasted.type)) {
                    pasted.type = originalType;
                    pasted.parentId = originalParent;
                } else if (!"moon".equalsIgnoreCase(pasted.type)) {
                    pasted.parentId = originalParent;
                }
            }

            activeSystem.bodies.set(targetIndex, pasted);
            selectedBody = pasted;
            notifyChanged();
            showStatus("Pasted: " + targetId, false);
            buildLayout();
        }
    }

    private void resetSelectedBodyToDefaults() {
        if (selectedBody == null) return;
        selectedBody.orbit      = new PlanetInstance.Orbit();
        selectedBody.atmosphere = new PlanetInstance.Atmosphere();
        selectedBody.ring       = new PlanetInstance.Ring();
        selectedBody.sky        = new PlanetInstance.Sky();
        selectedBody.fog        = new PlanetInstance.SurfaceInstance.SurfaceFog();
        if (selectedBody.isBlackHole()) {
            selectedBody.atmosphere.enabled = false;
            selectedBody.ring.enabled = false;
        }
        selectedBody.radius     = DataConfig.Body.RESET_RADIUS;
        selectedBody.oxygen     = DataConfig.Body.OXYGEN_DEF;
        selectedBody.temperature = DataConfig.Body.TEMPERATURE.defF();
        selectedBody.yaw = selectedBody.pitch = selectedBody.roll = DataConfig.Body.ROT_YAW.defF();
        notifyChanged();
        showStatus("Reset: " + selectedBody.id, false);
        buildLayout();
    }

    private void notifyChanged() {
        if (activeSystem != null) {
            CelestialJsonLoader.setActiveSelectedSystemId(activeSystem.id);
            CelestialJsonLoader.updateSystemInMemory(activeSystem, true);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        CelestialJsonLoader.resetToCurrentDimension();
        super.onClose();
    }
}
