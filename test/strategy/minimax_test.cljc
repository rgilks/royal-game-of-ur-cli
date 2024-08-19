(ns strategy.minimax-test
  (:require [clojure.test :refer [deftest is testing]]
            [game]
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
