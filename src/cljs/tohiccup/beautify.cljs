(ns tohiccup.beautify
  (:require [clojure.string :refer [join split trim blank?]]))

(declare beautify-hiccup)

(defn beautify-hiccup-coll [hiccup-expression indent]
  (join (str "\n" indent)
        (filter some?
                (map #(beautify-hiccup % (str " " indent))
                     hiccup-expression))))

(defn beautify-hiccup-vector [hiccup-expression indent]
  (str
   "["
   (if (map? (second hiccup-expression))
     (let [tag-name (first hiccup-expression)
           attrs (second hiccup-expression)
           id (:id attrs)
           classes (split (:class attrs) #"\s")
           new-attrs (dissoc attrs :id :class)
           tag-name-with-id (if (some? id) (str (name tag-name) "#" id) (name tag-name))
           tag-name-with-classes (if (seq classes) (str tag-name-with-id "." (join "." classes)) tag-name-with-id)
           new-tag-name (keyword tag-name-with-classes)]
       (beautify-hiccup-coll (assoc hiccup-expression 0 new-tag-name 1 new-attrs) indent))
     (beautify-hiccup-coll hiccup-expression indent))
   "]"))

(defn beautify-hiccup [hiccup-expression indent]
  (cond
   (seq? hiccup-expression) (beautify-hiccup-coll hiccup-expression indent)
   (vector? hiccup-expression) (beautify-hiccup-vector hiccup-expression indent)
   (= hiccup-expression {}) nil
   (blank? hiccup-expression) nil
   :else (pr-str hiccup-expression)))
