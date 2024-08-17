(ns sim-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is are testing]]
            [config]
            [sim]
            [state]))

(deftest test-play-turn
  (testing "play-turn advances game state"
    (let [initial-state (state/initialize-game)
          new-state (sim/play-turn initial-state)]
      (are [pred] (pred new-state)
        #(not= initial-state %)
        #(contains? #{:roll-dice :end-game} (:state %))))))

(deftest test-play-game
  (testing "play-game reaches end state"
    (let [final-state (sim/play-game :first-in-list :strategic false)]
      (are [expected actual] (= expected actual)
        :end-game (:state final-state)
        true (:game-over final-state)
        true (state/game-over? final-state)))))

(deftest test-run-simulation
  (testing "run-simulation completes specified number of games"
    (let [num-games 2
          results (sim/run-simulation num-games :first-in-list :strategic false)]
      (are [expected actual] (= expected actual)
        num-games (+ (:A results) (:B results)))
      (are [pred] (pred results)
        #(>= (:A %) 0)
        #(>= (:B %) 0)))))

(deftest test-print-simulation-results
  (testing "print-simulation-results formats output correctly"
    (let [results {:A 6, :B 4}
          num-games 10
          strategy-a :first-in-list
          strategy-b :strategic
          output (with-out-str
                   (sim/print-simulation-results results num-games strategy-a strategy-b))]
      (is (string? output))
      (are [expected] (str/includes? output expected)
        "Total games: 10"
        "Player A wins: 6"
        "Player B wins: 4"
        "Player A win percentage: 60%"))))

(deftest test-full-game-simulation
  (testing "Simulating a full game"
    (let [final-state (sim/play-game :first-in-list :strategic false)]
      (are [expected actual message] (= expected actual)
        :end-game (:state final-state) "Game should end after a full simulation"
        true (:game-over final-state) "Game should be marked as over")

      (is (or (= 7 (get-in final-state [:players :A :off-board]))
              (= 7 (get-in final-state [:players :B :off-board])))
          "One player should have all 7 pieces off the board")

      (is (every? #(or (nil? %) (#{:A :B} %)) (:board final-state))
          "Board should only contain nil, :A, or :B values")

      (are [player] (<= (+ (get-in final-state [:players player :in-hand])
                           (get-in final-state [:players player :off-board])
                           (count (filter #(= % player) (:board final-state))))
                        7)
        :A
        :B))))