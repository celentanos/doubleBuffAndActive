package activeCircles;

import java.awt.*;

/**
 * A moving circle is a circle that moves around the screen bouncing off
 * the walls of the area it is bounded too
 */
class MovingCircle {
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
