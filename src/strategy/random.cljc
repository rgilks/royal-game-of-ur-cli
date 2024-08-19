(ns strategy.random
  (:require [game :refer [select-move]]))

(defmethod select-move :random [_ game]
  (when-let [possible-moves (game/get-possible-moves game)]
    (rand-nth possible-moves)))
