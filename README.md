QDN
===

QDN is to [QML](http://en.wikipedia.org/wiki/QML) what
[Hiccup](https://github.com/weavejester/hiccup) is to HTML.

How to get it
-------------

It's on Clojars. Just put `[qdn "0.1.2"]` in your project.clj's :dependencies
vector.

Syntax
------

This QDN (docs/example.edn):

```edn
(import [QtQuick "2.0"]
        [QtQuick.Controls "1.0" :as Controls]
        ["mandelbrot"]
        ["cljs.js" :as CLJS])

[Rectangle#recklessRect
 (defproperty :color textColor "limeGreen")
 (defproperty :method flip (fn [] (set! rotation (+ rotation 180))))
 {:width 600
  :height 450
  :color "red"}

 [Text
  {:anchors.centerIn parent
   :color recklessRect.textColor
   :text "I'm so handsome and brave."}]

 [MouseArea
  {:anchors.fill parent
   :onClicked (.flip parent)}]

 [Behavior on rotation [RotationAnimation]]]
```

will produce this QML (docs/example.qml):

```QML
import QtQuick 2.0
import QtQuick.Controls 1.0 as Controls

Rectangle {
  property color textColor: "limeGreen"
  function flip() {
    rotation = (rotation + 180);
  }
  id: recklessRect
  width: 600
  height: 450
  color: "red"
  Text {
    anchors.centerIn: parent
    color: recklessRect.textColor
    text: "I'm so handsome and brave."
  }
  MouseArea {
    anchors.fill: parent
    onClicked: parent.flip()
  }
  Behavior on rotation {
    RotationAnimation {
    }
  }
}

```

(This is the actual output and so isn't in the best QML style (yet). But the
QML compiler doesn't care.)

You can use keywords or symbols as Component names. If you are not reading
an edn file, symbols need to be quoted.

For now QDN uses [my fork of Scriptjure](https://github.com/nilern/scriptjure)
to generate the values of QML properties. It is a far cry from ClojureScript,
but bindings and event handlers should be simple anyway (the good ol'
"don't put logic in your views" -thing).

The API Functions
-----------------

But how do you carry out the translation? Use these:

```
qdn.core/edn->QML
([ui-tree] [imports ui-tree])
  Takes a tree of vectors `ui-tree` and optionally a list of import vectors
   `imports`. Returns the corresponding string of QML.

qdn.core/edn->QML-file
([file imports ui-tree])
  Like edn->QML, but `spit`s the output string into `file`.

qdn.core/file->vector
([filename])
  Reads a QDN file `filename` and returns the contents as a vector
   `[imports ui-tree]`

qdn.core/edn-file->QML-file
([edn-file] [edn-file qml-file])
  Reads the file `edn-file` with `file->vector` and writes the corresponding
   QML into the file `qml-file` (the default is UI.edn => UI.qml).

qdn.core/list-element
([e])
      {:foo "bar"} ; => '[ListElement {:foo "bar"}]
       [:foo "bar"] ; => '[ListElement {:foo "bar"}]
       "a lot" ; => '[ListElement {:value "a lot"}]`

qdn.core/list-model
([coll] [props coll])
  Turns `coll` into a QDN `'ListModel`. If `props` is supplied, it becomes the
   property map of the ListModel.
```

### Dynamic Item creation

```
qdn.dynamic/qml-object!
([qml parent] [qml parent filepath])
  Instantiate the Component defined by the QML string `qml` as a child Item of
   the `parent` Item. Wraps `js/Qt.createQmlObject`.

qdn.dynamic/qdn-object!
([qdn-imports qdn-ui-tree parent] [qdn-imports qdn-ui-tree parent filepath])
  Like `qml-object!` but takes the qdn imports and ui-tree instead of a string
   of QML.

qdn.dynamic/binding
([f])
  Create a QML binding from the zero-argument function `f`.
   Wraps `js/Qt.binding`.`
```

License
-------

Copyright © 2014-2015 Pauli Jaakkola

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
