# JavaTorrent
Java Torrent is a BitTorrent Protocol implementation in Java which aims to provide a highly configurable BitTorrent library. The library is well featured with
 extensions on the BitTorrent Protocol to allow high compatibility with popular torrent applications.

## Information
- [Documentation](https://github.com/Johnnei/JavaTorrent/wiki)
- [Issue Tracker](https://jira.johnnei.org/browse/JBT)

## Status
- Master [![build status](http://git.johnnei.org/Johnnei/JavaTorrent/badges/master/build.svg)](http://git.johnnei.org/Johnnei/JavaTorrent/commits/master)
- Develop [![build status](http://git.johnnei.org/Johnnei/JavaTorrent/badges/develop/build.svg)](http://git.johnnei.org/Johnnei/JavaTorrent/commits/develop)

## Available modules:
| BEP | Description         | Depends on  | Module                                       |
| --- | ------------------- | ----------- | -------------------------------------------- |
|  3  | BitTorrent Protocol | None        | javatorrent-bittorrent                       |
|  9  | Metadata sending    | 10          | javatorrent-bittorrent-extension-ut-metadata |
| 10  | Extension Protocol  | 3           | javatorrent-bittorrent-extension             |
| 15  | UDP Trackers        | 3           | javatorrent-bittorrent-tracker-udp           |
| 29  | uTP support         | 3           | javatorrent-bittorrent-utp                   |

## Roadmap
These are the things you can expect in the future of JavaTorrent (the order is not definitive):
- Refactoring of the Tracker model to reduce code duplication for protocol support.
- Add support for HTTP Tracker compact peer lists (BEP 23)
- Add support for uTP Selective ACKs extensions (Optional of BEP 29)
- Add opt-in analytics to the Extension Protocol to allow for analysis of commonly used extensions
- Add scraping usage to decide whether an announce to the tracker will be made
- Add support for HTTP Tracker scrape requests (Not official standard but based on convention)
- Add DHT support (BEP 5)

## Version System
The version system will follow [Semantic Versioning](http://semver.org/). The version reported in the BitTorrent protocol may be different from the maven
version due to the standard of only using 4 characters for the version section in the PeerID. The version reported in extension dictionary (BEP 10) will match
the actual version.

The public api is defined by exclusions: Classes are part of the public API unless they are within `org.johnnei.javatorrent.internal.**`

Deprecation will be handled in several phases and versions:
1. `@Deprecated` will be added to the offended class/method in version x (let's start at 1.4.0) and a replacement (if it will be replaced) will be introduced.
2. In version y (2.0.0) the functionality will still be maintained. This will prevent last minute deprecations causing trouble.
3. In version z (3.0.0) the deprecated class/method will be dropped.
