package quadrat;

import javax.swing.*;
import java.awt.*;

public class QuadratActive extends JFrame implements Runnable {

    private int pos_x;
    private Thread thread;
    private boolean flag = true;

    // Variablen für's DoubleBuffering
    private Image image_buffer;
    private Graphics graphics_buffer;

    // Konstruktor
    public QuadratActive() {
        super("DoubleBuffering");
        pos_x = 0;
        thread = new Thread(this);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 500);
        setVisible(true);

        thread.start();
    }

    public void run() {
        // Qudadrat solange bewegen, bis es einen bestimmten
        // Punkt ereicht hat
        while (pos_x < 400) {
            if (flag == true)
                pos_x++;
            else
                pos_x--;

            if (pos_x >= 400) {
                pos_x--;
                flag = false;
            } else if (pos_x <= 0) {
                pos_x++;
                flag = true;
            }

            paintScreen(); // Neuzeichnen

            try {
                Thread.sleep(5);
            } catch (InterruptedException ex) {
                // Ignorieren
            }
        }
    }

    /**********************
     * DoubleBuffering
     ***********************/

    // Inhalt wie gewohnt in die paint()-Methode
    public void paint(Graphics gr) {
        // Das Quadrat zeichnen
        gr.setColor(Color.red);
        super.paint(gr);
        gr.fillRect(pos_x, 200, 100, 100);
    }

    // DoubleBuffering in die update()-Methode
    public void update(Graphics gr) {
        // Momentanen Screen in den Buffer schreiben
        if (image_buffer == null) {
            image_buffer = createImage(getWidth(), getHeight());
            graphics_buffer = image_buffer.getGraphics();
        }

        // Bildschirm leeren
        graphics_buffer.setColor(getBackground());
        graphics_buffer.fillRect(0, 0, getWidth(), getHeight());

        paint(graphics_buffer); // paint-Methode aufrufen

        // Den Buffer zeichnen
        gr.drawImage(image_buffer, 0, 0, this);
    }

    // Active Rendering
    public void paintScreen() {
        Graphics gr;

        update(getGraphics());

        try {
            gr = getGraphics();

            if (image_buffer != null) {
                gr.drawImage(image_buffer, 0, 0, this);
            }

            Toolkit.getDefaultToolkit().sync();
            gr.dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**********************
     * DoubleBuffering
     ***********************/

    public static void main(String[] args) {
        QuadratActive quadrat = new QuadratActive();
    }
}
