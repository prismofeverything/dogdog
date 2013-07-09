(ns gort.twp
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]))

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

(defn forget-word-of-type [word t]
  (jdbc/with-connection +db-specs+
    (jdbc/delete-rows t ["word = ?" word])))

(defn three-word-poem
  ([] (three-word-poem {}))
  ([options]
    (let [adjectives (concat
                      (remove nil? [(:adjective options)])
                      (take 3 (repeatedly random-adjective)))
          nouns (concat
                 (remove nil? [(:noun options)])
                 (take 3 (repeatedly random-noun)))
          adjective-count (min 2 (+ (int (rand 3))
                                    (if (:adjective options) 1 0)))
          noun-count (- 3 adjective-count)
          words (concat (take adjective-count adjectives) (take noun-count nouns))]
      (string/join " " words))))

;;(defn three-word-poem []
;;  (let [rv (rand 100)
;;        words (if (< 33 rv)
;;                [(random-adjective) (random-noun) (random-noun)]
;;                (if (< 67 rv)
;;                  [(random-adjective) (random-adjective) (random-noun)]
;;                  [(random-noun) (random-noun) (random-noun)]))]
;;        (str (first words) " " (second words) " " (nth words 2))))
