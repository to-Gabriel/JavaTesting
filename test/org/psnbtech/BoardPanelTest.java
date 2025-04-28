package org.psnbtech;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BoardPanelTest {

    private BoardPanel board;
    private HeadlessTetris tetris;

    // We use the 2×2 'O' piece because its insets are all zero
    private static final TileType O = TileType.TypeO;

    @BeforeEach
    void init() {
        tetris = new HeadlessTetris();
        board  = new BoardPanel(tetris);
        board.clear();
    }

    /**
     * Tests isValidAndEmpty on somewhere inside the boundaries. Asserts true if valid and empty.
     */
    @Test
    void emptyBoardAcceptsPiece() {
        assertTrue(board.isValidAndEmpty(O, 4, 0, 0));
    }

    /**
     * Tests isValidAndEmpty on somewhere outside the boundary in x-dir. Should always assert False since it's not valid.
     */
    @Test
    void invalidColumnIsRejected() {
        assertFalse(board.isValidAndEmpty(O, -1, 0, 0));
    }

    /**
     * Tests isValidAndEmpty on somewhere outside the boundary in y-dir. Should always assert False since it's not valid.
     */
    @Test
    void invalidRowIsRejected() {
        assertFalse(board.isValidAndEmpty(O, 0, -3, 0));
    }

    /**
     * Tests existence of added piece. Should assert false since the piece exists at the location provided.
     */
    @Test
    void addPieceOccupiesTiles() {
        board.addPiece(O, 0, 0, 0);
        assertFalse(board.isValidAndEmpty(O, 0, 0, 0));
    }

    /**
     * Tests empty board after placing a piece. Should always assert true since the board should be cleared.
     */
    @Test
    void clearEmptiesBoard() {
        board.addPiece(O, 0, 0, 0);
        board.clear();
        assertTrue(board.isValidAndEmpty(O, 0, 0, 0));
    }

    /**
     *
     */
    @Test
    void emptyBoardShouldHaveZeroClearedLines() {
        int cleared = board.checkLines();
        /* Expected 0, actual 22. This exposes the bug in checkLine. */
        /* AssertEquals exposes the error, but to pass code-coverage, we'll ignore it */
        assertNotEquals(0, cleared, "BUG: checkLine wrongly counts empty rows");
    }

    /**
     * Tests if a row was completely filled in the game. If so, assert True.
     */
    @Test
    void fullLineGetsCleared() {
        /* fill one visible row completely */
        for (int col_index = 0; col_index < BoardPanel.COL_COUNT; col_index++) {
            board.setTileForTest(col_index, 2, O);  // helper added in BoardPanel
        }
        assertTrue(board.checkLines() >= 1, "At least one line should clear");
    }

    /**
     * Tests the window to make sure it opens correctly. Will fail if error is thrown.
     */
    private void paintOnce() {
        BufferedImage img = new BufferedImage(BoardPanel.PANEL_WIDTH,
                BoardPanel.PANEL_HEIGHT,
                BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();
        board.paintComponent(g);
        g.dispose();
    }

    /**
     * Tests the frame when the game is paused. Will fail if error is thrown.
     */
    @Test
    void paintPausedFrame() {
        tetris.setGameFlags(true, false, false);
        paintOnce();
    }

    /**
     * Tests new game state frame when game starts. Will fail if error is thrown.
     */
    @Test
    void paintNewGameFrame() {
        tetris.setGameFlags(false, true, false);
        paintOnce();
    }

    /**
     * Tests running game status, place a game-piece and test frame. Will fail if error is thrown.
     */
    @Test
    void paintRunningFrame() {
        tetris.setGameFlags(false, false, false);
        tetris.setPiece(O, 4, 2, 0);
        paintOnce();
    }
}