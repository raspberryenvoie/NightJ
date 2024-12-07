<center>

<h1>NightJ</h1>

<img src="bluej/icons/bluej-icon-512-embossed.png" width="128">

<h3>Dark Mode for BlueJ</h3>

</center>

---------

## 1. Usage

Simply download NightJ from the [releases page](https://github.com/raspberryenvoie/NightJ/releases)!

## 2. Contributions are needed

While NightJ aims to implement dark mode for BlueJ, many GUI elements still use light mode.

So feel free to make a pull request to improve it!

## 3. Implementation

NightJ uses the Mocha [Catppuccin](https://github.com/catppuccin/catppuccin) palette. This was essentially done by modifying the CSS files in `bluej/lib/stylesheets`.

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
