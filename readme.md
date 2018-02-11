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

Note it is possible to easily switch which system you want to target. To do so, just switch which build type is uncommented. When you do this, you will have to run a clean `gradlew clean` in order to clear out any old artifacts. 

## USB cameras
The code is designed to read from local cameras over the Raspberry Pi's USB ports.  Before launching the code, plug in a USB camera.  This device will be read as "/dev/video0" and is identified in the code as device 0.

To select between the types, open the `Main.java` file in `src/main/java`, and scroll down to the line that says "Selecting a Camera". Follow the directions there to select one.

## Building and running on the Raspberry Pi
If you are running the build for your specific platform on the device you plan on running, you can use `./gradlew run` to run the code directly. You can also run `./gradlew build` to run a build without executing it.
When doing this, the output files will be placed into `output\`. From there, you can run either the .bat file on windows or the shell script on unix in order to run your project.

The wpilib toolchain must be built on the Pi to allow the software to execute correctly
1. Clone wpiutil from https://github.com/wpilibsuite/wpiutil
   * Build & release: `./gradlew -PskipAthena publish`
2. Clone ntcore from https://github.com/wpilibsuite/ntcore
   * Build: `./gradlew -PskipAthena build`
   * Publish `./gradlew -PskipAthena publish`

By default, the gradle projects should publish to `~/releases/maven/development`.

## Building for another platform
If you are building for another platform, trying to run `gradlew run` will not work, as the OpenCV binaries will not be set up correctly. In that case, when you run `gradlew build`, a zip file
is placed in `output\`. This zip contains the built jar, the OpenCV library for your selected platform, and either a .bat file or shell script to run everything. All you have to do is copy
this file to the system, extract it, then run the .bat or shell script to run your program

## Other configuration options
The build script provides a few other configuration options. These include selecting the main class name, and providing an output name for the project.
Please see the `build.gradle` file for where to change these.

# Network Interface

The server is designed to provide a collection of video feeds, including targetting information for the gear lift peg.

Video feeds are available at the following network addresses:
* Vision detection debug feed (`http://<raspberry-pi.IP.address>:1186/stream.mjpg`)
* Front-facing display (`http://<raspberry-pi.IP.address>:1187/stream.mjpg`)
* Read-view display (`http://<raspberry-pi.IP.address>:1188/stream.mjpg`)
* Navigation feed (`http://<raspberry-pi.IP.address>:1181/nav`)

The navigation feed returns as a JSON object containing two values:
* 'direction':  
    The relative angle to the target's center, in a range of +/-360.  Positive numbers are to the right, negative numbers to the left.
    The values scale based upon the viewfinder angle of camera #0 (front-facing).
* 'closeness':  
    A relative scale indicating how close the target is to the camera.  This value is computed based on the size and
relative distance of the two targets (when visible).
    The value is larger when the camera is close to the target.  See `distance.md` for more details.

