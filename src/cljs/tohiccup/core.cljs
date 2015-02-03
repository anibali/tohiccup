(ns tohiccup.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [om-tools.dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [hickory.core :as hickory]
            [sablono.core :as html :refer-macros [html]]))

(defonce app-state (atom
                    {:html-text ""
                     :hiccup-text ""}))

(defn process-action [[action-name action-data]]
  (case action-name
    :update-hiccup
    (let [html-text action-data
          dom-node (hickory/parse html-text)
          new-hiccup-text (hickory/as-hiccup dom-node)]
      (swap! app-state assoc :hiccup-text new-hiccup-text))))

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
  (render-state [this {:keys [action-chan]}]
    (html [:div
           [:h2 "[:hiccup]"]
           [:textarea
            {:class "form-control"
             :value (@app-state :hiccup-text)
             :onChange (fn [e]
                         (let [new-value (.. e -target -value)]
                           (swap! app-state assoc :hiccup-text new-value)))}]])))

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
