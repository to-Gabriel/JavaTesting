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
public class TetrisUpdateGameTest {

    private Tetris tetris;

    @Mock
    private BoardPanel board;

    @Mock
    private Clock logicTimer;

    private Method updateGameMethod;

    @BeforeEach
    void setUp() throws Exception {
        // package-private allows us to make tetris objects in tests
        tetris = new Tetris();

        // Inject mocks into private fields
        setPrivateField(tetris, "board", board);
        setPrivateField(tetris, "logicTimer", logicTimer);

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
