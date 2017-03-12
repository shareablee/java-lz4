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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import net.jpountz.util.SafeUtils;

public final class LZ4InputStream extends FilterInputStream {
    // Use 64kb block size
    // TODO: Adapt this while de-compressing using the hint returned by lz4f
    // Use a 1mb buffer for uncompressed data.
    // TODO: Adapt this after reading the block frame info
    private byte[] outputBuffer;
    private byte[] inputBuffer;
    private int inputOffset = 0;
    private int outputOffset = 0;
    private int inputAvailable = 0;
    private int outputAvailable = 0;
    private int result;
    private long lz4fContext;
    private LZ4JNI.LZ4FError error = new LZ4JNI.LZ4FError();

    public LZ4InputStream(InputStream in) throws IOException {
        this(in, 64 * 1024, 64 * 1024);
    }

    public LZ4InputStream(InputStream in, int inputBlockSize, int outputBlockSize) throws IOException {
        super(in);
        outputBuffer = new byte[outputBlockSize];
        inputBuffer = new byte[inputBlockSize];
        lz4fContext = LZ4JNI.LZ4F_createDecompressionContext(error);
        error.check();
    }

    @Override
    public int available() throws IOException {
        return outputBuffer.length - outputOffset;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        SafeUtils.checkRange(b, off, len);
        if (outputOffset == outputAvailable) refill();
        if (inputAvailable == -1 && outputOffset == outputAvailable) return -1;
        len = Math.min(len, outputAvailable - outputOffset);
        System.arraycopy(outputBuffer, outputOffset, b, off, len);
        outputOffset += len;
        return len;
    }

    @Override
    public void close() throws IOException {
        super.close();
        LZ4JNI.LZ4F_freeDecompressionContext(lz4fContext, error);
        error.check();
    }

    private void refill() throws IOException {
        outputOffset = 0;
        outputAvailable = 0;
        while (outputAvailable < outputBuffer.length && inputAvailable != -1) {
            if (inputOffset == inputAvailable) {
                inputAvailable = in.read(inputBuffer, 0, inputBuffer.length - inputOffset);
                inputOffset = 0;
                if (inputAvailable == -1) return;
            }
            result = LZ4JNI.LZ4F_decompress(lz4fContext,
                                            inputBuffer, inputOffset, inputAvailable - inputOffset,
                                            outputBuffer, outputOffset, outputBuffer.length - outputOffset,
                                            error);
            error.check();
            if (result >= 0) {
                outputAvailable += result;
                inputOffset = inputAvailable;
            } else {
                inputOffset += -result-1;
                outputAvailable = outputBuffer.length;
            }
        }
    }

    @Override
    public boolean markSupported() { return false; }

    @SuppressWarnings("sync-override")
    @Override
    public void mark(int readlimit) {}

    @SuppressWarnings("sync-override")
    @Override
    public void reset() throws IOException { throw new IOException("mark/reset not supported"); }

    @Override
    public int read() throws IOException { throw new IOException("please dont read single bytes from lz4 streams"); }

    @Override
    public long skip(long n) throws IOException { throw new IOException("please dont skip bytes in lz4 streams"); }

}
