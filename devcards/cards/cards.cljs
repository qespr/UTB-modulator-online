(ns ^:figwheel-hooks modulator.cards
  (:require [devcards.core]
            [modulator.online.core]))

(enable-console-print!)

(defn render []
  (devcards.core/start-devcard-ui!))

(defn ^:after-load render-on-reload []
  (render))

(render)
