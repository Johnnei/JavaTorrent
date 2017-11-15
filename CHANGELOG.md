# Unreleased

## Added
- [JBT-19](https://jira.johnnei.org/browse/JBT-19): Support for HTTP(s) trackers with the `HttpTrackerModule`.
- [JBT-8](https://jira.johnnei.org/browse/JBT-8): Support for base-32 magnet links.
- [JBT-10](https://jira.johnnei.org/browse/JBT-10): A dedicated class `Metadata` to represent the Torrent Metadata.

## Improvements
- [JBT-59](https://jira.johnnei.org/browse/JBT-59): Rebuild the uTP implementation from scratch. It's much more stable now
- [JBT-39](https://jira.johnnei.org/browse/JBT-39): `Optional<T>` types have been removed as arguments and overloads have been added to compensate.
- [JBT-10](https://jira.johnnei.org/browse/JBT-10): The internal `Job` system now relies on the `AbstractFileSet` of the given `Piece`. This makes the system
more re-usable for other systems.
- [JBT-33](https://jira.johnnei.org/browse/JBT-33): Doubled Torrent download throughput (According to integration test).
- [JBT-70](https://jira.johnnei.org/browse/JBT-70): `ISocket` and `TcpSocket` have become part of the public API.

## Fixes
- [JBT-44](https://jira.johnnei.org/browse/JBT-44): Completion of pieces no longer has a chance to cause `IndexOutOfBounds`
- [JBT-75](https://jira.johnnei.org/browse/JBT-75): The default included `PhaseData` no longer ingores choke states.

## API Changes
- [JBT-50](https://jira.johnnei.org/browse/JBT-50): `IBencodedValue#serialise()` now returns `byte[]` instead of `String` to correctly be capable of handling
Strings which represent byte arrays containing data which is not valid UTF-8.
- [JBT-10](https://jira.johnnei.org/browse/JBT-10): `Peer#addBlockRequest` now accepts a `Piece` instead of the index of the piece.
- [JBT-10](https://jira.johnnei.org/browse/JBT-10): `Torrent#getMetadata` now returns a dedicated non-null Metadata object with an optional `AbstractFileSet` in
 case downloading the metadata is supported.
- [JBT-33](https://jira.johnnei.org/browse/JBT-33): `TorrentClient` now requires `IRequestLimiter` to control how many requests a peer may have. The
`RateBasedLimiter` is the old behaviour (but slightly improved to reach higher throughput).

# 0.5.1
## Added
- [JBT-43](https://jira.johnnei.org/browse/JBT-43): `IBencodedValue#asBytes` allows for efficient transport of string representing raw bytes
 (ex. the hashes in the .torrent file)

## Improvements
- [JBT-32](https://jira.johnnei.org/browse/JBT-32): Prevent reads on Piece#checkHash when the hash can't possibly be correct.
- [JBT-38](https://jira.johnnei.org/browse/JBT-38): uTP is now better capable of dealing with packet loss.

## Fixes
- [JBT-40](https://jira.johnnei.org/browse/JBT-40): The ITracker interface was not correctly added to the torrent lifecycle causes no peers to be fetched.
- [JBT-41](https://jira.johnnei.org/browse/JBT-41): `Torrent#isDownloadingMetadata()` returned `false` too early. This has been corrected.
- [JBT-42](https://jira.johnnei.org/browse/JBT-42): Fix `FullPieceSelect` having inconsistent piece sorting.
- [JBT-43](https://jira.johnnei.org/browse/JBT-43): `Bencoding` now takes in an `InStream` and reads the input as raw bytes as intended by the spec. This also
 resolves the encoding issues.

## Removal
- [JBT-43](https://jira.johnnei.org/browse/JBT-43): `Bencoding#getCharactersRead()` The corrected API takes in a stream which won't be consumed further than
 needed.

# 0.5.0
Initial release as Maven Module. With this release a lot of the code has been remade or refactored in order to be a modular and decoupled system.
