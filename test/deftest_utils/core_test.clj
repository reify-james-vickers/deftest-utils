(ns deftest-utils.core-test
  (:require [clojure.test :refer [is testing]]
            [deftest-utils.core :refer [deftest-2]]))

(deftest-2 {:timeout {:timeout-ms 500}} test-timeout-setup
  (testing "hi"
    (Thread/sleep 10000)))

(def tries (atom 0))

(deftest-2 {:retry {:max-retries 20}} test-retry-setup
  (testing "hi"
    (if (< (swap! tries inc) 10)
      (do
        (println (str "failing on try " @tries))
        (throw (IllegalArgumentException. (str "failing on try " @tries))))
      (is true))))