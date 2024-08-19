(ns strategy.first-in-list
  (:require [state :refer [select-move]]))

(defmethod select-move :first-in-list [_ game]
  (when-let [possible-moves (state/get-possible-moves game)]
    (first possible-moves)))