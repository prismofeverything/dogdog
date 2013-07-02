(ns gort.twp
  (:require [clojure.java.jdbc :as jdbc]))

;; make this configurable
(def +db-path+  "resources/words.db")
(def +db-specs+ {:classname "org.sqlite.JDBC",
                 :subprotocol "sqlite",
                 :subname +db-path+})

(def +noun-query+ "select word from noun order by random() limit 0,1")
(def +adjective-query+ "select word from adjective order by random() limit 0,1")

(defn random-word [q]
  (jdbc/with-connection +db-specs+
    (jdbc/with-query-results results [q]
        ((first results) :word))))

(defn random-adjective []
  (random-word +adjective-query+))

(defn random-noun []
  (random-word +noun-query+))

(defn add-word-of-type [word t]
  (jdbc/with-connection +db-specs+
    (jdbc/insert-records t {:word word})))

(defn three-word-poem []
  (let [rv (rand 100)
        words (if (< 33 rv)
                [(random-adjective) (random-noun) (random-noun)]
                (if (< 67 rv)
                  [(random-adjective) (random-adjective) (random-noun)]
                  [(random-noun) (random-noun) (random-noun)]))]
        (str (first words) " " (second words) " " (nth words 2))))
