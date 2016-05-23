;; Copyright (c) 2016 Maria Carrasco
;;
;; This file is part of mrunner.
;;
;; mrunner is free software: you can redistribute it and/or modify
;; it under the terms of the GNU Affero General Public License as
;; published by the Free Software Foundation, either version 3 of the
;; License, or (at your option) any later version.
;;
;; mrunner is distributed in the hope that it will be useful, but
;; WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
;; Affero General Public License for more details.
;;
;; You should have received a copy of the GNU Affero General Public
;; License along with Mittagessen.  If not, see
;; <http://www.gnu.org/licenses/>.

(ns mrunner.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! timeout]]
            [cljs.core.match :refer-macros [match]]
            [cljs.core.match]
            [clojure.string :as str]
            [goog.events :as events]
            [mrunner.routes :as routes]))


;;
;;  Application state
;;  =================
;;

(defonce app-state
  (r/atom {:data nil
           :game nil
           :view [:init]}))

(def initial-state
  {:key nil
   :pos-y 0
   :pos-x 0
   :speed-x 0.1
   :speed-y 0
   :cur-time nil
   :down false
   })

(defn dbg [x]
  (println x)
  x)

(def jump-speed 1.6)
(def gravity -0.009)

;; 37 left, 38 up, 39 right, 40 down
(defn handle-key [state key]
  (case (.-keyCode key)
    38 (when (= (:pos-y @state) 0)
         (swap! state assoc :speed-y jump-speed))
    40 (swap! state assoc :down true)
    nil))

(defn handle-key-up [state key]
  (case (.-keyCode key)
    40 (swap! state assoc :down false)
    nil))


(defn game-loop [state end]
  (let [{:keys [cur-time]} @state
        next-time (js/performance.now)
        delta (- next-time cur-time)]
    (when-not (nil? cur-time)
      (swap! state update :pos-x + (* (:speed-x @state) delta))
      (swap! state update :pos-y #(max 0 (+ % (* (:speed-y @state) delta))))
      (swap! state update :speed-y + (* gravity delta)))
    (swap! state assoc :cur-time next-time))
  (when-not @end
    (js/requestAnimationFrame #(game-loop state end))))

(defn game-view [state]
  (r/with-let [end (atom false)
               keys [(events/listen js/window "keydown" #(handle-key state %))
                     (events/listen js/window "keyup" #(handle-key-up state %))]
               loop (js/requestAnimationFrame #(game-loop state end))]
    [:div.game
     "Welcome to mrunner"
     [:div.sky {:style {:background-position-x (/ (- (:pos-x @state )) 5)}}]
     [:div.road {:style {:background-position-x (- (:pos-x @state ))}}]
     [:div.runner {:class (when (:down @state) "down")
                   :style {:transform (str "translateY(-" (:pos-y @state) "px)")}}]]

    (finally (dorun (map events/unlistenByKey keys))
             (reset! end true))))

(defn main-view [state]
  (r/with-let [game-state (r/cursor state [:game])]
    [:div.game-wrapper
     [:h1 "mRunner"]
     (if @game-state
       [game-view game-state]
       [:button {:on-click #(reset! game-state initial-state)} "start"])]))


(defn not-found-view []
  [:div#not-found
   [:h1 "Page not found!"]])

(defn root-view [state]
  (match [(:view @state)]
         [[:init]]  [:div]
         [[:main]]  [main-view state]
         :else      [not-found-view]))

(defn init-components! [app-state]
  (r/render-component
    [root-view app-state]
    (.getElementById js/document "components")))


;;
;; Application
;; ===========
;;

(defn init-app! []
  (enable-console-print!)
  (prn "mrunner app started!")
  (routes/init-router! app-state)
  (init-components! app-state))

(defn on-figwheel-reload! []
  (prn "Figwheel reloaded...")
  (swap! app-state update-in [:__figwheel_counter] inc))

(init-app!)
