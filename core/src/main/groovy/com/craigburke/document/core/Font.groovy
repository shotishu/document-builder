package com.craigburke.document.core

class Font implements AssignableColor, Cloneable {
	String family = "Helvetica"
	Integer size = 12
	Boolean bold = false
	Boolean italic = false

	def leftShift(Map properties) {
		properties.each { key, value -> this[key] = value }
	}

	Object clone() {
		Font result = new Font(family: family, size: size, bold: bold, italic: italic)
		result.color = "#${color.hex}"
		result
	}
	
	
}