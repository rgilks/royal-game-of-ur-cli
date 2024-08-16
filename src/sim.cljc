(ns sim
  (:require [config]
            [schema]
            [state]
            [view]))

(defn play-sim [game-state rolls]
  (loop [state game-state
         remaining-rolls rolls]

    (when (#{:choose-action :end-game} (:state state))
      (view/print-game-state state))

    (let [[new-state new-rolls]
          (state/transition state remaining-rolls)]
      (if (or (= (:state new-state) :end-game)
              (and (= (:state new-state) :roll-dice) (empty? new-rolls)))
        [new-state new-rolls]
        (recur new-state new-rolls)))))

;; TODO: move to util
(defn rolls []
  (reduce + (repeatedly 4 #(rand-int 2))))

(defn play
  ([rolls]
   (loop [game-state (state/initialize-game)
          remaining-rolls rolls]
     (if (or (= (:state game-state) :end-game) (empty? remaining-rolls))
       (do
         (view/print-game-state game-state)
         (when (= (:state game-state) :end-game)
           (view/print-winner-message (:current-player game-state)))
         game-state)
       (let [[new-state new-rolls]
             (play-sim game-state remaining-rolls
                       {:move-strategy :first-in-list})]
         (recur new-state new-rolls)))))
  ([] (play (repeatedly rolls))))

(defn -main []
  (view/print-welcome-message)
  (play))

(comment
  (play))