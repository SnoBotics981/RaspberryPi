# SnoBotics Vision Coprocessor

This software is derived from the WPILib sample; this will run a co-processor to handle the game-related vision subsystem.
The software is designed primarily for Raspberry Pi and is only directly supported when running Raspbian, but other linux-based environments may work also.

The core dependencies are:

* WPILib (FRC-compliant interfaces)
* OpenCV (Vision processing)
* Jetty (Multi-purpose custom network services)

## Cross-platform development
The software is designed to run on a Raspberry Pi, but can be compiled from any system which supports Gradle.
In the file `build.gradle`, there is a setting line which starts `ext.buildType =`.  This value can be changed to compile for other environments.

Note it is possible to easily switch which system you want to target. To do so, just switch which build type is uncommented. When you do this, you will have to run a clean `gradlew clean` in order to
clear out any old artifacts. 

## USB cameras
The code is designed to read from local cameras over the Raspberry Pi's USB ports.  Before launching the code, plug in a USB camera.  This device will be read as "/dev/video0" and is identified in the code as device 0.

To select between the types, open the `Main.java` file in `src/main/java`, and scroll down to the line that says "Selecting a Camera". Follow the directions there to select one.

## Building and running on the Raspberry Pi
If you are running the build for your specific platform on the device you plan on running, you can use `./gradlew run` to run the code directly. You can also run `./gradlew build` to run a build without executing it.
When doing this, the output files will be placed into `output\`. From there, you can run either the .bat file on windows or the shell script on unix in order to run your project.

## Building for another platform
If you are building for another platform, trying to run `gradlew run` will not work, as the OpenCV binaries will not be set up correctly. In that case, when you run `gradlew build`, a zip file
is placed in `output\`. This zip contains the built jar, the OpenCV library for your selected platform, and either a .bat file or shell script to run everything. All you have to do is copy
this file to the system, extract it, then run the .bat or shell script to run your program

## What this gives you
This sample gets an image either from a USB camera or an already existing stream. It then restreams the input image in it's raw form in order to make it viewable on another system.
It then creates an OpenCV sink from the camera, which allows us to grab OpenCV images. It then creates an output stream for an OpenCV image, for instance so you can stream an annotated
image. The default sample just performs a color conversion from BGR to HSV, however from there it is easy to create your own OpenCV processing in order to run everything. In addition, it is possible
to run a pipeline generated from GRIP. In addition, a connection to NetworkTables is set up, so you can send data regarding the targets back to your robot.

## Other configuration options
The build script provides a few other configuration options. These include selecting the main class name, and providing an output name for the project.
Please see the `build.gradle` file for where to change these.
