---
layout: home
---

Welcome to the documentation of the JavaTorrent BitTorrent library.

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

The public api is defined by exclusions: Classes are part of the public API unless:
 - They are within `org.johnnei.javatorrent.internal.**`
 - Their visibility is 'default' (package private).

Deprecation will be handled in several phases and versions:
`@Deprecated` will be added to the offended class/method in version x (let's start at 1.4.0) and a replacement (if it will be replaced) will be introduced.
Before it's actually removed **at least** one feature release (ex. 1.5.0) will be done before 2.0.0 is allowed to be released. 
This will prevent last minute deprecations causing trouble.
