(ns gort.novelty
  (:require [clojure.string :as string]))

(defn choose
  [p]
  (>= p (rand)))

(defn novelty
  [past novel ps]
  (if (choose (first ps))
    (novel past)
    (loop [seek past
           inner-ps (rest ps)]
      (if-not (and (seq seek) (seq inner-ps))
        (novel past)
        (if-not (choose (first inner-ps))
          (first seek)
          (recur (rest seek) (rest inner-ps)))))))

(defn novelty-chain
  [novel ps]
  (iterate
   (fn [past]
     (let [future (novelty past novel ps)]
       (cons future past)))
   nil))

(def alphabet "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")

(defn rand-alpha
  []
  (rand-nth alphabet))

(defn novelty-string
  [length p]
  (let [chain (novelty-chain
               (fn [past]
                 (rand-alpha))
               (iterate identity p))
        generate (nth chain length)]
    (string/join (reverse generate))))
