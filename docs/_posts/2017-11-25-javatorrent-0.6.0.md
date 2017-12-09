---
layout: post
title:  "JavaTorrent 0.6"
date:   2017-11-25 14:00:00 +0100
categories: release update
---

I'm glad to announce the release of JavaTorrent 0.6.0. With this release a couple of new features are introduced and bugs been squashed.

The following tickets are noteworthy:
## New Features
- [JBT-19](https://jira.johnnei.org/browse/JBT-19): Support for HTTP(s) trackers with the `HttpTrackerModule`.
- [JBT-8](https://jira.johnnei.org/browse/JBT-8): Support for base-32 magnet links.

## Improvements
- [JBT-59](https://jira.johnnei.org/browse/JBT-59): Rebuild the uTP implementation from scratch. It's much more stable now
- [JBT-33](https://jira.johnnei.org/browse/JBT-33): Doubled Torrent download throughput (According to integration test).
- [JBT-70](https://jira.johnnei.org/browse/JBT-70): `ISocket` and `TcpSocket` have become part of the public API.

## Fixes
- [JBT-75](https://jira.johnnei.org/browse/JBT-75): The default included `PhaseData` no longer ignores choke states.

## API Changes
- [JBT-50](https://jira.johnnei.org/browse/JBT-50): `IBencodedValue#serialise()` now returns `byte[]` instead of `String` to correctly be capable of handling

Please be advised that eventhough the new uTP system is much, much more stable it's still significantly slower than the TCP variant. This will be a point to
address in a later date.

The 0.6.0 artifacts have been published in the Sonatype repositories and will sync to Maven Central soon.
