# Required Server Changes for SDK RTT and Latency Measurement

The SDK needs a reachable HTTPS endpoint to measure real RTT and latency from the Android device.

## Current Issue

HTTPS connection to port `443` is refused.

This means the domain is reachable, but the server is not accepting HTTPS traffic on port `443`.

## What Needs To Be Changed

### 1. Enable HTTPS on Port 443

The server must accept HTTPS connections on:

```text
banglalink-ftp.fiberathomeglobal.net:443
```

### 2. Install a Valid TLS Certificate

The TLS certificate must be valid for:

```text
banglalink-ftp.fiberathomeglobal.net
```

### 3. Provide a Lightweight Test Endpoint

Please add a simple endpoint:

```text
GET https://banglalink-ftp.fiberathomeglobal.net/ping
```

Recommended response:

```text
HTTP 204 No Content
```

or:

```json
{
  "ok": true
}
```

### 4. Keep the Endpoint Minimal

The endpoint should not use:

- database calls
- authentication
- redirects
- large response bodies
- file downloads
- heavy server-side processing

### 5. Optional Response Header

If possible, include server processing time in milliseconds:

```text
X-Server-Processing-Time: 1
```

## Why This Is Needed

Terminal ping uses ICMP, but the Android SDK measures latency using an HTTPS request.

So terminal ping can work while SDK latency still fails if HTTPS port `443` is closed or refused.

## Final SDK URL After Server Change

```text
https://banglalink-ftp.fiberathomeglobal.net/ping
```

Without this HTTPS endpoint, the SDK can only estimate latency from TCP RTT, not measure real HTTP latency.
