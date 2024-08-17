(ns util-test
  (:require [clojure.string :as str]
            [clojure.test :refer [are deftest is testing]]
            [util :as util]))

(deftest test-logging
  (testing "log function"
    (are [enabled? input expected]
         (let [_ (if enabled? (util/enable-show!) (util/disable-show!))
               output (with-out-str (util/show input))]
           (if expected
             (str/includes? output input)
             (empty? output)))

      true  "Test message" true
      false "Test message" false))

  (testing "enable-logging! and disable-logging! functions"
    (are [action expected]
         (do (action)
             (= @util/show-enabled expected))

      util/enable-show!  true
      util/disable-show! false)))

(deftest test-multiple-args
  (testing "log function with multiple arguments"
    (util/enable-show!)
    (is (str/includes?
         (with-out-str (util/show "Test" "multiple" "args"))
         "Test multiple args")
        "Should print all arguments")))

(deftest test-logging-state-persistence
  (testing "logging state persists across function calls"
    (are [setup-fn expected]
         (let [_ (setup-fn)
               output (with-out-str
                        (util/show "First log")
                        (util/show "Second log"))]
           (if expected
             (and (str/includes? output "First log")
                  (str/includes? output "Second log"))
             (empty? output)))

      util/enable-show!  true
      util/disable-show! false)))