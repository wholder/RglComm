import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

/*
 *  Test Program to communicate with and control Rigol devices using IEEE 488 Commands
 *
 *  Author: Wayne Holder, 2019
 *  License: MIT (https://opensource.org/licenses/MIT)
 */

public class RglComm extends JFrame {
  private transient Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
  private transient boolean     running;
  private static List<Rigol>    devices = new LinkedList<>();
  private JTextArea             text = new JTextArea();
  private JTextField            command;
  private JComboBox<Rigol>      select;
  private USBIO                 usb;
  private byte                  bTag;

  static class Rigol {
    String  name;
    short   vend, prod;

    Rigol (String name, int vend, int prod) {
      this.name = name;
      this.vend = (short) vend;
      this.prod = (short) prod;
    }

    public String toString () {
      return name;
    }
  }

  /*
   *   Vendor 0x1AB1, Product 0x09C4
   *   Manufacturer: Rigol Technologies
   *   Product:      DM3000 SERIES
   *
   *   Vendor 0x1AB1, Product 0x0E11
   *   Manufacturer: Rigol Technologies.
   *   Product:      DP800 Serials
   *
   *   Vendor 0x1AB1, Product 0x04B1
   *   Manufacturer: Rigol Technologies
   *   Product:      DS4000 Series
   *
   *   Vendor 0x1AB1, Product 0x0588
   *   Manufacturer: Rigol Technologies
   *   Product:      DS1000 SERIES
   *
   *   Vendor 0x1AB1, Product 0x0641
   *   Manufacturer: Rigol Technologies
   *   Product:      DG4162
   *
   *   Vendor 0x1AB1, Product 0x0960
   *   Manufacturer: Rigol Technologies
   *   Product:      DSA815
   */

  static {
    //                     Selector Description           VendId  ProdId
    devices.add(new Rigol("DM3058 Digital Multimeter",    0x1AB1, 0x09C4));
    devices.add(new Rigol("DP832 Prog DC Power Supply",   0x1AB1, 0x0E11));
    devices.add(new Rigol("DS4024 Digital Oscilloscope",  0x1AB1, 0x04B1));
    devices.add(new Rigol("DS1102E Digital Oscilloscope", 0x1AB1, 0x0588));
    devices.add(new Rigol("DSA815 Spectrum Analyzer",     0x1AB1, 0x0960));
    devices.add(new Rigol("DG4162 Func/Wave Generator",   0x1AB1, 0x0641)); // Shows as PID 0x0588 in "Printer" mode
    devices.add(new Rigol("DS1054Z Digital Oscilloscope", 0x1AB1, 0x04CE)); // Not verified
  }

  class PopMenuTextField extends JTextField {
    private Map<String,String> shortcuts = new LinkedHashMap<>();
    {
      shortcuts.put("Identify", "*IDN?");
      shortcuts.put("Clear Error", "*CLS");
      shortcuts.put("Self Test", "*TST?");
      shortcuts.put("DS4024/Screen Capture", ":DISPlay:DATA?");
      shortcuts.put("DG4162/Screen Capture", ":HCOPy:SDUMp:DATA?");
      shortcuts.put("DM3058/Measure DC Voltage", ":FUNCtion:VOLTage:DC;*WAI;:MEASure:VOLTage:DC?");
      shortcuts.put("DM3058/Measure AC Voltage", ":FUNCtion:VOLTage:AC;*WAI;:MEASure:VOLTage:AC?");
      shortcuts.put("DM3058/Measure Resistance", ":FUNCtion:RESistance;*WAI;:MEASure:RESistance?");
    }

    PopMenuTextField (JComboBox<Rigol> select) {
      setToolTipText("Right click for shortcut commands");
      addMouseListener(new MouseAdapter() {
        public void mouseReleased (MouseEvent ev1) {
          processMouse(ev1);
        }
        public void mousePressed (MouseEvent ev1) {
          processMouse(ev1);
        }
        void processMouse (MouseEvent ev1) {
          if (ev1.isPopupTrigger()) {
            Rigol item = (Rigol) select.getSelectedItem();
            JPopupMenu menu = new JPopupMenu();
            boolean addSep = true;
            for (String key : shortcuts.keySet()) {
              String[] parts = key.split("/");
              JMenuItem menuItem = null;
              if (parts.length == 1) {
                // Add common commands
                menuItem = new JMenuItem(key);
              } else if (parts.length == 2 && item != null) {
                if (addSep) {
                  menu.addSeparator();
                  addSep = false;
                }
                // Add device-specific commands
                String[] tmp = item.name.split(" ");
                if (tmp.length >= 2 && tmp[0].equals(parts[0])) {
                  menuItem = new JMenuItem(parts[1]);
                }
              }
              if (menuItem != null) {
                menu.add(menuItem);
                menuItem.addActionListener(ev2 -> {
                  setText(shortcuts.get(key));
                  runCommand();
                });
              }
            }
            menu.show(ev1.getComponent(), ev1.getX(), ev1.getY());
          }
        }
      });
    }
  }

  public static void main (String[] args) {
    new RglComm();
  }

  private void doCommand () {
    String cmd = command.getText();
    if ("scan".equalsIgnoreCase(cmd)) {
      appendLine(RglScan.doScan());
    } else {
      running = true;
      try {
        Rigol sel = (Rigol) select.getSelectedItem();
        if (sel == null) {
          return;
        }
        usb = new USBIO(sel.vend, sel.prod);
        command.setText("");
        String[] parts = cmd.split(";");
        for (String part : parts) {
          byte[] rsp = sendCmd(part);
          if (rsp != null) {
            if (isDataBlock(rsp)) {
              String prefix = new String(Arrays.copyOf(rsp, 11));
              byte[] body = Arrays.copyOfRange(rsp, 11, rsp.length);
              if (body[0] == 'B' && body[1] == 'M') {
                // :DISPlay:DATA?
                appendLine("Rsp: bitmap received: " + prefix);
                new ImageViewer(prefs, body);
              } else if (body[0] == (byte) 0xFF && body[1] == (byte) 0xD8) {
                // :HCOPy:SDUMp:DATA?
                appendLine("Rsp: data block received: " + prefix);
                new ImageViewer(prefs, body);
              }
            } else {
              if (rsp.length > 0) {
                appendLine("Rsp: " + new String(rsp).trim());
              }
            }
          }
        }
      } catch (Exception ex) {
        appendLine("Err: " + ex.toString());
        ex.printStackTrace();
      } finally {
        if (usb != null) {
          usb.close();
        }
        running = false;
      }
    }
  }

  private boolean isDataBlock (byte[] data) {
    if (data[0] == '#' && data.length >= 11) {
      for (int ii = 1; ii < 11; ii++) {
        if (data[ii] < '0' || data[ii] > '9') {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  private RglComm () {
    super("RglComm");
    select = new JComboBox<>(devices.toArray(new Rigol[0]));
    text.setFont(getCodeFont(12));
    text.setColumns(40);
    text.setRows(20);
    text.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    text.setEditable(false);
    JScrollPane scroll = new JScrollPane(text, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    add(scroll, BorderLayout.CENTER);
    JPanel controls = new JPanel(new FlowLayout());
    command = new PopMenuTextField(select);
    command.setText("*IDN?");
    command.setColumns(30);
    controls.add(command);
    try {
      select.setSelectedIndex(prefs.getInt("select", 0));
    } catch (Exception ex) {
      // Ignore
    }
    select.addActionListener(ev -> prefs.putInt("select", select.getSelectedIndex()));
    controls.add(select);
    JButton run = new JButton("RUN");
    run.addActionListener(e -> runCommand());
    command.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent ev) {
        if(ev.getKeyCode() == KeyEvent.VK_ENTER) {
          runCommand();
        }
      }
    });
    controls.add(run);
    add(controls, BorderLayout.SOUTH);
    setLocationRelativeTo(null);
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    pack();
    setLocation(prefs.getInt("window.x", 10), prefs.getInt("window.y", 10));
    // Track window resize/move events and save in prefs
    addComponentListener(new ComponentAdapter() {
      public void componentMoved (ComponentEvent ev)  {
        Rectangle bounds = ev.getComponent().getBounds();
        prefs.putInt("window.x", bounds.x);
        prefs.putInt("window.y", bounds.y);
      }
    });
    setVisible(true);
  }

  private void runCommand () {
    if (!running){
      Thread worker = new Thread(this::doCommand);
      worker.start();
    }
  }

  private static Font getCodeFont (int points) {
    String os = System.getProperty("os.name").toLowerCase();
    if (os.contains("win")) {
      return new Font("Consolas", Font.PLAIN, points);
    } else if (os.contains("mac")) {
      return new Font("Menlo", Font.PLAIN, points);
    } else if (os.contains("linux")) {
      return new Font("Courier", Font.PLAIN, points);
    } else {
      return new Font("Courier", Font.PLAIN, points);
    }
  }

  private byte[] sendCmd (String cmd) {
    appendLine("Snd: " + cmd);
    cmd += '\n';
    //System.out.print("Block Size: " + usb.maxPkt);
    // Note: making blockSize larger than 128 breaks communication with some devices
    int blockSize = Math.min(usb.maxPkt, 512);
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    for (int idx = 0; idx < cmd.length(); idx += blockSize) {
      buf.reset();
      int pktSize = Math.min(blockSize, cmd.length() - idx);
      byte term = (byte) (idx + blockSize >= cmd.length() ? 0x01 : 0x00);
      bTag++;
      buf.write(1);                 //  0: MsgID
      buf.write(bTag);              //  1: bTag
      buf.write(bTag ^ 0xFF);       //  2: bTagInverse
      buf.write(0x00);              //  3: Reserved
      buf.write(pktSize & 0xFF);    //  4: TransferSize
      buf.write(pktSize >> 8);      //  5: TransferSize
      buf.write(0x00);              //  6: TransferSize
      buf.write(0x00);              //  7: TransferSize
      buf.write(term);              //  8: bmTransfer Attributes (EOM is set on last packet)
      buf.write(0x00);              //  9: Reserved(0x00)
      buf.write(0x00);              // 10: Reserved(0x00)
      buf.write(0x00);              // 11: Reserved(0x00)
      for (int ii = 0; ii < pktSize; ii++) {
        buf.write(cmd.charAt(idx + ii));
      }
      while ((buf.size() & 0x03) != 0) {
        buf.write(0x00);          // Pad to multiple of 4
      }
      if (buf.size() > blockSize) {
        throw new IllegalStateException("buf.size(): " + buf.size() + " > " + "blockSize" + blockSize);
      }
      usb.send(buf.toByteArray());
    }
    System.out.println();
    if (cmd.contains("?")) {
      bTag++;
      ByteArrayOutputStream rec = new ByteArrayOutputStream();
      buf.reset();
      buf.write(2);                 //  0: MsgID
      buf.write(bTag);              //  1: bTag
      buf.write(bTag ^ 0xFF);       //  2: bTagInverse
      buf.write(0x00);              //  3: Reserved
      buf.write(blockSize & 0xFF);  //  4: TransferSize
      buf.write(blockSize >> 8);    //  5: TransferSize
      buf.write(0x00);              //  6: TransferSize
      buf.write(0x00);              //  7: TransferSize
      buf.write(0x00);              //  8: bmTransfer Attributes
      buf.write(0x00);              //  9: Reserved(0x00)
      buf.write(0x00);              // 10: Reserved(0x00)
      buf.write(0x00);              // 11: Reserved(0x00)
      byte[] data;
      do {
        if (buf.size() > blockSize) {
          throw new IllegalStateException("buf.size(): " + buf.size() + " > " + "blockSize" + blockSize);
        }
        usb.send(buf.toByteArray());
        // delay(50);
        data = usb.receive();
        int size = ((int) data[4] & 0xFF) + (((int) data[5] & 0xFF) << 8);
        for (int ii = 0; ii < size; ii++) {
          rec.write(data[12 + ii]);
        }
      } while (data[8] == 0);
      return rec.toByteArray();
    }
    return null;
  }

  private void appendLine (String line) {
    text.append(line + "\n");
    text.setCaretPosition(text.getDocument().getLength());
  }
}
