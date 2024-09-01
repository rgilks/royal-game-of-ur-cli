(ns new-engine-test
  (:require [clojure.test :refer [are deftest is testing]]
            [new-engine :as engine]))

(deftest test-initial-state
  (let [initial (engine/initial-state)]
    (engine/print-game-state initial)
    (are [expected actual] (= expected actual)
      7 (engine/get-off-board initial :A)
      7 (engine/get-off-board initial :B)
      0 (engine/get-completed initial :A)
      0 (engine/get-completed initial :B)
      :A (engine/get-current-player initial)
      0 (engine/get-dice-roll initial)
      false (engine/get-extra-turn initial))))

(deftest test-bitwise-operations
  (let [state (-> (engine/initial-state)
                  (engine/set-position 3 1)
                  (engine/set-position 8 2)
                  (engine/set-off-board :A 5)
                  (engine/set-off-board :B 6)
                  (engine/set-completed :A 2)
                  (engine/set-completed :B 1)
                  (engine/set-dice-roll 3)
                  (engine/set-current-player :B)
                  (engine/set-extra-turn true))]
    (are [expected actual] (= expected actual)
      1 (engine/get-position state 3)
      2 (engine/get-position state 8)
      5 (engine/get-off-board state :A)
      6 (engine/get-off-board state :B)
      2 (engine/get-completed state :A)
      1 (engine/get-completed state :B)
      3 (engine/get-dice-roll state)
      :B (engine/get-current-player state)
      true (engine/get-extra-turn state))))

#_(deftest test-move-piece
    (let [initial (-> (engine/initial-state)
                      (engine/set-position 3 1)  ; A piece
                      (engine/set-position 8 2)  ; B piece
                      (engine/set-position 11 1) ; A piece on safe square
                      (engine/set-position 7 1)) ; A piece to move off-board
          _ (println "Initial state:" (engine/binary-str initial))
          move-a (engine/apply-move initial 3 8 :A)
          _ (println "After move-a:" (engine/binary-str move-a))
          move-b (engine/apply-move initial 8 11 :B)
          _ (println "After move-b:" (engine/binary-str move-b))
          move-off-board (engine/apply-move initial 7 :off-board :A)
          _ (println "After move-off-board:" (engine/binary-str move-off-board))
          move-to-rosette (engine/apply-move initial 3 6 :A)
          _ (println "After move-to-rosette:" (engine/binary-str move-to-rosette))
          move-from-rosette (engine/apply-move (engine/set-position initial 6 1) 6 8 :A)
          _ (println "After move-from-rosette:" (engine/binary-str move-from-rosette))]
      (are [expected actual] (= expected actual)
        0 (engine/get-position move-a 3)
        1 (engine/get-position move-a 8)
        8 (engine/get-off-board move-a :B)
        true (engine/get-extra-turn move-a)  ; rosette at 8
        :A (engine/get-current-player move-a)  ; extra turn, still A's turn

        0 (engine/get-position move-b 8)
        2 (engine/get-position move-b 11)
        1 (engine/get-position initial 11)  ; A piece still on safe square
        true (engine/get-extra-turn move-b)  ; rosette at 11
        :B (engine/get-current-player move-b)  ; extra turn, still B's turn

        0 (engine/get-position move-off-board 7)
        1 (engine/get-completed move-off-board :A)
        :B (engine/get-current-player move-off-board)  ; turn switches to B

        1 (engine/get-position move-to-rosette 6)
        true (engine/get-extra-turn move-to-rosette)
        :A (engine/get-current-player move-to-rosette)  ; extra turn, still A's turn

        1 (engine/get-position move-from-rosette 8)
        false (engine/get-extra-turn move-from-rosette)
        :B (engine/get-current-player move-from-rosette))))  ; turn switches to B

#_(deftest test-get-possible-moves
    (let [state (-> (engine/initial-state)
                    (engine/set-position 3 1)
                    (engine/set-position 8 2)
                    (engine/set-dice-roll 2))
          blocked-state (reduce #(engine/set-position %1 %2 1) state (range 20))]
      (are [player roll expected]
           (= expected
              (count (engine/get-possible-moves (-> state
                                                    (engine/set-current-player player)
                                                    (engine/set-dice-roll roll)))))
        :A 2 2  ; move from 3 to 1, or off-board to 2
        :B 2 2  ; move from 8 to 10, or off-board to 18
        :A 0 0  ; no moves possible with roll of 0
        :A 4 2  ; move from 3 to 11 (safe square), or off-board to 3
        :B 4 1) ; only move from off-board to 19 (8 is blocked)
      (is (empty? (engine/get-possible-moves blocked-state))))) ; no moves possible on full board

#_(deftest test-apply-move
    (let [initial (-> (engine/initial-state)
                      (engine/set-position 3 1)
                      (engine/set-dice-roll 2))
          move {:from 3 :to 1}
          rosette-move {:from 3 :to 6}
          new-state (engine/apply-move initial move)
          rosette-state (engine/apply-move (engine/set-dice-roll initial 3) rosette-move)]
      (are [expected actual] (= expected actual)
        0 (engine/get-position new-state 3)
        1 (engine/get-position new-state 1)
        :B (engine/get-current-player new-state)
        0 (engine/get-dice-roll new-state)

        1 (engine/get-position rosette-state 6)
        :A (engine/get-current-player rosette-state)
        0 (engine/get-dice-roll rosette-state))))

(deftest test-game-over-and-winner
  (are [completed-a completed-b expected-over? expected-winner]
       (let [state (-> (engine/initial-state)
                       (engine/set-completed :A completed-a)
                       (engine/set-completed :B completed-b))]
         (and (= expected-over? (engine/game-over? state))
              (= expected-winner (engine/winner state))))
    6 6 false nil
    7 6 true :A
    6 7 true :B
    7 7 true :A))  ; A wins in case of a tie (shouldn't happen in normal play)

(defn- all-rolls-in-range? [rolls]
  (every? #(<= 0 % 4) rolls))

(defn- probability-within-range? [rolls value min-prob max-prob]
  (let [c (count (filter #(= % value) rolls))
        probability (/ c (count rolls))]
    (<= min-prob probability max-prob)))

(defn- distribution-correct? [rolls]
  (let [frequencies (frequencies rolls)
        total (count rolls)]
    (every? (fn [[value count]]
              (let [probability (/ count total)]
                (if (or (= value 0) (= value 4))
                  (<= 0.05 probability 0.08)  ; roughly 1/16
                  (<= 0.15 probability 0.28))))  ; roughly 3/16
            frequencies)))

#_(deftest test-roll-dice
    (let [rolls (take 1000 (repeatedly engine/roll-dice))]
      (are [description check] check
        "All rolls are between 0 and 4"
        (all-rolls-in-range? rolls)

        "Probability of rolling 0 is roughly 1/16"
        (probability-within-range? rolls 0 0.05 0.08)

        "Probability of rolling 4 is roughly 1/16"
        (probability-within-range? rolls 4 0.05 0.08)

        "Distribution of all roll values is correct"
        (distribution-correct? rolls))))

#_(deftest test-special-rules
    (let [initial (engine/initial-state)]
      (testing "Rosette landings"
        (are [from to] (engine/get-extra-turn (engine/move-piece (engine/set-position initial from 1) from to :A))
          3 6   ; First rosette
          2 11  ; Middle rosette
          1 16  ; Last rosette for player A
          15 0)) ; First rosette again

      (testing "Safe square (middle rosette)"
        (let [state (-> initial
                        (engine/set-position 10 1)  ; A piece
                        (engine/set-position 11 2)) ; B piece on safe square
              new-state (engine/move-piece state 10 11 :A)]
          (is (= 2 (engine/get-position new-state 11)) "B piece should not be captured on safe square")))

      (testing "Capture"
        (let [state (-> initial
                        (engine/set-position 9 1)   ; A piece
                        (engine/set-position 10 2)) ; B piece
              new-state (engine/move-piece state 9 10 :A)]
          (is (= 1 (engine/get-position new-state 10)) "A should capture B's piece")
          (is (= 8 (engine/get-off-board new-state :B)) "B's captured piece should return to off-board")))

      (testing "Moving off the board"
        (let [state (engine/set-position initial 7 1)
              new-state (engine/move-piece state 7 :off-board :A)]
          (is (= 0 (engine/get-position new-state 7)) "Piece should be removed from the board")
          (is (= 1 (engine/get-completed new-state :A)) "Completed pieces should increment")))))

#_(deftest test-game-flow
    (let [initial-state (engine/initial-state)]
      (testing "Full game simulation"
        (loop [state initial-state
               moves 0]
          (if (or (engine/game-over? state) (> moves 1000))
            (is (engine/game-over? state) "Game should end within 1000 moves")
            (let [roll (engine/roll-dice)
                  state (engine/set-dice-roll state roll)
                  possible-moves (engine/get-possible-moves state)]
              (if (seq possible-moves)
                (recur (engine/apply-move state (first possible-moves)) (inc moves))
                (recur (update state :current-player #(if (= % :A) :B :A)) moves))))))

      (testing "No possible moves"
        (let [state (-> initial-state
                        (engine/set-current-player :A)
                        (engine/set-dice-roll 0))
              new-state (if (seq (engine/get-possible-moves state))
                          (engine/apply-move state (first (engine/get-possible-moves state)))
                          (update state :current-player #(if (= % :A) :B :A)))]
          (is (= :B (engine/get-current-player new-state)) "Player should switch when no moves are possible")))))
