import QtQuick 2.0
import QtQuick.Controls 1.0 as Controls
import "mandelbrot"
import "cljs.js" as CLJS

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
