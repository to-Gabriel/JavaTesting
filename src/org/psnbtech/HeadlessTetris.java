package org.psnbtech;

import java.awt.Component;

/**
 * A silent Tetris class helper that never shows a window but lets BoardPanel query it.
 * This is useful for testing the game as a mock.
 */
class HeadlessTetris extends Tetris {
    /*
     * / Disable other UI components
     */
    @Override public void setVisible(boolean b) {}
    @Override public void pack()                {}
    @Override public void setLocationRelativeTo(Component c) {}
    @Override public void addKeyListener(java.awt.event.KeyListener l) {}

    /**
     * Set the states we wish to control through testing. This will be accessed by the BoardPanel.
     */
    private boolean paused;
    private boolean newGame;
    private boolean gameOver;
    private TileType pieceType;
    private int pieceCol;
    private int pieceRow;
    private int pieceRot;

    /**
     * This sets the game state, used for testing different states of the game.
     * @param paused If the game was paused.
     * @param newGame If the game started.
     * @param gameOver If the game ended.
     */
    void setGameFlags(boolean paused, boolean newGame, boolean gameOver) {
        this.paused   = paused;
        this.newGame  = newGame;
        this.gameOver = gameOver;
    }

    /**
     * This places an individual piece on the board.
     * @param t The piece type, (i.e. O, L, ...)
     * @param col The column of the board
     * @param row The row of the board
     * @param rot The rotation state of the piece
     */
    void setPiece(TileType t, int col, int row, int rot) {
        this.pieceType = t;  this.pieceCol = col;  this.pieceRow = row;  this.pieceRot = rot;
    }

    /**
     * Getters used for getting private methods.
     */
    @Override public boolean  isPaused()        { return paused; }
    @Override public boolean  isNewGame()       { return newGame; }
    @Override public boolean  isGameOver()      { return gameOver; }
    @Override public TileType getPieceType()    { return pieceType; }
    @Override public int      getPieceCol()     { return pieceCol; }
    @Override public int      getPieceRow()     { return pieceRow; }
    @Override public int      getPieceRotation(){ return pieceRot; }
}
