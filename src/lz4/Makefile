lz4:
	$(CC) -c -O3 -fPIC \
		-I. \
		-I$(JAVA_HOME)/include \
		-I$(JAVA_HOME)/include/linux \
		-I../../build/jni-headers \
		../jni/net_jpountz_lz4_LZ4JNI.c \
		./lz4frame.c \
		./xxhash.c \
		./lz4.c \
		./lz4hc.c \
		../jni/net_jpountz_xxhash_XXHashJNI.c

	$(CC) -shared -o liblz4-java.so \
		./net_jpountz_lz4_LZ4JNI.o \
		./lz4frame.o \
		./xxhash.o \
		./lz4.o \
		./lz4hc.o \
		./net_jpountz_xxhash_XXHashJNI.o

clean:
	rm -rf *.o *.so *JNI*
