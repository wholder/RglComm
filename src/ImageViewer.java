import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

class ImageViewer extends JFrame {
  static class Surface extends JPanel {
    private Image img;

    Surface(Image img) {
      this.img = img;
      setPreferredSize(new Dimension(img.getWidth(null), img.getHeight(null)));
    }

    @Override
    public void paintComponent(Graphics g) {
      Graphics2D g2d = (Graphics2D) g;
      g2d.drawImage(img, 0, 0, null);
    }
  }

  ImageViewer (byte[] data) throws Exception {
    setTitle("ImageViewer");
    ByteArrayInputStream bis = new ByteArrayInputStream(data);
    BufferedImage img = ImageIO.read(bis);
    add(new Surface(img));
    pack();
    setLocationRelativeTo(null);
    setVisible(true);
  }
}