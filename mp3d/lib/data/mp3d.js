function checkAll(self) {
  for each (var element in self.form.elements) {
    if (element.type == 'checkbox' && element != self) {
      element.checked = self.checked;
    }
  }
}
