(ns strategy.first-in-list
  (:require [game :refer [select-move]]))

(defmethod select-move :first-in-list [_ game]
  (when-let [possible-moves (game/get-possible-moves game)]
    (first possible-moves)))