# Third-party notices

The Curved180 mod's own code is GPL-2.0-only. Dependencies and build tools retain their respective licenses.

## Gradle Wrapper

The Gradle Wrapper scripts and `gradle/wrapper/gradle-wrapper.jar` are Gradle build tooling, distributed under the Apache License 2.0. Copyright notices in the generated scripts are retained. The full license is in `LICENSES/Apache-2.0.txt`.

Source: https://github.com/gradle/gradle
Distribution: https://services.gradle.org/distributions/gradle-9.5.0-bin.zip

The wrapper is a separate build tool, not part of the mod JAR.

## External dependencies

Minecraft, Fabric Loader, Fabric API, Iris, Sodium, Java, LWJGL, JOML, and the test libraries are external runtime/build dependencies. Their binaries are not vendored into this source repository or bundled into the Curved180 mod JAR. Obtain them through their official distributions or the dependencies declared in `build.gradle`; their upstream licenses apply.

Complementary Reimagined is a separately obtained shader pack. No shader-pack source or assets are redistributed here.

Iris and Minecraft source were inspected for interoperability. QuakeProMax was consulted during architectural research; its implementation was not copied.
