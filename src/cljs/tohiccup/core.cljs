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
    (html [:h2 "Under construction"])))

(defn main []
  (om/root converter-view app-state
           {:target (. js/document (getElementById "app"))}))
