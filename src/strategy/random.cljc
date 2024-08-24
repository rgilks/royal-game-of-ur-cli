(ns strategy.random
  (:require [engine :refer [select-move]]))

(defmethod select-move :random [_ game]
  (when-let [possible-moves (engine/get-possible-moves game)]
    (rand-nth possible-moves)))
