/*
 * DizzyingBlocks
 * 
 * A dizzying Tetris-esque game.
 * 
 * Originally created as a university class project.
 * 
 */

import java.awt.*;
import java.awt.event.*;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import com.sun.opengl.util.*;
import com.sun.opengl.util.j2d.TextRenderer;
import javax.swing.JFrame;

public class DizzyingBlocks extends JFrame implements GLEventListener, KeyListener {
    
    //Important Variables
    private final GLCanvas canvas;
    private int winW = 1024, winH = 768;
    
    //GL Shading + Transformation Variables
    private float angle_x = 0.0f;
    private float angle_y = 0.0f;
    
    //GL Context + Variables
    private GL gl;
    private final GLU glu = new GLU();
    private final GLUT glut = new GLUT();
    private FPSAnimator animator;
    
    //Game Variables
    private int[][] gameboard = new int[10][24];
    private FallingTile fallingTile;
    private FallingTile nextTile;
    private int timer = 15;
    private boolean speedUp = false;
    private boolean gameOver = false;
    private int rowComplete = -1;
    private boolean rowAnimation = false;
    public int score;
    
    //Material Colors
    private float mat_red[] = { 1f, 0f, 0f, 1f };
    private float mat_green[] = { 0.5f, 1f, 0f, 1f };
    private float mat_blue[] = { 0f, 0.5f, 1f, 1f };
    private float mat_yellow[] = { 1f, 1f, 0f, 1f };
    private float mat_cyan[] = { 0f, 1f, 1f, 1f };
    private float mat_magenta[] = { 1f, 0f, 1f, 1f };
    private float mat_orange[] = { 1f, 0.5f, 0f, 1f };
    private float mat_gray[] = { 0.1f, 0.1f, 0.1f, 1f };
    private float mat_white[] = { 1f, 1f, 1f, 1f };
    
    //Fancy camera rotations
    private boolean camerax = true;
    private boolean cameray = true;
    
    //Text Rendering
    TextRenderer largeFont;
    TextRenderer smallFont;
    
    //Main
    public static void main(String args[]) {
        new DizzyingBlocks();
    }
    
    //Constructor
    public DizzyingBlocks() {
        super("DizzyingBlocks");
        canvas = new GLCanvas();
        canvas.addGLEventListener(this);
        canvas.addKeyListener(this);
        animator = new FPSAnimator(canvas, 60);
        getContentPane().add(canvas);
        setSize(winW, winH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
        animator.start();
        canvas.requestFocus();
        startGame();
    }
    
    //GL Display Function
    public void display(GLAutoDrawable drawable) {
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        
        //Handle Game Logic
        gameLogic();
        
        //Load Identity Matrix
        gl.glLoadIdentity();
        
        //Push
        gl.glPushMatrix();
        
        //Zoom the camera away from the game board
        gl.glTranslatef(0.1f, 0f, -9f);
        
        //Fancy rotations
        if(camerax) angle_x+=0.2;
        else angle_x-=0.2;
        if(Math.abs(angle_x)>35) camerax=Math.signum(angle_x)==-1;
        if(cameray) angle_y+=0.4;
        else angle_y-=0.4;
        if(Math.abs(angle_y)>35) cameray=Math.signum(angle_y)==-1;
        gl.glRotatef(angle_x, 1.0f, 0.0f, 0.0f);
        gl.glRotatef(angle_y, 0.0f, 1.0f, 0.0f);
        
        //Gray Color
        gl.glMaterialfv(GL.GL_FRONT, GL.GL_DIFFUSE, mat_gray, 0);
        
        //Draw the game board back
        gl.glPushMatrix();
        gl.glTranslatef(-0.1f, -0.1f, -0.2f);
        gl.glScalef(1.2f, 2f, 0.1f);
        glut.glutSolidCube(2f);
        gl.glPopMatrix();
        
        //Draw the game board left side
        gl.glPushMatrix();
        gl.glTranslatef(-1.2f, -0.1f, -0f);
        gl.glScalef(0.1f, 2f, 0.1f);
        glut.glutSolidCube(2f);
        gl.glPopMatrix();
        
        //Draw the game board right side
        gl.glPushMatrix();
        gl.glTranslatef(1f, -0.1f, -0f);
        gl.glScalef(0.1f, 2f, 0.1f);
        glut.glutSolidCube(2f);
        gl.glPopMatrix();
        
        //Draw the game board base
        gl.glPushMatrix();
        gl.glTranslatef(-0.1f, -2.2f, -0.1f);
        gl.glScalef(1.2f, 0.1f, 0.2f);
        glut.glutSolidCube(2f);
        gl.glPopMatrix();
        
        //Move the game to the center of the screen
        gl.glTranslatef(-1f, -2f, 0f);
        
        //Draw the game blocks
        for(int i=0; i<10; i++) {
            gl.glPushMatrix();
            gl.glTranslatef(i*0.2f, 0f, 0f);
            for(int j=0; j<24; j++) {
                if(gameboard[i][j]!=0) {
                    gl.glMaterialfv(GL.GL_FRONT, GL.GL_DIFFUSE, getBlockColor(gameboard[i][j]), 0);
                    gl.glPushMatrix();
                    gl.glTranslatef(0f, j*0.2f, 0f);
                    glut.glutSolidCube(0.19f);
                    gl.glPopMatrix();
                }
            }
            gl.glPopMatrix();
        }
        
        //Pop
        gl.glPopMatrix();
        
        //Draw the next shape
        gl.glMaterialfv(GL.GL_FRONT, GL.GL_DIFFUSE, getBlockColor(nextTile.color), 0);
        gl.glTranslatef(-0.34f, 0.12f, -1f);
        for(int i=0; i<4; i++) {
            gl.glPushMatrix();
            gl.glTranslatef(i*0.021f, 0f, 0f);
            for(int j=0; j<4; j++) {
                if(nextTile.shape[i][j]) {
                    gl.glPushMatrix();
                    gl.glTranslatef(0f, j*0.021f, 0f);
                    glut.glutSolidCube(0.02f);
                    gl.glPopMatrix();
                }
            }
            gl.glPopMatrix();
        }
        
        //Render Text
        largeFont.beginRendering(winW, winH);
        largeFont.setColor(1f, 1f, 1f, 1f);
        largeFont.draw("DizzyingBlocks", 0, winH-48);
        String stringScore = ""+score;
        largeFont.draw(stringScore, winW-stringScore.length()*27, winH-48);
        if(gameOver) largeFont.draw("Game Over", winW/2-120, winH/2);
        largeFont.endRendering();
        smallFont.beginRendering(winW, winH);
        smallFont.setColor(1f, 1f, 1f, 1f);
        smallFont.draw("Next Piece:", 3, winH-110);
        smallFont.draw("Controls", 3, 103);
        smallFont.draw("Left/Right: Move", 3, 83);
        smallFont.draw("Up: Rotate", 3, 63);
        smallFont.draw("Down: Speed Up", 3, 43);
        smallFont.draw("R: Restart", 3, 23);
        smallFont.draw("Q: Quit", 3, 3);
        smallFont.endRendering();
        
    }
    
    //Initialization
    public void init(GLAutoDrawable drawable) {
        gl = drawable.getGL();
        gl.setSwapInterval(1);
        
        //Set clear color: this determines the background color
        gl.glClearColor(0f, 0.2f, 0.3f, 1.0f);
        gl.glClearDepth(5.0f);
        
        //White Light
        float light0_position[] = { 0, 0.5f, 1, 0 };
        float light0_diffuse[] = { 1, 1, 1, 1 };
        float light0_specular[] = { 1, 1, 1, 1 };
        gl.glLightfv( GL.GL_LIGHT0, GL.GL_POSITION, light0_position, 0);
        gl.glLightfv( GL.GL_LIGHT0, GL.GL_DIFFUSE, light0_diffuse, 0);
        gl.glLightfv( GL.GL_LIGHT0, GL.GL_SPECULAR, light0_specular, 0);
        
        //Shininess + Specular Lighting
        gl.glMateriali(GL.GL_FRONT, GL.GL_SHININESS, 64);
        float mat_specular[] = { 0.2f, 0.2f, 0.2f, 1f };
        gl.glMaterialfv(GL.GL_FRONT, GL.GL_SPECULAR, mat_specular, 0);
        
        gl.glEnable(GL.GL_NORMALIZE);
        gl.glEnable(GL.GL_LIGHTING);
        gl.glEnable(GL.GL_LIGHT0);
        
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthFunc(GL.GL_LESS);
        gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_FASTEST);
        gl.glCullFace(GL.GL_BACK);
        gl.glEnable(GL.GL_CULL_FACE);
        gl.glShadeModel(GL.GL_FLAT);
        gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
        
        //Font
        largeFont = new TextRenderer(new Font("Monospaced", Font.BOLD, 48));
        smallFont = new TextRenderer(new Font("Monospaced", Font.BOLD, 20));
    }
    
    //Reshape callback function: called when the size of the window changes
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        //The easy way to fix the next piece not appearing in the right spot...
        glu.gluPerspective(30.0f, (float) winW / (float) winH, 0.01f, 100.0f);
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL.GL_MODELVIEW);
    }
    
    //Check Key Presses
    public void keyPressed(KeyEvent e) {
        switch(e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
            case KeyEvent.VK_Q:
                System.exit(0);
                break;
            case KeyEvent.VK_R:
                startGame();
                break;
            case KeyEvent.VK_UP:
                if(!gameOver&&!rowAnimation&&!fallingTile.atBottom) fallingTile.rotate();
                break;
            case KeyEvent.VK_DOWN:
                if(!gameOver&&!rowAnimation) speedUp=true;
                break;
            case KeyEvent.VK_LEFT:
                if(!gameOver&&!rowAnimation&&!fallingTile.atBottom) fallingTile.moveLeft();
                break;
            case KeyEvent.VK_RIGHT:
                if(!gameOver&&!rowAnimation&&!fallingTile.atBottom) fallingTile.moveRight();
                break;
        }
    }
    
    //Check Key Releases
    public void keyReleased(KeyEvent e) {
        if(e.getKeyCode()==KeyEvent.VK_DOWN) speedUp=false;
    }
    
    //Start The Game
    private void startGame() {
        gameOver=false;
        //Game Board
        for(int i=0; i<10; i++) {
            for(int j=0; j<24; j++) {
                gameboard[i][j] = 0;
            }
        }
        fallingTile = new FallingTile(3, 20, 1+(int) Math.floor(Math.random()*7));
        nextTile = new FallingTile(3, 20, 1+(int) Math.floor(Math.random()*7));
        fallingTile.colorShape();
        rowAnimation=false;
        rowComplete=-1;
        score=0;
    }
    
    //Game Logic
    private void gameLogic() {
        if(gameOver) return;
        if(timer>0&&!speedUp) {
            timer--;
        }
        else {
            timer = 60;
            if(rowComplete!=-1) {
                shiftTilesDown(rowComplete);
                rowComplete=-1;
                score++;
                return;
            }
            if(fallingTile.atBottom&&fallingTile.isAtTop()) {
                gameOver=true;
            }
            else if(fallingTile.atBottom) {
                rowComplete=checkRows();
                if(rowComplete!=-1) {
                    rowAnimation=true;
                    speedUp=false;
                    timer = 15;
                }
                else {
                    rowAnimation=false;
                    fallingTile = nextTile;
                    nextTile = new FallingTile(3, 20, 1+(int) Math.floor(Math.random()*7));
                    fallingTile.colorShape();
                }
            }
            else if(!rowAnimation) fallingTile.moveDown();
        }
    }
    
    private float[] getBlockColor(int v) {
        switch(v) {
            case 1: return mat_red;
            case 2: return mat_green;
            case 3: return mat_blue;
            case 4: return mat_yellow;
            case 5: return mat_cyan;
            case 6: return mat_magenta;
            case 7: return mat_orange;
            case -1: return mat_white;
            default: return null;
        }
    }
    
    //Check if any rows are complete, returns the complete row
    private int checkRows() {
        boolean complete = false;
        for(int j=0; j<24; j++) {
            complete=true;
            for(int i=0; i<10; i++) {
                if(gameboard[i][j]==0) complete=false;
            }
            if(complete) {
                for(int i=0; i<10; i++) {
                    gameboard[i][j]=-1;
                }
                return j;
            }
        }
        return -1;
    }
    
    //Shifts all tiles above the column down, erasing the column
    private void shiftTilesDown(int column) {
        for(int i=0; i<10; i++) {
            for(int j=column; j<23; j++) {
                gameboard[i][j] = gameboard[i][j+1];
            }
            gameboard[i][23]=0;
        }
    }
    
    //Falling Tile
    private class FallingTile {
        private int x;
        private int y;
        private int color;
        private boolean[][] shape = new boolean[4][4];
        private boolean atBottom = false;
        private FallingTile(int i, int j, int c) {
            x=i;
            y=j;
            color=c;
            selectShape();
        }
        
        private void selectShape() {
            for(int i=0; i<4; i++) {
                for(int j=0; j<4; j++) {
                    shape[i][j]=false;
                }
            }
            switch(color) {
                case 1:
                    shape[1][1] = true;
                    shape[2][1] = true;
                    shape[0][2] = true;
                    shape[1][2] = true;
                    return;
                case 2:
                    shape[0][1] = true;
                    shape[1][1] = true;
                    shape[1][2] = true;
                    shape[2][2] = true;
                    return;
                case 3:
                    shape[2][1] = true;
                    shape[0][2] = true;
                    shape[1][2] = true;
                    shape[2][2] = true;
                    return;
                case 4:
                    shape[1][1] = true;
                    shape[2][1] = true;
                    shape[1][2] = true;
                    shape[2][2] = true;
                    return;
                case 5:
                    shape[0][1] = true;
                    shape[1][1] = true;
                    shape[2][1] = true;
                    shape[3][1] = true;
                    return;
                case 6:
                    shape[1][1] = true;
                    shape[0][2] = true;
                    shape[1][2] = true;
                    shape[2][2] = true;
                    return;
                case 7:
                    shape[0][1] = true;
                    shape[0][2] = true;
                    shape[1][2] = true;
                    shape[2][2] = true;
                    return;
            }
        }
        
        private void colorShape() {
            for(int i=0; i<4; i++) {
                for(int j=0; j<4; j++) {
                    if(shape[i][j]) gameboard[x+i][y+j] = color;
                }
            }
        }
        
        private void eraseShape() {
            for(int i=0; i<4; i++) {
                for(int j=0; j<4; j++) {
                    if(shape[i][j]) gameboard[x+i][y+j] = 0;
                }
            }
        }
        
        private boolean isOccupied() {
            for(int i=0; i<4; i++) {
                for(int j=0; j<4; j++) {
                    if(shape[i][j]&&((x+i<0||y+j<0||x+i>9||gameboard[x+i][y+j]!=0))) return true;
                }
            }
            return false;
        }
        
        private boolean isAtTop() {
            for(int i=0; i<4; i++) {
                for(int j=0; j<4; j++) {
                    if(shape[i][j]&&y+j>19) return true;
                }
            }
            return false;
        }
        
        private void moveLeft() {
            eraseShape();
            x--;
            if(isOccupied()) x++;
            colorShape();
        }
        
        private void moveRight() {
            eraseShape();
            x++;
            if(isOccupied()) x--;
            colorShape();
        }
        
        private void moveDown() {
            eraseShape();
            y--;
            if(isOccupied()) {
                y++;
                atBottom = true;
            }
            colorShape();
        }
        
        private void rotate() {
            boolean[][] oldShape = shape;
            boolean[][] newShape = new boolean[4][4];
            for(int i=0; i<4; i++) {
                for(int j=0; j<4; j++) {
                    newShape[i][j]=oldShape[3-j][i];
                }
            }
            eraseShape();
            shape=newShape;
            if(isOccupied()) shape=oldShape;
            colorShape();
        }
        
    }
    
    //Unused Functions
    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {}
    public void keyTyped(KeyEvent e) { }
    
}