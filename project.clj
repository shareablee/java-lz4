(defproject lz4 "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0" :scope "provided"]]
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :auto-clean false
  :aot :all
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.9.0"]]}}
  :aliases {"jar" ["do" "clean" ["run" "-m" "jni/build"] "jar"]})
