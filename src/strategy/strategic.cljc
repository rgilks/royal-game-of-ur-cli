(ns strategy.strategic
  (:require [config]
            [state :refer [select-move]]))

(defmethod select-move :strategic [_ possible-moves game-state]
  (when (seq possible-moves)
    (let [player (:current-player game-state)
          path (get-in config/board [:paths player])
          last-square (last path)]
      (or
       ;; Prioritize moving a piece off the board if possible
       (first (filter #(= (:to %) :off-board) possible-moves))
       ;; Next, prioritize moving to the last square on the path
       (first (filter #(= (:to %) last-square) possible-moves))
       ;; Then, prioritize capturing opponent's pieces
       (first (filter :captured possible-moves))
       ;; Next, prioritize moving to a rosette
       (first (filter #(contains? (:rosettes config/board) (:to %)) possible-moves))
       ;; Finally, move the furthest piece along the path
       (last (sort-by (fn [move]
                        (cond
                          (= (:from move) :entry) -1
                          (= (:to move) :off-board) (count path)
                          :else (or (first (keep-indexed #(when (= %2 (:from move)) %1) path)) 0)))
                      possible-moves))))))
