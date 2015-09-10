package activeCircles;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.util.concurrent.BlockingQueue;

/**
 * GuiCreatorRunnable is a runnable, that creates the GUI and a
 * BufferStrategy, and stores the reference to the strategy to later
 * retrieve.
 */
class GuiCreatorRunnable implements Runnable {
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
