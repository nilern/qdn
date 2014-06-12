import QtQuick.Controls 1.2
import QtQuick.Layouts 1.1

ApplicationWindow {
  title: "Musistant"
  width: 700
  height: 500

  menuBar: MenuBar {
             Menu {
               title: "File"
               MenuItem { text: "Open"}
               MenuItem { text: "Save"}
               MenuItem { text: "Save As"}
             }
  }

  toolBar: ToolBar {
  					 RowLayout {
  					  anchors.fill: parent
             	TabView {
				  			Layout.fillHeight: true
					  		Layout.fillWidth: true
    	        	Tab {
      	           title: "Insert"
        	         RowLayout {
        	           anchors.fill: parent
          	         ToolButton { iconName: "save" }
            	     }
              	 }
               	Tab { title: "Edit" }
               	Tab { title: "Analyze" }
             	}
             }
  }

  SplitView {
    anchors.fill: parent
    TextArea {
      text: "Select something, anything!"
      Layout.fillHeight: true
      Layout.fillWidth: true
    }
    TabView {
      Layout.fillHeight: true
      Layout.fillWidth: true
      Tab { title: "Notation" }
      Tab { title: "Mixer" }
    }
  }
}
