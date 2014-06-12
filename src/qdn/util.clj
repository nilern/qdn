(ns qdn.util)

(defn single? [coll]
  (and (coll? coll) (empty? (rest coll))))

(defn first-key [m]
  (first (keys m)))

(defn keyword->str [k]
  (subs (str k) 1))
