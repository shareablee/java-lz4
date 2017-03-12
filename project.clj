(defproject lz4 "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0" :scope "provided"]]
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :jar-exclusions [#".*\.clj"]
  :resource-paths ["src/main/resource"]
  :auto-clean false
  :aot :all)
