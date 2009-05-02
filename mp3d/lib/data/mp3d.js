function checkAll(self) {
  var elements = self.form.elements;
  for (var i = 0; i < elements.length; ++i) {
    var element = elements[i];
    if (element.type == 'checkbox' && element != self) {
      element.checked = self.checked;
    }
  }
}
