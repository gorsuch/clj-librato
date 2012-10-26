(ns clj-librato.metrics-test
  (:use clojure.test
        clj-librato.metrics))

(def user   (System/getenv "LIBRATO_METRICS_USER"))
(def apikey (System/getenv "LIBRATO_METRICS_API_KEY"))

(when-not user
  (println "export LIBRATO_METRICS_USER=\"...\" to run these tests."))
(when-not apikey
  (println "export LIBRATO_METRICS_API_KEY=\"...\" to run these tests."))

(defn now []
  (long (/ (System/currentTimeMillis) 1000)))

(deftest parse-kw-test
         (is (= (parse-kw {"hello_there" {"yarr" 2}})
                {:hello-there {"yarr" 2}})))

(deftest unparse-kw-test
         (is (= (unparse-kw {:hello-there [{:tiny-kitten 3}]})
                {"hello_there" [{"tiny_kitten" 3}]})))

(deftest collate-test
         (testing "gauge"
                  (let [gauge {:name "test.gauge"
                               :source "clj-librato"
                               :value (rand)
                               :measure-time (now)}]
                    ; Submit gauge
                    (collate user apikey [gauge] [])

                    ; Confirm receipt
                    (let [metric (get-metric user apikey (:name gauge)
                                             {:end-time (:measure-time gauge)
                                              :count 1
                                              :resolution 1})
                          m (-> metric
                              :measurements
                              (get (:source gauge))
                              (first))]
                      (is (= (:name metric) (:name gauge)))
                      (is (= (:type metric) "gauge"))
                      (is m)
                      (is (= (:measure-time m) (:measure-time gauge)))
                      (is (= (:value m) (:value gauge)))
                      (is (= (:count m) 1)))
                  )))