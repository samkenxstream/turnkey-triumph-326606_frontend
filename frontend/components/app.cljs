(ns frontend.components.app
  (:require [cljs.core.async :as async :refer [>! <! alts! chan sliding-buffer put! close!]]
            [frontend.components.build :as build-com]
            [frontend.components.dashboard :as dashboard]
            [frontend.components.add-projects :as add-projects]
            [frontend.components.footer :as footer]
            [frontend.components.inspector :as inspector]
            [frontend.components.key-queue :as keyq]
            [frontend.components.navbar :as navbar]
            [frontend.components.placeholder :as placeholder]
            [frontend.components.common :as common]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ankha.core :as ankha]
            [sablono.core :as html :refer-macros [html]]))

(def keymap
  (atom nil))

(defn loading [app owner opts]
  (reify
    om/IRender
    (render [_] (html [:div.loading-spinner common/spinner]))))

(defn dominant-component [app-state]
  (condp = (get-in app-state [:navigation-point])
    :build build-com/build
    :dashboard dashboard/dashboard
    :add-projects add-projects/add-projects
    :loading loading))

(defn app [app owner opts]
  (reify
    om/IRender
    (render [_]
      (let [controls-ch (get-in opts [:comms :controls])
            persist-state! #(put! controls-ch [:state-persisted])
            restore-state! #(put! controls-ch [:state-restored])
            dom-com (dominant-component app)]
        (reset! keymap {["ctrl+s"] persist-state!
                        ["ctrl+r"] restore-state!})
        (html/html
         [:div.inner
          (om/build keyq/KeyboardHandler app
                    {:opts {:keymap keymap
                            :error-ch (get-in app [:comms :errors])}})
          ;; for some reason this makes things render 10x slower
          ;; (om/build inspector/inspector app {:opts opts})
          [:header
           (om/build navbar/navbar app {:opts opts})]
          [:main
           (om/build dom-com app {:opts opts})]
          [:footer
           (footer/footer)]])))))
