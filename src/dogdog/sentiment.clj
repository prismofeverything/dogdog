(ns dogdog.sentiment
  (:import [java.util Properties]
           [edu.stanford.nlp.pipeline Annotation StanfordCoreNLP]
           [edu.stanford.nlp.ling.CoreAnnotations]
           [edu.stanford.nlp.sentiment.SentimentCoreAnnotations]
           [edu.stanford.nlp.neural.rnn RNNCoreAnnotations]))

(def pipeline
  (let [p (Properties.)
        _ (.setProperty p "annotators" "tokenize, ssplit, parse, sentiment")]
    (StanfordCoreNLP. p)))

(defn sentiment [text]
  (let [annotation (.process pipeline text)
        sentences (.get annotation edu.stanford.nlp.ling.CoreAnnotations$SentencesAnnotation)
        sentiments (map (fn [sentence]
                          (let [tree (.get sentence edu.stanford.nlp.sentiment.SentimentCoreAnnotations$AnnotatedTree)]
                            (. RNNCoreAnnotations getPredictedClass tree))) sentences)]
    sentiments))

(defn analyze [text]
  (sentiment text))

(def canned {
             0 [
                "Woahhhhh"
                "Calm down, dude"
                "High five!"
                "You might want to reexamine your assumptions."
                nil nil nil nil nil
                ]
             1 [
                "*hug*"
                "Let me offer my condolences"
                "I believe you"
                nil nil nil nil nil nil nil nil nil nil
                ]
             3 [
                "Yay!" nil nil nil nil nil nil nil
                ]
             4 [
                "I love you"
                "YOLO!"
                "Ba da bing, ba da boom!"
                "FUCKING BRILLIANT"
                nil nil nil nil nil nil
                ]
})

(defn respond [sentiment {:keys [text nick] :as msg}]
  (-> canned (get sentiment [nil]) rand-nth))
