package thunder.hack.features.hud.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.gui.font.FontRenderer;
import thunder.hack.core.Managers;
import thunder.hack.setting.Setting;
import thunder.hack.utility.render.Render2DEngine;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class WaterMark extends HudElement {

    public WaterMark() {
        super("WaterMark", 100, 35);
    }

    public static final Setting<Mode> mode = new Setting<>("Mode", Mode.Nurseltan);

    private enum Mode {
        Big, Small, Classic, BaltikaClient, Rifk, Nurseltan
    }

    // ===================== NURSELTAN STYLE =====================

    private int fpsCount = 0;

    @Override
    public void onUpdate() {
        fpsCount = mc.getCurrentFps();
    }

    public void onRender2D(DrawContext context) {
        super.onRender2D(context);

        if (mode.getValue() != Mode.Nurseltan) {
            renderOtherModes(context);
            return;
        }

        if (mc.player == null) return;

        MatrixStack matrix = context.getMatrices();
        float startX = getPosX();
        float startY = getPosY();

        FontRenderer textFont = FontRenderers.sf_medium_mini;
        FontRenderer iconFont = FontRenderers.icons;
        if (textFont == null || iconFont == null) return;

        int speed = 5;
        int intex = 24;
        int themeColor = HudEditor.getColor(0);
        int darkColor = multDark(themeColor, 0.45f);

        // === ROW 1: Client name block + info block ===
        String clientName = "ThunderHack";
        float nameWidth = textFont.getStringWidth(clientName);
        float block1Width = 26.5f + nameWidth;

        drawBlock(matrix, startX, startY, block1Width, 14.5f);
        drawIconChar(matrix, iconFont, "p", startX + 4.4f, startY + 6f, speed, intex, 0, themeColor, darkColor);
        drawSeparator(matrix, startX + 17f, startY + 5.5f);
        drawFadeText(matrix, textFont, clientName, startX + 22f, startY + 5.5f, speed, intex, 0, themeColor, darkColor);

        String username = mc.getSession().getUsername();
        String fpsText = fpsCount + " Fps";
        String timeText = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        float usernameWidth = textFont.getStringWidth(username);
        float fpsWidth = textFont.getStringWidth(fpsText);
        float timeWidth = textFont.getStringWidth(timeText);
        float iconWWidth = iconFont.getStringWidth("w");
        float iconXWidth = iconFont.getStringWidth("x");
        float iconVWidth = iconFont.getStringWidth("v");

        float rightWidth = 5 + iconWWidth + 3 + usernameWidth + 3 +
                5 + iconXWidth + 3 + fpsWidth + 3 +
                5 + iconVWidth + 3 + timeWidth + 4;
        float rightX = startX + block1Width + 2;

        drawBlock(matrix, rightX, startY, rightWidth, 14.5f);

        float subX = rightX + 5;
        drawIconChar(matrix, iconFont, "w", subX, startY + 6.2f, speed, intex, 20, themeColor, darkColor);
        subX += iconWWidth + 3;
        drawFadeText(matrix, textFont, username, subX, startY + 5.7f, speed, intex, 20, themeColor, darkColor);
        subX += usernameWidth + 5;
        drawSeparator(matrix, subX - 3, startY + 5.5f);
        subX += 2;

        drawIconChar(matrix, iconFont, "x", subX, startY + 6.2f, speed, intex, 40, themeColor, darkColor);
        subX += iconXWidth + 3;
        drawFadeText(matrix, textFont, fpsText, subX, startY + 5.7f, speed, intex, 40, themeColor, darkColor);
        subX += fpsWidth + 5;
        drawSeparator(matrix, subX - 3, startY + 5.5f);
        subX += 2;

        drawIconChar(matrix, iconFont, "v", subX, startY + 6.2f, speed, intex, 60, themeColor, darkColor);
        subX += iconVWidth + 3;
        drawFadeText(matrix, textFont, timeText, subX, startY + 5.7f, speed, intex, 60, themeColor, darkColor);

        // === ROW 2: Coords, Ping, TPS, BPS ===
        float bottomY = startY + 14.5f + 2;
        float currentX = startX;

        String coords = (int) mc.player.getX() + ", " + (int) mc.player.getY() + ", " + (int) mc.player.getZ();
        float coordsWidth = textFont.getStringWidth(coords);
        float blockW = 27 + coordsWidth;
        drawBlock(matrix, currentX, bottomY, blockW, 14.5f);
        drawIconChar(matrix, iconFont, "f", currentX + 4.2f, bottomY + 6.05f, speed, intex, 80, themeColor, darkColor);
        drawSeparator(matrix, currentX + 17, bottomY + 5.4f);
        drawFadeText(matrix, textFont, coords, currentX + 22, bottomY + 5.7f, speed, intex, 80, themeColor, darkColor);
        currentX += blockW + 2;

        String pingStr = Managers.SERVER.getPing() + " Ping";
        float pingWidth = textFont.getStringWidth(pingStr);
        blockW = 27 + pingWidth;
        drawBlock(matrix, currentX, bottomY, blockW, 14.5f);
        drawIconChar(matrix, iconFont, "q", currentX + 4.2f, bottomY + 6.05f, speed, intex, 100, themeColor, darkColor);
        drawSeparator(matrix, currentX + 17, bottomY + 5.4f);
        drawFadeText(matrix, textFont, pingStr, currentX + 22, bottomY + 5.7f, speed, intex, 100, themeColor, darkColor);
        currentX += blockW + 2;

        String tpsStr = String.format("%.1f", Managers.SERVER.getTPS()) + " Ticks";
        float tpsWidth = textFont.getStringWidth(tpsStr);
        blockW = 27 + tpsWidth;
        drawBlock(matrix, currentX, bottomY, blockW, 14.5f);
        drawIconChar(matrix, iconFont, "t", currentX + 4.2f, bottomY + 6.05f, speed, intex, 120, themeColor, darkColor);
        drawSeparator(matrix, currentX + 17, bottomY + 5.4f);
        drawFadeText(matrix, textFont, tpsStr, currentX + 22, bottomY + 5.7f, speed, intex, 120, themeColor, darkColor);
        currentX += blockW + 2;

        double dx = mc.player.getX() - mc.player.prevX;
        double dz = mc.player.getZ() - mc.player.prevZ;
        double bps = Math.sqrt(dx * dx + dz * dz) * 20.0;
        String bpsStr = String.format("%.1f", bps) + " Bps";
        float bpsWidth = textFont.getStringWidth(bpsStr);
        blockW = 27 + bpsWidth;
        drawBlock(matrix, currentX, bottomY, blockW, 14.5f);
        drawIconChar(matrix, iconFont, "g", currentX + 4.2f, bottomY + 6.05f, speed, intex, 140, themeColor, darkColor);
        drawSeparator(matrix, currentX + 17, bottomY + 5.4f);
        drawFadeText(matrix, textFont, bpsStr, currentX + 22, bottomY + 5.7f, speed, intex, 140, themeColor, darkColor);
        currentX += blockW;

        setBounds(getPosX(), getPosY(), (int)(currentX - startX), 31);
    }

    // === Helpers ===

    private void drawBlock(MatrixStack matrix, float x, float y, float w, float h) {
        Render2DEngine.drawRoundedBlur(matrix, x, y, w, h, 3.5f,
                new Color(0, 0, 0, 75));
        Render2DEngine.drawRound(matrix, x, y, w, h, 3.5f,
                new Color(18, 19, 20, 75));
    }

    private void drawSeparator(MatrixStack matrix, float x, float y) {
        Render2DEngine.drawRect(matrix, x, y, 0.5f, 7f,
                new Color(255, 255, 255, 55));
    }

    private void drawFadeText(MatrixStack matrix, FontRenderer font, String text,
                               float x, float y, int speed, int intex, int offset,
                               int color1, int color2) {
        for (int i = 0; i < text.length(); i++) {
            int c = fade(speed, offset + i * intex, color1, color2);
            String ch = String.valueOf(text.charAt(i));
            font.drawString(matrix, ch, x, y, c);
            x += font.getStringWidth(ch);
        }
    }

    private void drawIconChar(MatrixStack matrix, FontRenderer font, String ch,
                               float x, float y, int speed, int intex, int offset,
                               int color1, int color2) {
        int c = fade(speed, offset, color1, color2);
        font.drawString(matrix, ch, x, y, c);
    }

    private int fade(int speed, int offset, int color1, int color2) {
        long time = System.currentTimeMillis();
        float t = (float)((Math.sin((time * speed / 1000.0 + offset / 100.0) * Math.PI) + 1.0) / 2.0);
        int r1 = (color1 >> 16) & 0xFF, g1 = (color1 >> 8) & 0xFF, b1 = color1 & 0xFF;
        int r2 = (color2 >> 16) & 0xFF, g2 = (color2 >> 8) & 0xFF, b2 = color2 & 0xFF;
        int r = (int)(r1 + (r2 - r1) * t);
        int g = (int)(g1 + (g2 - g1) * t);
        int b = (int)(b1 + (b2 - b1) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private int multDark(int color, float factor) {
        int r = (int)(((color >> 16) & 0xFF) * factor);
        int g = (int)(((color >> 8) & 0xFF) * factor);
        int b = (int)((color & 0xFF) * factor);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    // === Other modes (giữ nguyên từ bản gốc) ===
    private void renderOtherModes(DrawContext context) {
        // paste nội dung Big/Small/Classic/BaltikaClient/Rifk từ bản gốc vào đây
    }
}
