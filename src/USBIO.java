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
  private Context           context = new Context();
  private byte              iFace, outEnd, inEnd;

  USBIO (short vendorId, short productId, byte iFace, byte outEnd, byte inEnd) {
    this.iFace = iFace;
    this.outEnd = outEnd;
    this.inEnd = inEnd;
    int error = LibUsb.init(context);
    if (error != LibUsb.SUCCESS) {
      throw new LibUsbException("Unable to initialize libusb", error);
    }
    DeviceList list = new DeviceList();
    if ((error = LibUsb.getDeviceList(context, list)) < 0) {
      throw new LibUsbException("Unable to get device list", error);
    }
    for (Device device : list) {
      DeviceDescriptor desc = new DeviceDescriptor();
      LibUsb.getDeviceDescriptor(device, desc);
      if (desc.idVendor() == vendorId && desc.idProduct() == productId) {
        handle = new DeviceHandle();
        if ((error = LibUsb.open(device, handle)) >= 0) {
          if ((error = LibUsb.claimInterface(handle, iFace)) == LibUsb.SUCCESS) {
            return;
          } else {
            if (LibUsb.detachKernelDriver(handle, iFace) == LibUsb.SUCCESS) {
              if ((error = LibUsb.claimInterface(handle, iFace)) == LibUsb.SUCCESS) {
                return;
              }
              throw new LibUsbException("Unable to claim interface", error);
            }
          }
        }
      }
    }
    throw new LibUsbException("Unable to open device", error);
  }

  /*
   *  Bus: 000 Device 0x024: Vendor 0x1AB1, Product 09C4
   *   Interface: 0
   *     BLK add: 0x01 (OUT) pkt: 64
   *     BLK add: 0x82 (IN)  pkt: 64
   *     INT add: 0x83 (IN)  pkt: 8
   */

  /*
      ByteBuffer buffer = ByteBuffer.allocateDirect(8);
      buffer.put(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 });
      IntBuffer transfered = IntBuffer.allocate(1);
      int result = LibUsb.bulkTransfer(handle, 0x03, buffer, transfered, timeout);
      if (result != LibUsb.SUCCESS) throw new LibUsbException("Control transfer failed", transfered);
      System.out.println(transfered.get() + " bytes sent");

      Rigol Technologies,DM3058,DM3L180200001,01.01.00.02.02.00\n
   */

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
