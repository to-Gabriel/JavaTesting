package org.psnbtech;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;

@ExtendWith(MockitoExtension.class)
public class TetrisTest {

    private Tetris tetris;

    @Mock
    private BoardPanel board;

    @Mock
    private Clock logicTimer;

    @Mock
    private SidePanel side;


    private Method updateGameMethod;

    @BeforeEach
    void setUp() throws Exception {
        // package-private allows us to make tetris objects in tests
        tetris = new Tetris();

        // Inject mocks into private fields
        setPrivateField(tetris, "board", board);
        setPrivateField(tetris, "logicTimer", logicTimer);
        setPrivateField(tetris, "side", side);


        // Initialize other necessary fields
        setPrivateField(tetris, "random", new Random());
        setPrivateField(tetris, "currentType", TileType.TypeI);
        setPrivateField(tetris, "currentCol", 5);
        setPrivateField(tetris, "currentRow", 10);
        setPrivateField(tetris, "currentRotation", 0);
        setPrivateField(tetris, "gameSpeed", 1.0f);
        setPrivateField(tetris, "dropCooldown", 0);
        setPrivateField(tetris, "level", 1);
        setPrivateField(tetris, "score", 0);
        setPrivateField(tetris, "nextType", TileType.TypeO);

        // Access private updateGame method
        updateGameMethod = Tetris.class.getDeclaredMethod("updateGame");
        updateGameMethod.setAccessible(true);
    }

    /*----------updateGame() Tests--------------*/
    @Test
    void testPieceMovingDown() throws Exception {
        // Test Path 1 for PPC (refer to cfg): [1, 2, 6]
        when(board.isValidAndEmpty(any(TileType.class), anyInt(), anyInt(), anyInt())).thenReturn(true);

        int initialRow = (int) getPrivateField(tetris, "currentRow");

        updateGameMethod.invoke(tetris);

        int newRow = (int) getPrivateField(tetris, "currentRow");
        assertEquals(initialRow + 1, newRow);

        verify(board, never()).addPiece(any(TileType.class), anyInt(), anyInt(), anyInt());
    }

    @Test
    void testPieceLandingWithLineClearance() throws Exception {
        // Test Path 2 for PPC (refer to cfg): [1, 3, 4, 5, 6]
        when(board.isValidAndEmpty(any(TileType.class), anyInt(), anyInt(), anyInt())).thenReturn(false);
        when(board.checkLines()).thenReturn(1);

        int initialScore = (int) getPrivateField(tetris, "score");
        float initialSpeed = (float) getPrivateField(tetris, "gameSpeed");

        updateGameMethod.invoke(tetris);

        verify(board).addPiece(any(TileType.class), anyInt(), anyInt(), anyInt());
        verify(board).checkLines();

        int newScore = (int) getPrivateField(tetris, "score");
        assertEquals(initialScore + 100, newScore);

        float newSpeed = (float) getPrivateField(tetris, "gameSpeed");
        assertTrue(newSpeed > initialSpeed);

        verify(logicTimer).setCyclesPerSecond(anyFloat());
        verify(logicTimer).reset();

        int cooldown = (int) getPrivateField(tetris, "dropCooldown");
        assertEquals(25, cooldown);

        int level = (int) getPrivateField(tetris, "level");
        assertEquals((int) (newSpeed * 1.70f), level);
    }

    @Test
    void testPieceLandingWithoutLineClearance() throws Exception {
        // Test Path 3 for PPC (refer to cfg): [1, 3, 5, 6]
        when(board.isValidAndEmpty(any(TileType.class), anyInt(), anyInt(), anyInt())).thenReturn(false);
        when(board.checkLines()).thenReturn(0);

        int initialScore = (int) getPrivateField(tetris, "score");
        float initialSpeed = (float) getPrivateField(tetris, "gameSpeed");

        updateGameMethod.invoke(tetris);

        verify(board).addPiece(any(TileType.class), anyInt(), anyInt(), anyInt());
        verify(board).checkLines();

        int newScore = (int) getPrivateField(tetris, "score");
        assertEquals(initialScore, newScore);

        float newSpeed = (float) getPrivateField(tetris, "gameSpeed");
        assertTrue(newSpeed > initialSpeed);

        verify(logicTimer).setCyclesPerSecond(anyFloat());
        verify(logicTimer).reset();

        int cooldown = (int) getPrivateField(tetris, "dropCooldown");
        assertEquals(25, cooldown);

        int level = (int) getPrivateField(tetris, "level");
        assertEquals((int) (newSpeed * 1.70f), level);
    }
    /*-----------------------------------------------*/

    /*----------renderGame() Tests--------------*/
    @Test
    void testRenderGameWhenPaused() throws Exception {
        /**
         * Test that renderGame() repaints correctly when the game is paused.
         */
        setPrivateField(tetris, "isPaused", true);

        tetris.renderGame(); // package private so it can be used in test

        verify(board).repaint();
        verify(side).repaint();
    }

    @Test
    void testRenderGameWhenNewGame() throws Exception {
        /**
         * Test that renderGame() repaints correctly when the game is starting (new game).
         */
        setPrivateField(tetris, "isNewGame", true);
        setPrivateField(tetris, "isPaused", false);

        tetris.renderGame();

        verify(board).repaint();
        verify(side).repaint();
    }

    @Test
    void testRenderGameWhenGameOver() throws Exception {
        /**
         * Test that renderGame() repaints correctly when the game is over.
         */
        setPrivateField(tetris, "isGameOver", true);
        setPrivateField(tetris, "isPaused", false);

        tetris.renderGame();

        verify(board).repaint();
        verify(side).repaint();
    }

    @Test
    void testRenderGameDuringActivePlay() throws Exception {
        /**
         * Test that renderGame() repaints correctly when the game is actively being played.
         */
        setPrivateField(tetris, "isPaused", false);
        setPrivateField(tetris, "isNewGame", false);
        setPrivateField(tetris, "isGameOver", false);

        tetris.renderGame();

        verify(board).repaint();
        verify(side).repaint();
    }
    /*-----------------------------------------------*/

    /*---------- rotatePiece() Tests--------------*/
    @Test
    void testRotatePieceValidPosition() throws Exception {
        /**
         * Tests normal rotation where the new position is already valid.
         * This exercises the simplest path through rotatePiece where no shifting is needed.
         * Important for basic functionality and ensures that piece updates happen when possible.
         */
        when(board.isValidAndEmpty(any(TileType.class), eq(5), eq(10), eq(1))).thenReturn(true);

        setPrivateField(tetris,"currentRotation", 0);
        setPrivateField(tetris,"currentCol", 5);
        setPrivateField(tetris,"currentRow", 10);
        setPrivateField(tetris,"currentType", TileType.TypeI);

        Method rotatePiece = Tetris.class.getDeclaredMethod("rotatePiece", int.class);
        rotatePiece.setAccessible(true);
        rotatePiece.invoke(tetris, 1);

        assertEquals(1, getPrivateField(tetris, "currentRotation"));
        assertEquals(5, getPrivateField(tetris, "currentCol"));
        assertEquals(10, getPrivateField(tetris, "currentRow"));
    }

    @Test
    void testRotatePieceLeftShift() throws Exception {
        /**
         * Tests left wall kick logic where the piece is partially outside the left boundary.
         * After correcting the source code to <=, adjustment moves the piece to column 2.
         */
        TileType type = TileType.TypeI;

        // Only stub the real call that happens
        when(board.isValidAndEmpty(eq(type), eq(2), eq(10), eq(1))).thenReturn(true);

        setPrivateField(tetris,"currentRotation", 0);
        setPrivateField(tetris,"currentCol", -2);
        setPrivateField(tetris,"currentRow", 10);
        setPrivateField(tetris,"currentType", type);

        Method rotatePiece = Tetris.class.getDeclaredMethod("rotatePiece", int.class);
        rotatePiece.setAccessible(true);
        rotatePiece.invoke(tetris, 1);

        assertEquals(1, getPrivateField(tetris, "currentRotation"));
        assertEquals(2, getPrivateField(tetris, "currentCol")); // Adjusted to column 2
    }


    @Test
    void testRotatePieceRightShift() throws Exception {
        /**
         * Tests right wall kick logic where the piece would extend past the right board edge.
         * This activates the right inset shift logic.
         * Important to validate wall kick functionality near right edge.
         */
        TileType type = TileType.TypeI;
        int startCol = BoardPanel.COL_COUNT - 1;
        int expectedCol = BoardPanel.COL_COUNT - type.getDimension() + type.getRightInset(1) - 1;

        when(board.isValidAndEmpty(eq(type), eq(expectedCol), eq(10), eq(1))).thenReturn(true);

        setPrivateField(tetris,"currentRotation", 0);
        setPrivateField(tetris,"currentCol", startCol);
        setPrivateField(tetris,"currentRow", 10);
        setPrivateField(tetris,"currentType", type);

        Method rotatePiece = Tetris.class.getDeclaredMethod("rotatePiece", int.class);
        rotatePiece.setAccessible(true);
        rotatePiece.invoke(tetris, 1);

        assertEquals(1, getPrivateField(tetris, "currentRotation"));
        assertEquals(expectedCol, getPrivateField(tetris, "currentCol"));
    }

    @Test
    void testRotatePieceInvalidAfterAdjustment() throws Exception {
        /**
         * Tests the case where rotation results in an invalid position even after adjustments.
         * The piece should not rotate in this case.
         * Important for ensuring game stability and rule adherence.
         */
        TileType type = TileType.TypeI;
        when(board.isValidAndEmpty(any(TileType.class), anyInt(), anyInt(), anyInt())).thenReturn(false);

        setPrivateField(tetris,"currentRotation", 0);
        setPrivateField(tetris,"currentCol", 5);
        setPrivateField(tetris,"currentRow", 10);
        setPrivateField(tetris,"currentType", type);

        Method rotatePiece = Tetris.class.getDeclaredMethod("rotatePiece", int.class);
        rotatePiece.setAccessible(true);
        rotatePiece.invoke(tetris, 1);

        assertEquals(0, getPrivateField(tetris, "currentRotation")); // Rotation should remain unchanged
        assertEquals(5, getPrivateField(tetris, "currentCol"));
        assertEquals(10, getPrivateField(tetris, "currentRow"));
    }
    /*-----------------------------------------------*/

    /*---------- spawnPiece() Tests--------------*/
    @Test
    void testSpawnPieceValidPosition() throws Exception {
        /**
         * Tests spawnPiece() when the new piece spawns in a valid position.
         * Covers the branch where the board allows the spawn, so the game continues normally.
         */
        TileType type = TileType.TypeO;

        setPrivateField(tetris, "nextType", type);
        when(board.isValidAndEmpty(eq(type), eq(type.getSpawnColumn()), eq(type.getSpawnRow()), eq(0)))
                .thenReturn(true);

        Method spawnPiece = Tetris.class.getDeclaredMethod("spawnPiece");
        spawnPiece.setAccessible(true);
        spawnPiece.invoke(tetris);

        // Confirm piece was spawned correctly
        assertEquals(type, getPrivateField(tetris, "currentType"));
        assertEquals(0, getPrivateField(tetris, "currentRotation"));
        assertEquals(type.getSpawnColumn(), getPrivateField(tetris, "currentCol"));
        assertEquals(type.getSpawnRow(), getPrivateField(tetris, "currentRow"));

        // Game should not be over
        assertFalse((boolean) getPrivateField(tetris, "isGameOver"));
        verify(logicTimer, never()).setPaused(true);
    }

    @Test
    void testSpawnPieceInvalidPosition() throws Exception {
        /**
         * Tests spawnPiece() when the new piece cannot be placed on the board.
         * Covers the branch where isValidAndEmpty is false, triggering a game over and pausing the game.
         */
        TileType type = TileType.TypeT;

        setPrivateField(tetris, "nextType", type);
        when(board.isValidAndEmpty(eq(type), eq(type.getSpawnColumn()), eq(type.getSpawnRow()), eq(0)))
                .thenReturn(false);

        Method spawnPiece = Tetris.class.getDeclaredMethod("spawnPiece");
        spawnPiece.setAccessible(true);
        spawnPiece.invoke(tetris);

        // Game should now be over
        assertTrue((boolean) getPrivateField(tetris, "isGameOver"));
        verify(logicTimer).setPaused(true);
    }
    /*-----------------------------------------------*/

    /*---------- resetGame() Tests--------------*/
    @Test
    void testResetGameWithValidSpawn() throws Exception {
        /**
         * Ensures resetGame sets the correct fields and spawns a piece when valid.
         * Covers the full reset path including a successful spawn.
         */
        int chosenIndex = 3; // TypeL
        TileType next = TileType.values()[chosenIndex];

        Random fakeRandom = mock(Random.class);
        when(fakeRandom.nextInt(anyInt())).thenReturn(chosenIndex);

        setPrivateField(tetris, "random", fakeRandom);
        when(board.isValidAndEmpty(eq(next), eq(next.getSpawnColumn()), eq(next.getSpawnRow()), eq(0)))
                .thenReturn(true);

        Method resetGame = Tetris.class.getDeclaredMethod("resetGame");
        resetGame.setAccessible(true);
        resetGame.invoke(tetris);

        assertEquals(1, getPrivateField(tetris, "level"));
        assertEquals(0, getPrivateField(tetris, "score"));
        assertEquals(1.0f, getPrivateField(tetris, "gameSpeed"));
        assertFalse((boolean) getPrivateField(tetris, "isNewGame"));
        assertFalse((boolean) getPrivateField(tetris, "isGameOver"));
        assertEquals(next, getPrivateField(tetris, "nextType"));

        verify(board).clear();
        verify(logicTimer).reset();
        verify(logicTimer).setCyclesPerSecond(1.0f);
    }

    @Test
    void testResetGameWithInvalidSpawn() throws Exception {
        /**
         * Tests resetGame where the spawned piece is invalid.
         * This ensures isGameOver is set and logicTimer is paused.
         */
        TileType next = TileType.TypeT;

        setPrivateField(tetris, "random", new Random(0));
        setPrivateField(tetris, "nextType", next);

        when(board.isValidAndEmpty(eq(next), eq(next.getSpawnColumn()), eq(next.getSpawnRow()), eq(0)))
                .thenReturn(false); // Trigger game over

        Method resetGame = Tetris.class.getDeclaredMethod("resetGame");
        resetGame.setAccessible(true);
        resetGame.invoke(tetris);

        assertTrue((boolean) getPrivateField(tetris, "isGameOver"));
        verify(logicTimer).setPaused(true); // Game over branch is hit
    }
    /*-----------------------------------------------*/

    /*----------Getters Tests--------------*/
    @Test
    void testGetterMethods() throws Exception {
        setPrivateField(tetris,"isPaused", true);
        setPrivateField(tetris,"isNewGame", true);
        setPrivateField(tetris,"isGameOver", true);
        setPrivateField(tetris,"score", 150);
        setPrivateField(tetris,"level", 3);
        setPrivateField(tetris,"currentType", TileType.TypeL);
        setPrivateField(tetris,"nextType", TileType.TypeS);
        setPrivateField(tetris,"currentCol", 4);
        setPrivateField(tetris,"currentRow", 12);
        setPrivateField(tetris,"currentRotation", 2);

        assertTrue(tetris.isPaused(), "isPaused should return true");
        assertTrue(tetris.isNewGame(), "isNewGame should return true");
        assertTrue(tetris.isGameOver(), "isGameOver should return true");

        assertEquals(150, tetris.getScore(), "Score should match");
        assertEquals(3, tetris.getLevel(), "Level should match");

        assertEquals(TileType.TypeL, tetris.getPieceType(), "Piece type should be TypeL");
        assertEquals(TileType.TypeS, tetris.getNextPieceType(), "Next piece should be TypeS");

        assertEquals(4, tetris.getPieceCol(), "Piece column should be 4");
        assertEquals(12, tetris.getPieceRow(), "Piece row should be 12");
        assertEquals(2, tetris.getPieceRotation(), "Piece rotation should be 2");
    }
    /*-----------------------------------------------*/

    // === Private Helpers ===

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        if (field != null) {
            field.setAccessible(true);
            field.set(target, value);
        }
    }

    private Object getPrivateField(Object target, String fieldName) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        if (field != null) {
            field.setAccessible(true);
            return field.get(target);
        }
        return null;
    }

    private Field findField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) {
                return findField(clazz.getSuperclass(), fieldName);
            }
            return null;
        }
    }
}
