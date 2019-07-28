import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.prefs.Preferences;

/*
 *  Test Program to communicate with and control Rigol devices using IEEE 488 Commands
 *
 *  Author: Wayne Holder, 2019
 *  License: MIT (https://opensource.org/licenses/MIT)
 */

public class RigolComm extends JFrame {
  private transient Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
  private static List<Rigol>    devices = new LinkedList<>();
  private JTextArea             text = new JTextArea();
  private JTextField            command;
  private JComboBox<Rigol>      select;
  private USBIO                 usb;
  private byte                  bTag;



  static class Rigol {
    String  name;
    short   vend, prod;
    byte    intFace, outEnd, inEnd;

    Rigol (String name, int vend, int prod, int intFace, int outEnd, int inEnd) {
      this.name = name;
      this.vend = (short) vend;
      this.prod = (short) prod;
      this.intFace = (byte) intFace;
      this.outEnd = (byte) outEnd;
      this.inEnd = (byte) inEnd;
    }

    public String toString () {
      return name;
    }
  }

  /*
   *   Vendor 0x1AB1, Product 0x09C4
   *   Manufacturer: Rigol Technologies
   *   Product:      DM3000 SERIES
   *     Interface: 0
   *       BLK add: 0x01 (OUT) pkt: 64
   *       BLK add: 0x82 (IN)  pkt: 64
   *       INT add: 0x83 (IN)  pkt: 8
   *
   *   Vendor 0x1AB1, Product 0x0E11
   *   Manufacturer: Rigol Technologies.
   *   Product:      DP800 Serials
   *     Interface: 0
   *       INT add: 0x81 (IN)  pkt: 64
   *       BLK add: 0x82 (IN)  pkt: 512
   *       BLK add: 0x03 (OUT) pkt: 512
   *
   *   Vendor 0x1AB1, Product 0x04B1
   *   Manufacturer: Rigol Technologies
   *   Product:      DS4000 Series
   *     Interface: 0
   *       BLK add: 0x85 (IN)  pkt: 512
   *       BLK add: 0x06 (OUT) pkt: 512
   *       INT add: 0x81 (IN)  pkt: 64*
   *
   *   Vendor 0x1AB1, Product 0x0588
   *   Manufacturer: Rigol Technologies
   *   Product:      DS1000 SERIES
   *     Interface: 0
   *       BLK add: 0x01 (OUT) pkt: 64
   *       BLK add: 0x82 (IN)  pkt: 64
   *       INT add: 0x83 (IN)  pkt: 8
   *
   *   Vendor 0x1AB1, Product 0x0641
   *   Manufacturer: Rigol Technologies
   *   Product:      DG4162
   *     Interface: 0
   *       INT add: 0x88 (IN)  pkt: 64
   *       BLK add: 0x02 (OUT) pkt: 512
   *       BLK add: 0x86 (IN)  pkt: 512
   *
   *   Vendor 0x1AB1, Product 0x0960
   *   Manufacturer: Rigol Technologies
   *   Product:      DSA815
   *     Interface: 0
   *       INT add: 0x88 (IN)  pkt: 64
   *       BLK add: 0x02 (OUT) pkt: 512
   *       BLK add: 0x86 (IN)  pkt: 512
   */

  static {
    //                     Selector Description             Vend    Prod  I   OUT    IN
    devices.add(new Rigol("DM3058 Digital Multimeter",    0x1AB1, 0x09C4, 0, 0x01, 0x82));
    devices.add(new Rigol("DP832 Prog DC Power Supply",   0x1AB1, 0x0E11, 0, 0x03, 0x82));
    devices.add(new Rigol("DS4024 Digital Oscilloscope",  0x1AB1, 0x04B1, 0, 0x06, 0x85));
    devices.add(new Rigol("DS1102E Digital Oscilloscope", 0x1AB1, 0x0588, 0, 0x01, 0x82));
    devices.add(new Rigol("DSA815 Spectrum Analyzer",     0x1AB1, 0x0960, 0, 0x02, 0x86));
    devices.add(new Rigol("DG4162 Func/Wave Generator",   0x1AB1, 0x0641, 0, 0x02, 0x86));
  }

  public static void main (String[] args) {
    new RigolComm();
  }

  private void doCommand () {
    try {
      Rigol sel = (Rigol) select.getSelectedItem();
      if (sel == null)
        return;
      usb = new USBIO(sel.vend, sel.prod, sel.intFace, sel.outEnd, sel.inEnd);
      String cmd = command.getText();
      //command.setText("");
      String rsp = sendCmd(cmd);
      if (rsp != null && rsp.length() > 0) {
        appendLine("Rsp: " + rsp.trim());
      }
    } catch (Exception ex) {
      appendLine(ex.getMessage());
      ex.printStackTrace();
    } finally {
      if (usb != null) {
        usb.close();
      }
    }
  } 

  private RigolComm () {
    super("RigolComm");
    text.setColumns(30);
    text.setRows(20);
    text.setFont(new Font("Monaco", Font.PLAIN, 12));
    text.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    JScrollPane scroll = new JScrollPane(text, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    add(scroll, BorderLayout.CENTER);
    JPanel controls = new JPanel(new FlowLayout());
    command = new JTextField();
    command.setText("*IDN?");
    command.setColumns(30);
    controls.add(command);
    select = new JComboBox<>(devices.toArray(new Rigol[0]));
    try {
      select.setSelectedIndex(prefs.getInt("select", 0));
    } catch (Exception ex) {}
    select.addActionListener(ev -> prefs.putInt("select", select.getSelectedIndex()));
    controls.add(select);
    JButton run = new JButton("RUN");
    run.addActionListener(e -> startTests());
    command.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent ev) {
        if(ev.getKeyCode() == KeyEvent.VK_ENTER) {
          startTests();
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

  private void startTests () {
    Thread worker = new Thread(this::doCommand);
    worker.start();
  }

  private String sendCmd (String cmd) {
    bTag++;
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    buf.write(1);             //  0: MsgID
    buf.write(bTag);          //  1: bTag
    buf.write(bTag ^ 0xFF);   //  2: bTagInverse
    buf.write(0x00);          //  3: Reserved
    buf.write(cmd.length());  //  4: TransferSize
    buf.write(0x00);          //  5: TransferSize
    buf.write(0x00);          //  6: TransferSize
    buf.write(0x00);          //  7: TransferSize
    buf.write(0x01);          //  8: bmTransfer Attributes (EOM is set)
    buf.write(0x00);          //  9: Reserved(0x00)
    buf.write(0x00);          // 10: Reserved(0x00)
    buf.write(0x00);          // 11: Reserved(0x00)
    for (int ii = 0; ii < cmd.length(); ii++) {
      buf.write(cmd.charAt(ii));
    }
    while ((buf.size() & 0x03) != 0) {
      buf.write(0x00);        // Padding
    }
    appendLine("Snd: " + cmd);
    usb.send(buf.toByteArray());
    if (cmd.contains("?")) {
      try {
        // Allow time for instument to switch mode, if needed
        Thread.sleep(200);
      } catch (InterruptedException ex) {
        ex.printStackTrace();
      }
      bTag++;
      StringBuilder rec = new StringBuilder();
      buf.reset();
      buf.write(2);             //  0: MsgID
      buf.write(bTag);          //  1: bTag
      buf.write(bTag ^ 0xFF);   //  2: bTagInverse
      buf.write(0x00);          //  3: Reserved
      buf.write(32-12);         //  4: TransferSize
      buf.write(0x00);          //  5: TransferSize
      buf.write(0x00);          //  6: TransferSize
      buf.write(0x00);          //  7: TransferSize
      buf.write(0x00);          //  8: bmTransfer Attributes (EOM is set)
      buf.write(0x00);          //  9: Reserved(0x00)
      buf.write(0x00);          // 10: Reserved(0x00)
      buf.write(0x00);          // 11: Reserved(0x00)
      byte[] data;
      do {
        usb.send(buf.toByteArray());
        data = usb.receive();
        int size = data[4];
        for (int ii = 0; ii < size; ii++) {
          rec.append((char) data[12 + ii]);
        }
      } while (data[8] == 0);
      return rec.toString();
    }
    return null;
  }

  private void appendLine (String line) {
    text.append(line + "\n");
    text.setCaretPosition(text.getDocument().getLength());
  }
}
