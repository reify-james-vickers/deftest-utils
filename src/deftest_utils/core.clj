(ns deftest-utils.core
  (:require [clojure.spec.alpha :as s]
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

(defmacro deftest-2
  [{:keys [retry timeout] :as config} & body] ; TODO diehard default retries is infinite (?), should probably default to like 3 here.
  (when-let [info (s/explain-data ::deftest-timed-config config)]
    (throw (Exception. (str info))))
  (let [test-body# (-> body rest first) ; remove test name var
        decorated-body# (cond->> test-body#
                          timeout (list 'diehard.core/with-timeout timeout)
                          retry (list 'diehard.core/with-retry retry))]
    `(clojure.test/deftest ~(first body) ; var name of test
       (try ; wrap in try-catch to give better test output on failure, don't need diehard stack trace.
         ~decorated-body#
         (catch dev.failsafe.TimeoutExceededException e#
           (clojure.test/is false (clojure.core/format "Test timed out after %s ms" ~(:timeout-ms timeout))))))))

(comment
  (macroexpand-1 '(deftest-2 {:timeout {:timeout-ms 5} :retry {:retry-on Exception :max-retries 3}}
                    some-test (let [x 5] (println x) (do (+ x 5)))))
  (macroexpand '(deftest-2 {:timeout-ms 5} some-test (let [x 5]))))