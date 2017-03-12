(ns jni
  (:require [clojure.java.shell :as sh]
            [clojure.string :as s]))

(def javah-classes
  ["net.jpountz.lz4.LZ4JNI"
   "net.jpountz.xxhash.XXHashJNI"])

(defn run [& args]
  (let [cmd (apply str (interpose " " args))
        _ (println "run:" cmd)
        res (sh/sh "bash" "-c" cmd)]
    (assert (-> res :exit (= 0)) (assoc res :cmd cmd))
    (.trim (:out res))))

(def root (System/getProperty "user.dir"))

(def compile-path (str root "/target"))

(def native-src (str root "/src/lz4"  ))

(def native-target (str native-src "/liblz4-java.so"))

(def native-path (str root "/target/classes/net/jpountz/util/linux/amd64"))

(def classpath (remove s/blank?  (s/split (System/getProperty "java.class.path") #":")))

(defn javah
  []
  (doseq [c javah-classes]
    (let [args ["javah"
                "-classpath" (s/join ":" (cons compile-path classpath))
                "-d" native-src c]]
      (apply run args))))

(defn gcc
  [& args]
  (sh/with-sh-dir native-src
    (run
      "gcc -c -O3 -fPIC"
      "-I."
      "-I$JAVA_HOME/include "
      "-I$JAVA_HOME/include/linux "
      "-I../../build/jni-headers "
      "../jni/net_jpountz_lz4_LZ4JNI.c "
      "./lz4frame.c "
      "./xxhash.c "
      "./lz4.c "
      "./lz4hc.c "
      "../jni/net_jpountz_xxhash_XXHashJNI.c")
    (run
      "gcc -shared -o liblz4-java.so"
      "./net_jpountz_lz4_LZ4JNI.o"
      "./lz4frame.o"
      "./xxhash.o"
      "./lz4.o"
      "./lz4hc.o"
      "./net_jpountz_xxhash_XXHashJNI.o")))

(defn clean
  [& args]
  (sh/with-sh-dir native-src
    (run "rm -rf *.o *.so *JNI*")))

(defn copy
  []
  (run "mkdir -p" native-path)
  (run "mv" native-target native-path))

(defn build
  []
  (clean)
  (javah)
  (gcc)
  (copy)
  (shutdown-agents))
