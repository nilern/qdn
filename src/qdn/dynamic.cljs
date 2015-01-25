(ns qdn.dynamic
  "Dynamically create QML Items and bindings."
  (:require [qdn.core :refer [edn->QML]]))

(defn qml-object!
  "Instantiate the Component defined by the QML string `qml` as a child Item of
   the `parent` Item. Wraps `js/Qt.createQmlObject`."
  ([qml parent] (.createQmlObject js/Qt qml parent))
  ([qml parent filepath] (.createQmlObject js/Qt qml parent filepath)))

(defn qdn-object!
  "Like `qml-object!` but takes the qdn imports and ui-tree instead of a string
   of QML."
  ([qdn-imports qdn-ui-tree parent]
    (qml-object! (edn->QML qdn-imports qdn-ui-tree) parent))
  ([qdn-imports qdn-ui-tree parent filepath]
    (qml-object! (edn->QML qdn-imports qdn-ui-tree) parent filepath)))

(defn binding
  "Create a QML binding from the zero-argument function `f`.
   Wraps `js/Qt.binding`."
  [f]
  (.binding js/Qt f))
