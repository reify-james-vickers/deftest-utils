(ns deftest-utils.core
  (:require [clojure.spec.alpha :as s]
            [clojure.set]
            [diehard.core]))

(s/def ::pos-int (s/and int? pos?))

(s/def ::timeout-ms ::pos-int)
(s/def ::timeout
  (s/keys :req-un [::timeout-ms]))

(s/def ::max-retries ::pos-int)
(s/def ::retry-on symbol?)
(s/def ::retry
  (s/keys :opt-un [::max-retries
                   ::retry-on]))
(s/def ::deftest-timed-config
  (s/keys :opt-un [::retry
                   ::timeout]))

(defmacro deftest-configured
  "deftest with support for test retry and timeout.
   The first argument is a map configuring retry and/or timeout, remaining arguments are test body
   starting with the test var name, e.g. (deftest-configured {:timeout {:timeout-ms 5000}} test-name (let [])...).
   The values for :retry and :timeout in that map are exactly what diehard
   takes for with-retry and with-timeout respectively, except that max-retries
   defaults to 3 instead of infinite."
  [{:keys [retry timeout] :as config} & body]
  (when-let [info (s/explain-data ::deftest-timed-config config)]
    (throw (Exception. (str info))))
  (let [test-body# (-> body rest first) ; remove test name var
        retry-with-defaults (when retry (merge {:max-retries 3} retry))
        decorated-body# (cond->> test-body#
                          timeout (list 'diehard.core/with-timeout timeout)
                          retry-with-defaults (list 'diehard.core/with-retry retry-with-defaults))]
    `(clojure.test/deftest ~(first body) ; var name of test
       (try ; wrap in try-catch to give better test output on failure, don't need diehard stack trace.
         ~decorated-body#
         (catch dev.failsafe.TimeoutExceededException e#
           (clojure.test/is false (clojure.core/format "Test timed out after %s ms" ~(:timeout-ms timeout))))))))

(comment
  (macroexpand-1 '(deftest-2 {:timeout {:timeout-ms 5} :retry {:retry-on Exception :max-retries 3}}
                    some-test (let [x 5] (println x) (do (+ x 5)))))
  (macroexpand '(deftest-2 {:timeout-ms 5} some-test (let [x 5]))))