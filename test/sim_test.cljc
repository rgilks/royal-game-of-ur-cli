(ns sim-test
  (:require [clojure.test :refer [are deftest is testing]]
            [config]
            [sim]
            [state-machine :as ur]))

(def test-board
  [:A nil :B :A nil nil :B :A nil :B nil nil nil
   nil nil nil :A :B nil nil nil nil :A :B])

(def test-game-state
  {:board test-board
   :players {:A {:in-hand 2 :off-board 0}
             :B {:in-hand 2 :off-board 0}}
   :current-player :A
   :roll 2
   :state :choose-action
   :selected-move nil})

(deftest test-play-turn
  (testing "play-turn advances game state"
    (let [initial-state (ur/initialize-game)
          [new-state _] (sim/play-sim initial-state (repeat 4 2) {})]
      (is (not= initial-state new-state))
      (is (contains? #{:roll-dice :end-game} (:state new-state))))))

(deftest test-play-game
  (testing "play-game reaches end state"
    (let [final-state (sim/play (repeatedly 1000 #(rand-int 5)))]
      (is (= :end-game (:state final-state)))
      (is (ur/game-over? final-state)))))

(deftest test-render-cell
  (testing "render-cell returns correct symbols"
    (are [board idx expected] (= expected (sim/render-cell board idx))
      test-board 0 "1"
      test-board 1 "-"
      test-board 2 "2"
      test-board 4 " "  ; Excluded cell
      (assoc test-board 0 :rosette) 0 "✸")))

(deftest test-play-sim
  (testing "play-sim advances game through multiple states"
    (let [initial-state (assoc (ur/initialize-game) :state :roll-dice)
          [final-state _] (sim/play-sim initial-state [2 3 1 0 2 1] {})]
      (is (not= (:board initial-state) (:board final-state)))
      (is (some? (some identity (:board final-state)))))))

(deftest test-print-board
  (testing "print-board doesn't throw exceptions"
    (is (nil? (sim/print-board test-board)))))

(deftest test-print-game-state
  (testing "print-game-state handles different states"
    (are [state] (nil? (sim/print-game-state (assoc test-game-state :state state)))
      :choose-action
      :switch-turns
      :land-on-rosette
      :move-piece-off-board)))

(deftest test-full-game-simulation
  (testing "Simulating a full game with predetermined dice rolls"
    (let [roll-sequence (cycle [0 1 2 3 4])
          final-state (sim/play roll-sequence)]

      (is (= :end-game (:state final-state))
          "Game should end after a long sequence of rolls")

      (is (or (= 7 (get-in final-state [:players :A :off-board]))
              (= 7 (get-in final-state [:players :B :off-board])))
          "One player should have all 7 pieces off the board")

      (is (= {:board
              [:A :A nil nil nil nil nil nil :A nil :A :A :A
               nil nil nil nil nil nil nil nil nil nil nil],
              :players
              {:A {:in-hand 1, :off-board 0}, :B {:in-hand 0, :off-board 7}},
              :current-player :B,
              :roll 4,
              :state :end-game,
              :selected-move {:from 23, :to :off-board, :captured nil}}
             final-state))

    ;;   (println "Final game state:")
    ;;   (pprint final-state)
      )))
