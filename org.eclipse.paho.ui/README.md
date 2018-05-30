## Eclipse Paho MQTT Tool

- Build eclipse based Paho MQTT Tool with: **mvn clean install**, and you can find the following binaries for different platforms in the maven repository:
  - Windows 32-bit
    ``org.eclipse.paho.ui.app-{version}-win32.win32.x86``

  - Windows 64-bit
    ``org.eclipse.paho.ui.app-{version}-win32.win32.x86_64.zip``

  - Linux 32-bit
    ``org.eclipse.paho.ui.app-{version}-linux.gtk.x86.tar.gz``

  - Linux 64-bit
    ``org.eclipse.paho.ui.app-{version}-linux.gtk.x86_64.tar.gz``

  - Mac OS X 64-bit
    ``org.eclipse.paho.ui.app-{version}-macosx.cocoa.x86_64.tar.gz``

- **Note**: add execution permission for Linux and Mac OS X for the executable file if necessary.
If you got "The Paho executable launch unable to locate its companion shared library" such issue on Windows,this is probably because the file path name exceeds the max path length which is defined as 260 characters on Windows.So please make sure the paho ui app is **NOT** in a directory that may exceed max path length.