(ns view-test
  (:require [clojure.test :refer [are deftest is testing]]
            [config]
            [util :as util]
            [view]))

(deftest test-cell
  (let [board {0 :A, 8 :B}]
    (testing "cell function"
      (are [idx expected] (= expected (view/cell board idx))
        0 (view/symbols :A)
        8 (view/symbols :B)
        1 (view/symbols :empty)
        6 (view/symbols :rosette)
        20 (view/symbols :blank)))))

(deftest test-board-row
  (let [board {0 :A, 4 :B}
        expected (str (util/str :cyan "A ")
                      (view/symbols :A)
                      (view/symbols :empty)
                      (view/symbols :empty)
                      (view/symbols :empty)
                      (view/symbols :B)
                      (view/symbols :blank)
                      (view/symbols :rosette)
                      (view/symbols :empty))]
    (is (= expected (view/board-row board 0 8 "A")))))

(deftest test-show-board
  (testing "show-board function"
    (let [board {0 :A, 8 :B}
          calls (atom [])]
      (with-redefs [util/show (fn [& args] (swap! calls conj args))]
        (view/show-board board)
        (are [idx expected] (= expected (nth @calls idx))
          1 [:cyan "    1 2 3 4 5 6 7 8"]
          3 [:cyan "│" (view/board-row board 0 8 "A") "│"])
        (is (= 7 (count @calls)))))))

(deftest test-show-roll
  (testing "show-roll function"
    (with-redefs [shuffle identity]
      (is (= "\u001b[1m▲\u001b[0m\u001b[1m▲\u001b[0m\u001b[1m△\u001b[0m\u001b[1m△\u001b[0m" (view/show-roll 2))))))

(deftest test-player-stats
  (testing "player-stats function"
    (is (= "\u001b[31m 5 → 2\u001b[0m" (view/player-stats :red {:in-hand 5 :off-board 2})))))

(deftest test-show-state
  (let [state {:board {0 :A 8 :B}
               :players {:A {:in-hand 5 :off-board 2}
                         :B {:in-hand 4 :off-board 3}}
               :roll 2}]
    (testing "show-state doesn't throw an exception"
      (with-redefs [util/show (constantly nil)]
        (is (nil? (view/show-state state)))))))

(deftest test-format-move
  (testing "format-move function"
    (are [move expected] (= expected (view/format-move move))
      {:from :entry :to 4} "entry → A5"
      {:from 0 :to 4} "A1 → A5"
      {:from 15 :to :off-board} "B8 → off"
      {:from 7 :to 8 :captured true} "A8 → B1\u001b[31m capture\u001b[0m")))

(deftest test-show-moves
  (testing "show-moves function"
    (let [moves [{:from :entry :to 4} {:from 0 :to 4}]
          calls (atom [])]
      (with-redefs [util/show (fn [& args] (swap! calls conj args))]
        (view/show-moves moves)
        (is (= [[:red 1 " " "entry → A5"]
                [:red 2 " " "A1 → A5"]]
               @calls))))))

(deftest test-show-winner
  (testing "show-winner function"
    (are [winner expected] (= expected
                              (let [calls (atom [])]
                                (with-redefs [util/show (fn [& args] (swap! calls conj args))]
                                  (view/show-winner winner)
                                  @calls)))
      :A [nil [:red "GAME OVER"] [:green "You win!"]]
      :B [nil [:red "GAME OVER"] [:green "AI wins!"]])))

(deftest test-show-welcome
  (testing "show-welcome function"
    (let [calls (atom [])]
      (with-redefs [util/show (fn [& args] (swap! calls conj args))]
        (view/show-welcome)
        (is (= [[:red "The Royal Game of Ur"]
                nil
                [(util/str :red "●") " Your pieces"]
                [(util/str :yellow "●") " AI pieces"]
                nil
                ["Press 'q' to quit at any time."]
                ["Press Enter to begin!"]]
               @calls))))))

(deftest test-simple-messages
  (testing "simple message functions"
    (with-redefs [util/show (fn [& args] args)]
      (are [func expected] (= expected (func))
        view/show-no-moves ["  No moves"]
        view/show-goodbye ["Thanks for playing! Goodbye."]))))

(deftest test-show-invalid-choice
  (testing "show-invalid-choice function"
    (with-redefs [util/show (fn [& args] args)]
      (is (= [:red "Invalid choice. Enter 1-" 5 " or 'q' to quit"]
             (view/show-invalid-choice 5))))))

(deftest test-show-ai-move
  (testing "show-ai-move function"
    (with-redefs [util/show (fn [& args] args)]
      (is (= [:yellow "  AI: " "A1 → B2"]
             (view/show-ai-move {:from 0 :to 9}))))))

(deftest test-coords
  (testing "coords map"
    (are [idx expected] (= expected (get view/coords idx))
      0 "A1"
      7 "A8"
      8 "B1"
      15 "B8"
      16 "C1"
      23 "C8")))
