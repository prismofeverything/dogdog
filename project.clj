(defproject gort "0.0.3"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :jvm-opts ["-Xmx2G"]
  :main gort.core
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/java.jdbc "0.3.0-alpha4"]
                 [org.xerial/sqlite-jdbc "3.7.2"]
                 [markov "0.0.17"]
                 [zalgo "11.6.13"]
                 [irclj "0.5.0-alpha2"]])
