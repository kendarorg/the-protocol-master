You use this wrapper exactly like a normal `AsynchronousServerSocketChannel` — except the TLS handshake is completed **before** you receive the connection.

Below is a practical, production-style example of how to wire it up and use it correctly.

---

# 1️⃣ Create SSLContext (Server Side)

You need a server certificate in a keystore.

```java
KeyStore ks = KeyStore.getInstance("PKCS12");
try (InputStream in = Files.newInputStream(Path.of("server.p12"))) {
    ks.load(in, "password".toCharArray());
}

KeyManagerFactory kmf =
        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
kmf.init(ks, "password".toCharArray());

SSLContext context = SSLContext.getInstance("TLS");
context.init(kmf.getKeyManagers(), null, null);
```

Java 11+ enables TLS 1.3 automatically.

---

# 2️⃣ Start the TLS Server

```java
InetSocketAddress address = new InetSocketAddress(8443);

AsynchronousServerSSLSocketChannel server =
        new AsynchronousServerSSLSocketChannel(address, context);

server.accept(new AsynchronousServerSSLSocketChannel.SSLAcceptHandler() {

    @Override
    public void completed(AsynchronousSSLSocketChannel channel) {
        System.out.println("TLS connection established");

        handleClient(channel);
    }

    @Override
    public void failed(Throwable exc) {
        exc.printStackTrace();
    }
});
```

✔ At this point:

* TLS handshake is complete
* You are in encrypted application-data mode
* TLS 1.2 and 1.3 both work

---

# 3️⃣ Reading Encrypted Data

```java
private static void handleClient(AsynchronousSSLSocketChannel channel) {

    ByteBuffer buffer = ByteBuffer.allocate(4096);

    channel.read(buffer, new CompletionHandler<>() {

        @Override
        public void completed(Integer bytesRead, Void att) {

            if (bytesRead <= 0) {
                return;
            }

            buffer.flip();

            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);

            System.out.println("Received: " + new String(data));

            buffer.clear();

            // Continue reading
            channel.read(buffer, this);
        }

        @Override
        public void failed(Throwable exc, Void att) {
            exc.printStackTrace();
        }
    });
}
```

Important:

* You must continue calling `read()` after each completion
* This wrapper already handles TLS unwrap internally

---

# 4️⃣ Writing Encrypted Data

```java
ByteBuffer response =
        ByteBuffer.wrap("Hello over TLS\n".getBytes());

channel.write(response, new CompletionHandler<>() {

    @Override
    public void completed(Integer result, Void attachment) {
        System.out.println("Sent " + result + " bytes");
    }

    @Override
    public void failed(Throwable exc, Void attachment) {
        exc.printStackTrace();
    }
});
```

The wrapper:

* Encrypts using `SSLEngine.wrap`
* Handles partial writes
* Handles TLS 1.3 correctly

---

# 5️⃣ Closing Cleanly

```java
channel.close();
```

This sends:

* `close_notify`
* Flushes encrypted shutdown
* Closes underlying socket

---

# 6️⃣ Testing It

You can test with:

```bash
openssl s_client -connect localhost:8443 -tls1_3
```

Or:

```bash
curl -k https://localhost:8443
```

---

# 🔥 What This Wrapper Guarantees

It correctly handles:

| Problem                       | Solved? |
| ----------------------------- | ------- |
| TLS 1.3 handshake transitions | ✅       |
| FINISHED skipped state        | ✅       |
| BUFFER_UNDERFLOW              | ✅       |
| BUFFER_OVERFLOW               | ✅       |
| Partial writes                | ✅       |
| close_notify                  | ✅       |
| Delegated tasks               | ✅       |
| Fully async                   | ✅       |

---

# ⚠️ Important Production Notes

This implementation is safe, but for high-throughput servers you may want:

* Direct `ByteBuffer.allocateDirect`
* A write queue (if multiple concurrent writes)
* Connection-level backpressure
* ALPN for HTTP/2
* Session reuse tuning
* Custom cipher selection

---

If you'd like, tell me:

* Is this for HTTP?
* Raw binary protocol?
* High-performance gateway?
* Mutual TLS?

I can tailor the usage pattern to your exact workload.
