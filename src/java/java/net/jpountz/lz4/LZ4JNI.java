package net.jpountz.lz4;

import java.io.IOException;

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

import java.nio.ByteBuffer;

import net.jpountz.util.Native;

enum LZ4JNI {
  ;

  public static class LZ4FError {
    String message = null;
    public void check() throws IOException {
      if (message != null)
        throw new IOException(message);
    }
  }

  static {
    Native.load();
    init();
  }

  static native void init();
  static native long LZ4F_createCompressionContext(LZ4FError error);
  static native void LZ4F_freeCompressionContext(long context, LZ4FError error);
  static native int  LZ4F_compressBegin(long context, int compressionLevel, byte[] dstArray, int dstOffset, int dstLen, LZ4FError error);
  static native int  LZ4F_compressBound(int srcSize);
  static native int  LZ4F_compressUpdate(long context, byte[] srcArray, int srcOffset, int srcLen, byte[] dstArray, int dstOffset, int dstLen, LZ4FError error);
  static native int  LZ4F_compressEnd(long context, byte[] dstArray, int dstOffset, int dstLen, LZ4FError error);
  static native int  LZ4F_compressFlush(long context, byte[] dstArray, int dstOffset, int dstLen, LZ4FError error);
  static native long LZ4F_createDecompressionContext(LZ4FError error);
  static native void LZ4F_freeDecompressionContext(long context, LZ4FError error);
  static native int  LZ4F_decompress(long context, byte[] srcArray, int srcOff, int srcLen, byte[] dstArray, int dstOff, int dstLen, LZ4FError error);
}
