package org.psnbtech;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
    private Method rotatePieceMethod; // For calling rotatePiece
    private Method resetGameMethod;   // For calling resetGame

    private KeyAdapter keyAdapter;    // To store the KeyAdapter instance

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
        setPrivateField(tetris, "isPaused", false);
        setPrivateField(tetris, "isGameOver", false);
        setPrivateField(tetris, "isNewGame", false); // Assume game is ongoing unless specified

        // Access private methods needed for testing key adapter logic effects
        updateGameMethod = Tetris.class.getDeclaredMethod("updateGame");
        updateGameMethod.setAccessible(true);

        rotatePieceMethod = Tetris.class.getDeclaredMethod("rotatePiece", int.class);
        rotatePieceMethod.setAccessible(true);

        resetGameMethod = Tetris.class.getDeclaredMethod("resetGame");
        resetGameMethod.setAccessible(true);

        // Get the KeyAdapter instance using reflection
        // Tetris adds it directly, so it should be the first KeyListener
        if (tetris.getKeyListeners().length > 0 && tetris.getKeyListeners()[0] instanceof KeyAdapter) {
            keyAdapter = (KeyAdapter) tetris.getKeyListeners()[0];
        } else {
            fail("Could not find KeyAdapter in Tetris instance");
        }

        // Reset interactions on mocks before each test
        reset(board, logicTimer, side);
    }

    // Helper to create a dummy KeyEvent
    private KeyEvent createKeyEvent(int keyCode, char keyChar) {
        // Using the Tetris frame itself as the source component
        return new KeyEvent(tetris, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, keyCode, keyChar);
    }
    private KeyEvent createKeyReleaseEvent(int keyCode, char keyChar) {
        // Using the Tetris frame itself as the source component
        return new KeyEvent(tetris, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, keyCode, keyChar);
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
    void testRenderGame() throws Exception {
        // The game state doesn't affect the behavior of renderGame()
        tetris.renderGame();

        // Verify that both panels are repainted regardless of game state
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

    @Test
    void testRotatePieceTopShift() throws Exception {
        /**
         * Tests vertical adjustment logic when the piece would extend past the top board edge.
         * This activates the top inset shift logic.
         * Important to validate wall kick functionality near top edge.
         */
        TileType type = TileType.TypeI;
        int topInset = type.getTopInset(1);
        int startRow = -4; // Place the piece beyond the top border
        int expectedRow = -topInset; // Should adjust to this position

        // Only stub the call that will be made after adjustment
        when(board.isValidAndEmpty(eq(type), eq(5), eq(expectedRow), eq(1))).thenReturn(true);

        setPrivateField(tetris, "currentRotation", 0);
        setPrivateField(tetris, "currentCol", 5);
        setPrivateField(tetris, "currentRow", startRow);
        setPrivateField(tetris, "currentType", type);

        Method rotatePiece = Tetris.class.getDeclaredMethod("rotatePiece", int.class);
        rotatePiece.setAccessible(true);
        rotatePiece.invoke(tetris, 1);

        // Verify that position was adjusted properly
        assertEquals(1, getPrivateField(tetris, "currentRotation"));
        assertEquals(5, getPrivateField(tetris, "currentCol")); // Column unchanged
        assertEquals(expectedRow, getPrivateField(tetris, "currentRow")); // Row shifted down

        // Verify the board was checked with the adjusted position
        verify(board).isValidAndEmpty(eq(type), eq(5), eq(expectedRow), eq(1));
    }

    @Test
    void testRotatePieceBottomShift() throws Exception {
        /**
         * Tests vertical adjustment logic when the piece would extend past the bottom board edge.
         * This activates the bottom inset shift logic.
         * Important to validate wall kick functionality near bottom edge.
         */
        TileType type = TileType.TypeI;
        int dimension = type.getDimension();
        int bottomInset = type.getBottomInset(1);

        // Calculate a starting row that puts the piece beyond the bottom boundary after rotation
        int startRow = BoardPanel.ROW_COUNT - dimension + bottomInset + 2;
        int expectedRow = BoardPanel.ROW_COUNT - dimension + bottomInset - 1;

        // Only stub the call that will be made after adjustment
        when(board.isValidAndEmpty(eq(type), eq(5), eq(expectedRow), eq(1))).thenReturn(true);

        setPrivateField(tetris, "currentRotation", 0);
        setPrivateField(tetris, "currentCol", 5);
        setPrivateField(tetris, "currentRow", startRow);
        setPrivateField(tetris, "currentType", type);

        Method rotatePiece = Tetris.class.getDeclaredMethod("rotatePiece", int.class);
        rotatePiece.setAccessible(true);
        rotatePiece.invoke(tetris, 1);

        // Verify that position was adjusted properly
        assertEquals(1, getPrivateField(tetris, "currentRotation"));
        assertEquals(5, getPrivateField(tetris, "currentCol")); // Column unchanged
        assertEquals(expectedRow, getPrivateField(tetris, "currentRow")); // Row shifted up

        // Verify the board was checked with the adjusted position
        verify(board).isValidAndEmpty(eq(type), eq(5), eq(expectedRow), eq(1));
    }

    @Test
    void testRotatePieceTopAndLeftShift() throws Exception {
        /**
         * Tests combined vertical and horizontal adjustment logic.
         * This tests when the piece needs adjustment in both X and Y coordinates.
         * Important to validate that both adjustments happen correctly together.
         */
        TileType type = TileType.TypeI;

        // Place the piece beyond both left and top edges
        int startCol = -2;
        int startRow = -3;

        // Instead of trying to pre-calculate exact adjustment values,
        // we'll use lenient stubbing to allow any valid position
        when(board.isValidAndEmpty(any(TileType.class), anyInt(), anyInt(), anyInt())).thenReturn(true);

        setPrivateField(tetris, "currentRotation", 0);
        setPrivateField(tetris, "currentCol", startCol);
        setPrivateField(tetris, "currentRow", startRow);
        setPrivateField(tetris, "currentType", type);

        Method rotatePiece = Tetris.class.getDeclaredMethod("rotatePiece", int.class);
        rotatePiece.setAccessible(true);
        rotatePiece.invoke(tetris, 1);

        // After rotation, verify positions changed from starting values
        int newCol = (int) getPrivateField(tetris, "currentCol");
        int newRow = (int) getPrivateField(tetris, "currentRow");
        assertEquals(1, getPrivateField(tetris, "currentRotation"));

        // The piece should have moved to valid coordinates
        assertTrue(newCol > startCol, "Column should have been adjusted rightward");
        assertTrue(newRow > startRow, "Row should have been adjusted downward");

        // Capture the actual arguments passed to isValidAndEmpty
        ArgumentCaptor<Integer> colCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> rowCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(board).isValidAndEmpty(eq(type), colCaptor.capture(), rowCaptor.capture(), eq(1));

        // Assert that the captured values match the new position
        assertEquals(newCol, colCaptor.getValue().intValue());
        assertEquals(newRow, rowCaptor.getValue().intValue());
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

    /*---------- KeyAdapter Tests --------------*/

    @Test
    void testKeyPressed_S_Drop_StartsFastDrop() throws Exception {
        // Arrange
        setPrivateField(tetris, "isPaused", false);
        setPrivateField(tetris, "dropCooldown", 0);
        KeyEvent keyEvent = createKeyEvent(KeyEvent.VK_S, 's');

        // Act
        keyAdapter.keyPressed(keyEvent);

        // Assert
        verify(logicTimer).setCyclesPerSecond(25.0f);
    }

    @Test
    void testKeyPressed_S_Drop_DoesNothingWhenPaused() throws Exception {
        // Arrange
        setPrivateField(tetris, "isPaused", true);
        setPrivateField(tetris, "dropCooldown", 0);
        KeyEvent keyEvent = createKeyEvent(KeyEvent.VK_S, 's');

        // Act
        keyAdapter.keyPressed(keyEvent);

        // Assert
        verify(logicTimer, never()).setCyclesPerSecond(anyFloat());
    }

    @Test
    void testKeyPressed_S_Drop_DoesNothingDuringCooldown() throws Exception {
        // Arrange
        setPrivateField(tetris, "isPaused", false);
        setPrivateField(tetris, "dropCooldown", 5); // Cooldown active
        KeyEvent keyEvent = createKeyEvent(KeyEvent.VK_S, 's');

        // Act
        keyAdapter.keyPressed(keyEvent);

        // Assert
        verify(logicTimer, never()).setCyclesPerSecond(anyFloat());
    }

    @Test
    void testKeyReleased_S_Drop_RestoresGameSpeed() throws Exception {
        // Arrange
        setPrivateField(tetris, "isPaused", false);
        float currentGameSpeed = 1.5f; // Example speed
        setPrivateField(tetris, "gameSpeed", currentGameSpeed);
        KeyEvent keyEvent = createKeyReleaseEvent(KeyEvent.VK_S, 's');


        // Act
        keyAdapter.keyReleased(keyEvent);

        // Assert
        verify(logicTimer).setCyclesPerSecond(currentGameSpeed);
        verify(logicTimer).reset();
    }

    @Test
    void testKeyPressed_A_MoveLeft_Valid() throws Exception {
        // Arrange
        setPrivateField(tetris, "isPaused", false);
        int startCol = 5;
        setPrivateField(tetris, "currentCol", startCol);
        when(board.isValidAndEmpty(any(TileType.class), eq(startCol - 1), anyInt(), anyInt())).thenReturn(true);
        KeyEvent keyEvent = createKeyEvent(KeyEvent.VK_A, 'a');

        // Act
        keyAdapter.keyPressed(keyEvent);

        // Assert
        assertEquals(startCol - 1, getPrivateField(tetris, "currentCol"));
    }

    @Test
    void testKeyPressed_A_MoveLeft_Invalid() throws Exception {
        // Arrange
        setPrivateField(tetris, "isPaused", false);
        int startCol = 5;
        setPrivateField(tetris, "currentCol", startCol);
        when(board.isValidAndEmpty(any(TileType.class), eq(startCol - 1), anyInt(), anyInt())).thenReturn(false);
        KeyEvent keyEvent = createKeyEvent(KeyEvent.VK_A, 'a');

        // Act
        keyAdapter.keyPressed(keyEvent);

        // Assert
        assertEquals(startCol, getPrivateField(tetris, "currentCol")); // Position unchanged
    }

    @Test
    void testKeyPressed_A_MoveLeft_Paused() throws Exception {
        // Arrange
        setPrivateField(tetris, "isPaused", true);
        int startCol = 5;
        setPrivateField(tetris, "currentCol", startCol);
        // No need to mock board, it shouldn't be called
        KeyEvent keyEvent = createKeyEvent(KeyEvent.VK_A, 'a');

        // Act
        keyAdapter.keyPressed(keyEvent);

        // Assert
        assertEquals(startCol, getPrivateField(tetris, "currentCol")); // Position unchanged
        verify(board, never()).isValidAndEmpty(any(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void testKeyPressed_D_MoveRight_Valid() throws Exception {
        // Arrange
        setPrivateField(tetris, "isPaused", false);
        int startCol = 5;
        setPrivateField(tetris, "currentCol", startCol);
        when(board.isValidAndEmpty(any(TileType.class), eq(startCol + 1), anyInt(), anyInt())).thenReturn(true);
        KeyEvent keyEvent = createKeyEvent(KeyEvent.VK_D, 'd');

        // Act
        keyAdapter.keyPressed(keyEvent);

        // Assert
        assertEquals(startCol + 1, getPrivateField(tetris, "currentCol"));
    }

    @Test
    void testKeyPressed_D_MoveRight_Invalid() throws Exception {
        // Arrange
        setPrivateField(tetris, "isPaused", false);
        int startCol = 5;
        setPrivateField(tetris, "currentCol", startCol);
        when(board.isValidAndEmpty(any(TileType.class), eq(startCol + 1), anyInt(), anyInt())).thenReturn(false);
        KeyEvent keyEvent = createKeyEvent(KeyEvent.VK_D, 'd');

        // Act
        keyAdapter.keyPressed(keyEvent);

        // Assert
        assertEquals(startCol, getPrivateField(tetris, "currentCol")); // Position unchanged
    }

    @Test
    void testKeyPressed_D_MoveRight_Paused() throws Exception {
        // Arrange
        setPrivateField(tetris, "isPaused", true);
        int startCol = 5;
        setPrivateField(tetris, "currentCol", startCol);
        KeyEvent keyEvent = createKeyEvent(KeyEvent.VK_D, 'd');

        // Act
        keyAdapter.keyPressed(keyEvent);

        // Assert
        assertEquals(startCol, getPrivateField(tetris, "currentCol")); // Position unchanged
        verify(board, never()).isValidAndEmpty(any(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void testKeyPressed_Q_RotateAntiClockwise_CallsRotatePiece() throws Exception {
        // Arrange
        setPrivateField(tetris, "isPaused", false);
        int currentRotation = 1;
        int expectedNewRotation = 0; // 1 - 1 = 0
        setPrivateField(tetris, "currentRotation", currentRotation);
        // Assume rotation is valid for simplicity here, rotatePiece tests cover validity
        when(board.isValidAndEmpty(any(TileType.class), anyInt(), anyInt(), eq(expectedNewRotation))).thenReturn(true);
        KeyEvent keyEvent = createKeyEvent(KeyEvent.VK_Q, 'q');

        // Act
        keyAdapter.keyPressed(keyEvent);

        // Assert
        // We verify the *effect* of the call to rotatePiece
        assertEquals(expectedNewRotation, getPrivateField(tetris, "currentRotation"));
        // Could potentially use a Spy to verify rotatePiece was called, but checking state effect is often sufficient.
    }

    @Test
    void testKeyPressed_Q_RotateAntiClockwise_HandlesWrapAround() throws Exception {
        // Arrange
        setPrivateField(tetris, "isPaused", false);
        int currentRotation = 0;
        int expectedNewRotation = 3; // Wraps around: (0 == 0) ? 3 : 0 - 1
        setPrivateField(tetris, "currentRotation", currentRotation);
        when(board.isValidAndEmpty(any(TileType.class), anyInt(), anyInt(), eq(expectedNewRotation))).thenReturn(true);
        KeyEvent keyEvent = createKeyEvent(KeyEvent.VK_Q, 'q');

        // Act
        keyAdapter.keyPressed(keyEvent);

        // Assert
        assertEquals(expectedNewRotation, getPrivateField(tetris, "currentRotation"));
    }

    @Test
    void testKeyPressed_Q_RotateAntiClockwise_Paused() throws Exception {
        // Arrange
        setPrivateField(tetris, "isPaused", true);
        int currentRotation = 1;
        setPrivateField(tetris, "currentRotation", currentRotation);
        KeyEvent keyEvent = createKeyEvent(KeyEvent.VK_Q, 'q');

        // Act
        keyAdapter.keyPressed(keyEvent);

        // Assert
        // Rotation should not change because game is paused
        assertEquals(currentRotation, getPrivateField(tetris, "currentRotation"));
        // Ensure board check (part of rotatePiece) was not called
        verify(board, never()).isValidAndEmpty(any(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void testKeyPressed_E_RotateClockwise_CallsRotatePiece() throws Exception {
        // Arrange
        setPrivateField(tetris, "isPaused", false);
        int currentRotation = 1;
        int expectedNewRotation = 2; // 1 + 1 = 2
        setPrivateField(tetris, "currentRotation", currentRotation);
        when(board.isValidAndEmpty(any(TileType.class), anyInt(), anyInt(), eq(expectedNewRotation))).thenReturn(true);
        KeyEvent keyEvent = createKeyEvent(KeyEvent.VK_E, 'e');

        // Act
        keyAdapter.keyPressed(keyEvent);

        // Assert
        assertEquals(expectedNewRotation, getPrivateField(tetris, "currentRotation"));
    }

    @Test
    void testKeyPressed_E_RotateClockwise_HandlesWrapAround() throws Exception {
        // Arrange
        setPrivateField(tetris, "isPaused", false);
        int currentRotation = 3;
        int expectedNewRotation = 0; // Wraps around: (3 == 3) ? 0 : 3 + 1
        setPrivateField(tetris, "currentRotation", currentRotation);
        when(board.isValidAndEmpty(any(TileType.class), anyInt(), anyInt(), eq(expectedNewRotation))).thenReturn(true);
        KeyEvent keyEvent = createKeyEvent(KeyEvent.VK_E, 'e');

        // Act
        keyAdapter.keyPressed(keyEvent);

        // Assert
        assertEquals(expectedNewRotation, getPrivateField(tetris, "currentRotation"));
    }

    @Test
    void testKeyPressed_E_RotateClockwise_Paused() throws Exception {
        // Arrange
        setPrivateField(tetris, "isPaused", true);
        int currentRotation = 1;
        setPrivateField(tetris, "currentRotation", currentRotation);
        KeyEvent keyEvent = createKeyEvent(KeyEvent.VK_E, 'e');

        // Act
        keyAdapter.keyPressed(keyEvent);

        // Assert
        assertEquals(currentRotation, getPrivateField(tetris, "currentRotation"));
        verify(board, never()).isValidAndEmpty(any(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void testKeyPressed_P_Pause_TogglesPauseOn() throws Exception {
        // Arrange
        setPrivateField(tetris, "isPaused", false);
        setPrivateField(tetris, "isGameOver", false);
        setPrivateField(tetris, "isNewGame", false);
        KeyEvent keyEvent = createKeyEvent(KeyEvent.VK_P, 'p');

        // Act
        keyAdapter.keyPressed(keyEvent);

        // Assert
        assertTrue((boolean) getPrivateField(tetris, "isPaused"));
        verify(logicTimer).setPaused(true);
    }

    @Test
    void testKeyPressed_P_Pause_TogglesPauseOff() throws Exception {
        // Arrange
        setPrivateField(tetris, "isPaused", true);
        setPrivateField(tetris, "isGameOver", false);
        setPrivateField(tetris, "isNewGame", false);
        KeyEvent keyEvent = createKeyEvent(KeyEvent.VK_P, 'p');

        // Act
        keyAdapter.keyPressed(keyEvent);

        // Assert
        assertFalse((boolean) getPrivateField(tetris, "isPaused"));
        verify(logicTimer).setPaused(false);
    }

    @Test
    void testKeyPressed_P_Pause_DoesNothingWhenGameOver() throws Exception {
        // Arrange
        setPrivateField(tetris, "isPaused", false);
        setPrivateField(tetris, "isGameOver", true); // Game is over
        setPrivateField(tetris, "isNewGame", false);
        KeyEvent keyEvent = createKeyEvent(KeyEvent.VK_P, 'p');

        // Act
        keyAdapter.keyPressed(keyEvent);

        // Assert
        assertFalse((boolean) getPrivateField(tetris, "isPaused")); // State unchanged
        verify(logicTimer, never()).setPaused(anyBoolean()); // Timer pause state not changed
    }

    @Test
    void testKeyPressed_P_Pause_DoesNothingWhenNewGame() throws Exception {
        // Arrange
        setPrivateField(tetris, "isPaused", false);
        setPrivateField(tetris, "isGameOver", false);
        setPrivateField(tetris, "isNewGame", true); // New game state
        KeyEvent keyEvent = createKeyEvent(KeyEvent.VK_P, 'p');

        // Act
        keyAdapter.keyPressed(keyEvent);

        // Assert
        assertFalse((boolean) getPrivateField(tetris, "isPaused")); // State unchanged
        verify(logicTimer, never()).setPaused(anyBoolean()); // Timer pause state not changed
    }

    @Test
    void testKeyPressed_Enter_StartsNewGameWhenGameOver() throws Exception {
        // Arrange
        setPrivateField(tetris, "isGameOver", true);
        setPrivateField(tetris, "isNewGame", false);
        // Mock necessary parts for resetGame to run without error
        Random fakeRandom = mock(Random.class);
        when(fakeRandom.nextInt(anyInt())).thenReturn(0); // Select first tile type
        setPrivateField(tetris, "random", fakeRandom);
        when(board.isValidAndEmpty(any(), anyInt(), anyInt(), anyInt())).thenReturn(true); // Allow spawn
        KeyEvent keyEvent = createKeyEvent(KeyEvent.VK_ENTER, '\n');

        // Act
        keyAdapter.keyPressed(keyEvent);

        // Assert
        // Verify resetGame effects indirectly:
        assertFalse((boolean) getPrivateField(tetris, "isGameOver")); // Should no longer be game over
        assertFalse((boolean) getPrivateField(tetris, "isNewGame"));  // Should no longer be new game
        assertEquals(1, getPrivateField(tetris, "level"));       // Level reset
        assertEquals(0, getPrivateField(tetris, "score"));       // Score reset
        verify(board).clear(); // Board cleared is a key part of resetGame
    }

    @Test
    void testKeyPressed_Enter_StartsNewGameWhenNewGame() throws Exception {
        // Arrange
        setPrivateField(tetris, "isGameOver", false);
        setPrivateField(tetris, "isNewGame", true); // Is new game state
        // Mock necessary parts for resetGame
        Random fakeRandom = mock(Random.class);
        when(fakeRandom.nextInt(anyInt())).thenReturn(0);
        setPrivateField(tetris, "random", fakeRandom);
        when(board.isValidAndEmpty(any(), anyInt(), anyInt(), anyInt())).thenReturn(true);
        KeyEvent keyEvent = createKeyEvent(KeyEvent.VK_ENTER, '\n');

        // Act
        keyAdapter.keyPressed(keyEvent);

        // Assert
        assertFalse((boolean) getPrivateField(tetris, "isGameOver"));
        assertFalse((boolean) getPrivateField(tetris, "isNewGame"));
        assertEquals(1, getPrivateField(tetris, "level"));
        assertEquals(0, getPrivateField(tetris, "score"));
        verify(board).clear();
    }

    @Test
    void testKeyPressed_Enter_DoesNothingDuringGame() throws Exception {
        // Arrange
        setPrivateField(tetris, "isGameOver", false);
        setPrivateField(tetris, "isNewGame", false); // Game is active
        KeyEvent keyEvent = createKeyEvent(KeyEvent.VK_ENTER, '\n');
        int initialLevel = (int)getPrivateField(tetris, "level"); // Store pre-state

        // Act
        keyAdapter.keyPressed(keyEvent);

        // Assert
        // Verify no state changes associated with resetGame occurred
        assertFalse((boolean) getPrivateField(tetris, "isGameOver"));
        assertFalse((boolean) getPrivateField(tetris, "isNewGame"));
        assertEquals(initialLevel, getPrivateField(tetris, "level")); // Level unchanged
        verify(board, never()).clear(); // resetGame was not called
    }

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
