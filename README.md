# Tray Camera

## About

Tray Camera is a small application that sits in the system tray
![icon](app/src/main/resources/camera_off.png)
, and takes screenshots when double-clicked.
The files are saved as PNG files to the ".traycamera" folder within the current users home folder,
along with a generated html page to view the images. 

By default, it takes images from all screens. (The screens to capture can be configured from the menu.)

I use this during online meetings, allowing me to take notes later whilst concentrating on the presentation
during the event.

It is written in Kotlin and requires a Java8 runtime or higher.

## Build instructions

To build the application use the command:

```bash
./gradlew buildExecutableJar
```

The JAR will be generated as `app/build/libs/TrayCamera.jar`

## Launch instructions

To launch TrayCamera use the command line:

```bash
java -jar TrayCamera.jar
```

Alternatively you may double-click the TrayCamera.jar from your file browser.

> If you see an error dialog stating "Java virtual machine launcher", "A Java exception has occurred"
> then check that you are running Java 8 or above.
> 
> e.g. On Windows right-click the TrayCamera.jar and select a different runtime from the "Open With" submenu.