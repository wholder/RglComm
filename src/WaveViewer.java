import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.prefs.Preferences;

class WaveViewer extends JFrame {
  static class Surface extends JPanel {
    private BufferedImage img;

    Surface(byte[] data) {
      img = new BufferedImage(600, 512, BufferedImage.TYPE_INT_ARGB);
      Dimension size = new Dimension(img.getWidth(null), img.getHeight(null));
      setPreferredSize(size);
      Graphics2D g2 = img.createGraphics();
      g2.setColor(Color.WHITE);
      g2.fillRect(0, 0, size.width, size.height);
      RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g2.setRenderingHints(hints);
      g2.setStroke(new BasicStroke((1.0f)));
      g2.setPaint(Color.lightGray);
      g2.draw(new Line2D.Double(0, 256, 599, 255));
      for (int ii = 0; ii < 256; ii += 64) {
        g2.draw(new Line2D.Double(0, 256 + ii, 599, 256 + ii));
        g2.draw(new Line2D.Double(0, 256 - ii, 599, 256 - ii));
      }
      g2.setPaint(Color.darkGray);
      for (int ii = 60; ii < 600; ii += 60) {
        g2.draw(new Line2D.Double(ii, 256 - 5, ii, 256 + 5));
      }
      g2.setStroke(new BasicStroke((1.0f)));
      g2.setPaint(Color.BLACK);
      Path2D.Double path = new Path2D.Double();
      path.moveTo(0, ((int) data[0] & 0xFF) * 2);
      for (int ii = 1; ii < data.length; ii++) {
        path.lineTo(ii, ((int) data[ii] & 0xFF) * 2);
      }
      g2.draw(path);
    }

    @Override
    public void paintComponent(Graphics g) {
      Graphics2D g2d = (Graphics2D) g;
      g2d.drawImage(img, 0, 0, null);
    }
  }

  WaveViewer (Preferences prefs, byte[] data) throws Exception {
    setTitle("WaveViewer");
    Surface surface = new Surface(data);
    add(surface);
    JMenuBar menuBar = new JMenuBar();
    setJMenuBar(menuBar);
    JMenu menu = new JMenu("File");
    menuBar.add(menu);
    JMenuItem save = new JMenuItem("Save as PNG");
    menu.add(save);
    save.addActionListener(ev -> {
      JFileChooser chooser = new JFileChooser();
      String fileDir = prefs.get("file.dir", null);
      if (fileDir != null) {
        chooser.setCurrentDirectory(new File(fileDir));
      }
      chooser.setDialogType(JFileChooser.SAVE_DIALOG);
      chooser.setSelectedFile(new File("screen.png"));
      chooser.setFileFilter(new FileNameExtensionFilter("png file","png"));
      if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        File file = chooser.getSelectedFile();
        prefs.put("file.dir", chooser.getCurrentDirectory().toString());
        try {
          ImageIO.write(surface.img, "png", file);
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    });
    pack();
    setLocationRelativeTo(null);
    setVisible(true);
  }
}