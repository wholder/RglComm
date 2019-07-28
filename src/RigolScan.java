import org.usb4java.*;

class RigolScan {
  static String doScan () {
    StringBuilder buf = new StringBuilder();
    Context context = new Context();
    int result = LibUsb.init(context);
    if (result < 0) {
      throw new LibUsbException("Unable to initialize libusb", result);
    }
    DeviceList list = new DeviceList();
    result = LibUsb.getDeviceList(context, list);
    if (result < 0) {
      throw new LibUsbException("Unable to get device list", result);
    }
    try {
      boolean deviceFound = false;
      for (Device device : list) {
        int address = LibUsb.getDeviceAddress(device);
        int busNumber = LibUsb.getBusNumber(device);
        DeviceDescriptor desc = new DeviceDescriptor();
        result = LibUsb.getDeviceDescriptor(device, desc);
        byte numConfigs = desc.bNumConfigurations();
        if (result < 0) {
          throw new LibUsbException("Unable to read device descriptor", result);
        }
        String usbClass = DescriptorUtils.getUSBClassName(desc.bDeviceClass());
        short vend = desc.idVendor();
        short prod = desc.idProduct();
        if (!"hub".equalsIgnoreCase(usbClass) && vend == (short) 0x1AB1) {
          deviceFound = true;
          buf.append(String.format("\nVendor 0x%04X, Product 0x%04X%n", vend, prod));
          DeviceHandle handle = new DeviceHandle();
          if (LibUsb.open(device, handle) >= 0) {
            buf.append("Manufacturer: ");
            buf.append(LibUsb.getStringDescriptor(handle, desc.iManufacturer()));
            buf.append("\nProduct:      ");
            buf.append(LibUsb.getStringDescriptor(handle, desc.iProduct()));
            buf.append("\nSerialNumber: ");
            buf.append(LibUsb.getStringDescriptor(handle, desc.iSerialNumber()));
            buf.append("\n");
            LibUsb.close(handle);
          }
          for (byte ii = 0; ii < numConfigs; ii++) {
            ConfigDescriptor cDesc = new ConfigDescriptor();
            if (LibUsb.getConfigDescriptor(device, ii, cDesc) >= 0) {
              Interface[] ifaces = cDesc.iface();
              for (Interface iface : ifaces) {
                InterfaceDescriptor[] iDescs = iface.altsetting();
                for (InterfaceDescriptor iDesc : iDescs) {
                  byte iNum = iDesc.bInterfaceNumber();
                  byte numEndpoints = iDesc.bNumEndpoints();
                  if (numEndpoints > 0) {
                    buf.append("  Interface: ").append(iNum).append("\n");
                    EndpointDescriptor[] eDescs = iDesc.endpoint();
                    for (EndpointDescriptor eDesc : eDescs) {
                      byte endAdd = eDesc.bEndpointAddress();
                      byte eAttr = eDesc.bmAttributes();
                      int maxPkt = eDesc.wMaxPacketSize();
                      String[] tTypes = {"CON", "ISO", "BLK", "INT"};
                      String tType = tTypes[eAttr & 0x03];
                      String dir = (endAdd & 0x80) != 0 ? " (IN) " : " (OUT)";
                      buf.append("    ").append(tType).append(" add: ");
                      buf.append(String.format("0x%02X", endAdd)).append(dir).append(" pkt: ").append(maxPkt).append("\n");
                    }
                  }
                }
              }
            }
          }
        }
      }
      if (!deviceFound) {
        buf.append("No Rigol devices detected\n");
      }
    } catch (Exception ex) {
      buf.append(ex.getMessage());
    } finally {
      LibUsb.freeDeviceList(list, true);
    }
    LibUsb.exit(context);
    return buf.toString();
  }

  public static void main (String[] args) {
    System.out.println(doScan());
  }
}
