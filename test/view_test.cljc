(ns view-test
  (:require [clojure.test :refer [are deftest is testing]]
            [config]
            [view]))

(deftest test-c-str
  (testing "c-str with color"
    (are [color text expected] (= expected (view/c-str color text))
      :red "Hello" "\u001b[31mHello\u001b[0m"
      :blue "World" "\u001b[34mWorld\u001b[0m"
      :green "Test" "\u001b[32mTest\u001b[0m"))

  (testing "c-str without color"
    (is (= "Plain text" (view/c-str "Plain text"))))

  (testing "c-str with multiple text arguments"
    (is (= "\u001b[31mHello World\u001b[0m" (view/c-str :red "Hello" " " "World")))))

(deftest test-show
  (testing "show function calls util/show"
    (with-redefs [util/show (fn [& args] (apply str args))]
      (is (= "Hello" (view/show "Hello")))
      (is (= "\u001b[31mWorld\u001b[0m" (view/show :red "World"))))))

(deftest test-cell
  (let [board {0 :A, 8 :B}]
    (testing "cell function"
      (is (= (view/symbols :A) (view/cell board 0)))
      (is (= (view/symbols :B) (view/cell board 8)))
      (is (= (view/symbols :empty) (view/cell board 1)))
      (is (= (view/symbols :rosette) (view/cell board 4)))
      (is (= (view/symbols :blank) (view/cell board 20))))))

(deftest test-board-row
  (let [board {0 :A, 4 :B}]
    (testing "board-row function"
      (is (= (str (view/c-str :cyan "A ")
                  (view/symbols :A)
                  (view/symbols :empty)
                  (view/symbols :empty)
                  (view/symbols :empty)
                  (view/symbols :B)
                  (view/symbols :empty)
                  (view/symbols :empty)
                  (view/symbols :empty))
             (view/board-row board 0 8 "A"))))))

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
      (with-redefs [view/show (constantly nil)]
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
          expected-calls [[:red 1 " " "entry → A5"]
                          [:red 2 " " "A1 → A5"]]]
      (with-redefs [view/show (fn [& args] args)]
        (is (= expected-calls (view/show-moves moves)))))))

(deftest test-show-winner
  (testing "show-winner function"
    (with-redefs [view/show (fn [& args] args)]
      (is (= [nil
              [:red "GAME OVER"]
              [:green "You win!"]]
             (view/show-winner :A)))
      (is (= [nil
              [:red "GAME OVER"]
              [:green "AI wins!"]]
             (view/show-winner :B))))))

(deftest test-show-welcome
  (testing "show-welcome function"
    (with-redefs [view/show (fn [& args] args)
                  view/c-str (fn [color & text] (apply str text))]
      (is (= [[:red "The Royal Game of Ur"]
              nil
              ["● Your pieces"]
              ["● AI pieces"]
              nil
              ["Press 'q' to quit at any time."]
              ["Press Enter to begin!"]]
             (view/show-welcome))))))

(deftest test-show-no-moves
  (testing "show-no-moves function"
    (with-redefs [view/show (fn [& args] args)]
      (is (= ["  No moves"] (view/show-no-moves))))))

(deftest test-show-goodbye
  (testing "show-goodbye function"
    (with-redefs [view/show (fn [& args] args)]
      (is (= ["Thanks for playing! Goodbye."] (view/show-goodbye))))))

(deftest test-show-invalid-choice
  (testing "show-invalid-choice function"
    (with-redefs [view/show (fn [& args] args)]
      (is (= [:red "Invalid choice. Enter 1-" 5 " or 'q' to quit"]
             (view/show-invalid-choice 5))))))

(deftest test-show-ai-move
  (testing "show-ai-move function"
    (with-redefs [view/show (fn [& args] args)]
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
