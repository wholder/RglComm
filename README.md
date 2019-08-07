# RglComm

<p align="center"><img src="https://github.com/wholder/RglComm/blob/master/images/RglComm%20Screenshot.png"></p>

**RglComm** is a GUI-based program written in the Java Language that I created to experiment with communicating with and controlling Rigol™ devices using IEEE 488 Commands [USBTMC-USB488](http://sdpha2.ucsd.edu/Lab_Equip_Manuals/usbtmc_usb488_subclass_1_00.pdf) sent over the instrument's USB interface.  My eventual goal is to use this code as the basis for a program that can run simple scripts to make various measurements and perform calculations (sort of a poor man's LabVIEW™), so stay tuned if you're interested.  However, I'm publishing it here so that others can learn the basic techniques needed to use usb4java to communicate these kinds of devices.  My implementation of the USBTMC-USB488 protocol is just enough to enable RglComm to send commands and receive responses and does not implement all the details of the full specification.

To use RglComm, first select the device to communicate with using the selector, then type the command into the text field and press the Enter key, or press the "**`RUN`**" button.  Note; some devices, such as the Rigol DG4162 Function/Arbitrary Waveform Generator make need to be set to "PC" mode in the I/O menu before they will respond to commands:

#### Precautions
  - Be careful when using commends that switch measuring modes, such as issuing a **`:MEASure:CURRent:DC?`** command when the instrument is connected to a voltage source, as this can damage the instrument.
  - Using the **`MEASure`** command switch from one mode to another can sometimes result in a read timeout, as it takes time for the DM3058 to internally make the mode change.  So, it's better to first use a **`FUNCtion`** command t0 select the measurement mode before issuing a **`MEASure`** command.
  - Try to connect all instruments directly to the host computer, as adding intermediate USB hubs can interfere with communication and cause timeout errors.
  - Make sure the device's USB I/O Mode is set to '**`PC`**' and not to '**`Printer`**' as it's not possible to communicate with the device when set to Printer Mode.

#### Commands common to most instruments include:

  - **`*IDN?`** - Queries the equipment ID and returns a String of text info
  - **`*CLS`** - Clear Status Register
  - **`*RST`**  - Resets the instrument to factory defined condition
  - **`*WAI`** - Waits until all pending commands are completed, before executing any other commands
 
#### Commands for the Rigol DM3058 Digital Multimeter include:

  - **`:FUNCtion:VOLTage:DC`** - Sets DM3058 to measure DC Voltage
  - **`:MEASure:VOLTage:DC?`** - Measure DC Voltage
  - **`:FUNCtion:VOLTage:AC`** - Sets DM3058 to measure AC Voltage
  - **`:MEASure:VOLTage:AC?`** - Measure AC Voltage
  - **`:FUNCtion:CURRent:DC`** - Sets DM3058 to measure DC Current
  - **`:MEASure:CURRent:DC?`** - Measure DC Current
  - **`:FUNCtion:CURRent:AC`** - Sets DM3058 to measure AC Current
  - **`:MEASure:CURRent:AC?`** - Measure AC Current
  - **`:FUNCtion:RESistance`** - Sets DM3058 to measure Resistance
  - **`:MEASure:RESistance?`** - Measure Resistance
  
Note: the portions of the commands shown in lower case letters are optional and can be omitted.  So, for example, sending the command **`:FUNC:VOLT:DC`** is the same as sending the command **`:FUNCtion:VOLTage:DC`**.  Also, multiple commands can be entered as one line by separating each command with a '**_;_**' character.
  
 #### Commands for a Rigol DS4024 Digital Oscilloscope include:
 
  - **`:CHANnel1:COUPling?`** - Query coupling mode of Channel 1 (AC,DC,GND)
  - **`:CHANnel1:COUPling AC`** - Set coupling mode of Channel to AC
  - **`:CHANnel1:DISPlay?`** - Query display state of Channel 1 (1 = On, 0 = Off)
  - **`:CHANnel1:DISPlay 1`** - Enable display of Channel 1 
  - **`:CHANnel1:OFFSet?`** - Query vertical position of Channel 1
  - **`:CHANnel1:OFFSet -0.3`** - Set vertical position of Channel 1 to -300mV
  - **`:CHANnel1:SCALe?`** - Query vertical scale of Channel 1
  - **`:CHANnel1:SCALe 0.5`** - Set vertical scale of Channel 1 to 500mV
  - **`:TIMebase:SCALe?`** - Query Timebase Scale
  - **`:TIMebase:SCALe 0.00001`** - Set Timebase Scale to 10uS
  - **`:DISPlay:DATA?`** - Download and display Screen image from DS4024 (see below)
  
  <p align="center"><img src="https://github.com/wholder/RglComm/blob/master/images/DS4024%20Capture.png" width="55%" height="55%"></p>

 #### Commands for a Rigol DS1102E Digital Oscilloscope include:
 
  - **`:WAV:POIN:NOR;:WAVeform:DATA? CH1`** - Select normal mode then display waveform from DS1102E Ch 1 (see below)
  - **`:WAV:POIN:NOR;:WAVeform:DATA? CH2`** - Select normal mode then display waveform from DS1102E Ch 2

   <p align="center"><img src="https://github.com/wholder/RglComm/blob/master/images/DS1102E%20Capture.png" width="55%" height="55%"></p>

 #### Commands for a Rigol DG4162 Function/Arbitrary Waveform Generator include:
 
  - **`:SOURce1:FREQuency:FIXed 888888`** - Set Channel 1 Frequency to 888.888 kHz
  - **`:OUTPut1:STATe ON`** - Channel 1 Output On
  - **`:OUTPut1:STATe OFF`** - Channel 1 Output Off
  - **`:SOURce1:VOLTage?`** - Read Channel 1 Amplitude in Volts (pp)
  - **`:SOURce1:VOLTage 2.25`** - Channel 1 Amplitude to 2.25 Volts
  - **`:SOURce1:FUNCtion:SHAPe?`** - Read Selected Waveform Shape of Channel 1
  - **`:SOURce1:FUNCtion:SHAPe SQUare`** - Set Channel 1 Output to Square Wave
  - **`:SOURce1:FUNCtion:SHAPe SINusoid`** - Set Channel 1 Output to Sinusoid
  - **`:SOURce1:FUNCtion:SHAPe RAMP`** - Set Channel 1 Output to Ramp (Triangle)
  - **`:HCOPy:SDUMp:DATA?`** - Download and display Screen image from DG4162 (see below)
 
   <p align="center"><img src="https://github.com/wholder/RglComm/blob/master/images/DG4162%20Capture.png" width="40%" height="40%"></p>

#### Supported Devices

 Note: while I designed and tested RglComm with devices made by Rigol Technologies, it might also work with other devices that support IEEE 488 Commands sent over the devices's USB interface.  However, at the moment, I have only done basic testing with the following Rigol devices:
 
  - [**DM3058** Digital Multimeter](https://www.rigolna.com/products/digital-multimeters/dm3000/) - Vendor Id: **`1ab1`**, Product Id: **`09c4`**
  - [**DP832** Programmable DC Power Supply](https://www.rigolna.com/products/dc-power-loads/dp800/) - Vendor Id: **`1ab1`**, Product Id: **`0e11`**
  - [**DS4024** Digital Oscilloscope](https://www.rigolna.com/products/digital-oscilloscopes/4000/) - Vendor Id: **`1ab1`**, Product Id: **`04b1`**
  - [**DS1102E** Digital Oscilloscope](https://www.rigolna.com/products/digital-oscilloscopes/1000/) - Vendor Id: **`1ab1`**, Product Id: **`0588`**
  - [**DSA815** Spectrum Analyzer](https://www.rigolna.com/products/spectrum-analyzers/dsa800/) - Vendor Id: **`1ab1`**, Product Id: **`0960`**
  - [**DG4162** Function/Arbitrary Waveform Generator](https://www.rigolna.com/products/waveform-generators/dg4000/) - Vendor Id: **`1ab1`**, Product Id: **`0641`**
  
If you wish to use other devices, you will need to add them to the "devices" Map in RglComm.java.  The utility program RglScan.java (included in .jar file) can be used to scan for Rigol devices that are powered on and connected to the computer.  You can type "**`scan`**" into the command text field and then press either the Enter key, or the "**`RUN`**" button.  Or, you can run it from the command line like this:

  **`java -cp RglComm.jar RglScan`**
  
### **Requirements**
A [Java JDK or JVM](https://www.java.com/en/) or [OpenJDK](http://openjdk.java.net) version 8, or later must be installed in order to run the code.  There is also a [**Runnable JAR file**](https://github.com/wholder/RglComm/blob/master/out/artifacts/RglComm_jar) included in the checked in code that you can download and run without having to compile the cource code.

#### macOS
On a Mac, just double click the **`RglComm.jar`** file and it should start.  However, you'll need to right click on the .jar file and select "Open" the first time you run RglComm due to new Mac OS X security checks.  

#### Windows
Follow [these instructions](https://windowsreport.com/jar-file-windows/) to run the JAR file on Windows.  However, you will first need to install device drivers for the Rigol devices you are using.  The instructions on this page provides more details, but you can use the automated driver installer [Zadig](https://zadig.akeo.ie) to install a generic driver for each Rigol device using its Vendor and Product Ids, like this:

   <p align="center"><img src="https://github.com/wholder/RglComm/blob/master/images/zadig-2.4.png"></p>
  
#### Linux
  Follow [these instructions](https://itsfoss.com/run-jar-file-ubuntu-linux/)  to run the JAR file on a Linux system.  However, you will first need to setup the USB permissions for each device using a [udev-based USB permission rule](http://ask.xmodulo.com/change-usb-device-permission-linux.html).  For example, to set USB permissions for the DM3058 Digital Multimeter, create a file named something like "**`50-myusb.rules`**" in the "**`/lib/udev/rules.d/`**" directory that includes the following text:
  
    SUBSYSTEMS=="usb", ATTRS{idVendor}=="1ab1", ATTRS{idProduct}=="09c4", GROUP="users", MODE="0666"

This rule identifies the device by using its USB Vendor Id (**`1ab1`**) and Product Id (**`09c4`**).  Alternately, you can grant permissions for all Rigol devices by only specifying the Vendor Id (**`1ab1`**) in the file, such as:

    SUBSYSTEMS=="usb", ATTRS{idVendor}=="1ab1", GROUP="users", MODE="0666"

Then, save the file and restart Linux, or type this command to reload the udev rules:

    sudo udevadm control --reload-rules

## Credits
RglComm uses the following Java code to perform some of its functions, or build this project:
- [Usb4Java](http://usb4java.org) is used to perform low-level USB I/O using the USBTMC protocol
- [IntelliJ IDEA from JetBrains](https://www.jetbrains.com/idea/) (my favorite development environment for Java coding. Thanks JetBrains!)
