(ns strategy.random
  (:require [state :refer [select-move]]))

(defmethod select-move :random [_ possible-moves _]
  (when (seq possible-moves)
    (rand-nth possible-moves)))