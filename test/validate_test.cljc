(ns validate-test
  (:require [clojure.test :refer [are deftest is testing]]
            [test-utils :refer [thrown-with-msg?]]
            [validate]))

(def test-board
  [:A nil :B :A nil nil :B :A nil :B nil nil nil
   nil nil nil :A :B nil nil nil nil :A :B])

(def test-game
  {:board test-board
   :players {:A {:in-hand 2 :off-board 0}
             :B {:in-hand 2 :off-board 0}}
   :current-player :A
   :roll 2
   :state :choose-action
   :selected-move nil})

(deftest test-validate-game
  (testing "validate/game accepts valid states"
    (is (= test-game (validate/game test-game))))

  (testing "validate/game throws on invalid states"
    (are [invalid-state]
         (thrown-with-msg? #?(:clj Throwable :cljs :default)
                           #"Invalid game state"
                           (validate/game invalid-state))
      (assoc test-game :board [])  ; Invalid board size
      (assoc-in test-game [:players :A :in-hand] 8)  ; Too many pieces in hand
      (assoc test-game :current-player :C)  ; Invalid player
      (assoc test-game :roll 5)  ; Invalid roll
      (assoc test-game :state :invalid-state))))  ; Invalid state

(deftest test-validate-total-pieces
  (testing "validate/total-pieces accepts valid piece counts"
    (is (validate/total-pieces test-game)))

  (testing "validate/total-pieces fails on invalid piece counts"
    (is (not (validate/total-pieces
              (assoc-in test-game [:players :A :in-hand] 4))))))
