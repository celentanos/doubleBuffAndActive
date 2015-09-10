package activeCircles;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

/**
 * JFrameWithResizeListener is a JFrame that tell's it's sole listener
 * that it was created with the drawable bounds of it's frame on resizes.
 */
class JFrameWithResizeListener extends JFrame implements ComponentListener {
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
