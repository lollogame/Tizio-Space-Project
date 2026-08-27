package tizio.dev.tsp.core.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import tizio.dev.tsp.core.celestial.instance.elements.SolarSystemData;
import tizio.dev.tsp.core.celestial.instance.elements.planet.PlanetInstance;
import tizio.dev.tsp.core.gui.theme.SystemEditorTheme;

import java.util.*;
import java.util.function.Consumer;

public class EditorTreeWidget extends AbstractWidget {

    public static class TreeItem {
        public final String id;
        public final String label;
        public final String type;
        public final int depth;
        public final String parentId;
        public final PlanetInstance.Config bodyConfig;

        public boolean expanded = true;
        public boolean visible  = true;
        public final List<TreeItem> children = new ArrayList<>();

        public TreeItem(String id, String label, String type, int depth, String parentId, PlanetInstance.Config bodyConfig) {
            this.id = id;
            this.label = label;
            this.type = type;
            this.depth = depth;
            this.parentId = parentId;
            this.bodyConfig = bodyConfig;
        }
    }

    private final List<TreeItem> rootItems      = new ArrayList<>();
    private final List<TreeItem> visibleFlatList = new ArrayList<>();

    private String selectedId = null;
    private final Consumer<TreeItem> onSelect;

    private int scrollOffset = 0;
    private static final int ROW_H = 18;
    private String filterQuery = "";

    public EditorTreeWidget(int x, int y, int width, int height, Consumer<TreeItem> onSelect) {
        super(x, y, width, height, Component.literal("System Hierarchy Tree"));
        this.onSelect = onSelect;
    }

    public void rebuildFromSystem(SolarSystemData systemConfig, String currentSelectedId) {
        this.rootItems.clear();
        this.selectedId = currentSelectedId;
        if (systemConfig == null) {
            rebuildVisibleList();
            return;
        }

        TreeItem sysItem = new TreeItem(systemConfig.id, "[s] " + systemConfig.id, "system", 0, null, null);

        if (systemConfig.star != null) {
            String starType = systemConfig.star.isBlackHole() ? "blackhole" : "star";
            String tag = systemConfig.star.isBlackHole() ? "[BH] " : "[STAR] ";
            TreeItem starItem = new TreeItem(systemConfig.star.id, tag + systemConfig.star.id, starType, 1, systemConfig.id, null);
            sysItem.children.add(starItem);
        }

        Map<String, TreeItem> bodyItemMap = new HashMap<>();
        List<PlanetInstance.Config> unparented = new ArrayList<>();

        for (PlanetInstance.Config body : systemConfig.bodies) {
            String bType = body.type != null ? body.type.toLowerCase(Locale.ROOT) : "planet";
            String tag = switch (bType) {
                case "moon"      -> "[M] ";
                case "blackhole" -> "[BH] ";
                case "star"      -> "[STAR] ";
                default          -> "[PL] ";
            };
            TreeItem bItem = new TreeItem(body.id, tag + body.id, bType, 1, body.parentId, body);
            bodyItemMap.put(body.id, bItem);

            if (body.parentId == null || body.parentId.isBlank() || "sun".equalsIgnoreCase(body.parentId) || (systemConfig.star != null && systemConfig.star.id.equalsIgnoreCase(body.parentId))) {
                sysItem.children.add(bItem);
            } else {
                unparented.add(body);
            }
        }

        for (PlanetInstance.Config body : unparented) {
            TreeItem bItem = bodyItemMap.get(body.id);
            if (bItem == null) continue;
            TreeItem parentTreeItem = bodyItemMap.get(body.parentId);
            if (parentTreeItem != null) {
                TreeItem nestedItem = new TreeItem(body.id, bItem.label, bItem.type, parentTreeItem.depth + 1, body.parentId, body);
                parentTreeItem.children.add(nestedItem);
            } else {
                sysItem.children.add(bItem);
            }
        }

        rootItems.add(sysItem);
        rebuildVisibleList();
    }

    public void setFilterQuery(String query) {
        this.filterQuery = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
        rebuildVisibleList();
    }

    private void rebuildVisibleList() {
        visibleFlatList.clear();
        for (TreeItem root : rootItems) {
            flattenVisible(root);
        }
        clampScroll();
    }

    private void flattenVisible(TreeItem item) {
        boolean matchesFilter = filterQuery.isEmpty() || item.id.toLowerCase(Locale.ROOT).contains(filterQuery) || item.label.toLowerCase(Locale.ROOT).contains(filterQuery);
        if (matchesFilter || hasMatchingChild(item)) {
            visibleFlatList.add(item);
            if (item.expanded && !item.children.isEmpty()) {
                for (TreeItem child : item.children) {
                    flattenVisible(child);
                }
            }
        }
    }

    private boolean hasMatchingChild(TreeItem item) {
        if (filterQuery.isEmpty()) return true;
        for (TreeItem child : item.children) {
            if (child.id.toLowerCase(Locale.ROOT).contains(filterQuery) || child.label.toLowerCase(Locale.ROOT).contains(filterQuery) || hasMatchingChild(child)) {
                return true;
            }
        }
        return false;
    }

    private void clampScroll() {
        int totalH = visibleFlatList.size() * ROW_H;
        int maxScroll = Math.max(0, totalH - height);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (isHoveredOrFocused()) {
            scrollOffset = (int) Math.max(0, scrollOffset - amount * 12);
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible || !isHoveredOrFocused()) return false;

        int relY = (int) (mouseY - getY() + scrollOffset);
        int index = relY / ROW_H;

        if (index >= 0 && index < visibleFlatList.size()) {
            TreeItem clickedItem = visibleFlatList.get(index);
            int indentX = getX() + clickedItem.depth * 10 + 4;

            if (!clickedItem.children.isEmpty() && mouseX >= indentX && mouseX <= indentX + 12) {
                clickedItem.expanded = !clickedItem.expanded;
                rebuildVisibleList();
                return true;
            }

            this.selectedId = clickedItem.id;
            if (onSelect != null) {
                onSelect.accept(clickedItem);
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) return;

        // Outer container box
        g.fill(getX(), getY(), getX() + width, getY() + height, SystemEditorTheme.TREE_BG);
        g.fill(getX(), getY(), getX() + width, getY() + 1, SystemEditorTheme.TREE_BORDER);
        g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, SystemEditorTheme.TREE_BORDER);
        g.fill(getX(), getY(), getX() + 1, getY() + height, SystemEditorTheme.TREE_BORDER);
        g.fill(getX() + width - 1, getY(), getX() + width, getY() + height, SystemEditorTheme.TREE_BORDER);

        g.enableScissor(getX() + 2, getY() + 2, getX() + width - 2, getY() + height - 2);

        Font font = Minecraft.getInstance().font;
        int visibleRows = height / ROW_H + 2;
        int startIndex = scrollOffset / ROW_H;
        int endIndex = Math.min(visibleFlatList.size(), startIndex + visibleRows);

        for (int i = startIndex; i < endIndex; i++) {
            TreeItem item = visibleFlatList.get(i);
            int rowY = getY() + (i * ROW_H) - scrollOffset;

            boolean isHovered = mouseX >= getX() && mouseX <= getX() + width && mouseY >= rowY && mouseY < rowY + ROW_H;
            boolean isSelected = selectedId != null && selectedId.equalsIgnoreCase(item.id);

            if (isSelected) {
                g.fill(getX() + 2, rowY, getX() + width - 2, rowY + ROW_H, SystemEditorTheme.TREE_NODE_SEL_BG);
                //g.fill(getX() + 2, rowY, getX() + 5, rowY + ROW_H, SystemEditorTheme.TREE_NODE_SEL_STRIP);
            } else if (isHovered) {
                g.fill(getX() + 2, rowY, getX() + width - 2, rowY + ROW_H, SystemEditorTheme.TREE_NODE_HOVER_BG);
            }

            int indentX = getX() + item.depth * 10 + 6;

            if (item.depth > 0) {
                g.fill(indentX - 6, rowY + ROW_H / 2, indentX - 2, rowY + ROW_H / 2 + 1, SystemEditorTheme.TREE_CONNECTOR_LINE);
            }

            if (!item.children.isEmpty()) {
                String arrow = item.expanded ? "v" : ">";
                g.drawString(font, arrow, indentX, rowY + 5, SystemEditorTheme.TREE_EXPAND_ARROW, SystemEditorTheme.TEXT_SHADOW);
            }

            int textX = indentX + (!item.children.isEmpty() ? 10 : 4);
            int textColor = switch (item.type.toLowerCase(Locale.ROOT)) {
                case "system"    -> SystemEditorTheme.TREE_TEXT_SYSTEM;
                case "star"      -> SystemEditorTheme.TREE_TEXT_STAR;
                case "planet"    -> SystemEditorTheme.TREE_TEXT_PLANET;
                case "moon"      -> SystemEditorTheme.TREE_TEXT_MOON;
                case "blackhole" -> SystemEditorTheme.TREE_TEXT_BLACKHOLE;
                default          -> SystemEditorTheme.TREE_TEXT_PLANET;
            };

            g.drawString(font, item.label, textX, rowY + 5, textColor, SystemEditorTheme.TEXT_SHADOW);
        }

        g.disableScissor();

        // Render scrollbar if list exceeds tree height
        int totalH = visibleFlatList.size() * ROW_H;
        if (totalH > height) {
            int scrollbarH = Math.max(12, (height * height) / totalH);
            int maxScroll = totalH - height;
            int scrollbarY = getY() + (scrollOffset * (height - scrollbarH)) / maxScroll;
            int sbX = getX() + width - 4;

            g.fill(sbX, getY(), sbX + 3, getY() + height, SystemEditorTheme.TREE_SCROLLBAR_TRACK);
            g.fill(sbX, scrollbarY, sbX + 3, scrollbarY + scrollbarH, SystemEditorTheme.TREE_SCROLLBAR_THUMB);
        }
    }

    @Override
    protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {}
}
