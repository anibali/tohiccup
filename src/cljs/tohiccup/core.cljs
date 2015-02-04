(ns tohiccup.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [om-tools.dom :include-macros true]
            [clojure.string :refer [join trim blank?]]
            [cljs.core.async :refer [put! chan <!]]
            [hickory.core :as hickory]
            [sablono.core :as html :refer-macros [html]]))

(defonce app-state (atom
                    {:html-text ""
                     :hiccup-text ""}))

(defonce hiccup-ace-editor (atom nil))

(declare beautify-hiccup)

(defn beautify-hiccup-coll [hiccup-expression indent]
  (join (str "\n" indent)
        (filter #(not= % "")
                (map #(beautify-hiccup % (str " " indent))
                     hiccup-expression))))

(defn beautify-hiccup [hiccup-expression indent]
  (cond
   (seq? hiccup-expression) (beautify-hiccup-coll hiccup-expression indent)
   (vector? hiccup-expression) (str "[" (beautify-hiccup-coll hiccup-expression indent) "]")
   (= hiccup-expression {}) ""
   (blank? hiccup-expression) ""
   :else (pr-str hiccup-expression)))

(defn process-action [[action-name action-data]]
  (case action-name
    :update-hiccup
    (let [html-text action-data
          nodes (hickory/parse-fragment html-text)
          new-hiccup-text (beautify-hiccup (map hickory/as-hiccup nodes) "")]
      (swap! app-state assoc :hiccup-text new-hiccup-text))
    :update-html
    "TODO"))

(defn set-ace-editor-value [ace-editor value]
  (let [cursor (.getCursorPositionScreen ace-editor)]
    (.setValue ace-editor (@app-state :hiccup-text) cursor)))

(defcomponentk html-input-view
  "HTML text input view"
  [state owner]
  (display-name [this] "HtmlArea")
  (render-state [this {:keys [action-chan]}]
    (html [:div
           [:h2 "<html>"]
           [:textarea
            {:class "form-control"
             :value (@app-state :html-text)
             :onChange (fn [e]
                         (let [new-value (.. e -target -value)]
                           (put! action-chan [:update-hiccup new-value])
                           (swap! app-state assoc :html-text new-value)))}]])))

(defcomponentk hiccup-input-view
  "Hiccup text input view"
  [state owner]
  (display-name [this] "HiccupArea")
  (render [this] (html [:div
                        [:h2 "[:hiccup]"]
                        [:div#hiccup-ace-editor]]))
  (did-mount [this]
    (let [ace-editor (.edit js/ace "hiccup-ace-editor")]
      (.setOptions ace-editor (js-obj "minLines" 25 "maxLines" 25))
      (.setTheme ace-editor "ace/theme/github")
      (.setMode (.getSession ace-editor) "ace/mode/clojure")
      (.setReadOnly ace-editor true) ; Read-only for now
      (.. ace-editor
          getSession
          (on "change" #(put! (@state :action-chan) [:update-html (.getValue @hiccup-ace-editor)])))
      (set-ace-editor-value ace-editor (@app-state :hiccup-text))
      (reset! hiccup-ace-editor ace-editor)))
  (will-update [this _ {:keys [action-chan]}]
    (set-ace-editor-value @hiccup-ace-editor (@app-state :hiccup-text))))

(defcomponentk converter-view
  "Top-level HTML <-> Hiccup converter component"
  [data owner]
  (display-name [this] "HiccupConverter")
  (init-state [_]
    {:action-chan (chan)})
  (will-mount [_]
    (let [action-chan (om/get-state owner :action-chan)]
      (go-loop []
               (process-action (<! action-chan))
               (recur))))
  (render-state [this {:keys [action-chan html-text]}]
    (html [:div {:class "row"}
           [:div {:class "col-sm-6"}
            (om/build html-input-view data
                      {:init-state {:action-chan action-chan}})]
           [:div {:class "col-sm-6"}
            (om/build hiccup-input-view data
                      {:init-state {:action-chan action-chan}})]])))

(defn main []
  (om/root converter-view app-state
           {:target (. js/document (getElementById "app"))}))
