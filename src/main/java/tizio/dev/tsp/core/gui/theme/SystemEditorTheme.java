package tizio.dev.tsp.core.gui.theme;

import tizio.dev.tsp.core.utils.Color;

public class SystemEditorTheme {

    public static final boolean TEXT_SHADOW = true;

    public static final int BG_PANEL   = Color.iRGBA( 26,  26,  26, 230);
    public static final int BG_ELEMENT = Color.iRGBA( 34,  34,  34, 225);
    public static final int BG_HOVER   = Color.iRGBA( 44,  44,  44, 240);
    public static final int BG_SEL     = Color.iRGBA( 54,  54,  54, 230);

    public static final int BORDER_SUB = Color.iRGBA( 42,  42,  42, 255);
    public static final int BORDER_MID = Color.iRGBA( 70,  70,  70, 255);

    public static final int ACCENT     = Color.iRGBA(208, 120,  58, 255);
    public static final int ACCENT_DIM = Color.iRGBA(150,  86,  42, 255);

    public static final int TEXT_HI    = Color.iRGBA(232, 232, 232, 255);
    public static final int TEXT_MID   = Color.iRGBA(164, 164, 164, 255);
    public static final int TEXT_DIM   = Color.iRGBA(112, 112, 112, 255);

    public static final int RED        = Color.iRGBA(196,  64,  56, 255);
    public static final int GREEN      = Color.iRGBA( 95, 139, 110, 255);
    public static final int AMBER      = Color.iRGBA(191, 138,  62, 255);
    public static final int STAR_GOLD  = Color.iRGBA(211, 162,  75, 255);
    public static final int MOON_GREY  = Color.iRGBA(155, 149, 144, 255);
    public static final int VOID_PURP  = Color.iRGBA( 94,  56, 120, 255);

    public static int HEADER_BG             = Color.iRGBA( 14,  14,  14, 240);
    public static int HEADER_BORDER_BOT     = BORDER_MID;
    public static int PREVIEW_BANNER_BG     = Color.iRGBA( 14,  14,  14, 190);
    public static int PREVIEW_BANNER_TEXT   = TEXT_MID;

    public static int PANEL_BG             = BG_PANEL;
    public static int PANEL_BORDER         = BORDER_SUB;
    public static int PANEL_TITLE_TEXT     = TEXT_MID;

    public static int BTN_BG_NORMAL        = BG_ELEMENT;
    public static int BTN_BG_HOVER         = BG_HOVER;
    public static int BTN_BORDER_NORMAL    = BORDER_SUB;
    public static int BTN_BORDER_HOVER     = BORDER_MID;
    public static int BTN_TEXT_NORMAL      = TEXT_MID;
    public static int BTN_TEXT_HOVER       = TEXT_HI;
    public static int TAB_ACTIVE_ACCENT    = ACCENT;
    public static int TAB_ACTIVE_BG        = BG_SEL;

    public static int BTN_DELETE_BORDER_NORMAL = Color.iRGBA(150,  55,  48, 255);

    public static int SLIDER_CONTAINER_BG       = BG_ELEMENT;
    public static int SLIDER_CONTAINER_HOVER_BG = BG_HOVER;
    public static int SLIDER_BORDER_NORMAL       = BORDER_SUB;
    public static int SLIDER_BORDER_HOVER        = BORDER_MID;
    public static int SLIDER_LABEL_TEXT          = TEXT_DIM;
    public static int SLIDER_VALUE_TEXT          = TEXT_HI;
    public static int SLIDER_TRACK_BG            = Color.iRGBA( 16,  16,  16, 255);
    public static int SLIDER_TRACK_FILL          = ACCENT_DIM;
    public static int SLIDER_HANDLE_NORMAL       = ACCENT;
    public static int SLIDER_HANDLE_HOVER        = TEXT_HI;

    public static int TREE_BG              = Color.iRGBA( 18,  18,  18, 220);
    public static int TREE_BORDER          = BORDER_SUB;
    public static int TREE_CONNECTOR_LINE  = Color.iRGBA( 56,  56,  56, 255);
    public static int TREE_NODE_HOVER_BG   = Color.iRGBA( 44,  44,  44, 120);
    public static int TREE_NODE_SEL_BG     = BG_SEL;
    public static int TREE_EXPAND_ARROW    = TEXT_DIM;

    public static int TREE_TEXT_SYSTEM     = TEXT_HI;
    public static int TREE_TEXT_STAR       = STAR_GOLD;
    public static int TREE_TEXT_PLANET     = TEXT_MID;
    public static int TREE_TEXT_MOON       = MOON_GREY;
    public static int TREE_TEXT_BLACKHOLE  = VOID_PURP;

    public static int TREE_SCROLLBAR_TRACK = Color.iRGBA(  0,   0,   0,  60);
    public static int TREE_SCROLLBAR_THUMB = BORDER_MID;

    public static int COLOR_PREVIEW_BORDER    = BORDER_MID;
    public static int PALETTE_POPUP_BG        = Color.iRGBA( 18,  18,  18, 250);
    public static int PALETTE_POPUP_BORDER    = BORDER_MID;
    public static int PALETTE_SWATCH_BORDER   = BORDER_SUB;

    public static int DROPDOWN_BG             = Color.iRGBA( 18,  18,  18, 250);
    public static int DROPDOWN_BORDER_TOP     = BORDER_MID;
    public static int DROPDOWN_BORDER_SIDE    = BORDER_SUB;
    public static int DROPDOWN_ITEM_HOVER     = Color.iRGBA( 44,  44,  44, 120);
    public static int DROPDOWN_ITEM_SEL_BG    = BG_SEL;
    public static int DROPDOWN_ITEM_SEL_STRIP = ACCENT;
    public static int DROPDOWN_TEXT_NORMAL    = TEXT_MID;
    public static int DROPDOWN_TEXT_HOVER     = TEXT_HI;
    public static int DROPDOWN_TEXT_ACTIVE    = ACCENT;
    public static int DROPDOWN_TEXT_NEW       = AMBER;

    public static int DELETE_OVERLAY_BG    = Color.iRGBA(  0,   0,   0, 180);
    public static int DELETE_POPUP_BG      = Color.iRGBA( 18,  18,  18, 250);
    public static int DELETE_POPUP_TOP_BAR = RED;
    public static int DELETE_POPUP_BORDER  = BTN_DELETE_BORDER_NORMAL;
    public static int DELETE_TITLE_TEXT    = RED;
    public static int DELETE_BODY_TEXT     = TEXT_MID;
    public static int DELETE_TARGET_TEXT   = TEXT_HI;

    public static int INSPECTOR_LABEL_TEXT      = TEXT_DIM;
    public static int INSPECTOR_SCROLLBAR_BG    = Color.iRGBA(  0,   0,   0,  60);
    public static int INSPECTOR_SCROLLBAR_THUMB = BORDER_MID;

    public static int TOAST_SUCCESS_TEXT = GREEN;
    public static int TOAST_ERROR_TEXT   = RED;
}
