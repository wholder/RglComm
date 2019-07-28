# RigolComm

<p align="center"><img src="https://github.com/wholder/RigolComm/blob/master/images/RigolComm%20Screenshot.png"></p>

**RigolComm** is a GUI-based program written in the Java Language that I created to experiment with communicating with and controlling Rigol devices using IEEE 488 Commands sent over the instument's USB interface.  My eventnual goal is to use this code as the basis for a program that can run simple scripts to make various measurements and perform calculations.  However, I'm publishing it here so that others can learn the basic techniques needed to use usb4java to communicate these kinds of devices.  The following is a list of example commands that can command a Rigol DM3058 Digital Multimeter to perform various checks and measurements:

 - **`*IDN?`** - Queries the equipment ID.
 - **`*RST`**  - Resets the instrument
 - **`:MEASure:VOLTage:DC?`** - Measure DC Voltage
 - **`:MEASure:VOLTage:AC?`** - Measure AC Voltage
 - **`:MEASure:CURRent:DC?`** - Measure DC Current
 - **`:MEASure:CURRent:AC?`** - Measure AC Current
 - **`:MEASure:RESistance?`** - Measure Resistance
 
 Note: while I designed and tested RigolComm with devices made by Rigol Technologies, it might also work with other devices that support IEEE 488 Commands sent over the devices's USB interface.  However, at the moment, I have onlu tested it with the following devices:
 
  - Rigol DM3058 Digital Multimeter
  - DP832 Prog DC Power Supply
  - DS4024 Digital Oscilloscope
  - DS1102E Digital Oscilloscope
  - DSA815 Spectrum Analyzer
  - DG4162 Func/Wave Generator
  
If you wish to use other devices, you will need to add them to the "devices" Map in RigolComm.java.  The utility program RigolScan.java (included in .jar file) can be used to scan for Rigol devices that are powered on and connected to the computer.  You can run it from the command line like this:

  **`java -cp RigolComm.jar RigolScan`**
 
## Caution
Be careful when usiing commends that switch modes, such as issuing a **`:MEASure:CURRent:DC?`** command when the instrument is connected to a voltage source, as this can damage the instrument.

### Requirements
Java 8 JDK, or later must be installed in order to compile the code.  There is also a [**Runnable JAR file**](https://github.com/wholder/RigolComm/blob/master/out/artifacts/RigolComm_jar) included in the checked in code that you can download.   On a Mac, just double click the **`RigolComm.jar`** file and it should start.  However, you'll probably have to right click and select "Open" the  first time you run RigolComm due to new Mac OS X security checks.  You should also be able to run the JAR file on Windows or Linux systems, but you'll need to have a Java 8 JRE, or later installed and follow the appropriate process for each needed to run an executable JAR file.

## RigolComm
RigolComm uses the following Java code to perform some of its functions, or build this project:
- [Usb4Java](http://usb4java.org) is used to perform low-level USB I/O using the USBTMC protocol
- [IntelliJ IDEA from JetBrains](https://www.jetbrains.com/idea/) (my favorite development environment for Java coding. Thanks JetBrains!)
