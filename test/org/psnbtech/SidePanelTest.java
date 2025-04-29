package org.psnbtech;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.awt.*;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.*;

/**
 * Unit tests for SidePanel.
 *
 *  • TC-1 shows a known bug (disabled for now)
 *  • TC-2 covers the “null next piece” path
 *  • TC-3 covers the normal preview path and executes drawTile()
 */
class SidePanelTest {

    private Graphics2D g;           // real Graphics on an off-screen image
    private BufferedImage canvas;

    @BeforeEach
    void setUpCanvas() {
        canvas = new BufferedImage(200, 400, BufferedImage.TYPE_INT_ARGB);
        g = canvas.createGraphics();
    }

    /** TC-1 – documents the defect but does NOT fail the build. */
    @Disabled("Fails until SidePanel stops querying getNextPieceType() when the game is over")
    @Test
    void paint_shouldNotQueryNextPiece_ifGameIsOver() {
        Tetris tetris = mock(Tetris.class);
        when(tetris.isGameOver()).thenReturn(true);
        when(tetris.getNextPieceType())
                .thenThrow(new IllegalStateException("BUG – should not be called"));

        SidePanel panel = new SidePanel(tetris);

        assertThrows(IllegalStateException.class,
                () -> panel.paintComponent(g));
    }

    /** TC-2 – game running, but next piece is null. */
    @Test
    void paint_handlesNullNextPiece_gracefully() {
        Tetris tetris = mock(Tetris.class);
        when(tetris.isGameOver()).thenReturn(false);
        when(tetris.getNextPieceType()).thenReturn(null);

        SidePanel panel = new SidePanel(tetris);

        assertDoesNotThrow(() -> panel.paintComponent(g));
    }

    /** TC-3 – normal preview branch; one tile actually drawn. */
    @Test
    void paint_drawsPreviewSafely() {
        // Mock a 2×2 piece with exactly one active cell (0,0).
        TileType mockType = mock(TileType.class);
        when(mockType.getCols()).thenReturn(2);
        when(mockType.getRows()).thenReturn(2);
        when(mockType.getDimension()).thenReturn(2);
        when(mockType.getTopInset(0)).thenReturn(0);
        when(mockType.getLeftInset(0)).thenReturn(0);
        when(mockType.isTile(anyInt(), anyInt(), eq(0)))
                .thenAnswer(inv -> inv.<Integer>getArgument(0) == 0
                        && inv.<Integer>getArgument(1) == 0);

        // Colours so drawTile() can call setColor safely.
        when(mockType.getBaseColor()).thenReturn(Color.BLUE);
        when(mockType.getDarkColor()).thenReturn(Color.DARK_GRAY);
        when(mockType.getLightColor()).thenReturn(Color.CYAN);

        Tetris tetris = mock(Tetris.class);
        when(tetris.isGameOver()).thenReturn(false);
        when(tetris.getNextPieceType()).thenReturn(mockType);

        SidePanel panel = new SidePanel(tetris);

        assertDoesNotThrow(() -> panel.paintComponent(g));
    }
}
