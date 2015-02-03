(ns tohiccup.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [om-tools.dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [sablono.core :as html :refer-macros [html]]))

(defonce app-state (atom {}))

(defn process-action [_])

(defcomponentk html-input-view
  "HTML text input view"
  [data owner]
  (display-name [this] "HtmlArea")
  (render-state [this {:keys [action-chan new-item-title]}]
    (html [:div
           [:h2 "<html>"]
           [:textarea {:class "form-control"}]])))

(defcomponentk hiccup-input-view
  "Hiccup text input view"
  [data owner]
  (display-name [this] "HiccupArea")
  (render-state [this {:keys [action-chan new-item-title]}]
    (html [:div
           [:h2 "[:hiccup]"]
           [:textarea {:class "form-control"}]])))

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
  (render-state [this {:keys [action-chan new-item-title]}]
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
