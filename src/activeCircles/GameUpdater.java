package activeCircles;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * GameUpdater handles the high level rendering of the game at the
 * BufferStrategy level, as well as handles updating time based events
 * and animations.
 */
class GameUpdater implements ResizeListener {
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
