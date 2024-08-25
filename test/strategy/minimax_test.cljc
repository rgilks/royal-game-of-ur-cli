(ns strategy.minimax-test
  (:require [clojure.test :refer [deftest are is testing]]
            [engine]
            [strategy.minimax :as minimax]))

(deftest test-select-move
  (testing "minimax select-move returns a valid move"
    (let [game {:board (vec (repeat 24 nil))
                :players {:A {:in-hand 7 :off-board 0}
                          :B {:in-hand 7 :off-board 0}}
                :current-player :A
                :roll 4
                :state :choose-action
                :strategy {:name :minimax :params {:depth 3}}}
          move (minimax/select-move game)]
      (is (map? move))
      (is (contains? move :from))
      (is (contains? move :to))
      (is (contains? move :captured)))))

(deftest test-avg-prob
  (testing "avg-prob value"
    (is (= 1/5 minimax/avg-prob)
        "avg-prob should be 1/5 (0.2)")))

(deftest test-dampened-prob
  (testing "dampened-prob function"
    (are [roll dampening expected]
         (= expected (minimax/dampened-prob roll dampening))

      ; Tests with dampening 0 (should return original probabilities)
      0 0 1/16
      1 0 1/4
      2 0 3/8
      3 0 1/4
      4 0 1/16

      ; Tests with dampening 0.5 (halfway between original and average)
      0 0.5 0.13125
      1 0.5 0.225
      2 0.5 0.2875
      3 0.5 0.225
      4 0.5 0.13125

      ; Tests with dampening 1 (should return average probability for all rolls)
      0 1 1/5
      1 1 1/5
      2 1 1/5
      3 1 1/5
      4 1 1/5)))
