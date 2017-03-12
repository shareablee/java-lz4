package net.jpountz.lz4;

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class LZ4OutputStream extends FilterOutputStream {
    static private int LZ4_HEADER_SIZE = 19;
    static private int LZ4_FOOTER_SIZE = 4;
    private long lz4fContext;
    private byte[] outputBuffer = null;
    private byte[] inputBuffer;
    private int inputOffset = 0;
    private int written;
    private int to_read;

    LZ4JNI.LZ4FError error = new LZ4JNI.LZ4FError();

    public LZ4OutputStream(OutputStream out, int compressionLevel) throws IOException {
        this(out, 64 * 1024, compressionLevel);
    }

    public LZ4OutputStream(OutputStream out, int blocksize, int compressionLevel) throws IOException {
        super(out);
        lz4fContext = LZ4JNI.LZ4F_createCompressionContext(error);
        error.check();

        int frame_size = LZ4JNI.LZ4F_compressBound(blocksize);
        int outBufferSize = frame_size + LZ4_HEADER_SIZE + LZ4_FOOTER_SIZE;
        inputBuffer = new byte[blocksize];
        outputBuffer = new byte[outBufferSize];
        written = LZ4JNI.LZ4F_compressBegin(lz4fContext, compressionLevel, outputBuffer, 0, outputBuffer.length, error);
        error.check();
        out.write(outputBuffer, 0, written);
    }

    private void compressBuffer() throws IOException {
        written = LZ4JNI.LZ4F_compressUpdate(lz4fContext, inputBuffer, 0, inputOffset, outputBuffer, 0, outputBuffer.length, error);
        error.check();
        inputOffset = 0;
        out.write(outputBuffer, 0, written);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        while (len > 0) {
            to_read = Math.min(len, inputBuffer.length - inputOffset);
            System.arraycopy(b, off, inputBuffer, inputOffset, to_read);
            inputOffset += to_read;
            if (inputOffset == inputBuffer.length)
                compressBuffer();
            off += to_read;
            len -= to_read;
        }
    }

    @Override
    public void close() throws IOException {
        compressBuffer();
        written = LZ4JNI.LZ4F_compressEnd(lz4fContext, outputBuffer, 0, outputBuffer.length, error);
        error.check();
        out.write(outputBuffer, 0, written);
        LZ4JNI.LZ4F_freeCompressionContext(lz4fContext, error);
        error.check();
        out.close();
    }

    @Override
    public void flush() throws IOException {
        compressBuffer();
        written = LZ4JNI.LZ4F_compressFlush(lz4fContext, outputBuffer, 0, outputBuffer.length, error);
        error.check();
        out.write(outputBuffer, 0, written);
        super.flush();
    }

    @Override
    public void write(int b) throws IOException { throw new IOException("please dont write single bytes to lz4 streams"); }

}
