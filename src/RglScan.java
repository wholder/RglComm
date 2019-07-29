import org.usb4java.*;

class RglScan {
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
        DeviceDescriptor desc = new DeviceDescriptor();
        result = LibUsb.getDeviceDescriptor(device, desc);
        if (result < 0) {
          throw new LibUsbException("Unable to read device descriptor", result);
        }
        String usbClass = DescriptorUtils.getUSBClassName(desc.bDeviceClass());
        short vend = desc.idVendor();
        short prod = desc.idProduct();
        if (!"hub".equalsIgnoreCase(usbClass) && vend == (short) 0x1AB1) {
          deviceFound = true;
          buf.append(String.format("\nVendor Id:    0x%04X\nProduct Id:   0x%04X%n", vend, prod));
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
