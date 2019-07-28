import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.List;

/*
 *  Test Program to communicate with and control Rigol devices using IEEE 488 Commands
 *
 *  Author: Wayne Holder, 2019
 *  License: MIT (https://opensource.org/licenses/MIT)
 */

public class RigolComm extends JFrame {
  private static List<Rigol>    devices = new LinkedList<>();
  private JTextArea             text = new JTextArea();
  private JTextField            command;
  private JComboBox<Rigol>      select;
  private boolean               clearCmd;
  private USBIO                 usb;

  static class Rigol {
    String  name;
    short   vend, prod;
    byte    intFace, outEnd, inEnd;

    Rigol (String name, short vend, short prod, byte intFace, byte outEnd, byte inEnd) {
      this.name = name;
      this.vend = vend;
      this.prod = prod;
      this.intFace = intFace;
      this.outEnd = outEnd;
      this.inEnd = inEnd;
    }

    public String toString () {
      return name;
    }
  }

  /*
   *   Bus: 000 Device 0x012: Vendor 0x1AB1, Product 09C4
   *   Manufacturer: Rigol Technologies
   *   Product:      DM3000 SERIES
   *   SerialNumber: DM3L180200001
   *     Interface: 0
   *       BLK add: 0x01 (OUT) pkt: 64
   *       BLK add: 0x82 (IN)  pkt: 64
   *       INT add: 0x83 (IN)  pkt: 8
   *
   *   Bus: 000 Device 0x013: Vendor 0x1AB1, Product 0E11
   *   Manufacturer: Rigol Technologies.
   *   Product:      DP800 Serials
   *   SerialNumber: DP8C181901451
   *     Interface: 0
   *       INT add: 0x81 (IN)  pkt: 64
   *       BLK add: 0x82 (IN)  pkt: 512
   *       BLK add: 0x03 (OUT) pkt: 512
   */

  static {
    devices.add(new Rigol("DM3058 Digital Multimeter",  (short) 0x1AB1, (short) 0x09C4, (byte) 0, (byte) 0x01, (byte) 0x82));
    devices.add(new Rigol("DP832 Prog DC Power Supply", (short) 0x1AB1, (short) 0x0E11, (byte) 0, (byte) 0x03, (byte) 0x82));
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
    setVisible(true);
  }

  private void startTests () {
    Thread worker = new Thread(this::doCommand);
    worker.start();
  }

  byte bTag;

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
