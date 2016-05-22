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

(ns mrunner.routes
  (:require [clojure.string :as str]
            [secretary.core :as secretary :refer-macros [defroute]]
            [pushy.core :as pushy])
  (:import goog.History))


(defonce history (atom))

(defn nav! [token]
  (pushy/set-token! @history token))

(defn make-routes! [app]
  (defroute route-main "/" []
    (swap! app assoc-in [:view] [:main]))
  (defroute route-not-found "/not-found" []
    (swap! app assoc-in [:view] [:not-found])))

(defn- no-prefix [uri]
  (let [prefix (secretary/get-config :prefix)]
    (str/replace uri (re-pattern (str "^" prefix)) "")))

(defn init-history! []
  (when (aget js/window "MRUNNER_DEBUG_MODE")
    (print "MRUNNER_DEBUG_MODE enabled")
    (secretary/set-config! :prefix "/debug"))
  (letfn [(dispatch [uri] (secretary/dispatch! uri))
          (match-uri [uri]
            (if (secretary/locate-route (no-prefix uri))
              uri
              (route-not-found)))]
        (reset! history (pushy/pushy dispatch match-uri))
        (pushy/start! @history)))

(defn init-router! [app-state]
  (make-routes! app-state)
  (init-history!))
