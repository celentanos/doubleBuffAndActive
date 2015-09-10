package activeCircles;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * GameDataJPanel contains GUI components related to manipulating a
 * GameData object.
 */
class GameDataJPanel extends JPanel implements ActionListener {
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
