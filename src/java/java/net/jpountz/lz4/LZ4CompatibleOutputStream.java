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



public class LZ4CompatibleOutputStream extends FilterOutputStream {
    static private int LZ4_HEADER_SIZE = 19;
    static private int LZ4_FOOTER_SIZE = 4;
    long lz4fContext;
    byte[] outputBuffer = null;
    byte[] inputBuffer;
    int inputOffset = 0;
    int out_buffer_size;
    LZ4JNI.LZ4FError error = new LZ4JNI.LZ4FError();

    public LZ4CompatibleOutputStream(OutputStream out, int blocksize, int compressionLevel) throws IOException {
        super(out);
        lz4fContext = LZ4JNI.LZ4F_createCompressionContext(error);
        error.check();
        inputBuffer = new byte[blocksize];
        int frame_size = LZ4JNI.LZ4F_compressBound(blocksize);
        out_buffer_size = frame_size + LZ4_HEADER_SIZE + LZ4_FOOTER_SIZE;
        outputBuffer = new byte[out_buffer_size];
        int written = LZ4JNI.LZ4F_compressBegin(lz4fContext, compressionLevel, outputBuffer, 0, outputBuffer.length, error);
        error.check();
        out.write(outputBuffer, 0, written);
    }

    private void compressBuffer() throws IOException {
        int bufferSize = LZ4JNI.LZ4F_compressBound(inputOffset);
        if (outputBuffer == null || outputBuffer.length < bufferSize)
            outputBuffer = new byte[bufferSize];

        int written = LZ4JNI.LZ4F_compressUpdate(lz4fContext,
                                                 inputBuffer, 0, inputOffset,
                                                 outputBuffer, 0, outputBuffer.length, error);
        error.check();

        // We're done with the current input buffer, start filling it again.
        inputOffset = 0;

        out.write(outputBuffer, 0, written);
    }

    @Override
    public void write(int b) throws IOException {

        inputBuffer[inputOffset++] = (byte) b;

        if (inputOffset == inputBuffer.length)
            compressBuffer();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        // While there are still things to read
        while (len > 0) {
            // Read only what we can
            int to_read = Math.min(len, inputBuffer.length - inputOffset);
            System.arraycopy(b, off, inputBuffer, inputOffset, to_read);
            inputOffset += to_read;

            // If we're full, empty it
            if (inputOffset == inputBuffer.length)
                compressBuffer();

            off += to_read;
            len -= to_read;
        }
    }

    @Override
    public void flush() throws IOException {

        // Flush our own cache
        compressBuffer();

        // And flush lz4's own cache
        int written = LZ4JNI.LZ4F_compressFlush(lz4fContext, outputBuffer, 0, outputBuffer.length, error);
        error.check();
        out.write(outputBuffer, 0, written);

        super.flush();
    }

    @Override
    public void close() throws IOException {

        // Flush our own cache
        compressBuffer();

        // Write what was left in the cache, and properly closes the frame.
        int written = LZ4JNI.LZ4F_compressEnd(lz4fContext, outputBuffer, 0, outputBuffer.length, error);
        error.check();
        out.write(outputBuffer, 0, written);

        LZ4JNI.LZ4F_freeCompressionContext(lz4fContext, error);
        error.check();

        out.close();
    }

}
