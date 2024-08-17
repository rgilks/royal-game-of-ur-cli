(ns cli-test
  (:require [cli]
            [clojure.test :refer [are deftest is testing]]
            [platform]
            [state]
            [util]
            [view]))

(deftest test-get-user-move
  (testing "Valid single-key inputs"
    (are [input expected-move moves]
         (with-redefs [platform/read-single-key (constantly input)
                       view/show-moves (constantly nil)]
           (= expected-move (cli/get-user-move moves)))

      "1" {:from :entry :to 4} [{:from :entry :to 4} {:from 0 :to 4}]
      "2" {:from 0 :to 4} [{:from :entry :to 4} {:from 0 :to 4}]
      "q" :quit [{:from :entry :to 4} {:from 0 :to 4}]))

  (testing "Invalid inputs followed by valid input"
    (are [inputs expected-move moves]
         (let [input-atom (atom inputs)]
           (with-redefs [platform/read-single-key #(let [input (first @input-atom)]
                                                     (swap! input-atom rest)
                                                     input)
                         view/show-moves (constantly nil)
                         view/show-invalid-choice (constantly nil)]
             (= expected-move (cli/get-user-move moves))))

      ["a" "3" "2"] {:from 4 :to 8} [{:from :entry :to 4} {:from 0 :to 4} {:from 4 :to 8}]
      ["5" "0" "1"] {:from :entry :to 4} [{:from :entry :to 4} {:from 0 :to 4}]))

  (testing "Empty moves list"
    (with-redefs [platform/read-single-key (constantly "1")
                  view/show-moves (constantly nil)]
      (is (nil? (cli/get-user-move []))))))

(deftest test-handle
  (testing "Handle roll dice state"
    (with-redefs [state/dice-roll (constantly :rolled-game)]
      (is (= :rolled-game (cli/handle {:state :roll-dice})))))

  (testing "Handle choose action with no moves"
    (with-redefs [state/get-moves (constantly [])
                  view/show-no-moves (constantly nil)
                  platform/sleep (constantly nil)
                  state/choose-action (constantly :next-game)]
      (is (= :next-game (cli/handle {:state :choose-action, :current-player :A})))))

  (testing "Handle choose action for player A"
    (let [move {:from :entry :to 4}]
      (with-redefs [state/get-moves (constantly [move])
                    cli/get-user-move (constantly move)
                    state/choose-action (constantly :next-game)]
        (is (= :next-game (cli/handle {:state :choose-action, :current-player :A}))))))

  (testing "Handle choose action for player B (AI)"
    (let [move {:from :entry :to 4}]
      (with-redefs [state/get-moves (constantly [move])
                    state/select-move (constantly move)
                    state/choose-action (constantly :next-game)
                    view/show-ai-move (constantly nil)
                    platform/sleep (constantly nil)]
        (is (= :next-game (cli/handle {:state :choose-action, :current-player :B}))))))

  (testing "Handle user quit"
    (with-redefs [state/get-moves (constantly [{:from :entry :to 4}])
                  cli/get-user-move (constantly :quit)]
      (is (thrown-with-msg? platform/err #"User quit"
                            (cli/handle {:state :choose-action, :current-player :A})))))

  (testing "Handle end game state"
    (with-redefs [view/show-winner (constantly nil)]
      (is (thrown-with-msg? platform/err #"Game over"
                            (cli/handle {:state :end-game, :current-player :A}))))))

(deftest test-play-game
  (testing "Play game until completion"
    (with-redefs [platform/clear-console (constantly nil)
                  util/hide-cursor (constantly nil)
                  view/show-welcome (constantly nil)
                  platform/readln (constantly nil)
                  state/start-game (constantly {:state :end-game, :current-player :A})
                  view/show-state (constantly nil)
                  platform/sleep (constantly nil)
                  cli/handle (fn [game] (throw (ex-info "Game over" {:reason :expected})))
                  util/show-cursor (constantly nil)]
      (is (nil? (cli/play-game)))))

  (testing "Play game with unexpected error"
    (with-redefs [platform/clear-console (constantly nil)
                  util/hide-cursor (constantly nil)
                  view/show-welcome (constantly nil)
                  platform/readln (constantly nil)
                  state/start-game (constantly {:state :roll-dice})
                  view/show-state (constantly nil)
                  platform/sleep (constantly nil)
                  cli/handle (fn [game] (throw (ex-info "Unexpected error" {:reason :unexpected})))
                  util/show-cursor (constantly nil)]
      (is (thrown-with-msg? platform/err #"Unexpected error" (cli/play-game))))))

(deftest test-main
  (testing "Main function execution"
    (with-redefs [cli/play-game (constantly nil)
                  view/show-goodbye (constantly nil)]
      (is (nil? (cli/-main))))))
