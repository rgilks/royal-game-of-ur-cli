(ns strategy.strategic
  (:require [config]
            [game :refer [select-move]]))

(defmethod select-move :strategic [_ game]
  (let [possible-moves (game/get-possible-moves game)
        player (:current-player game)
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

      ;; move the piece furthest back first
     (last possible-moves))))
