import org.usb4java.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/**
 *  Implements a bulk transfer I/O driver that uses usb4java to communicate with a USB Device
 *  using the Usb4Java Library.
 *
 *  See: http://usb4java.org, and http://usb4java.org/apidocs/index.html for more info
 *
 *  http://libusb.sourceforge.net/api-1.0/
 */

class USBIO {
  private static final int  TIMEOUT = 500;
  private DeviceHandle      handle;
  private Context           context;
  private byte              iFace, outEnd, inEnd;

  USBIO (short vendorId, short productId,  byte iFace, byte outEnd, byte inEnd) {
    this.iFace = iFace;
    this.outEnd = outEnd;
    this.inEnd = inEnd;
    context = new Context();
    int result = LibUsb.init(context);
    if (result < 0) {
      throw new LibUsbException("Unable to initialize libusb", result);
    }
    DeviceList list = new DeviceList();
    if ((result = LibUsb.getDeviceList(context, list)) < 0) {
      throw new LibUsbException("Unable to get device list", result);
    }
    for (Device device : list) {
      DeviceDescriptor desc = new DeviceDescriptor();
      LibUsb.getDeviceDescriptor(device, desc);
      String usbClass = DescriptorUtils.getUSBClassName(desc.bDeviceClass());
      short vend = desc.idVendor();
      short prod = desc.idProduct();
      if (!"hub".equalsIgnoreCase(usbClass) && vend == vendorId && prod == productId) {
        handle = new DeviceHandle();
        if ((result = LibUsb.open(device, handle)) >= 0) {
          if ((result = LibUsb.claimInterface(handle, iFace)) == LibUsb.SUCCESS) {
            return;
          } else {
            if (LibUsb.detachKernelDriver(handle, iFace) == LibUsb.SUCCESS) {
              if ((result = LibUsb.claimInterface(handle, iFace)) == LibUsb.SUCCESS) {
                return;
              }
              throw new LibUsbException("Unable to claim interface", result);
            }
          }
          LibUsb.close(handle);
        }
      }
    }
    throw new LibUsbException("Unable to open device", result);
  }

  private String getPrefix (String val) {
    String[] vals = val.split(" ");
    return vals[0];
  }

  void send (byte[] data) {
    ByteBuffer outBuf = BufferUtils.allocateByteBuffer(data.length);
    outBuf.put(data);
    IntBuffer outNum = IntBuffer.allocate(1);
    int error;
    if ((error = LibUsb.bulkTransfer(handle, outEnd, outBuf, outNum, TIMEOUT)) < 0) {
      throw new LibUsbException("Unable to send data", error);
    }
  }

  byte[] receive () {
    ByteBuffer inBuf = ByteBuffer.allocateDirect(64).order(ByteOrder.LITTLE_ENDIAN);
    IntBuffer inNum = IntBuffer.allocate(1);                                // Used to get bytes read count
    int error;
    if ((error = LibUsb.bulkTransfer(handle, inEnd, inBuf, inNum, TIMEOUT)) >= 0) {
      if (inBuf.hasArray()) {
        return inBuf.array();
      } else {
        int cnt = inNum.get(0);
        byte[] data = new byte[cnt];
        for (int ii = 0; ii < cnt; ii++) {
          data[ii] = inBuf.get();
        }
        inBuf.clear();
        return data;
      }
    }
    throw new LibUsbException("Unable to receive data", error);
  }

  void close () {
    try {
      int error = LibUsb.releaseInterface(handle, iFace);
      if (error != LibUsb.SUCCESS) {
        throw new LibUsbException("Unable to release interface", error);
      }
    } finally {
      LibUsb.close(handle);
      LibUsb.exit(context);
    }
  }
}
