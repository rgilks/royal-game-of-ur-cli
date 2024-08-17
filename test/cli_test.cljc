(ns cli-test
  (:require [cli]
            [clojure.test :refer [deftest testing is]]
            [platform]
            [state]
            [util :refer [show]]
            [view]))

(deftest test-get-user-move
  (testing "Valid single-key input"
    (with-redefs [platform/read-single-key (constantly "1")
                  view/show-moves (constantly nil)
                  show (constantly nil)]
      (let [moves [{:from :entry :to 4} {:from 0 :to 4}]]
        (is (= (first moves) (cli/get-user-move moves))))))

  (testing "Invalid input followed by valid input"
    (let [inputs (atom ["a" "3" "2"])]
      (with-redefs [platform/read-single-key #(let [input (first @inputs)]
                                                (swap! inputs rest)
                                                input)
                    view/show-moves (constantly nil)
                    show (constantly nil)]
        (let [moves [{:from :entry :to 4} {:from 0 :to 4} {:from 4 :to 8}]]
          (is (= {:from 4 :to 8} (cli/get-user-move moves)))))))

  (testing "Out of range input followed by valid input"
    (let [inputs (atom ["5" "0" "2"])]
      (with-redefs [platform/read-single-key #(let [input (first @inputs)]
                                                (swap! inputs rest)
                                                input)
                    view/show-moves (constantly nil)
                    show (constantly nil)]
        (let [moves [{:from :entry :to 4} {:from 0 :to 4} {:from 4 :to 8}]]
          (is (= (second moves) (cli/get-user-move moves)))))))

  (testing "Quit input"
    (with-redefs [platform/read-single-key (constantly "q")
                  view/show-moves (constantly nil)
                  show (constantly nil)]
      (let [moves [{:from :entry :to 4} {:from 0 :to 4}]]
        (is (= :quit (cli/get-user-move moves))))))

  (testing "Empty moves list"
    (with-redefs [platform/read-single-key (constantly "1")
                  view/show-moves (constantly nil)
                  show (constantly nil)]
      (is (nil? (cli/get-user-move []))))))

(deftest test-play-game
  (testing "Game ends after one move"
    (with-redefs [state/start-game
                  (constantly {:state :roll-dice
                               :board (vec (repeat 24 nil))
                               :players {:A {:in-hand 7 :off-board 0}
                                         :B {:in-hand 7 :off-board 0}}
                               :current-player :A
                               :roll nil
                               :selected-move nil})
                  state/dice-roll
                  (constantly {:state :choose-action
                               :board (vec (repeat 24 nil))
                               :players {:A {:in-hand 7 :off-board 0}
                                         :B {:in-hand 7 :off-board 0}}
                               :current-player :A
                               :roll 4
                               :selected-move nil})
                  state/get-moves (constantly [{:from :entry :to 4}])
                  cli/get-user-move (constantly {:from :entry :to 4})
                  state/choose-action
                  (constantly {:state :end-game
                               :board (vec (repeat 24 nil))
                               :players {:A {:in-hand 6 :off-board 1}
                                         :B {:in-hand 7 :off-board 0}}
                               :current-player :A
                               :roll nil
                               :selected-move nil})
                  platform/clear-console (constantly nil)
                  view/show-welcome (constantly nil)
                  view/show-state (constantly nil)
                  view/show-winner (constantly nil)
                  platform/readln (constantly nil)
                  platform/sleep (constantly nil)]
      (is (nil? (cli/play-game)))))

  (testing "Game quits when user chooses to exit"
    (with-redefs [state/start-game
                  (constantly {:state :roll-dice
                               :board (vec (repeat 24 nil))
                               :players {:A {:in-hand 7 :off-board 0}
                                         :B {:in-hand 7 :off-board 0}}
                               :current-player :A
                               :roll nil
                               :selected-move nil})
                  state/dice-roll
                  (constantly {:state :choose-action
                               :board (vec (repeat 24 nil))
                               :players {:A {:in-hand 7 :off-board 0}
                                         :B {:in-hand 7 :off-board 0}}
                               :current-player :A
                               :roll 4
                               :selected-move nil})
                  state/get-moves (constantly [{:from :entry :to 4}])
                  cli/get-user-move (constantly :quit)
                  platform/clear-console (constantly nil)
                  view/show-welcome (constantly nil)
                  view/show-state (constantly nil)
                  platform/readln (constantly nil)
                  platform/sleep (constantly nil)
                  show (constantly nil)]
      (is (nil? (cli/play-game))))))