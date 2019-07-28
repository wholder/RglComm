# RigolComm

<p align="center"><img src="https://github.com/wholder/RigolComm/blob/master/images/RigolComm%20Screenshot.png"></p>

**RigolComm** is a GUI-based program written in the Java Language that I created to experiment with communicating with and controlling Rigol devices using IEEE 488 Commands [USBTMC-USB488](http://sdpha2.ucsd.edu/Lab_Equip_Manuals/usbtmc_usb488_subclass_1_00.pdf) sent over the instrument's USB interface.  My eventual goal is to use this code as the basis for a program that can run simple scripts to make various measurements and perform calculations (sort of a poor man's LabVIEW™), so stay tuned if you're interested.  However, I'm publishing it here so that others can learn the basic techniques needed to use usb4java to communicate these kinds of devices.  My implementation of the USBTMC-USB488 protocol is just enough to enable RigolComm to send commands and receive reponses and does not implement all the details of the full specification.

To use RigolComm, first select the device to communicate with using the selector, then type the command into the text field and press the Enter key, or press the "**`RUN`**" button.  Note; some devices, such as the Rigol DG4162 Function/Arbitrary Waveform Generator make need to be set to "PC" mode in the I/O menu before they will respond to commands:

#### Commands common to most instruments include:

  - **`*IDN?`** - Queries the equipment ID and returns a String of text info
  - **`*RST`**  - Resets the instrument to factory defined condition
  - **`*TST?`** - Run self tests (0 = passed, 1 = failed)
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
 
Caution: be careful when using commends that switch measuring modes, such as issuing a **`:MEASure:CURRent:DC?`** command when the instrument is connected to a voltage source, as this can damage the instrument.  Also, using the **`MEASure`** command switch from one mode to another may result in a read timeout, as it takes time for the DM3058 to internally make the mode change.  So, it's better to first use a **`FUNCtion`** command t0 select the measurment mode before issuing a **`MEASure`** command.

Also, the portions of the commands shown in lower case letters are optional and can be omitted.  So, for example, sending the command **`:FUNC:VOLT:DC`** is the same as sending the command **`:FUNCtion:VOLTage:DC`**.

#### Commands for a Rigol DG4162 Function/Arbitrary Waveform Generator include:
 
  - **`:SOURce1:FREQuency:FIXed 888888`** - Set Channel 1 Frequency to 888.888 kHz
  - **`:OUTPut1:STATe ON`** - Channel 1 Output On
  - **`:OUTPut1:STATe OFF`** - Channel 1 Output Off
  - **`:SOURce1:VOLTage?`** - Read Channel 1 Amplitude in Volts (pp)
  - **`:SOURce1:VOLTage 2.25`** - Channel 1 Amplitude to 2.25 Volts
  - **`:SOURce1:FUNCtion:SHAPe SQUare`** - Set Channel 1 Output to Square Wave
  - **`:SOURce1:FUNCtion:SHAPe SINusoid`** - Set Channel 1 Output to Sinusoid
  - **`:SOURce1:FUNCtion:SHAPe RAMP`** - Set Channel 1 Output to Ramp (Triangle)
 
 Note: while I designed and tested RigolComm with devices made by Rigol Technologies, it might also work with other devices that support IEEE 488 Commands sent over the devices's USB interface.  However, at the moment, I have only done basic testing with the following Rigol devices:
 
  - [**DM3058** Digital Multimeter](https://www.rigolna.com/products/digital-multimeters/dm3000/)
  - [**DP832** Prog DC Power Supply](https://www.rigolna.com/products/dc-power-loads/dp800/)
  - [**DS4024** Digital Oscilloscope](https://www.rigolna.com/products/digital-oscilloscopes/4000/)
  - [**DS1102E** Digital Oscilloscope](https://www.rigolna.com/products/digital-oscilloscopes/1000/)
  - [**DSA815** Spectrum Analyzer](https://www.rigolna.com/products/spectrum-analyzers/dsa800/)
  - [**DG4162** Func/Wave Generator](https://www.rigolna.com/products/waveform-generators/dg4000/)
  
If you wish to use other devices, you will need to add them to the "devices" Map in RigolComm.java.  The utility program RigolScan.java (included in .jar file) can be used to scan for Rigol devices that are powered on and connected to the computer.  You can type "**`scan`**" into the command text field and then press either the Enter key, or the "**`RUN`**" button.  Or, you can run it from the command line like this:

  **`java -cp RigolComm.jar RigolScan`**
  
### Requirements
A [Java JDK or JVM](https://www.java.com/en/) or [OpenJDK](http://openjdk.java.net) version 8, or later must be installed in order to run the code.  There is also a [**Runnable JAR file**](https://github.com/wholder/RigolComm/blob/master/out/artifacts/RigolComm_jar) included in the checked in code that you can download and run without having to compile the cource code.   On a Mac, just double click the **`RigolComm.jar`** file and it should start.  However, you'll need to right click on the .jar file and select "Open" the first time you run RigolComm due to new Mac OS X security checks.  You should also be able to run the JAR file on [Windows](https://windowsreport.com/jar-file-windows/) or [Linux](https://itsfoss.com/run-jar-file-ubuntu-linux/) systems, but you'll need to have a Java 8 JDK/JRE, or later installed and follow the appropriate process for each needed to run an executable JAR file on those platforms.

## RigolComm
RigolComm uses the following Java code to perform some of its functions, or build this project:
- [Usb4Java](http://usb4java.org) is used to perform low-level USB I/O using the USBTMC protocol
- [IntelliJ IDEA from JetBrains](https://www.jetbrains.com/idea/) (my favorite development environment for Java coding. Thanks JetBrains!)
