(ns qdn.core-test
  (:require [clojure.test :refer :all]
            [qdn.core :refer :all]))

(deftest import-form
  (is (= (edn-imports->qml
           '(import [Namespace "1.0"]
                    [Namespace "1.0" :as SingletonTypeIdentifier]
                    ["directory"]
                    ["file.js" :as ScriptIdentifier]))
         (str "import Namespace 1.0\n"
              "import Namespace 1.0 as SingletonTypeIdentifier\n"
              "import \"directory\"\n"
              "import \"file.js\" as ScriptIdentifier\n\n"))))

(deftest property
  (testing "keys"
    (are [k qml-k] (= (key->qml k) qml-k)
                   :key "key"
                   "key" "key"
                   'key "key"))
  (testing "values"
    (testing "literals"
      (testing "atoms"
        (are [v qml-v] (= (val->qml v) qml-v)
                       'val "val"
                       :val "val"
                       "val" "\"val\""
                       23 "23"
                       5.5 "5.5"))
      (testing "collections"
        (are [v qml-v] (= (val->qml v) qml-v)
                       [0 2 3] "[0, 2, 3]"
                       {:a 8 :b 7} "{b: 7, a: 8}"
                       {"a" 8 "b" 7} "{\"a\": 8, \"b\": 7}"))
      (testing "function"
        (testing "call"
          (is (= (val->qml '(high 5)) "high(5)")))
        (testing "definition"
          (is (= (val->qml '(fn [x] (+ x 20)))
                 (str "function (x) {\n"
                      "    (x + 20);\n"
                      "  }")))))
      (testing "property access"
        (is (= (val->qml 'parent.height) "parent.height")))
      (testing "conditional"
        (is (= (val->qml '(if (< x 0) x (inc x)))
               (str "if ((x < 0)) {\n"
                    "    x\n"
                    "  } else {\n"
                    "    (x + 1)\n"
                    "  }"))))
      (testing "imperative"
        (is (= (val->qml '(do (var x 1) (set! x 3)))
               (str "{\n"
                    "    var x;\n"
                    "    x = 1;\n"
                    "    x = 3;\n"
                    "  }")))))))

(deftest property-attribute
  (testing "declaration"
    (is (= (property-attribute->qml '(defproperty :real deal) 0)
           "  property real deal\n"))
    (testing "and initialization"
      (is (= (property-attribute->qml '(defproperty :real deal 0.95) 0)
             "  property real deal: 0.95\n"))))
  (testing "integration"
    (is (= (qt-item->qml
             '[MouseArea
               (defproperty :real mouseR (Math.sqrt (Math.pow mouseX 2)
                                                    (Math.pow mouseY 2)))
               {:anchors.fill parent}] 0 :block)
           (str "MouseArea {\n"
                "  property real mouseR: Math.sqrt(Math.pow(mouseX, 2), "
                "Math.pow(mouseY, 2))\n"
                "  anchors.fill: parent\n"
                "}\n")))))

(deftest method-attribute
  (is (= (qt-item->qml
           '[MouseArea
             (defproperty :method hide (fn [] (set! visible false)))
             {:anchors.fill parent
              :color "cyan"}] 0 :block)
         (str "MouseArea {\n"
              "  function hide() {\n"
              "    visible = false;\n"
              "  }\n"
              "  anchors.fill: parent\n"
              "  color: \"cyan\"\n"
              "}\n"))))

(deftest item-form
  (testing "component-name"
    (are [cn] (= (name->qml cn) "Component")
              'Component
              :Component)
    (testing "...and then some"
      (is (= (qt-item->qml '[Behavior on x
                             [NumberAnimation
                              {:easing.type Easing.OutQuad}]]
                           0 :block)
             (str "Behavior on x {\n"
                  "  NumberAnimation {\n"
                  "    easing.type: Easing.OutQuad\n"
                  "  }\n"
                  "}\n")))))
  (testing "empty form"
    (is (= (qt-item->qml '[Component] 0 :block)
           "Component {\n}\n")))
  (testing "id"
    (testing "normal"
      (is (= (qt-item->qml '[Component {:id ego}] 0 :block)
             "Component {\n  id: ego\n}\n")))
    (testing "shorthand"
      (is (= (qt-item->qml '[Component#ego] 0 :block)
             "Component {\n  id: ego\n}\n"))))
  (testing "property maps"
    (testing "single map"
      (is (= (qt-item->qml '[Rectangle
                             {:id ego
                              :color "red"
                              :border.radius 10}]
                           0 :block)
             "Rectangle {\n  id: ego\n  color: \"red\"\n  border.radius: 10\n}\n")))
    (testing "multiple maps"
      (is (= (qt-item->qml '[Rectangle {:id ego}
                             {:color "red"
                              :border.radius 10}]
                           0 :block)
             "Rectangle {\n  id: ego\n  color: \"red\"\n  border.radius: 10\n}\n"))))
  (testing "nested forms"
    (testing "simple"
      (is (= (qt-item->qml '[ListModel
                             {:id topModel}

                             [ListElement {:name "has great fame"}]
                             [ListElement {:name "what a shame"}]]
                           0 :block)
             (str "ListModel {\n  id: topModel\n"
                  "  ListElement {\n    name: \"has great fame\"\n  }\n"
                  "  ListElement {\n    name: \"what a shame\"\n  }\n"
                  "}\n"))))
    (testing "deeper"
      (is (= (qt-item->qml '[Rectangle [MouseArea [Text]]] 0 :block)
             (str "Rectangle {\n"
                  "  MouseArea {\n"
                  "    Text {\n"
                  "    }\n"
                  "  }\n"
                  "}\n"))))))

(deftest list-MVC
  (is (= (list-model (range 4))
         '[ListModel {}
           [ListElement {:value 0}]
           [ListElement {:value 1}]
           [ListElement {:value 2}]
           [ListElement {:value 3}]]))
  (is (= (list-model [{:val "kyrie"} {:val "iant"}])
         '[ListModel {}
           [ListElement {:val "kyrie"}]
           [ListElement {:val "iant"}]]))
  (is (= (list-model [[:val "kyrie"] [:val "iant"]])
         '[ListModel {}
           [ListElement {:val "kyrie"}]
           [ListElement {:val "iant"}]]))
  (is (= (list-model [{:val "kyrie" :key "to Heaven"}])
         '[ListModel {}
           [ListElement {:val "kyrie" :key "to Heaven"}]]))
  (is (= (list-model {:id 'wornOutBeliefs}
                     [{:val "kyrie" :key "to Heaven"}])
         '[ListModel {:id wornOutBeliefs}
           [ListElement {:val "kyrie" :key "to Heaven"}]])))