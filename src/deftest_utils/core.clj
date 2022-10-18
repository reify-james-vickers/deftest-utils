(ns deftest-utils.core
  (:require [clojure.spec.alpha :as s]
            [diehard.core :as dh]))

(s/def ::timeout-ms (s/and int? pos?))
(s/def ::deftest-timed-config
  (s/keys :req-un [::timeout-ms]))

(defmacro deftest-timed
  [{:keys [timeout-ms] :as config} & body]
  (when-let [info (s/explain-data ::deftest-timed-config config)]
    (throw (Exception. (str info))))
  (let [base-config# {:interrupt? true}]
    `(clojure.test/deftest ~(first body) ; var name of test
       (try ; wrap in try-catch to give better test output on failure, don't need diehard stack trace.
         (dh/with-timeout (merge ~base-config# ~config)
           ~@(rest body))
         (catch dev.failsafe.TimeoutExceededException e#
           (clojure.test/is false (clojure.core/format "Test timed out after %s ms" ~timeout-ms)))))))

(comment
  (macroexpand-1 '(deftest-timed {:timeout-ms 5 :foo "bar"} some-test (let [x 5])))
  (macroexpand '(deftest-timed {:timeout-ms 5} some-test (let [x 5]))))