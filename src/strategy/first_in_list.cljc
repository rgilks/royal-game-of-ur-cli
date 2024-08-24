(ns strategy.first-in-list
  (:require [engine :refer [select-move]]))

(defmethod select-move :first-in-list [_ game]
  (when-let [possible-moves (engine/get-possible-moves game)]
    (first possible-moves)))