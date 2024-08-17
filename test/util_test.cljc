(ns util-test
  (:require [clojure.string :as str]
            [clojure.test :refer [are deftest is testing]]
            [util :as util]))

(deftest test-logging
  (testing "log function"
    (are [enabled? input expected]
         (let [_ (if enabled? (util/enable-print-line!) (util/disable-print-line!))
               output (with-out-str (util/print-line input))]
           (if expected
             (str/includes? output input)
             (empty? output)))

      true  "Test message" true
      false "Test message" false))

  (testing "enable-logging! and disable-logging! functions"
    (are [action expected]
         (do (action)
             (= @util/print-line-enabled expected))

      util/enable-print-line!  true
      util/disable-print-line! false)))

(deftest test-multiple-args
  (testing "log function with multiple arguments"
    (util/enable-print-line!)
    (is (str/includes?
         (with-out-str (util/print-line "Test" "multiple" "args"))
         "Test multiple args")
        "Should print all arguments")))

(deftest test-logging-state-persistence
  (testing "logging state persists across function calls"
    (are [setup-fn expected]
         (let [_ (setup-fn)
               output (with-out-str
                        (util/print-line "First log")
                        (util/print-line "Second log"))]
           (if expected
             (and (str/includes? output "First log")
                  (str/includes? output "Second log"))
             (empty? output)))

      util/enable-print-line!  true
      util/disable-print-line! false)))

(deftest test-str
  (testing "c-str function"
    (are [input expected] (= expected (apply util/cstr input))
      [:red "Hello"] "\u001b[31mHello\u001b[0m"
      [:blue "World"] "\u001b[34mWorld\u001b[0m"
      [:green "Test"] "\u001b[32mTest\u001b[0m"
      ["Plain text"] "Plain text"
      [:red "Hello" " " "World"] "\u001b[31mHello World\u001b[0m")))

(deftest test-show
  (testing "show function calls util/show"
    (with-redefs [util/print-line (fn [& args] (apply str args))]
      (are [input expected] (= expected (apply util/show input))
        ["Hello"] "Hello"
        [:red "World"] "\u001b[31mWorld\u001b[0m"))))