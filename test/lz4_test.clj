(ns lz4-test
  (:require [clojure.java
             [io :as io]
             [shell :as sh]]
            [clojure.test :refer :all]
            [clojure.test.check
             [clojure-test :refer [defspec]]
             [generators :as gen]
             [properties :as prop]]
            [clojure.string :as s])
  (:import [java.io File IOException FileInputStream FileOutputStream ByteArrayInputStream ByteArrayOutputStream]
           [net.jpountz.lz4 LZ4InputStream LZ4OutputStream]
           net.jpountz.xxhash.XXHashFactory))

(defn times
  [n]
  (-> "TEST_CHECK_FACTOR" System/getenv (or "1") Double/parseDouble (* n) long))

(defn run [& args]
  (let [cmd (apply str (interpose " " args))
        res (clojure.java.shell/sh "bash" "-c" cmd)]
    (assert (-> res :exit (= 0)) (assoc res :cmd cmd))
    (.trim (:out res))))

(defn run [& args]
  (let [cmd (apply str (interpose " " args))
        res (sh/sh "bash" "-c" cmd)]
    (assert (-> res :exit (= 0)) (assoc res :cmd cmd))
    (.trim (:out res))))

(def gen-lines (->> gen/string-alphanumeric
                 (gen/scale #(* 0.01 %))
                 gen/vector))

;; TODO would this be better without test.check? just generate random
;; lines from /usr/sharea/dict/words and write them to a file,
;; compress it to a file, decompress to another file, xxhash original
;; and decompress and assert equal.
(defspec lz4-spec (times 10)
  (let [tmp-file (run "tempfile")]
    (prop/for-all [lines gen-lines]
      (let [txt (str (s/join "\n" lines) "\n")
            out (FileOutputStream. tmp-file)
            out-stream (LZ4OutputStream. out (* 64 1024) 0)
            _ (io/copy txt out-stream)
            _ (.close out)
            in (FileInputStream. tmp-file)
            in-stream (LZ4InputStream. in (* 64 1024) (* 64 1024))
            lines (line-seq (io/reader in-stream))]
        (.close in)
        (is (= txt (str (s/join "\n" lines) "\n")))))))

(defn sum
  [path]
  (let [h (.newStreamingHash64 (XXHashFactory/nativeInstance) 0)]
    (with-open [in (FileInputStream. path)]
      (let [buffer (make-array Byte/TYPE (* 64 1024))]
        (loop []
          (let [size (.read in buffer)]
            (when (pos? size)
              (do (.update h buffer 0 size)
                  (recur)))))
        (Long/toHexString (.getValue h))))))

;; TODO generate test data from /usr/share/dict/words and store in tmp
;; file
(deftest lz4
  []
  (testing "can encode"
    (time
     (with-open [in (FileInputStream. "/home/nathants/data/input")
                 out (LZ4OutputStream. (FileOutputStream. "/home/nathants/data/output.lz4") (* 64 1024) 0)]
       (io/copy in out)))
    (time
     (with-open [in (FileInputStream. "/home/nathants/data/input")
                 out (LZ4OutputStream. (FileOutputStream. "/home/nathants/data/output.lz4.small") (* 64 1024) 3)]
       (io/copy in out))))
  (testing "compression level is working"
    (is (< (.length (File. "/home/nathants/data/output.lz4.small"))
           (.length (File. "/home/nathants/data/output.lz4")))))
  (testing "can decode"
    (time
     (let [buf-size 1024]
       (with-open [in (LZ4InputStream. (FileInputStream. "/home/nathants/data/output.lz4") (* 64 1024) buf-size)
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
    (is (thrown? IOException (let [buf-size 1024]
                               (with-open [in (LZ4InputStream. (FileInputStream. "/home/nathants/data/output.lz4") (* 64 1024) buf-size)]
                                 (dorun (line-seq (io/reader in)))))))))
