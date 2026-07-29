# Security compatibility checklist

Use a physical device for these checks after changing widget receivers, media-controller
trust, or Listen Together network policy.

## Network policy rationale

Android Network Security Config supports fixed domains but not private CIDR ranges, mDNS,
or user-provided LAN hostnames. The base configuration must therefore permit cleartext so
Listen Together can reach dynamic local servers. Runtime URL validation limits `ws://` to
loopback, link-local, private IP, `.local`, `.lan`, and `.home.arpa` destinations; remote
servers must use `wss://`. The App has no bundled cleartext Internet endpoint.

## Widgets

- Add the Music, Turntable, Playlist, and Music Recognizer widgets.
- Verify play/pause, previous, next, like, playlist quick play, and recognition controls.
- Resize each widget and confirm its layout refreshes.
- Reboot the device and confirm every widget still updates.

## Lock-screen and notification controls

- Start playback, lock the screen, and verify play/pause, previous, and next.
- Verify the notification's like, shuffle, repeat, radio, library, and playlist actions when shown.
- Confirm playback metadata and artwork continue to refresh.

## Android Auto

- Connect Android Auto and browse every enabled library section.
- Start playback and verify play/pause, previous, next, queue, and seek controls.
- Verify custom actions shown by the car host still work.

## Listen Together

- Connect to the bundled public `wss://` server and create or join a room.
- Connect to a local server using a private IPv4, private IPv6, or `.local` `ws://` URL.
- Confirm a public `ws://` URL is rejected by the settings screen.
- Verify playback synchronization after the device screen is locked.
