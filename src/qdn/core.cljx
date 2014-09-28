(ns qdn.core
  "Turn (hiccup-like) edn forms into QML."
  (:require [clojure.string :as string]
   #+clj    [com.reasonr.scriptjure :refer [js]])
  ;#+cljs (:require-macros [com.reasonr.scriptjure :refer [js]])
  )

;;; Fake js macro with these fns because scriptjure is not cljx yet:

#+cljs
(defn js [expr]
  (cond
    (string? expr) (str "\"" expr "\"")
    (keyword? expr) (str (name expr))
    :else (str expr)))

#+cljs
(def clj identity)

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

(defn name->qml
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

(defn indent-js [js-str indent]
  (if (re-find #"\n" js-str)
    (->> (string/split-lines js-str)
         (map string/trim)
         (#(cons (first %)
                 (map (fn [a]
                        (let [i (if-not (= (first a) \}) 2 1)]
                          (str (indent-str (+ indent i)) a)))
                      (rest %))))
         (remove #(= (re-find #"\s*" %) %))
         (string/join "\n"))
    js-str))

(defn key->qml [k]
  (if (keyword? k)
    (name k)
    (str k)))

(declare qt-item->qml)

(defn val->qml
  ([v] (val->qml v 0))
  ([v indent]
   (cond (qt-item? v) (qt-item->qml v (inc indent) :inline)
         :else (indent-js (let [js-expr (js (clj v))]
                            (if (re-find #";\s*[^\s\}]" js-expr)
                              (str "{\n" js-expr "\n}")
                              (str js-expr)))
                          indent))))

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

(defn property-attribute? [expr]
  (and (coll? expr)
       (= (first expr) 'defproperty)))

(defn property-attribute->qml
  "`(defproperty :real deal) ;=> \"property real deal\"
    (defproperty :real deal 0.95);=> \"property real deal: 0.95\"`
    (defproperty :method hide (fn [] (set! visible false))
    ;=> \"function hide() { visible = false; }\"`"
  [[_ type name :as prop-attr] indent]
  (if (= (key->qml type) "method")
    (str "  "
         (indent-js
           (js (clj (apply list (concat (list 'function name)
                                                   (rest (nth prop-attr 3))))))
           indent)
         "\n")
    (str (indent-str (inc indent)) "property "
         (key->qml type) " " (name->qml name)
         (when (> (count prop-attr) 3)
           (str ": " (val->qml (nth prop-attr 3))))
         "\n")))

(defn qt-item->qml [im indent inline]
  (let [item (filter component-name? im)
        component-name (name->qml (first item))
        prop-attrs (filter property-attribute? im)
        prop-maps (filter map? im)
        sub-items (filter qt-item? im)]
    (if-let [id (second (re-find #"#(\S*)" component-name))]
      ;; id shorthand Item#id
      (recur (vec (concat
                    [(string/replace component-name #"#\S*" "") {:id (symbol id)}]
                    (rest im)))
             indent inline)
      (str (when (= inline :block) (indent-str indent))
           (string/join " " (map name->qml item)) " {\n"
           (string/join (map #(property-attribute->qml % indent) prop-attrs))
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

(defn list-element [e]
  ['ListElement (cond
                  (map? e) e
                  (coll? e) (apply hash-map e)
                  :else {:value e})])

(defn list-model
  ([coll] (list-model {} coll))
  ([props coll] (vec (concat ['ListModel props]
                             (map list-element coll)))))