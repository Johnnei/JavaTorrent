# Unreleased
## Improvements
- [JBT-32](https://jira.johnnei.org/browse/JBT-32): Prevent reads on Piece#checkHash when the hash can't possibly be correct.

## Fixes
- [JBT-40](https://jira.johnnei.org/browse/JBT-40): The ITracker interface was not correctly added to the torrent lifecycle causes no peers to be fetched.

# 0.5.0
Initial release as Maven Module. With this release a lot of the code has been remade or refactored in order to be a modular and decoupled system.