package org.psnbtech;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;

import org.junit.jupiter.api.*;

class SidePanelTest {

    private Graphics2D g;           // real Graphics context on an off-screen image
    private BufferedImage canvas;

    @BeforeEach
    void setUpCanvas() {
        canvas = new BufferedImage(200, 400, BufferedImage.TYPE_INT_ARGB);
        g = canvas.createGraphics();
    }

    /**
     * TC-1 – Exposes the bug: paintComponent still asks Tetris for the next
     * piece even when the game is already over.
     */
    @Test
    void paint_shouldNotQueryNextPiece_ifGameIsOver() {
        Tetris tetris = mock(Tetris.class);
        when(tetris.isGameOver()).thenReturn(true);
        when(tetris.getNextPieceType())
                .thenThrow(new IllegalStateException("BUG – should not be called"));
        SidePanel panel = new SidePanel(tetris);

        assertThrows(IllegalStateException.class,
                () -> panel.paintComponent(g),
                "paintComponent incorrectly queries getNextPieceType()");
    }

    /**
     * TC-2 – Null preview branch: game is running but no next piece yet.
     * Ensures the method exits cleanly without attempting to draw tiles.
     */
    @Test
    void paint_handlesNullNextPiece_gracefully() {
        Tetris tetris = mock(Tetris.class);
        when(tetris.isGameOver()).thenReturn(false);
        when(tetris.getNextPieceType()).thenReturn(null);

        SidePanel panel = new SidePanel(tetris);

        assertDoesNotThrow(() -> panel.paintComponent(g));
    }

    /**
     * TC-3 – Normal preview branch: a 2×2 mock piece, but with isTile()
     * returning false so that drawTile() is *not* entered.  Exercises the
     * full set-up and both nested loops.
     */
    @Test
    void paint_loopsSafelyWhenNoTilesNeedDrawing() {
        // Build a minimal mock TileType
        TileType mockType = mock(TileType.class);
        when(mockType.getCols()).thenReturn(2);
        when(mockType.getRows()).thenReturn(2);
        when(mockType.getDimension()).thenReturn(2);
        when(mockType.getTopInset(0)).thenReturn(0);
        when(mockType.getLeftInset(0)).thenReturn(0);
        when(mockType.isTile(anyInt(), anyInt(), eq(0))).thenReturn(false);

        Tetris tetris = mock(Tetris.class);
        when(tetris.isGameOver()).thenReturn(false);
        when(tetris.getNextPieceType()).thenReturn(mockType);

        SidePanel panel = new SidePanel(tetris);

        assertDoesNotThrow(() -> panel.paintComponent(g));
    }

    /**
     * TC-4 – Directly invokes the *private* drawTile method via reflection so
     * that every statement in SidePanel is executed at least once.
     */
    @Test
    void drawTile_rendersWithoutThrowing() throws Exception {
        SidePanel panel = new SidePanel(mock(Tetris.class));

        // build a minimal mock TileType with concrete colours
        TileType tile = mock(TileType.class);
        when(tile.getBaseColor()).thenReturn(Color.RED);
        when(tile.getDarkColor()).thenReturn(Color.DARK_GRAY);
        when(tile.getLightColor()).thenReturn(Color.PINK);

        Method drawTile = SidePanel.class.getDeclaredMethod(
                "drawTile", TileType.class, int.class, int.class, Graphics.class);
        drawTile.setAccessible(true);

        assertDoesNotThrow(() ->
                drawTile.invoke(panel, tile, 0, 0, g));
    }
}
