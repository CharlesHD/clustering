;; The MIT License (MIT)
;;
;; Copyright (c) 2016 Richard Hull
;;
;; Permission is hereby granted, free of charge, to any person obtaining a copy
;; of this software and associated documentation files (the "Software"), to deal
;; in the Software without restriction, including without limitation the rights
;; to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
;; copies of the Software, and to permit persons to whom the Software is
;; furnished to do so, subject to the following conditions:
;;
;; The above copyright notice and this permission notice shall be included in all
;; copies or substantial portions of the Software.
;;
;; THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
;; IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
;; FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
;; AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
;; LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
;; OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
;; SOFTWARE.

(ns clustering.core.hierarchical
  (:require
   [clojure.math.combinatorics :refer [combinations]]))

(defrecord BiCluster [branch? data left right distance])

(defn bi-cluster
  ([data] (BiCluster. false data nil nil 0))
  ([data [left right] distance] (BiCluster. true data left right distance)))

(defn find-closest
  "Loop through every pair looking for the smallest distance"

  [distance-fn points]
  (reduce
   (fn [state curr]
     (let [dist (apply distance-fn curr)]
       (if (< dist (or (first state) #?(:clj Integer/MAX_VALUE
                                        :cljs (.-MAX_SAFE_INTEGER js/Number))))
         [dist curr]
         state)))
   []
   (combinations points 2)))

(defn cluster [distance-fn average-fn dataset]
  (let [distance-fn (memoize
                     (fn [clust1 clust2]
                       (distance-fn (:data clust1) (:data clust2))))]
    (loop [clusters (set (map bi-cluster dataset))]
      (if (<= (count clusters) 1)
        (first clusters)
        (let [[closest lowest-pair] (find-closest distance-fn clusters)
              averaged-data (average-fn (map :data lowest-pair))
              new-cluster (bi-cluster averaged-data lowest-pair closest)]
          (recur
           (->
            (apply disj clusters lowest-pair)
            (conj new-cluster))))))))

(defn prefix-walk
  ([visitor-fn clust]
   (prefix-walk visitor-fn clust 0))

  ([visitor-fn clust level]
   (when-not (empty? clust)
     (visitor-fn clust level)
     (prefix-walk visitor-fn (:left clust) (inc level))
     (prefix-walk visitor-fn (:right clust) (inc level)))))
