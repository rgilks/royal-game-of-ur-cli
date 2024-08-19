(ns strategy.random
  (:require [state :refer [select-move]]))

(defmethod select-move :random [_ game]
  (when-let [possible-moves (state/get-possible-moves game)]
    (rand-nth possible-moves)))
