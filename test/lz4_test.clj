(ns lz4-test
  (:import java.io.BufferedReader
           java.io.BufferedWriter
           java.io.FileInputStream
           java.io.FileOutputStream
           net.jpountz.xxhash.XXHashFactory
           [net.jpountz.lz4
            LZ4CompatibleOutputStream
            LZ4CompatibleInputStream])
  (:require [clojure.java.shell :as sh]
            [clojure.test :refer :all]
            [clojure.java.io :as io]))

(defn run [& args]
  (let [cmd (apply str (interpose " " args))
        res (sh/sh "bash" "-c" cmd)]
    (assert (-> res :exit (= 0)) (assoc res :cmd cmd))
    (.trim (:out res))))

(defn sum
  [path]
  (let [h (.newStreamingHash64 (XXHashFactory/nativeInstance) 0)]
    (with-open [in (java.io.FileInputStream. path)]
      (let [buffer (make-array Byte/TYPE 4096)]
        (loop []
          (let [size (.read in buffer)]
            (when (pos? size)
              (do (.update h buffer 0 size)
                  (recur)))))
        (Long/toHexString (.getValue h))))))

(deftest lz4
  []

  (testing "can encode"
    (time
     (with-open [in (FileInputStream. "/home/nathants/data/input")
                 out (LZ4CompatibleOutputStream. (FileOutputStream. "/home/nathants/data/output.lz4") (* 4096 1024) 0)]
       (io/copy in out)))
    (time
     (with-open [in (FileInputStream. "/home/nathants/data/input")
                 out (LZ4CompatibleOutputStream. (FileOutputStream. "/home/nathants/data/output.lz4.small") (* 4096 1024) 3)]
       (io/copy in out))))

  (testing "compression level is working"
    (is (< (.length (java.io.File. "/home/nathants/data/output.lz4.small"))
           (.length (java.io.File. "/home/nathants/data/output.lz4")))))

  (testing "can decode"
    (time
     (let [buf-size 1024]
       (with-open [in (LZ4CompatibleInputStream. (FileInputStream. "/home/nathants/data/output.lz4") (* 4096 1024) buf-size)
                   out (FileOutputStream. "/home/nathants/data/output")]
         (io/copy in out :buffer-size buf-size)))))

  (testing "xxhsum"
    (let [expected (run "cat /home/nathants/data/input| xxhsum|cut -d' ' -f1")]
      (is (= expected (sum "/home/nathants/data/output")))
      (is (= expected (sum "/home/nathants/data/input")))
      (is (= expected (run "cat /home/nathants/data/output| xxhsum| cut -d' ' -f1")))))

  (testing "checksum fails when file is corrupted"
    (run "dd if=/dev/zero of=/home/nathants/data/output.lz4 bs=1 seek=1000 count=1 conv=notrunc")
    (is (thrown? AssertionError (run "lz4 -d /home/nathants/data/output.lz4 /dev/null")))
    (is (thrown? java.io.IOException (let [buf-size 1024]
                                       (with-open [in (LZ4CompatibleInputStream. (FileInputStream. "/home/nathants/data/output.lz4") (* 4096 1024) buf-size)]
                                         (dorun (line-seq (io/reader in)))))))))