(ns promesa.tests.exec-test
  (:require
   [promesa.core :as p]
   [promesa.exec.bulkhead :as pbh]
   [promesa.exec :as px]
   [clojure.test :as t]))

(t/use-fixtures
  :each
  (fn [next]
    (binding [px/*default-executor* (px/forkjoin-executor)]
      (next)
      (.shutdown px/*default-executor*))))

(def ^:dynamic *my-dyn-var* nil)

(t/deftest check-dynamic-vars-with-dispatch
  (let [executor (px/single-executor)
        result   (binding [*my-dyn-var* 10]
                   (px/with-dispatch executor
                     (+ *my-dyn-var* 1)))]
    (t/is (= 11 @result))))

(t/deftest check-dynamic-vars-future
  (let [result (binding [*my-dyn-var* 10]
                 (p/future (+ *my-dyn-var* 1)))]
    (t/is (= 11 @result))))

(t/deftest check-dynamic-vars-thread
  (let [result (binding [*my-dyn-var* 10]
                 (p/thread (+ *my-dyn-var* 1)))]
    (t/is (= 11 @result))))

(when px/vthreads-supported?
  (t/deftest check-dynamic-vars-vthread
    (let [result (binding [*my-dyn-var* 10]
                   (p/vthread (+ *my-dyn-var* 1)))]
      (t/is (= 11 @result)))))

(t/deftest with-executor-closes-pool-1
  (let [executor (px/single-executor)]
    (px/with-executor ^:interrupt executor
      (px/with-dispatch executor
        (Thread/sleep 1000)))
    (t/is (thrown? java.util.concurrent.RejectedExecutionException
                   (px/submit! executor (constantly nil))))))

(t/deftest with-executor-closes-pool-2
  (let [executor (px/single-executor)]
    (px/with-executor ^:interrupt executor
      (px/with-dispatch executor
        (Thread/sleep 1000)))
    (t/is (thrown? java.util.concurrent.RejectedExecutionException
                   (px/submit! executor (constantly nil))))))

(t/deftest pmap-sample-1
  (let [result (->> (range 5) (pmap inc) vec)]
    (t/is (= result [1 2 3 4 5]))))

(t/deftest pmap-sample-2
  (let [result (pmap + (range 5) (range 5))]
    (t/is (= result [0 2 4 6 8]))))
