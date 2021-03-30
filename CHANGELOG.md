# Unreleased

## Changed
- Library is now compiled against JDK 11
- [JBT-120](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/120): `UdpTrackerModule` now reads the incoming port from `TorrentClientSettings` in favor of duplicating it.
- [JBT-122](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/122): Rewrote the piece selection handling
  - `PieceSelector` has been replaced by `PiecePrioritzer`
  -  Piece/Block request state management is no longer owned by `Peer` (has been moved to an internal class)
  - `Peer#getFreeWorkTime()` has been removed
  - `PeerStateAccess` has been introduced to expose state based on internal state

## Fixed
 - [JBT-123](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/123): Ignore block message for blocks that are not expecting block data

# 0.7.0

## Added
- [JBT-104](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/104): `NioPeerConnector` is now available. This connector is optimized to run with out a dedicated thread but on
the executor threads.
- [JBT-110](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/110): `TorrentClientSettings` is introduced on `TorrentClient` to separate client state from configuration

## Changed
- [JBT-98](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/98): The network layer has been rewritten to be event driven in favor of polling.
- [JBT-100](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/100): `AbstractPeerConnectionAcceptor` has been replaced by `BitTorrentHandshakeHandler`
- [JBT-102](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/102): `ISocket` has been remodelled around Channels instead of Sockets.
- [JBT-104](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/104): `BitTorrentSocket` is now always a socket which has passed the handshake process.
- [JBT-99](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/99): Peer IO is now processed until it can no longer be executed without blocking.
- [JBT-106](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/106): Unsupported tracker protocols no longer throw an exception but will log a warning and return `Optional#empty`
- [JBT-107](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/107): UDP Trackers now support the common `/announce` suffix.
- [JBT-116](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/116): Connection queue has been given a priority strategy to (mostly) evenly split over torrents.
- [JBT-117](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/117): `Peer#addBlockRequest` now rejects when the peer is choked
- [JBT-117](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/117): Metadata phases now use a dedicated choking strategy to show interest into clients which have the metadata and the ut_metadata extension data
- [JBT-110](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/110): `UtpModule` now reuses the download port set on the `TorrentClient`

## Deprecated
- [JBT-102](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/104): `Peer#getBitTorrentSocket()` is now deprecated. The `BitTorrentSocket` will become an internal class.
Functionality will be replaced.
- [JBT-110](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/110): `UtpModule.Builder#listenOn(int)` now reuses the download port set on the `TorrentClient`

## Fixed
- [JBT-110](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/110): Remote connections coming in through uTP now correctly get ignored when `isAcceptingConnections` is false.

## Removed
- [JBT-102](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/102): `BitTorrentSocket` has forgotten how to process handshakes.

# 0.6.0
## Added
- [JBT-19](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/19): Support for HTTP(s) trackers with the `HttpTrackerModule`.
- [JBT-8](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/8): Support for base-32 magnet links.
- [JBT-10](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/10): A dedicated class `Metadata` to represent the Torrent Metadata.

## Improvements
- [JBT-59](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/59): Rebuild the uTP implementation from scratch. It's much more stable now
- [JBT-39](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/39): `Optional<T>` types have been removed as arguments and overloads have been added to compensate.
- [JBT-10](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/10): The internal `Job` system now relies on the `AbstractFileSet` of the given `Piece`. This makes the system
more re-usable for other systems.
- [JBT-33](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/33): Doubled Torrent download throughput (According to integration test).
- [JBT-70](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/70): `ISocket` and `TcpSocket` have become part of the public API.

## Fixes
- [JBT-44](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/44): Completion of pieces no longer has a chance to cause `IndexOutOfBounds`
- [JBT-75](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/75): The default included `PhaseData` no longer ingores choke states.

## API Changes
- [JBT-50](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/50): `IBencodedValue#serialise()` now returns `byte[]` instead of `String` to correctly be capable of handling
Strings which represent byte arrays containing data which is not valid UTF-8.
- [JBT-10](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/10): `Peer#addBlockRequest` now accepts a `Piece` instead of the index of the piece.
- [JBT-10](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/10): `Torrent#getMetadata` now returns a dedicated non-null Metadata object with an optional `AbstractFileSet` in
 case downloading the metadata is supported.
- [JBT-33](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/33): `TorrentClient` now requires `IRequestLimiter` to control how many requests a peer may have. The
`RateBasedLimiter` is the old behaviour (but slightly improved to reach higher throughput).

# 0.5.1
## Added
- [JBT-43](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/43): `IBencodedValue#asBytes` allows for efficient transport of string representing raw bytes
 (ex. the hashes in the .torrent file)

## Improvements
- [JBT-32](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/32): Prevent reads on Piece#checkHash when the hash can't possibly be correct.
- [JBT-38](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/38): uTP is now better capable of dealing with packet loss.

## Fixes
- [JBT-40](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/40): The ITracker interface was not correctly added to the torrent lifecycle causes no peers to be fetched.
- [JBT-41](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/41): `Torrent#isDownloadingMetadata()` returned `false` too early. This has been corrected.
- [JBT-42](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/42): Fix `FullPieceSelect` having inconsistent piece sorting.
- [JBT-43](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/43): `Bencoding` now takes in an `InStream` and reads the input as raw bytes as intended by the spec. This also
 resolves the encoding issues.

## Removal
- [JBT-43](https://git.johnnei.org/Johnnei/JavaTorrent/-/issues/43): `Bencoding#getCharactersRead()` The corrected API takes in a stream which won't be consumed further than
 needed.

# 0.5.0
Initial release as Maven Module. With this release a lot of the code has been remade or refactored in order to be a modular and decoupled system.
