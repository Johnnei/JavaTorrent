---
layout: post
title:  "JavaTorrent 0.7.0"
date:   2020-11-08 20:00:00 +0700
categories: release update
---

I'm glad to announce the release of JavaTorrent 0.7.0.
With this release the major change was the move towards NIO based IO.
Along with some minor bug fixes and fault tolerance improvements.

I'm still seeing a lot of concurrency issues which will be the focus of next release.
In general, I'm planning to separate model and logic more carefully along with moving more to immutable models.
Maybe I'll have to revisit how I schedule work to prevent concurrent operations on the same torrent as well.
The former should be very noticeable in the API. The latter should be mostly hidden behind implementation details.

To improve quality of live, I'm considering to start with sending Cancel messages on unresponsive/slow peers.
This should prevent torrents from hanging near the end of the download waiting for that one peer to start sending the last blocks.

The following tickets are noteworthy:

## Added
- [JBT-104](https://jira.johnnei.org/browse/JBT-104): `NioPeerConnector` is now available.
  This connector is optimized to run without a dedicated thread but on the executor threads.
  **Be aware**: The old socket model is no longer an option, the old classes have been removed.
- [JBT-110](https://jira.johnnei.org/browse/JBT-110): `TorrentClientSettings` is introduced on `TorrentClient` to separate client state from configuration

## Changed
- [JBT-100](https://jira.johnnei.org/browse/JBT-100): `AbstractPeerConnectionAcceptor` has been replaced by `BitTorrentHandshakeHandler`
- [JBT-102](https://jira.johnnei.org/browse/JBT-102): `ISocket` has been remodelled around Channels instead of Sockets.
- [JBT-104](https://jira.johnnei.org/browse/JBT-104): `BitTorrentSocket` is now always a socket which has passed the handshake process.
- [JBT-106](https://jira.johnnei.org/browse/JBT-106): Unsupported tracker protocols no longer throw an exception but will log a warning and return `Optional#empty`
- [JBT-107](https://jira.johnnei.org/browse/JBT-107): UDP Trackers now support the common `/announce` suffix.
- [JBT-117](https://jira.johnnei.org/browse/JBT-117): `Peer#addBlockRequest` now rejects when the peer is choked
- [JBT-110](https://jira.johnnei.org/browse/JBT-110): `UtpModule` now reuses the download port set on the `TorrentClient`

## Fixed
- [JBT-110](https://jira.johnnei.org/browse/JBT-110): Remote connections coming in through uTP now correctly get ignored when `isAcceptingConnections` is false.

The 0.7.0 artifacts will be published on Maven Central soon.
