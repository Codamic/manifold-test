(ns manifold-test.core
  (:require
   [manifold.stream :as s]
   [manifold.executor :as e])
  (:gen-class))

(defn normal-execution-single-stream
  [s1]
  (s/consume #(println "<<<<<<<<<< " %) s1)
  (loop [x 0]
    (when (< x 200)
      (s/put! s1 x)
      (recur (inc x)))))



(defn setup-exit
  [f]
  (.addShutdownHook
   (Runtime/getRuntime)
   (Thread. f)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [s1           (s/stream* {:buffer-size 100
                                 :executor (e/fixed-thread-executor 20)})]

    (setup-exit #(s/close! s1))
    (println "Manifold test->")
    (normal-execution-single-stream s1)
    (Thread/sleep 5000)))
