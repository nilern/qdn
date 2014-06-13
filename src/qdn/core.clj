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

(defn indent-str [i]
  (string/join (repeat i "  ")))

(defn qt-item? [m]
  (and (map? m) (single? m) (symbol? (first-key m))))

(declare item-map->qml)

(defn key->qml [k]
  (if (keyword? k)
    (keyword->str k)
    (str k)))

(declare qt-item->qml)

(defn val->qml [v indent]
  (if (qt-item? v)
    (qt-item->qml v (inc indent) :inline)
    (str v)))

(defn items->qml [items indent]
  (if (vector? items)
    (string/join (map #(str (qt-item->qml % (inc indent) :block)) items))
    (str (qt-item->qml items (inc indent) :block))))

(defn item-map-entry->qml [[k v] indent]
  (case k
    :Items (items->qml v indent)
    :functions nil
    (str (indent-str (inc indent))
           (key->qml k) (when-not (nil? v) ": ")
           (val->qml v indent) "\n")))

(defn item-map->qml [m indent]
  (string/join (map #(item-map-entry->qml % indent) m)))

(defn qt-item->qml [im indent inline]
  (let [item (first-key im)
        props (im item)]
    (str (when (= inline :block) (indent-str indent))
         item " {\n"
         (item-map->qml props indent)
         (indent-str indent) "}\n")))

(defn edn-ui-tree->qml [eut]
  (qt-item->qml eut 0 :block))

;;; API
;;; ============================================================================

(defn edn->qml [fl]
  (with-open [infile (java.io.PushbackReader. (clojure.java.io/reader fl))]
    (let [imports (edn/read infile)
          ui-tree (edn/read infile)]
      (str (edn-imports->qml imports);))))
           (edn-ui-tree->qml ui-tree)))))
