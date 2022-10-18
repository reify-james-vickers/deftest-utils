# deftest-utils

Utilities for Clojure `deftest` to conveniently timeout and/or retry tests.

```clj
[deftest-utils "0.0.0"]
```

## Usage

One core macro is provided, `deftest-configured`. It takes a config map as the first argument and produces a `deftest` form that uses the [diehard](https://github.com/sunng87/diehard) library to timeout and/or retry the test as specified. The config map has top level keys `:retry` and `:timeout` which have values that correspond to `with-retry` and `with-timeout` in diehard respectively.

Example:

```clojure
(deftest-configured {:timeout {:timeout-ms 500}
                     :retry {:retry-on IllegalArgumentException
                             :max-retries 5}}
  some-test
  (testing "a test"
    (Thread/sleep 10000)))
```

A var can also be used to share test config across a whole suite:

```clojure
(def test-config {:timeout {:timeout-ms 1} :retry {:retry-on Exception :max-retries 3}}})

(deftest-configured test-config a-test ...)

(deftest-configured test-config another-test ...)
```

Spec for the config map:

```clojure
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
```

## License
