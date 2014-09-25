(ns qdn.core-test
  (:require [clojure.test :refer :all]
            [qdn.core :refer :all]))

(deftest import-form
  (is (= (edn-imports->qml
           '(import [Namespace "1.0"]
                    [Namespace "1.0"
                     :as SingletonTypeIdentifier]
                    ["directory"]
                    ["file.js" :as ScriptIdentifier]))
         (str "import Namespace 1.0\n"
              "import Namespace 1.0 as SingletonTypeIdentifier\n"
              "import \"directory\"\n"
              "import \"file.js\" as ScriptIdentifier\n\n"))))

(deftest item-form
  (testing "component-name"
    (are [cn] (= (component-name->str cn) "Component")
              'Component
              "Component"
              :Component))
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