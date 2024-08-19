(ns strategy.mcts-test
  (:require [clojure.test :refer [deftest is testing]]
            [game :as game]
            [strategy.mcts :as mcts]))

(deftest test-mcts-node
  (testing "MCTS Node creation"
    (let [game (game/init)
          node (mcts/->Node game nil nil [] 0 0.0)]
      (is (= game (:game node)))
      (is (nil? (:move node)))
      (is (nil? (:parent node)))
      (is (empty? (:children node)))
      (is (zero? (:visits node)))
      (is (zero? (:value node))))))
