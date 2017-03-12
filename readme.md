### LZ4 Native Bindings for Java

Based on [jpountz/lz4-java](https://github.com/jpountz/lz4-java) with an inital port of the frames interface by [jpountz/lz4-java](https://github.com/danielfree/lz4-java). This has been extended to support checksums and to conform more closely with the canonical [frameCompress.c](https://github.com/lz4/lz4/blob/v1.7.5/examples/frameCompress.c) example.

LZ4 sources have been updated to the latest 1.7.5, and all data read or written by the Java bindings is interoperable with the LZ4 cli and C library.

There are *only* JNI bindings and it is *only* targeting Linux. If you need Java only implementations or other compile targets, look at the original projects.

As much code as possible has been deleted, and while the library remains pure Java/JNI/C, the packaging and testing is now Clojure based for simplicity.

### test

`bash test.sh`

### build

`lein jar`
`lein pom`

### deployment

Stick the jar and pom somewhere.

### usage

It's very straight forward, see the [test](https://github.com/nathants/lz4-java/blob/master/test/lz4_test.clj#L67).
