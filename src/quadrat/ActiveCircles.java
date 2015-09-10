package quadrat; /**
 * ActiveCircles.java, an example of active rendering while double buffering
 * The article on this source code can be found at:
 * http://jamesgames.org/resources/double_buffer/double_buffering_and_active_rendering.html
 * Code demonstrates: - Active rendering in swing in a resizable frame
 * - properly set width and height of a JFrame using Insets
 * - double buffering and active rendering via BufferStrategy
 * - usage of a high resolution timer for time based
 * animations
 * - stretching an application's graphics with resizes
 * - integrating Swing components with your active rendering
 * solution
 * The entire code base is in one class with a lot of static classes, this was
 * my idea of simplifying the distribution of the code and the viewing of the
 * source online for quick reference.
 *
 * @author James Murphy
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;
import java.util.concurrent.*;


public class ActiveCircles {
    public static void main(String[] args) {
        // Constructor kicks off the GUI
        ActiveCircles activeCirclesExample = new ActiveCircles(50, 700, 500);
        activeCirclesExample.start();
    }

    private GameUpdater gameUpdater;

    /**
     * Constructor for ActiveCircles
     *
     * @param numberOfCircles The number of circles you want the program to
     *                        display
     * @param width           The width of the program's inside portion of
     *                        the frame
     * @param height          The height of the program's inside portion of
     *                        the frame
     */
    public ActiveCircles(int numberOfCircles, final int width, final int height) {
        GameData gameData = new GameData(width, height, numberOfCircles);
        BlockingQueue<BufferStrategy> bufferStrategyQueue = new ArrayBlockingQueue<>(1);
        this.gameUpdater = new GameUpdater(gameData, bufferStrategyQueue, width, height);
        // This runnable will construct the GUI on the EDT, but also update our
        // GameUpdater with a reference to a SwingComponentDrawer object
        // that is created in this codebase, as well as store the created
        // BufferStrategy to use for active rendering to the BlockingQueue
        // passed.
        Runnable guiCreator = new GuiCreatorRunnable(width, height, gameData, gameUpdater, bufferStrategyQueue);

        try {
            SwingUtilities.invokeAndWait(guiCreator);
        } catch (InterruptedException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts the updating of the game's state such as animation and movement
     * in another thread.
     */
    void start() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                gameUpdater.updateLoop();
            }
        }).start();
    }

    /**
     * GuiCreatorRunnable is a runnable, that creates the GUI and a
     * BufferStrategy, and stores the reference to the strategy to later
     * retrieve.
     */
    static class GuiCreatorRunnable implements Runnable {
        private int width;
        private int height;
        private GameData gameData;
        private GameUpdater gameUpdater;
        private BlockingQueue<BufferStrategy> bufferStrategyQueue;

        /**
         * Constructs a GuiCreatorRunnableFuture, with the requested width
         * and height, and the GameUpdater object to update with a reference
         * to a
         * SwingComponentDrawer, a GameData object to pass to the GUI controls,
         * and the BlockingQueue to store a BufferStrategy
         */
        public GuiCreatorRunnable(int width, int height,
                                  GameData gameData,
                                  GameUpdater gameUpdater,
                                  BlockingQueue<BufferStrategy> bufferStrategyQueue) {
            this.width = width;
            this.height = height;
            this.gameData = gameData;
            this.gameUpdater = gameUpdater;
            this.bufferStrategyQueue = bufferStrategyQueue;
        }

        @Override
        public void run() {
            JFrame frame = new JFrameWithResizeListener(gameUpdater);
            frame.setTitle("Actively rendering and double buffering " +
                    "graphics and Swing components together");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setVisible(true);
            // Ignore repaints, as we will actively render the frame's graphics
            // ourselves
            frame.setIgnoreRepaint(true);
            // While we have the frame reference available, set it's content
            // pane to not be opaque.
            // The JFrame's content pane's background would otherwise paint over
            // any other graphics we painted ourselves
            if (frame.getContentPane() instanceof JComponent) {
                // JComponent's have a handy setOpaque method
                ((JComponent) frame.getContentPane()).setOpaque(false);
            } else {
                frame.getContentPane().setBackground(new Color(0, 0, 0, 0));
            }
            // Change width and height of window so that the available
            // screen space actually corresponds to what is passed, another
            // method is the Canvas object + pack()
            frame.setSize(width, height);
            Insets insets = frame.getInsets();
            int insetWide = insets.left + insets.right;
            int insetTall = insets.top + insets.bottom;
            frame.setSize(frame.getWidth() + insetWide,
                    frame.getHeight() + insetTall);


            GameDataJPanel gameDataPanel = new GameDataJPanel(gameData);
            frame.add(gameDataPanel);
            // Create the BufferStrategy, and store the reference to it
            frame.createBufferStrategy(2);
            try {
                bufferStrategyQueue.put(frame.getBufferStrategy());
            } catch (InterruptedException e) {
                // Should not be interrupted
                e.printStackTrace();
            }
            // GameUpdater will render all of the frame's components
            gameUpdater.setComponentToDraw(frame.getContentPane());
        }
    }


    /**
     * JFrameWithResizeListener is a JFrame that tell's it's sole listener
     * that it was created with the drawable bounds of it's frame on resizes.
     */
    static class JFrameWithResizeListener extends JFrame implements ComponentListener {
        // Sole listener to the resizes of the frame
        private ResizeListener resizeListener;

        public JFrameWithResizeListener(ResizeListener resizeListener) {
            this.addComponentListener(this);
            this.resizeListener = resizeListener;


        }

        @Override
        public void componentResized(ComponentEvent e) {
            Insets insets = this.getInsets();
            resizeListener.drawAreaChanged(
                    insets.left,
                    insets.top,
                    this.getWidth() - (insets.left + insets.right),
                    this.getHeight() - (insets.top + insets.bottom));
        }

        @Override
        public void componentMoved(ComponentEvent e) {
            // Do nothing, not needed
        }

        @Override
        public void componentShown(ComponentEvent e) {
            // Do nothing, not needed
        }

        @Override
        public void componentHidden(ComponentEvent e) {
            // Do nothing, not needed
        }
    }

    static interface ResizeListener {
        public void drawAreaChanged(int x, int y, int width, int height);
    }


    /**
     * GameDataJPanel contains GUI components related to manipulating a
     * GameData object.
     */
    static class GameDataJPanel extends JPanel implements ActionListener {
        // Set true to limit fps (sleep the thread), false to not
        private boolean limitingFPS;
        // Button to randomize circle colors
        private JButton changeColor;
        // Button to switch the value of limiting FPS
        private JButton limitFps;
        // GameData to draw
        private GameData gameData;


        /**
         * Creates a new GameDataJPanel. When what's drawn is larger or smaller
         * than the supplied width and height, the drawn image will stretch to
         * meet the current container's size.
         *
         * @param gameData Data of the game to draw
         */
        public GameDataJPanel(GameData gameData) {
            this.gameData = gameData;
            // Setting up the swing components;
            JPanel programTitlePanel = new JPanel(new FlowLayout());
            JLabel title = new JLabel("Actively rendering graphics and Swing " + "Components!");
            programTitlePanel.add(title);
            changeColor = new JButton("Change color");
            changeColor.addActionListener(this);
            JPanel changeColorPanel = new JPanel(new FlowLayout());
            changeColorPanel.add(changeColor);
            limitFps = new JButton("Unlimit FPS");
            limitFps.addActionListener(this);
            JPanel fpsAndUpdatePanel = new JPanel(new FlowLayout());
            fpsAndUpdatePanel.add(limitFps);

            JPanel holder = new JPanel(new GridLayout(2, 1));
            holder.add(programTitlePanel);
            holder.add(changeColorPanel);

            this.setLayout(new BorderLayout());
            this.add(BorderLayout.NORTH, holder);
            this.add(BorderLayout.SOUTH, fpsAndUpdatePanel);

            // Now set the JPanel's opaque, along with other Swing components
            // whose backgrounds we don't want shown, so we can see the
            // application's graphics underneath those components!
            // (Try commenting some out to see what would otherwise happen!)
            changeColorPanel.setOpaque(false);
            this.setOpaque(false);
            title.setOpaque(false);
            programTitlePanel.setOpaque(false);
            fpsAndUpdatePanel.setOpaque(false);
            holder.setOpaque(false);

            limitingFPS = true;
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == changeColor) {
                gameData.setAllCirclesToRandomColor();
            }
            if (e.getSource() == limitFps) {
                limitingFPS = !limitingFPS;
                if (limitingFPS) {
                    limitFps.setText("Unlimit FPS");
                    gameData.updateGameDataAtSlowRate();
                } else {
                    limitFps.setText("Limit FPS");
                    gameData.updateGameDataAtFastRate();
                }
            }
        }
    }


    /**
     * GameUpdater handles the high level rendering of the game at the
     * BufferStrategy level, as well as handles updating time based events
     * and animations.
     */
    static class GameUpdater implements ResizeListener {
        private GameData gameData;
        private BlockingQueue<BufferStrategy> bufferStrategyQueue;
        // Not initialized at creation, but passed in externally when created
        private BufferStrategy bufferStrategy;
        // The component to draw via EDT
        private Component componentToDraw;
        private final Rectangle drawAreaBounds;
        // We draw almost all graphics to this image, then stretch it over the
        // entire frame.
        // This allows a resize to make the game bigger, as opposed to
        // just providing a larger area for the sprites to be drawn onto.
        // We also are using this image's pixel coordinates as the coordinates
        // of our circle sprites.
        private BufferedImage drawing;

        public GameUpdater(GameData gameData,
                           BlockingQueue<BufferStrategy> bufferStrategyQueue, int drawingWidth, int drawingHeight) {
            this.gameData = gameData;
            this.bufferStrategyQueue = bufferStrategyQueue;
            this.drawAreaBounds = new Rectangle(0, 0, drawingWidth, drawingHeight);

            // We draw almost all graphics to this image,
            // then stretch it over the
            // entire frame.
            // This allows a resize to make the game bigger, as opposed to
            // just providing a larger area for the sprites to be drawn onto.
            drawing = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration()
                    .createCompatibleImage(drawingWidth, drawingHeight);
        }

        public synchronized void setComponentToDraw(Component componentToDraw) {
            this.componentToDraw = componentToDraw;
        }

        @Override
        public void drawAreaChanged(int x, int y, int width, int height) {
            synchronized (drawAreaBounds) {
                drawAreaBounds.setBounds(x, y, width, height);
            }
        }

        public void updateLoop() {
            // Wait for a buffer strategy from the queue, can't start the game
            // without being able to draw graphics
            try {
                bufferStrategy = bufferStrategyQueue.poll(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // Thread should not be interrupted, this method is the lowest
                // method the thread should execute from this point on, as the
                // method has an infinite loop
                e.printStackTrace();
            }
            if (bufferStrategy == null) {
                System.err.println("BufferStrategy could not be made!");
                System.exit(1);
            }

            // For max accuracy, resetting the time since last update so
            // animations and sprite positions remain in their standard first
            // position
            gameData.resetTimeOfLastUpdate();

            // Just loop and loop forever, update state and then draw.
            while (true) {
                long nanoTimeAtStartOfUpdate = System.nanoTime();

                gameData.updateData();
                try {
                    Graphics2D g = (Graphics2D) bufferStrategy.getDrawGraphics();
                    drawGame(g);
                    g.dispose();
                    if (!bufferStrategy.contentsLost()) {
                        bufferStrategy.show();
                    }
                }
                // This catch is to allow the application to not stop
                // working when the application encounters the possible bug:
                // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6933331
                // One work around to not encounter this is to Disable d3d
                // using -Dsun.java2d.d3d=false
                // Not sure why the bug is said to "... has no consequences
                // other than a stack trace dump in a console (no hang... ",
                // as people are generally not going to catch an
                // IllegalStateException...
                // You can try to see if you can get the exception to print
                // by resizing the window rapidly on the primary or secondary,
                // or dragging the window off and on the primary monitor.
                // This of course assumes you are using d3d
                catch (IllegalStateException e) {
                    e.printStackTrace();
                }

                waitUntilNextUpdate(nanoTimeAtStartOfUpdate);
            }
        }

        private synchronized void drawGame(Graphics2D g) {
            // Obtaining the graphics of our drawing image we use,
            // most of the graphics drawn are drawn to this object
            Graphics2D drawingBoard = drawing.createGraphics();
            gameData.drawGameData(drawingBoard, drawing.getWidth(), drawing.getHeight());
            drawingBoard.dispose();

            final Graphics swingAndOtherGuiGraphics = g.create();
            synchronized (drawAreaBounds) {
                // The translate is needed to to align our drawing of
                // components to their "clickable" areas (changes where 0, 0
                // actually is, comment it out and see what happens!)
                swingAndOtherGuiGraphics.translate(drawAreaBounds.x, drawAreaBounds.y);

                Graphics gameGraphics = g.create();
                // Image call that scales and stretches the game's graphics over
                // the entire frame
                // NOTE: This method of stretching graphics is not optimal.
                // This causes a computation of a stretched image each time. A
                // better implementation would be to cache an image of the
                // latest representation of a drawn circle,
                // and re-cache whenever there is a visible change (like color,
                // or it's size, which would be due to a window resize), and
                // draw that cached image at the correct calculated location.
                // Additionally, it also causes the rendering to this image to
                // be done on the CPU. See the improvements section in the
                // tutorial.
                gameGraphics.drawImage(
                        drawing,
                        drawAreaBounds.x,
                        drawAreaBounds.y,
                        drawAreaBounds.width,
                        drawAreaBounds.height, null);
                gameGraphics.dispose();
            }
            // componentToDraw is lazily set from the EDT during GUI creation
            if (componentToDraw != null) {
                // Paint our Swing components, to the graphics object of the
                // buffer, not the BufferedImage being used for the
                // application's sprites.
                // We do this, because Swing components don't resize on frame
                // resizes, they just reposition themselves, so we shouldn't
                // stretch their graphics at all.
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            if (componentToDraw instanceof JComponent) {
                                ((JComponent) componentToDraw).paintComponents(swingAndOtherGuiGraphics);
                            } else {
                                componentToDraw.paintAll(swingAndOtherGuiGraphics);
                            }
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (InvocationTargetException e) {
                    // should not happen
                    e.printStackTrace();
                }
            }
            // In addition, draw the FPS/UPS post stretch, so we always can read
            // the info even if you shrink the frame really small.
            swingAndOtherGuiGraphics.setColor(Color.WHITE);
            // Grab the font height to make sure we don't draw the stats outside
            // the panel, or over each other.
            int fontHeight = g.getFontMetrics(g.getFont()).getHeight();
            swingAndOtherGuiGraphics.drawString("FPS: " + gameData.getFPS(), 0, fontHeight);
            swingAndOtherGuiGraphics.drawString("UPS: " + gameData.getUPS(), 0, fontHeight * 2);
            swingAndOtherGuiGraphics.dispose();
        }

        /**
         * Sleeps the current thread if there's still sometime the application
         * can wait for until the time the next update is needed.
         *
         * @param nanoTimeCurrentUpdateStartedOn Time that current update
         *                                       started
         */
        private void waitUntilNextUpdate(long nanoTimeCurrentUpdateStartedOn) {
            // Only sleep to maintain the update speed if speed is higher than
            // zero, because Thread.sleep(0) is not defined on what that
            // exactly does
            long currentUpdateSpeed = gameData.getCurrentWaitTimeBetweenUpdates();
            if (currentUpdateSpeed > 0) {
                // This could be more accurate by sleeping what's needed on
                // average for the past few seconds
                long timeToSleep = currentUpdateSpeed - ((System.nanoTime() - nanoTimeCurrentUpdateStartedOn) / 10000000);
                // If the speed of updating was so slow that it's time for
                // the next update, then choose 0
                timeToSleep = Math.max(timeToSleep, 0);
                // Again, avoiding Thread.sleep(0)
                if (timeToSleep > 0) {
                    try {
                        Thread.sleep(timeToSleep);
                    } catch (InterruptedException e) {
                        // It's okay if we're interrupted, program will just run
                        // faster.
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }


    /**
     * Represents the data of the application to draw and update.
     */
    static class GameData {
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


    /**
     * A moving circle is a circle that moves around the screen bouncing off
     * the walls of the area it is bounded too
     */
    static class MovingCircle {
        private Rectangle boundedArea;
        private float x;
        private float y;
        private int width;
        private int height;
        private Color color;
        private boolean down;
        private boolean right;
        private float speed; // pixels per nanosecond

        public MovingCircle(Rectangle boundedArea,
                            float x,
                            float y,
                            int width,
                            int height,
                            boolean down,
                            boolean right,
                            float speed) {
            this.boundedArea = boundedArea;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.down = down;
            this.right = right;
            // Convert pixels per millisecond to nano second
            // Easier to originally think about speeds in milliseconds
            this.speed = speed / 1000000;
            this.color = Color.DARK_GRAY;
        }

        public synchronized void changeColor(Color color) {
            this.color = color;
        }

        /**
         * Update the circle, such as moving the circle, and detecting
         * collisions.
         *
         * @param elapsedTime The time that has elapsed since the last time
         *                    the circle was updated.
         */
        public synchronized void update(long elapsedTime) {
            float pixelMovement = elapsedTime * speed;
            if (down) {
                y = y + pixelMovement;
            } else {
                y = y - pixelMovement;
            }
            if (right) {
                x = x + pixelMovement;
            } else {
                x = x - pixelMovement;
            }

            // Test if circle hit a side of the known bounds
            // Also move the circle off the wall of the bounded area to prevent
            // the collision from sticking (comment out that code to see the
            // effect)
            if (y < 0) {
                down = !down;
                y = 0;
            }
            if (y > boundedArea.height - height) {
                down = !down;
                y = boundedArea.height - height;
            }
            if (x < 0) {
                right = !right;
                x = 0;
            }
            if (x > boundedArea.width - width) {
                right = !right;
                x = boundedArea.width - width;
            }
        }

        /**
         * Draw the circle
         */
        public synchronized void draw(Graphics g) {
            g.setColor(color);
            g.fillOval((int) x, (int) y, width, height);
        }
    }
}