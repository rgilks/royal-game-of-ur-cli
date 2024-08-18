(ns strategy.first-in-list
  (:require [state :refer [select-move]]))

(defmethod select-move :first-in-list [_ possible-moves _]
  (first possible-moves))
