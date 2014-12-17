JavaTorrent
===========

Java Torrent is a BitTorrent Protocol implementation in Java which aims to have a minimal console approach whilst having a fully featured GUI.

Implemented standard features:

BEP | Description | Implemented 
---:| ----------- | ----------- 
3   | BitTorrent Protocol | Fully 
9 | Metadata sending | Fully
10 | Extension Protocol | Fully
15 | UDP Trackers | Functional (Optimizations pending)
29 | uTP support | Planned

Peer flag description
===========
Flag | Description
---: | -----------
T    | Connected through TCP
U    | Connected through uTP
C    | Choked (Can't download from peer yet)
I    | Interested (Request to allow downloading)

Version System
===========
The version system will be similar to Semantic Versioning but as this isn't a library I will decide when "major" should be updated.
Version format: [major].[feature].[patch]

Used Resources
===========
http://www.jigsoaricons.com/ - Creative Commons Attribution 3.0 license | No longer exists
