# Nexconn ChatUI for Android

Nexconn ChatUI is the Android UI component for building chat experiences with
[Nexconn Chat](https://www.nexconn.ai/product/chat). It provides ready-to-use
channel and message screens, media workflows, notifications, and user and group
management UI. This repository contains only the ChatUI source code and its
standalone Gradle build. Nexconn Chat SDK is resolved as a Maven dependency and
remains subject to its own license.

## Quick Links

- [Sign up](https://console.nexconn.ai/agile/register) to create a Nexconn app
  and obtain an App Key.
- [Nexconn documentation](https://docs.nexconn.ai)
- [Nexconn Chat UI](https://www.nexconn.ai/product/chat#ui-showcase)
- [Source integration](docs/source-integration.md)
- [Version compatibility](docs/version-compatibility.md)

## Use Cases

- Build direct, group, community, open, and system-channel conversations with
  configurable channel and message presentation.
- Add image and video selection, media preview, voice messages, mentions,
  replies, forwarding, message editing, and combined-message preview.
- Customize channel lists, message providers, notifications, images, route
  destinations, and built-in fragments for an application-specific experience.

## Android Chat Tutorial

Start with the [Nexconn documentation](https://docs.nexconn.ai). It covers app
creation, App Key and token handling, SDK initialization, connection, channels,
and messages. Keep App Keys and user tokens in the host application or its
server-side token service; do not commit them to this repository.

## Core Features

- AndroidX-based Activities, Fragments, ViewModels, adapters, and configurable
  UI components.
- Channel list and channel screens backed by Nexconn Chat models and operations.
- Message rendering and input extensions for text, media, voice, mentions,
  replies, forwarding, retry, editing, and combined messages.
- Image selection, media preview, voice and short-video components.
- Local notifications plus user, friend, and group management screens.
- Fragment factories, route replacement, resource overrides, and configuration
  APIs for UI customization.

## Installation

### Build from source

Requirements:

- JDK 17
- Android SDK 36
- Android minSdk 21 in the host application

```bash
git clone https://github.com/NexconnAI-Dev/nexconn-chatui-android.git
cd nexconn-chatui-android
./gradlew :nexconn-chatui:assembleRelease --no-daemon
```

The standalone build must not require a private Git repository, private build
credentials, or a private `:nexconn-chat` project. The required Nexconn Chat
SDK version is recorded in `gradle.properties` and `RELEASE_METADATA.json`.

## Quick Start

Initialize ChatUI once in `Application.onCreate`, then connect after the host
application obtains a user token.

```java
import android.app.Application;
import ai.nexconn.chat.params.ConnectParams;
import ai.nexconn.chat.params.InitParams;
import ai.nexconn.chatui.NCChatUI;

public final class ChatApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        NCChatUI.initialize(new InitParams(this, "your-app-key"));
    }

    public void connect(String userToken) {
        NCChatUI.connect(new ConnectParams(userToken), (userId, error) -> {
            if (error == null) {
                // The user is connected.
            }
        });
    }
}
```

Open a channel list or channel after initialization and connection.

```java
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chatui.utils.route.RouteUtils;

RouteUtils.routeToChannelListActivity(context, "Messages");
RouteUtils.routeToChannelActivity(context, ChannelType.DIRECT, targetUserId);
```

## Usage

Configure `NCChatUIConfig` before calling `NCChatUI.initialize` to supply
channel-list data, message providers, interaction rules, notification behavior,
and an image engine. Register a `ChatUIFragmentFactory` to replace built-in
fragments, or use `RouteUtils.registerActivity` to replace route destinations.

ChatUI contributes Activities, media and microphone permissions, and two
content providers to the merged application manifest. Verify manifest merging,
request runtime permissions before media or voice operations, and ensure that
`${applicationId}.provider` and `${applicationId}.nc.EmojiInitProvider` do not
conflict with providers in the host application.

## Documentation

- [Source integration](docs/source-integration.md)
- [Version compatibility](docs/version-compatibility.md)
- [Nexconn documentation](https://docs.nexconn.ai)

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Public contributions are reviewed and
then applied to the private authoritative source before the next deterministic
export. Do not include credentials, private URLs, generated build output,
Sample applications, or Demo applications in a pull request.

## License

ChatUI source code is licensed under the Apache License 2.0. Nexconn Chat SDK
and third-party content retain their independent licenses. See
[LICENSE](LICENSE) and [NOTICE](NOTICE).
