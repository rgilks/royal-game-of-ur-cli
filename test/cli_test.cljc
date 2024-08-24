(ns cli-test
  (:require [cli]
            [clojure.test :refer [are deftest is testing]]
            [config]
            [platform]
            [play]
            [sim :as sim]))

(def test-config
  {:num-games 10
   :debug false
   :show false
   :parallel 8
   :validate false
   :strategies {:A {:name :minimax :params {:depth 4}}
                :B {:name :minimax :params {:depth 4}}}})

(deftest test-parse-arg
  (testing "parse-arg function"
    (are [input expected] (= expected (cli/parse-arg input))
      "key1=value1" [:key1 (platform/parse-value "value1")]
      "num-games=20" [:num-games (platform/parse-value "20")]
      "debug=true" [:debug (platform/parse-value "true")])))

(deftest test-parse-strategy-args
  (testing "parse-strategy-args function"
    (are [input expected] (= expected (cli/parse-strategy-args input))
      {:strategy-A "minimax"
       :strategy-A-depth "3"
       :strategy-B "random"}
      {:A {:name :minimax :params {:depth "3"}}
       :B {:name :random}}

      {:strategy-A-param1 "value1"
       :strategy-B-param2 "value2"}
      {:A {:params {:param1 "value1"}}
       :B {:params {:param2 "value2"}}}

      {}
      {})))

(deftest test-parse-args
  (testing "parse-args function"
    (with-redefs [config/game (atom test-config)]
      (cli/parse-args ["num-games=20"
                       "debug=true"
                       "show=false"
                       "parallel=4"
                       "validate=false"
                       "strategy-A=mcts"
                       "strategy-A-iterations=200"
                       "strategy-B=first-in-list"])
      (is (= {:num-games 20
              :debug true
              :show false
              :parallel 4
              :validate false
              :strategy-A :mcts
              :strategy-A-iterations 200
              :strategy-B :first-in-list
              :strategies {:A {:name :mcts :params {:iterations 200}}
                           :B {:name :first-in-list}}}
             @config/game)))))

(deftest test-print-config
  (testing "print-config function"
    (with-redefs [config/game (atom {:num-games 50
                                     :debug true
                                     :show false
                                     :parallel 4
                                     :validate true
                                     :strategies {:A {:name :mcts :params {:iterations 100}}
                                                  :B {:name :minimax :params {:depth 3}}}})]
      (is (= (str "Running 50 games...\n"
                  "Player :A strategy: :mcts {:iterations 100}\n"
                  "Player :B strategy: :minimax {:depth 3}\n"
                  "debug: true\n"
                  "show: false\n"
                  "parallel: 4\n"
                  "validate: true\n")
             (with-out-str (cli/print-config)))))))

(deftest test-main
  (testing "-main function"
    (with-redefs [config/game (atom test-config)
                  cli/print-config (constantly nil)
                  cli/parse-args (constantly nil)
                  sim/run-and-report (constantly nil)
                  play/ur (constantly nil)]
      (are [args expected-call] (= expected-call
                                   (with-out-str (apply cli/-main args)))
        ["sim" "num-games=30" "debug=true"]
        ""

        ["play"]
        ""))))