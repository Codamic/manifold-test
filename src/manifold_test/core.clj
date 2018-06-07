(ns manifold-test.core
  (:require
   [manifold.stream :as s]
   [manifold.deferred :as d]
   [manifold.executor :as e])
  (:import
   [java.util.concurrent ArrayBlockingQueue]
   [java.util.concurrent Executors]
   [java.util.concurrent TimeUnit])
  (:gen-class))

;;  newFixedThreadPool
(defn threadpool
  [size]
  (Executors/newFixedThreadPool size))

(defn execute
  [tp f]
  (.execute tp f))

(defn stream
  [size]
  (ArrayBlockingQueue. size))

(defn put!
  ([q v]
   (.offer q v))
  ([q v t]
   (.offer q v t)))

(defn poll!
  ([q]
   (.poll q))
  ([q t]
   (.poll q t TimeUnit/MILLISECONDS)))

(defn take!
  [q]
  (.take q))

(defn consume-loop
  [q f]
  (loop []
    (let [x (take! q)]
      (when x
        (f x)
        (recur)))))

(defn consume-async
  [q f]
  (let [x (poll! q)]
    (f x)))


(defn printer
  [v]
  (println ">> " v))

(defn execute-job
  [enqueue-fn jobs f in out]
  ;;(println "Executing the job...")
  (consume-async in
                 (fn [x]
                   (when x
                     (put! out (f x)))
                   (apply enqueue-fn [jobs f in out]))))

(defn enqueue
  [jobs-queue f in out]
  ;;(println "Enqueuing a new job")
  (put! jobs-queue
        {:in in :out out
         :fn (fn []
               ;;(println "Inside a job...")
               (execute-job enqueue
                            jobs-queue f in out))}))

(defn setup-exit
  [f]
  (.addShutdownHook
   (Runtime/getRuntime)
   (Thread. f)))

(defn worker
  [job-queue x]
  (fn []
    (consume-loop job-queue
                  (fn [job]
                    ;;(println "Worker number " x ": executing a job")
                    ((:fn job))))))

(defn f1
  [x]
  (println "f1: " x)
  (inc x))

(defn f2
  [x]
  (println "f2: " x)
  (* x x))

(defn -main
  [& args]
  (let [tp  (threadpool 5)
        jobs (stream 100)
        s1i  (stream 200)
        s1o  (stream 200)
        s2o  (stream 200)]
    (setup-exit (fn []
                  (println "Shutting down...")
                  (.shutdown tp)))

    (doseq [x (range 5)]
      (println "Spawining worker number " x)
      (execute tp (worker jobs x)))

    (enqueue jobs f1 s1i s1o)
    (enqueue jobs f2 s1o s2o)

    (doseq [x (range 1000000)]
      (put! s1i x))

    (println "Input generated")
    (consume-loop s2o printer)))

;; (defn normal-execution-single-stream
;;   [s1 s2]
;;   (s/consume-async (fn [x]
;;                      (Thread/sleep 100)
;;                      (println (.getName (Thread/currentThread)) "  " x))
;;                    s2)
;;   (s/put-all! s1 (range 100)))
;;   ;; (loop [x 0]
;;   ;;   (when (< x 200)
;;   ;;     (s/put! s1 x)
;;   ;;     (recur (inc x))))



;; (defn setup-exit
;;   [f]
;;   (.addShutdownHook
;;    (Runtime/getRuntime)
;;    (Thread. f)))

;; (defn -main
;;   "I don't do a whole lot ... yet."
;;   [& args]
;;   (let [dummy (s/stream)
;;         s1 (s/stream* {:buffer-size 50
;;                        :executor (e/fixed-thread-executor 20)})
;;         s2 (s/stream* {:buffer-size 50
;;                        :executor (e/fixed-thread-executor 20)})]

;;     (setup-exit (fn []
;;                   (println "xxxxxxxxxxxxxxxxxxxxxxx")
;;                   (s/close! s1)
;;                   (s/close! s2)))

;;     (s/connect s1 s2)
;;     (println "Manifold test->")
;;     (normal-execution-single-stream s1 s2)
;;     @(s/take! dummy)))
