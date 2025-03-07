# BurpJDSer-ng


A Burp Extender plugin, that will deserialized java objects and encode them in ~~JSON~~ XML using the [Xtream](https://x-stream.github.io/) library.

Based in part on [khai-tran](https://github.com/khai-tran/BurpJDSer)'s work but written from scratch to work with the new Extender API introduced in Burp-1.5.01

## Usage

### 1) Find and download client *.jar files
Few methods to locate the required jar files containing the classes we'll be deserializing.
* In case of a .jnlp file use [jnpdownloader](https://code.google.com/p/jnlpdownloader/)
* Locating jars in browser cache
* Looking for .jar in burp proxy history

Finally, create a "libs/" directory next to your burp.jar and put all the jars in it.

### 2) Start Burp plugin
For the the extension be able to access internal Java classes that are protected, add something like this to BurpSuitePro.vmoptions:

--add-opens=java.base/java.util=ALL-UNNAMED

You may need to add several lines with different class paths depeding on your target.

Download from Releases page and load it in the Extender tab, the Output window will list all the loaded jars from ./libs/ 

### 3) Inspect serialized Java traffic
Serialized Java content will automagically appear in the `Java Object` tab in appropriate locations (proxy history, interceptor, repeater, etc.)
Any changes made to the JSON will serialize back once you switch to a different tab or send the request.

**Please note that if you mess up the JSON schema or edit an object in a funny way, the re-serialization will fail and the error will be displayed in the Dashboard tab**

In case you need to add more JARs, right click anywhere and select "BurpJDSer-ng: Reload JARs"


## Build
./gradlew clean

./gradlew shadowJar

Jars located in ./build/libs/.

Build with custom JDK: 
./gradlew shadowJar -Dorg.gradle.java.home=/path/to/jdk
