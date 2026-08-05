# Source Integration

The repository is a standalone Gradle project containing only `:nexconn-chatui`.

1. Check out the required ChatUI tag.
2. Confirm JDK 17 and Android SDK 36 are installed.
3. Run `./gradlew :nexconn-chatui:assembleRelease --no-daemon`.
4. Integrate the module using Gradle composite build or copy the module into the application repository while preserving its exact Chat SDK Maven dependency.

The source build must not be changed to reference a private `:nexconn-chat` project.
