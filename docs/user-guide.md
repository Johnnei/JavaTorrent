---
layout: page
title: User Guide
permalink: /user-guide/
---

# Getting Started
The core usage of this library comes in the form of the `TorrentClient` and `Torrent`. To start downloading torrents you'll need to configure a TorrentClient
with the capabilities which you require (defaults will come at a later point). This instance can be acquired via the `TorrentClient.Builder` class.

## Configuration of TorrentClient
At the bare minimum you'll need to configure the 'Connection Degradation' (Connection options), 'Peer Connector' (How to connect), 'Peer Distributor'
(Coordinate peer maximums across torrents), 'Tracker Protocols' (How to get peers), 'Phase Regulator' (Defines the steps to download) and finally a 'Executor
Service' (To actually do the work).

So let's get started:

### 1. Connection Degradation
To start we'll need to configure which types of connections we support and in which order we prefer them (ex. prefer uTP over TCP).
The torrent client will connect with the default connection type first and then 'degrade' to less preferred protocols if the connection can't be established. 
The core implementation only supplies a TCP implementation.

```java
TorrentClient client = new TorrentClient.Builder()
    .setConnectionDegradation(new ConnectionDegradation.Builder()
        .registerDefaultConnectionType(TcpSocket.class, TcpSocket::new)
        .build())
    // ...other configuration
    .build();
```

If you'd also use the `javatorrent-bittorrent-utp` artifact you're likely to want to prefer uTP over TCP. Which results in the following configuration:
```java
UtpModule utpModule = new UtpModule.Builder()
    .listenOn(6881)
    .build();
TorrentClient client = new TorrentClient.Builder()
    .registerModule(utpModule)
    .setConnectionDegradation(new ConnectionDegradation.Builder()
        .registerDefaultConnectionType(utpModule.getUtpSocketClass(), utpModule.createSocketFactory(), NioTcpSocket.class)
        .registerConnectionType(NioTcpSocket.class, NioTcpSocket::new)
        .build())
    // ...other configuration
    .build();

```

### 2. Peer Connector
Now we know how you connect with peer, we also need a system which actually connects to peers.
The Peer Connector is used to establish connections with peers (Commonly the result of an announce on a tracker). The core implementation provides a NIO based
connector. This connector takes advantage of the shared Executor Service instead of a dedicated thread.

```java
TorrentClient = new TorrentClient.Builder()
    .setPeerConnector(tc -> new NioPeerConnector(tc, 4))
    // ...other configuration
    .build();
```

### 3. Tracker Protocols
Now you've set up a way to connect to peers and which protocol you prefer, now you also need a source to get peers from. This is where Trackers come in.
Trackers provide a simple way to announce yourself in the peer pool and in return you get a list of peers to connect to.

The BitTorrent protocol specification has 'built-in' support for HTTP Trackers thus you don't need any extra artifacts for trackers. However HTTP trackers have
fallen out of favor for larger trackers due to the significant overhead of TCP and HTTP. They are now commonly using UDP based Trackers which is available in
the `javatorrent-bittorrent-tracker-udp` artifact.

```java
TorrentClient = new TorrentClient.Builder()
    .registerModule(new UdpTrackerModule.Builder().setPort(8661).build())
    .registerModule(new HttpTrackerModule())
    // ... other configuration
    .build();
```

### 4. Phase Regulator
Now you've got your peers ready and waiting to interact you first need to tell which phases each `Torrent` have to go through.
The core implementation provides 2 phases the 'Data' phase in which you download the torrent and the 'Seed' phase in which you do you fair share and provide the
download to others.

```java
TorrentClient client = new TorrentClient.Builder()
    .setPhaseRegulator(new PhaseRegulator.Builder()
        .registerInitialPhase(PhaseData.class, PhaseData::new, PhaseSeed.class)
        .registerPhase(PhaseSeed.class, PhaseSeed::new)
        .build())
    // ... other configuration
    .build();
```

Now this is fun and all if you have the actual .torrent file but magnet links are quite popular these days as well.
To be able to download based on a magnet link you need support for the uTorrent Metadata protocol. This protocol allows us to download the torrent file based
on the magnet link. To enable this protocol you'll need both `javatorrent-bittorrent-extension` and `javatorrent-bittorrent-extension-ut-metadata` artifacts.

```java
TorrentClient client = new TorrentClient.Builder()
    // Register the Extension protocol on which the ut_metadata protocol is based.
    .registerModule(new ExtensionModule.Builder()
        // Register the ut_metadata protocol.
        .registerExtension(new UTMetadataExtension(torrentFileFolder, downloadFolder))
        .build())
    // ... other configuration
    .build();
```

### 5. Executor Service
Last but not least you'll need to pick how many threads you want the torrent client to use for non-blocking operations. Most blocking operations operate on
their own threads.

```java
TorrentClient client = new TorrentClient.Builder()
    .setExecutorService(Executors.newScheduledThreadPool(2))
    // ... other configuration
    .build();
```

You're now know all the steps to set up a torrent client with commonly used extra features 
