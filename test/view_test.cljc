(ns view-test
  (:require [clojure.test :refer [are deftest is testing]]
            [platform]
            [view]))

(deftest test-colorize
  (are [text color expected] (= expected (view/cs text color))
    "Hello" :red "\u001b[31mHello\u001b[0m"
    "World" :blue "\u001b[34mWorld\u001b[0m"
    "Test" :green "\u001b[32mTest\u001b[0m"))

(deftest test-format-move
  (are [move expected] (= expected (view/format-move move))
    {:from :entry :to 4} "entry → A5"
    {:from 0 :to 4} "A1 → A5"
    {:from 15 :to :off-board} "B8 → off"
    {:from 7 :to 8 :captured true} "A8 → B1\u001b[31m capture\u001b[0m"))

(deftest test-index-to-coord
  (are [idx expected] (= expected (get view/index-to-coord idx))
    0 "A1"
    7 "A8"
    8 "B1"
    15 "B8"
    16 "C1"
    23 "C8"))

(deftest test-print-game-state
  (let [state {:board {0 :A 8 :B}
               :players {:A {:in-hand 5 :off-board 2}
                         :B {:in-hand 4 :off-board 3}}
               :roll 2}]
    (testing "print-game-state doesn't throw an exception"
      (is (nil? (view/print-game-state state))))))

(deftest test-print-winner-message
  (testing "print-winner-message doesn't throw an exception"
    (is (nil? (view/print-winner-message :A)))
    (is (nil? (view/print-winner-message :B)))))