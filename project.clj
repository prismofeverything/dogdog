(defproject dogdog "0.0.4"
  :description "An affable irc bot that mimics your own grammatical structure as it goes along"
  :url "http://github.com/prismofeverything/dogdog"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :jvm-opts ["-Xmx2G"]
  :main dogdog.core
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/java.jdbc "0.3.0-alpha4"]
                 [org.xerial/sqlite-jdbc "3.7.2"]
                 [markov "0.0.19"]
                 [zalgo "11.6.13"]
                 [irclj "0.5.0-alpha2"]
                 [org.clojure/tools.cli "0.3.1"]
                 [twitter-api "0.7.8"]
                 [edu.stanford.nlp/stanford-corenlp "3.4.1"]
                 [edu.stanford.nlp/stanford-corenlp "3.4.1" :classifier "models"]
                 ])
