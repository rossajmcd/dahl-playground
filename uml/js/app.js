function main(container) {
  if (!mxClient.isBrowserSupported()) {
    mxUtils.error('Browser is not supported!', 200, false);
  } else {
    var graph = new mxGraph(container);
    // graph.setCellsResizable(false);
    // graph.setCellsMovable(false);
    // graph.setResizeContainer(false);
    // graph.setCellsLocked(true);
    // graph.minimumContainerSize = new mxRectangle(0, 0, 800, 600);
    graph.setBorder(10);

    // Stops editing on enter key, handles escape
    new mxKeyHandler(graph);

    // Overrides method to disallow edge label editing
    graph.isCellEditable = function(cell) {
      return !this.getModel().isEdge(cell);
    };

    // Overrides method to provide a cell label in the display
    graph.convertValueToString = function(cell) {
      if (mxUtils.isNode(cell.value)) {
        if (cell.value.nodeName.toLowerCase() == 'node') {
          return cell.getAttribute('name', '');
        }
        else if (cell.value.nodeName.toLowerCase() == 'edge') {
          return cell.getAttribute('name');
        }
      }
      return '';
    };

    // Overrides method to store a cell label in the model
    var cellLabelChanged = graph.cellLabelChanged;
    graph.cellLabelChanged = function(cell, newValue, autoSize) {
      if (mxUtils.isNode(cell.value) && cell.value.nodeName.toLowerCase() == 'node') {
        var pos = newValue.indexOf(' ');
        var name = (pos > 0) ? newValue.substring(0, pos) : newValue;

        // Clones the value for correct undo/redo
        var elt = cell.value.cloneNode(true);
        elt.setAttribute('name', name);

        newValue = elt;
        autoSize = true;
      }
      cellLabelChanged.apply(this, arguments);
    };

    // Overrides method to create the editing value
    var getEditingValue = graph.getEditingValue;
    graph.getEditingValue = function(cell) {
      if (mxUtils.isNode(cell.value) && cell.value.nodeName.toLowerCase() == 'node') {
        return cell.getAttribute('name', '');
      }
    };

    // Adds a special tooltip for edges
    graph.setTooltips(true);

    var getTooltipForCell = graph.getTooltipForCell;
    graph.getTooltipForCell = function(cell) {
      // Adds some relation details for edges
      if (graph.getModel().isEdge(cell)) {
        var src = this.getLabel(this.getModel().getTerminal(cell, true));
        var trg = this.getLabel(this.getModel().getTerminal(cell, false));
        return src + ' ' + cell.value.nodeName + ' ' +  trg;
      }
      return getTooltipForCell.apply(this, arguments);
    };

    // Enables rubberband selection
    new mxRubberband(graph);

    // Adds an option to view the XML of the graph
    document.body.appendChild(mxUtils.button('View XML', function() {
      var encoder = new mxCodec();
      var node = encoder.encode(graph.getModel());
      mxUtils.popup(mxUtils.getPrettyXml(node), true);
    }));

    // Changes the style for match the markup
    // Creates the default style for vertices
    var style = graph.getStylesheet().getDefaultVertexStyle();
    // style[mxConstants.STYLE_STROKECOLOR] = 'gray';
    style[mxConstants.STYLE_ROUNDED] = true;
    // style[mxConstants.STYLE_SHADOW] = true;
    // style[mxConstants.STYLE_FILLCOLOR] = '#DFDFDF';
    // style[mxConstants.STYLE_GRADIENTCOLOR] = 'white';
    // style[mxConstants.STYLE_FONTCOLOR] = 'black';
    // style[mxConstants.STYLE_FONTSIZE] = '12';
    // style[mxConstants.STYLE_SPACING] = 4;
    //
    // // Creates the default style for edges
    style = graph.getStylesheet().getDefaultEdgeStyle();
    // style[mxConstants.STYLE_STROKECOLOR] = '#0C0C0C';
    style[mxConstants.STYLE_LABEL_BACKGROUNDCOLOR] = 'white';
    style[mxConstants.STYLE_EDGE] = mxEdgeStyle.ElbowConnector;
    style[mxConstants.STYLE_ROUNDED] = true;
    // style[mxConstants.STYLE_FONTCOLOR] = 'black';
    // style[mxConstants.STYLE_FONTSIZE] = '10';

    // Gets the default parent for inserting new cells. This
    // is normally the first child of the root (ie. layer 0).
    var parent = graph.getDefaultParent();

    // Adds cells to the model in a single step
    graph.getModel().beginUpdate();

    var doc = mxUtils.createXmlDocument();

    var node1 = doc.createElement('Node');
    node1.setAttribute('name', 'Ready');

    var node2 = doc.createElement('Node');
    node2.setAttribute('name', 'Ingredients');

    var node3 = doc.createElement('Node');
    node3.setAttribute('name', 'Make Drink');

    var relation1 = doc.createElement('Edge');
    relation1.setAttribute('name', 'Select Ingredients');

    var relation2 = doc.createElement('Edge');
    relation2.setAttribute('name', 'Add Beverage');

    var relation3 = doc.createElement('Edge');
    relation3.setAttribute('name', 'Add Milk');

    var relation4 = doc.createElement('Edge');
    relation4.setAttribute('name', 'Add Sugar');

    var relation5 = doc.createElement('Edge');
    relation5.setAttribute('name', 'Make Drink');

    try {
      var v1 = graph.insertVertex(parent, null, node1, 10, 10,  80, 30);
      var v2 = graph.insertVertex(parent, null, node2, 10, 110, 80, 30);
      var v3 = graph.insertVertex(parent, null, node3, 10, 210, 80, 30);

      var e1 = graph.insertEdge(parent, null, relation1, v1, v2);
      var e2 = graph.insertEdge(parent, null, relation2, v2, v2);
      e2.getGeometry().points = [new mxPoint(200, 110)];
      var e3 = graph.insertEdge(parent, null, relation3, v2, v2);
      e3.getGeometry().points = [new mxPoint(300, 110)];
      var e4 = graph.insertEdge(parent, null, relation4, v2, v2);
      e4.getGeometry().points = [new mxPoint(400, 110)];
      var e5 = graph.insertEdge(parent, null, relation5, v2, v3);
    } finally {
      // Updates the display
      graph.getModel().endUpdate();
    }

    // Implements a properties panel that uses
    // mxCellAttributeChange to change properties
    graph.getSelectionModel().addListener(mxEvent.CHANGE, function(sender, evt) {
      selectionChanged(graph);
    });

    selectionChanged(graph);
  }

  /**
   * Updates the properties panel
   */
  function selectionChanged(graph) {
    var div = document.getElementById('properties');
    // Forces focusout in IE
    graph.container.focus();
    // Clears the DIV the non-DOM way
    div.innerHTML = '';
    // Gets the selection cell
    var cell = graph.getSelectionCell();

    if (cell == null) {
      mxUtils.writeln(div, 'Nothing selected.');
    } else {
      // Writes the title
      var center = document.createElement('center');
      mxUtils.writeln(center, cell.value.nodeName);
      div.appendChild(center);
      mxUtils.br(div);

      // Creates the form from the attributes of the user object
      var form = new mxForm();
      var attrs = cell.value.attributes;

      for (var i = 0; i < attrs.length; i++) {
        createTextField(graph, form, cell, attrs[i]);
      }

      div.appendChild(form.getTable());
      mxUtils.br(div);
    }
  }

  /**
   * Creates the textfield for the given property.
   */
  function createTextField(graph, form, cell, attribute) {
    var input = form.addText(attribute.nodeName + ':', attribute.nodeValue);
    var applyHandler = function() {
      var newValue = input.value || '';
      var oldValue = cell.getAttribute(attribute.nodeName, '');

      if (newValue != oldValue) {
        graph.getModel().beginUpdate();

        try {
          var edit = new mxCellAttributeChange(cell, attribute.nodeName, newValue);
          graph.getModel().execute(edit);
          graph.updateCellSize(cell);
        } finally {
          graph.getModel().endUpdate();
        }
      }
    };

    mxEvent.addListener(input, 'keypress', function (evt) {
      // Needs to take shift into account for textareas
      if (evt.keyCode == /*enter*/13 && !mxEvent.isShiftDown(evt)) {
        input.blur();
      }
    });

    if (mxClient.IS_IE) {
      mxEvent.addListener(input, 'focusout', applyHandler);
    } else {
      // Note: Known problem is the blurring of fields in
      // Firefox by changing the selection, in which case
      // no event is fired in FF and the change is lost.
      // As a workaround you should use a local variable
      // that stores the focused field and invoke blur
      // explicitely where we do the graph.focus above.
      mxEvent.addListener(input, 'blur', applyHandler);
    }
  }
};
