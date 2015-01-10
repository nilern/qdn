(ns qdn.core
  "Turn (hiccup-like) edn forms into QML."
  (:require [clojure.string :as string]
   #+clj    [clojure.edn :as edn]
   #+cljs   [cljs.reader :as edn]
   #+clj    [clojure.java.io :as io]
   #+clj    [com.reasonr.scriptjure :refer [js]])
  ;#+cljs (:require-macros [com.reasonr.scriptjure :refer [js]])
  )

;;; Fake js macro with these fns because scriptjure is not cljx yet:

#+cljs
(defn js [expr]
  (cond
    (string? expr) (str "\"" expr "\"")
    (keyword? expr) (str (name expr))
    (list? expr) (str (js (first expr))
                      "(" (string/join ", " (map js (rest expr))) ")")
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
  "Is cn a valid part of a component name (a symbol or keyword)?"
  [cn]
  (or (symbol? cn) (keyword? cn)))

(defn name->qml
  "    cn ;=> \"cn\"
       \"cn\" ;=> \"cn\"
       :cn ;=> \"cn\""
  [cn]
  (str (name cn)))

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
         (string? v) (js (clj v))
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
                    [(symbol (string/replace component-name #"#\S*" ""))
                     {:id (symbol id)}]
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
  "Takes a tree of vectors `ui-tree` and optionally a list of import vectors
   `imports`. Returns the corresponding string of QML."
  ([ui-tree] (edn-ui-tree->qml ui-tree))
  ([imports ui-tree] (str (edn-imports->qml imports)
                          (edn-ui-tree->qml ui-tree))))

#+clj
(defn edn->QML-file
  "Like edn->QML, but `spit`s the output string into `file`."
  [file imports ui-tree]
  (spit file (edn->QML imports ui-tree)))

#+clj
(defn file->vector
  "Reads a QDN file `filename` and returns the contents as a vector
   `[imports ui-tree]`"
  [filename]
  (with-open [infile (java.io.PushbackReader. (io/reader filename))]
    (binding [*in* infile]
      (let [imports (edn/read *in*)
            ui-tree (edn/read *in*)]
        [imports ui-tree]))))

#+clj
(defn edn-file->QML-file
  "Reads the file `edn-file` with `file->vector` and writes the corresponding
   QML into the file `qml-file` (the default is UI.edn => UI.qml)."
  ([edn-file] (edn-file->QML-file edn-file (string/replace edn-file
                                                      #"\.\w*$"
                                                      ".qml")))
  ([edn-file qml-file]
    (apply edn->QML-file qml-file (file->vector edn-file))))

(defn list-element
  "    {:foo \"bar\"} ; => '[ListElement {:foo \"bar\"}]
       [:foo \"bar\"] ; => '[ListElement {:foo \"bar\"}]
       \"a lot\" ; => '[ListElement {:value \"a lot\"}]`"
  [e]
  ['ListElement (cond
                  (map? e) e
                  (coll? e) (apply hash-map e)
                  :else {:value e})])

(defn list-model
  "Turns `coll` into a QDN `'ListModel`. If `props` is supplied, it becomes the
   property map of the ListModel."
  ([coll] (list-model {} coll))
  ([props coll] (vec (concat ['ListModel props]
                             (map list-element coll)))))