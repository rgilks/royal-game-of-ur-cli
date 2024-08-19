(ns validate
  (:require [config]
            [malli.core :as m]
            [malli.error :as me]
            [schema]
            [util :refer [print-line]]))

(defn total-pieces [state]
  (every? (fn [[player-key player-data]]
            (let [{:keys [in-hand off-board]} player-data
                  on-board (count (filter #{player-key} (:board state)))]
              (= 7 (+ in-hand off-board on-board))))
          (:players state)))

(defn game [state]
  (if-let [error (m/explain schema/game state)]
    (do
      (print-line "Invalid game state:")
      (print-line state)
      (print-line "Validation error:")
      (print-line error)
      (throw (ex-info "Invalid game state structure" (me/humanize error))))
    (if-not (total-pieces state)
      (throw (ex-info "Invalid total pieces"
                      {:error "Total pieces for each player must be exactly 7"}))
      state)))
