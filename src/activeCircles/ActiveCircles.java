package activeCircles;

import javax.swing.*;
import java.awt.image.BufferStrategy;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
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
}
