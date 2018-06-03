(ns manifold-test.core
  (:require
   [manifold.stream :as s]
   [manifold.executor :as e])
  (:gen-class))

(defn normal-execution-single-stream
  [s1]
  (s/consume (fn [x]
               (Thread/sleep 100)
               (println (.getName (Thread/currentThread)) "  " x)) s1)
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
  (let [dummy (s/stream)
        s1 (s/stream* {:buffer-size 100
                       :executor (e/fixed-thread-executor 20)})]

    (setup-exit (fn []
                  (println "xxxxxxxxxxxxxxxxxxxxxxx")
                  (s/close! s1)))

    (println "Manifold test->")
    (normal-execution-single-stream s1)
    @(s/take! dummy)))
