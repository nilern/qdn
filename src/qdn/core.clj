(ns qdn.core
  (:require [qdn.util :refer :all]
            [clojure.string :as string]
            [clojure.edn :as edn]))

;;; Import section
;;; ============================================================================

(defn import-vec->str [iv]
  (str (string/join " " (cons "import" iv)) "\n"))

(defn edn-imports->qml [eimp]
  (str (string/join (map import-vec->str (rest eimp))) "\n"))

;;; UI tree section
;;; ============================================================================

(defn qt-item? [m]
  (and (map? m) (single? m) (symbol? (first-key m))))

(declare item-map->qml)

(defn val->qml [v]
  (if (qt-item? v)
    (item-map->qml v)
    (str v)))

(defn map-pair->str [mp]
  (let [k (first mp)
        v (second mp)]
    (if (= k :Item)
      (str (val->qml v) "\n")
      (str (keyword->str k) ": " (val->qml v) "\n"))))

(defn map->str [m]
  (string/join (map map-pair->str m)))

(defn item-map->qml [im]
  (let [item (first-key im)
        props (im item)]
    (str item " {\n"
         (map->str props)
         "}\n")))

(defn edn-ui-tree->qml [eut]
  (item-map->qml eut))

;;; API
;;; ============================================================================

(defn edn->qml [fl]
  (with-open [infile (java.io.PushbackReader. (clojure.java.io/reader fl))]
    (let [imports (edn/read infile)
          ui-tree (edn/read infile)]
      (str (edn-imports->qml imports);))))
           (edn-ui-tree->qml ui-tree)))))
