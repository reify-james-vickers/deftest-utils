(ns deftest-utils.core-test
  (:require [clojure.test :refer [is testing]]
            [deftest-utils.core :refer [deftest-configured]]))

(deftest-configured {:timeout {:timeout-ms 500}} test-timeout-setup
  (testing "hi"
    (Thread/sleep 10000)))

(def tries (atom 0))

(deftest-configured {:retry {:max-retries 20}} test-retry-setup
  (testing "hi"
    (if (< (swap! tries inc) 10)
      (do
        (println (str "failing on try " @tries))
        (throw (IllegalArgumentException. (str "failing on try " @tries))))
      (is true))))

(def test-config {:timeout {:timeout-ms 1}}) ; :retry {:retry-on Exception :max-retries 3}})

(deftest-configured test-config test-with-config-var
  (Thread/sleep 100))