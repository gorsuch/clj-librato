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
                    (let [metric (metric user apikey (:name gauge)
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

(deftest annotate-test
         (let [name  "test.annotations"
               event {:title (str "A test event: " (rand 10000000))
                      :source "clj-librato"
                      :description "Testing clj-librato annotations"
                      :start-time (now)
                      :end-time (+ 10 (now))}
               res (annotate user apikey name event)
               e (annotation user apikey name (:id res))]

           ; Verify annotation was created.
           (is res)
           (is e)
           (is (= res e))
           (is (= (:title e) (:title event)))
           (is (= (:description e) (:description event)))
           (is (= (:source e) (:source event)))
           (is (= (:start-time e) (:start-time event)))
           (is (= (:end-time e) (:end-time event)))

           ; Update annotation
           (annotate user apikey name (:id res) 
                     {:end-time (inc (:end-time event))})

           ; Verify update was applied.
           (is (= (inc (:end-time event))
                  (:end-time (annotation user apikey name (:id res)))))))

(deftest annotation-test
         (is (nil? (annotation user apikey "asdilhugflsdbfg" 1234)))
         (is (nil? (annotation user apikey name 2345235624534))))
