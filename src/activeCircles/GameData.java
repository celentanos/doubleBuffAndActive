package activeCircles;

import java.awt.*;
import java.util.Random;

/**
 * Represents the data of the application to draw and update.
 */
class GameData {
    // Milliseconds between updates when not running the game at max speed
    private static final long slowWaitTimeBetweenUpdates = 5;
    // Milliseconds between updates when running the game at max speed,
    // in this case, no waiting at all with a time of 0
    private static final long fastWaitTimeBetweenUpdates = 0;

    // Represents the known bounds of the game's area
    private Rectangle boundedGameArea;

    // Array of our sprites
    private MovingCircle[] circles;
    private long currentUpdateSpeed;
    private long oldTime;
    private long timeSinceLastFPSCalculation;
    private int frames;
    private int updates;
    // Holds the latest calculated value of frames per second
    private int fps;
    // Holds the latest calculated value of updates per
    private int ups;


    /**
     * Constructs a new GameData. GameData's constructor creates and
     * schedules an initial updating task
     *
     * @param width  The width of the bounded game area
     * @param height The height of the bounded game area
     */
    public GameData(int width, int height, int numberOfCircles) {
        boundedGameArea = new Rectangle(width, height);
        circles = new MovingCircle[numberOfCircles];
        int circleWidth = 50;
        int circleHeight = 50;
        float maxSpeed = .5f;
        Random random = new Random();
        for (int i = 0; i < circles.length; i++) {
            circles[i] = new MovingCircle(
                    boundedGameArea.getBounds(),
                    random.nextFloat() * (width - circleWidth),
                    random.nextFloat() * (height - circleHeight),
                    circleWidth, circleHeight, random.nextBoolean(),
                    random.nextBoolean(), random.nextFloat()
                    * maxSpeed);
        }

        // Initialize the time, fps, and other variables
        currentUpdateSpeed = slowWaitTimeBetweenUpdates;
        oldTime = System.nanoTime();
        timeSinceLastFPSCalculation = 0;
        frames = 0;
        updates = 0;
        fps = 0;
        ups = 0;
        ups = 0;

    }

    /**
     * Resets the last known time of the last update,
     * useful for when there has not been an update in a long time,
     * or when the application first starts where there may have been
     * some time the application spent doing other things between when
     * updating starts and the elaboration of the object.
     */
    public synchronized void resetTimeOfLastUpdate() {
        oldTime = System.nanoTime();
    }

    /**
     * Retrieves the time between updates to wait
     */
    public synchronized long getCurrentWaitTimeBetweenUpdates() {
        return currentUpdateSpeed;
    }

    /**
     * Returns the latest calculated updates per second
     */
    public synchronized int getUPS() {
        return ups;
    }

    /**
     * Returns the latest calculated frames per second
     */
    public synchronized int getFPS() {
        return fps;
    }

    /**
     * Modifies all circles to have the same new random color
     */
    public synchronized void setAllCirclesToRandomColor() {
        Random random = new Random();
        Color newCircleColor = new Color(random.nextInt(256),
                random.nextInt(256),
                random.nextInt(256));
        for (MovingCircle circle : circles) {
            circle.changeColor(newCircleColor);
        }
    }

    /**
     * Start updating the game data at a slow update speed
     */
    public synchronized void updateGameDataAtSlowRate() {
        currentUpdateSpeed = slowWaitTimeBetweenUpdates;
    }

    /**
     * Start updating the game data at a fast update speed
     */
    public synchronized void updateGameDataAtFastRate() {
        currentUpdateSpeed = fastWaitTimeBetweenUpdates;
    }

    /**
     * Updates any objects that need to know how much time has elapsed to
     * update any needed movements, animations, or events.
     */
    public synchronized void updateData() {
        // Calculating a new fps/ups value every second
        if (timeSinceLastFPSCalculation >= 1000000000) {
            fps = frames;
            ups = updates;
            timeSinceLastFPSCalculation = timeSinceLastFPSCalculation - 1000000000;
            frames = 0;
            updates = 0;
        }

        long elapsedTime = System.nanoTime() - oldTime;
        oldTime = oldTime + elapsedTime;
        timeSinceLastFPSCalculation = timeSinceLastFPSCalculation + elapsedTime;

        // Loop through all circles, update them
        for (MovingCircle circle : circles) {
            circle.update(elapsedTime);
        }

        // An update occurred, increment.
        updates++;
    }

    /**
     * Draws the GameData as needed
     */
    public synchronized void drawGameData(Graphics2D drawingBoard,
                                          int drawAreaWidth,
                                          int drawAreaHeight) {
        // This allows our text and graphics to be nice and smooth
        drawingBoard.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        drawingBoard.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // Always draw over the image with a blank background, so we
        // don't see the last frame's drawings! (comment this out and
        // see what happens, it's fun pressing the change color button
        // rapidly too!)
        drawingBoard.setColor(Color.LIGHT_GRAY);
        drawingBoard.fillRect(0, 0, drawAreaWidth, drawAreaHeight);


        // Creating a graphics object to not clobber parameter drawingBoard
        // where MovingCircle's drawing method may change some state of
        // the drawingBoard parameter graphics object
        Graphics circleGraphics = drawingBoard.create();
        // Now draw all the circles, location 0,0 will be top left
        // corner within the borders of the window
        for (MovingCircle circle : circles) {
            circle.draw(circleGraphics);
        }
        circleGraphics.dispose();

        frames++;
    }
}
