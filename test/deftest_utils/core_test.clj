(ns deftest-utils.core-test
  (:require [clojure.test :refer [testing]]
            [deftest-utils.core :refer [deftest-timed]]))

(deftest-timed {:timeout-ms 500} foo
  (testing "hi"
    (let [x 5]
      (println "hello")
      (Thread/sleep 10000))))