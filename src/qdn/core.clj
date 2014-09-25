(ns qdn.core
  "Turn (hiccup-like) edn forms into QML."
  (:require [clojure.string :as string]))

;;; Import section
;;; ============================================================================

(defn import-atom->qml
  "    symbol ;=> \"symbol\"
       \"string\" ;=> \"\"string\"\"
       :keyword ;=> \"keyword\"
       \"1.0\" ;=> 1.0"
  [a]
  (str (cond
         (symbol? a) a
         (string? a) (if (re-find #"\d+.\d+" a)
                       a ; version number
                       (str "\"" a "\""))
         (keyword? a) (name a))))

(defn import-vec->qml
  "    [Namespace \"VersionMajor.VersionMinor\"]
       ;=> \"import Namespace VersionMajor.VersionMinor\"
       [Namespace \"VersionMajor.VersionMinor\" :as SingletonTypeIdentifier]
       ;=> \"import Namespace VersionMajor.VersionMinor as SingletonTypeIdentifier\"
       [\"directory\"]
       ;=> \"import \"directory\"\"
       [\"file.js\" :as ScriptIdentifier]
       ;=> \"import \"file.js\" as ScriptIdentifier\""
  [iv]
  (str
    (string/join " " (cons "import" (map import-atom->qml iv)))
    "\n"))

(defn edn-imports->qml
  "eimp is of the form (import & import-vecs). Create the corresponding QML
   import forms. For details see the documentation for import-vec->str."
  [eimp]
  (str
    (string/join (map import-vec->qml (rest eimp)))
    "\n"))

;;; UI tree section
;;; ============================================================================

(defn component-name?
  "Is cn a valid component name (a symbol, string or keyword)?"
  [cn]
  (or (symbol? cn) (string? cn) (keyword? cn)))

(defn component-name->str
  "    cn ;=> \"cn\"
       \"cn\" ;=> \"cn\"
       :cn ;=> \"cn\""
  [cn]
  (cond
    (or (symbol? cn) (string? cn)) (str cn)
    (keyword? cn) (str (name cn))))

(defn qt-item?
  "Is m a qt-item, i.e. a vector whose first entry is a component-name?"
  [m]
  (and (vector? m) (component-name? (first m))))

(defn indent-str [i]
  (string/join (repeat i "  ")))

(defn key->qml [k]
  (if (keyword? k)
    (name k)
    (str k)))

(declare qt-item->qml)

(defn val->qml [v indent]
  (cond (qt-item? v) (qt-item->qml v (inc indent) :inline)
        (string? v) (str "\"" v "\"")
        :else (str v)))

(defn items->qml [items indent]
  (if (vector? items)
    (string/join (map #(str (qt-item->qml % (inc indent) :block)) items))
    (str (qt-item->qml items (inc indent) :block))))

(defn item-map-entry->qml
  "Turn one item map entry [:key value] into qml."
  [[k v] indent]
  (case k
    :Items (items->qml v indent)
    :functions nil
    (str (indent-str (inc indent))
         (key->qml k) (when-not (nil? v) ": ")
         (val->qml v indent) "\n")))

(defn item-map->qml
  "Turn the property map { :id ...} into qml id: ..."
  [m indent]
  (string/join (map #(item-map-entry->qml % indent) m)))

(defn qt-item->qml [im indent inline]
  (let [item (component-name->str (first im))
        prop-maps (filter map? im)
        sub-items (filter qt-item? im)]
    (if-let [id (second (re-find #"#(\S*)" item))]
      (recur (vec (concat
                    [(string/replace item #"#\S*" "") {:id (symbol id)}]
                    (rest im)))
             indent inline)
      (str (when (= inline :block) (indent-str indent))
           item " {\n"
           (string/join (map #(item-map->qml % indent) prop-maps))
           (string/join (map #(qt-item->qml % (inc indent) inline) sub-items))
           (indent-str indent) "}\n"))))

(defn edn-ui-tree->qml [eut]
  (qt-item->qml eut 0 :block))

;;; API
;;; ============================================================================

(defn edn->QML
  ([ui-tree] (edn-ui-tree->qml ui-tree))
  ([imports ui-tree] (str (edn-imports->qml imports)
                          (edn-ui-tree->qml ui-tree))))

(defn to-QML [qml]
  (str "import QtQuick 2.2\n\n" qml))

(defn list-element [name]
  (str "ListElement { name: \"" name "\"\n}\n"))

(defn pc-list-element [pc]
  (str "ListElement { pc: " pc "\n}\n"))

(defn list-model [props id]
  (str "ListModel {\n"
       (if id
         (str "id: " id "\n")
         "")
       (reduce #(str %1 %2) props)
       "}\n"))



