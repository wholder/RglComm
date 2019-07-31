import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;

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
    JMenuBar menuBar = new JMenuBar();
    setJMenuBar(menuBar);
    JMenu menu = new JMenu("File");
    menuBar.add(menu);
    JMenuItem save = new JMenuItem("Save as PNG");
    menu.add(save);
    save.addActionListener(ev -> {
      JFileChooser chooser = new JFileChooser();
      chooser.setDialogType(JFileChooser.SAVE_DIALOG);
      chooser.setSelectedFile(new File("screen.png"));
      chooser.setFileFilter(new FileNameExtensionFilter("png file","png"));
      if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        File file = chooser.getSelectedFile();
        try {
          ImageIO.write(img, "png", file);
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