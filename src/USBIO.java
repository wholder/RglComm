import org.usb4java.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import static org.usb4java.LibUsb.ERROR_NOT_FOUND;

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
  short                     maxPkt;
  private boolean           handleOpen, contextOpen, interfaceOpen;

  USBIO (short vendorId, short productId) {
    context = new Context();
    int result = LibUsb.init(context);
    contextOpen = true;
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
      if (!"hub".equalsIgnoreCase(usbClass) && vend == vendorId) {
        if (prod == productId) {
          handle = new DeviceHandle();
          byte numConfigs = desc.bNumConfigurations();
          for (byte ii = 0; ii < numConfigs; ii++) {
            ConfigDescriptor cDesc = new ConfigDescriptor();
            if (LibUsb.getConfigDescriptor(device, ii, cDesc) >= 0) {
              Interface[] ifaces = cDesc.iface();
              for (Interface iface : ifaces) {
                InterfaceDescriptor[] iDescs = iface.altsetting();
                for (InterfaceDescriptor iDesc : iDescs) {
                  this.iFace = iDesc.bInterfaceNumber();
                  byte numEndpoints = iDesc.bNumEndpoints();
                  if (numEndpoints > 0) {
                    EndpointDescriptor[] eDescs = iDesc.endpoint();
                    for (EndpointDescriptor eDesc : eDescs) {
                      byte endAdd = eDesc.bEndpointAddress();
                      byte eAttr = eDesc.bmAttributes();
                      if ((eAttr & 0x03) == 2) {
                        if ((endAdd & 0x80) != 0) {
                          this.inEnd = endAdd;
                        } else {
                          maxPkt = eDesc.wMaxPacketSize();
                          this.outEnd = endAdd;
                        }
                      }
                    }
                  }
                }
              }
            }
          }
          if ((result = LibUsb.open(device, handle)) >= 0) {
            handleOpen = true;
            if ((result = LibUsb.claimInterface(handle, iFace)) == LibUsb.SUCCESS) {
              interfaceOpen = true;
              return;
            } else {
              if (LibUsb.detachKernelDriver(handle, iFace) == LibUsb.SUCCESS) {
                if ((result = LibUsb.claimInterface(handle, iFace)) == LibUsb.SUCCESS) {
                  interfaceOpen = true;
                  return;
                }
                throw new LibUsbException("Unable to claim interface", result);
              }
            }
            LibUsb.close(handle);
            handleOpen = false;
          }
        }
      }
    }
    LibUsb.exit(context);
    throw new LibUsbException("Unable to open selected device", result < 0 ? result : ERROR_NOT_FOUND);
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
    ByteBuffer inBuf = ByteBuffer.allocateDirect(maxPkt + 12).order(ByteOrder.LITTLE_ENDIAN);
    IntBuffer inNum = IntBuffer.allocate(1);                                // Used to get bytes read count
    int error;
    int retry = 3;
    do {
      if ((error = LibUsb.bulkTransfer(handle, inEnd, inBuf, inNum, TIMEOUT)) >= 0) {
        if (inBuf.hasArray()) {
          return inBuf.array();
        } else {
          int cnt = inNum.get(0);
          int cap = inBuf.capacity();
          byte[] data = new byte[cnt];
          for (int ii = 0; ii < cnt && ii < cap; ii++) {
            data[ii] = inBuf.get();
          }
          inBuf.clear();
          return data;
        }
      }
    } while (error == -7 && --retry > 0);
    throw new LibUsbException("Unable to receive data", error);
  }

  void close () {
    try {
      if (interfaceOpen && handleOpen) {
        int error = LibUsb.releaseInterface(handle, iFace);
        if (error != LibUsb.SUCCESS) {
          throw new LibUsbException("Unable to release interface", error);
        }
      }
    } finally {
      if (handleOpen) {
        LibUsb.close(handle);
        handleOpen = false;
      }
      if (contextOpen) {
        LibUsb.exit(context);
        contextOpen = false;
      }
    }
  }
}
