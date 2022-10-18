(ns deftest-utils.core
  (:require [clojure.spec.alpha :as s]
            [clojure.set]
            [diehard.core]))

(s/def ::pos-int (s/and int? pos?))

(s/def ::timeout-ms ::pos-int)
(s/def ::timeout
  (s/keys :req-un [::timeout-ms]))

(s/def ::max-retries ::pos-int)
(s/def ::retry
  (s/keys :opt-un [::max-retries]))
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
  [config & [test-name body]]
  (let [resolved (if (symbol? config) (resolve config) config)
        config (if (var? resolved) (var-get resolved) resolved)
        _ (when-let [info (s/explain-data ::deftest-timed-config config)]
            (throw (Exception. (str info))))
        {:keys [retry timeout]} config
        retry-with-defaults (when retry (merge {:max-retries 3} retry))
        timeout-with-defaults (when timeout (merge {:interrupt? true} timeout))
        decorated-body# (cond->> body
                          timeout-with-defaults (list 'diehard.core/with-timeout timeout-with-defaults)
                          retry-with-defaults (list 'diehard.core/with-retry retry-with-defaults))]
    `(clojure.test/deftest ~test-name
       (try ; wrap in try-catch to give better test output on failure, don't need diehard stack trace.
         ~decorated-body#
         (catch dev.failsafe.TimeoutExceededException e#
           (clojure.test/is false (clojure.core/format "Test timed out after %s ms" ~(:timeout-ms timeout))))))))

(comment
  (def cfg {:timeout {:timeout-ms 5} :retry {:retry-on Exception :max-retries 3}})
  (macroexpand-1 '(deftest-configured cfg some-test (let [x 5] (println x) (do (+ x 5)))))
  (macroexpand-1 '(deftest-configured {:timeout {:timeout-ms 5} :retry {:retry-on Exception :max-retries 3}}
                    some-test (let [x 5] (println x) (do (+ x 5)))))
  (macroexpand '(deftest-configured {:timeout-ms 5} some-test (let [x 5]))))