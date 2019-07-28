import org.usb4java.*;

class RigolScan {
  private static String doScan () {
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
        DeviceDescriptor descriptor = new DeviceDescriptor();
        result = LibUsb.getDeviceDescriptor(device, descriptor);
        byte numConfigs = descriptor.bNumConfigurations();
        if (result < 0) {
          throw new LibUsbException("Unable to read device descriptor", result);
        }
        String usbClass = DescriptorUtils.getUSBClassName(descriptor.bDeviceClass());
        short vendor = descriptor.idVendor();
        if (!"hub".equalsIgnoreCase(usbClass) && vendor == (short) 0x1AB1) {
          deviceFound = true;
          buf.append(String.format("Bus: %03d Device 0x%03d: Vendor 0x%04X, Product %04X%n",
                                   busNumber, address, vendor, descriptor.idProduct()));
          DeviceHandle handle = new DeviceHandle();
          if (LibUsb.open(device, handle) >= 0) {
            String sManufacturer = LibUsb.getStringDescriptor(handle, descriptor.iManufacturer());
            String sProduct = LibUsb.getStringDescriptor(handle, descriptor.iProduct());
            String sSerialNumber = LibUsb.getStringDescriptor(handle, descriptor.iSerialNumber());
            buf.append("Manufacturer: ");
            buf.append(sManufacturer);
            buf.append("\nProduct:      ");
            buf.append(sProduct);
            buf.append("\nSerialNumber: ");
            buf.append(sSerialNumber);
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
