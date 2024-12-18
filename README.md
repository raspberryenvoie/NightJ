<h1 align="center">NightJ</h1>

<p align="center">
    <img src="bluej/icons/bluej-icon-512-embossed.png" width="128">
</p>

<p align="center">
    <img src="bluej/resources/nightj-screenshot.jpg">
</p>
<h3 align="center">Dark Mode in BlueJ</h3>

---------

## 1. Usage

Simply download NightJ from the [releases page](https://github.com/raspberryenvoie/NightJ/releases)!

## 2. Contributions are needed

While NightJ aims to implement dark mode in BlueJ, many GUI elements still use light mode.

So feel free to make a pull request to improve it!

## 3. Implementation

NightJ was essentially implemented by modifying the CSS files in `bluej/lib/stylesheets`.
See [JavaFX CSS Reference Guide](https://docs.oracle.com/javase/8/javafx/api/javafx/scene/doc-files/cssref.html).

It uses the Mocha [Catppuccin](https://github.com/catppuccin/catppuccin) color palette.

## 4. Building and running

BlueJ uses Gradle as its automated build tool. To build you will first need to install a Java (21) JDK. Check out the repository then execute the following command to run BlueJ:

```
./gradlew runBlueJ
```

## 5. Building Installers

The installers are built automatically on Github. If you want to build them manually you will need to set any appropriate tool paths in tools.properties, and run the appropriate single command of the following set:

```
./gradlew packageBlueJWindows
./gradlew packageBlueJLinux
./gradlew packageBlueJMacIntel
./gradlew packageBlueJMacAarch
```

None of the installers can be cross-built, so you must build Windows on Windows, Mac on Mac and Linux on Debian/Ubuntu. Windows requires an installation of WiX 3.10 and MinGW64 to build the installer. On Mac, JAVA_HOME must point to an Intel JDK for the Intel build, and an Aarch/ARM JDK for the Aarch build, so you cannot run them in the same command.

## 6. License

This repository contains the source code for BlueJ and Greenfoot, licensed under the GPLv2 with classpath exception (see [LICENSE.txt](LICENSE.txt)).
