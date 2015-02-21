(ns tohiccup.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [om-tools.dom :include-macros true]
            [tohiccup.beautify :refer [beautify-hiccup]]
            [cljs.core.async :refer [put! chan <!]]
            [hickory.core :as hickory]
            [sablono.core :as html :refer-macros [html]]))

(defonce app-state (atom
                    {:html-text ""
                     :hiccup-text ""}))
(defonce hiccup-ace-editor (atom nil))
(defonce html-ace-editor (atom nil))

(defn process-action [[action-name action-data]]
  (case action-name
    :update-hiccup
    (let [html-text action-data
          nodes (hickory/parse-fragment html-text)
          new-hiccup-text (beautify-hiccup (map hickory/as-hiccup nodes) "")]
      (swap! app-state assoc :hiccup-text new-hiccup-text))
    :update-html
    "TODO: Update HTML from Hiccup input"))

(defn set-ace-editor-value [ace-editor value]
  (let [cursor (.getCursorPositionScreen ace-editor)]
    (.setValue ace-editor (@app-state :hiccup-text) cursor)))

(defcomponentk html-input-view
  "HTML text input view"
  [state owner]
  (display-name [this] "HtmlArea")
  (render [this] (html [:div
                        [:h2 "<html>"]
                        [:div {:ref "html-ace-editor"}]]))
  (did-mount [this]
    (let [action-chan (@state :action-chan)
          ace-editor (.edit js/ace (om/get-node owner "html-ace-editor"))
          ace-session (.getSession ace-editor)]
      (.setOptions ace-editor (js-obj "minLines" 30 "maxLines" 30))
      (.setTheme ace-editor "ace/theme/github")
      (.setMode ace-session "ace/mode/html")
      (reset! html-ace-editor ace-editor)
      (.on ace-session "change"
           #(put! action-chan [:update-hiccup
                               (.getValue @html-ace-editor)])))))
      ;(set-ace-editor-value ace-editor (@app-state :html-text))
  ;(will-update [this _ {:keys [action-chan]}]
  ;  (set-ace-editor-value @html-ace-editor (@app-state :html-text))))

(defcomponentk hiccup-input-view
  "Hiccup text input view"
  [state owner]
  (display-name [this] "HiccupArea")
  (render [this] (html [:div
                        [:h2 "[:hiccup]"]
                        [:div {:ref "hiccup-ace-editor"}]]))
  (did-mount [this]
    (let [action-chan (@state :action-chan)
          ace-editor (.edit js/ace (om/get-node owner "hiccup-ace-editor"))
          ace-session (.getSession ace-editor)]
      (.setOptions ace-editor (js-obj "minLines" 30 "maxLines" 30))
      (.setTheme ace-editor "ace/theme/github")
      (.setMode ace-session "ace/mode/clojure")
      (.setReadOnly ace-editor true) ; Read-only for now
      (reset! hiccup-ace-editor ace-editor)
      (.on ace-session "change"
           #(put! action-chan [:update-html
                               (.getValue @hiccup-ace-editor)]))
      (set-ace-editor-value ace-editor (@app-state :hiccup-text))))
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
