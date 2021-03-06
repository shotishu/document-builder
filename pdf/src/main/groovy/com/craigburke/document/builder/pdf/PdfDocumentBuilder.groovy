package com.craigburke.document.builder.pdf

import com.craigburke.document.builder.pdf.render.ParagraphRenderer
import com.craigburke.document.builder.pdf.render.TableRenderer
import com.craigburke.document.core.LineBreak
import groovy.transform.InheritConstructors
import groovy.xml.MarkupBuilder

import com.craigburke.document.core.builder.DocumentBuilder
import com.craigburke.document.core.Document
import com.craigburke.document.core.Paragraph
import com.craigburke.document.core.Table
import com.craigburke.document.core.Row
import com.craigburke.document.core.Cell
import com.craigburke.document.core.Image
import com.craigburke.document.core.Text

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.common.PDMetadata

@InheritConstructors
class PdfDocumentBuilder extends DocumentBuilder {

	void createDocument(Document document, OutputStream out) {
        PdfDocument pdfDocument = new PdfDocument(document)
        document.item = pdfDocument

        pdfDocument.x = document.margin.left
        pdfDocument.y = document.margin.top

        document.item = pdfDocument
        document
    }
	
	void addParagraphToDocument(Paragraph paragraph, Document document) {
		document.item.x = paragraph.margin.left + document.margin.left
		document.item.y += paragraph.margin.top
	}

	void addImageToParagraph(Image image, Paragraph paragraph) {
        // Handled in onParagraphComplete
	}
	
	void addLineBreakToParagraph(LineBreak lineBreak, Paragraph paragraph) {
        // Handled in onParagraphComplete
	}
	
	void addTextToParagraph(Text text, Paragraph paragraph) {
        // Handled in onParagraphComplete
	}
	
	def onParagraphComplete = { Paragraph paragraph ->
        int maxLineWidth = document.item.currentPage.mediaBox.width - document.margin.left - document.margin.right - paragraph.margin.left - paragraph.margin.right
        int renderStartX = document.margin.left + paragraph.margin.left

        ParagraphRenderer paragraphRenderer = new ParagraphRenderer(paragraph, document, renderStartX, maxLineWidth)
        paragraphRenderer.render()

        if (document.item.remainingPageHeight < paragraph.margin.bottom) {
            int marginDiff = paragraph.margin.bottom - document.item.remainingPageHeight
            document.item.addPage()
            document.item.y += marginDiff
        }
        else {
            document.item.y += paragraph.margin.bottom
        }

    }

	void addTableToDocument(Table table, Document document) {
	}

	void addRowToTable(Row row, Table table) {

    }
	
	void addCellToRow(Cell cell, Row row) {

	}
	
	void addParagraphToCell(Paragraph paragraph, Cell cell) {
	}

    def onTableComplete = { Table table ->
        TableRenderer tableRenderer = new TableRenderer(table, document)
        tableRenderer.render()
    }
	

	void write(Document document, OutputStream out) {
		addMetadata()
		document.item.contentStream?.close()
		document.item.pdDocument.save(out)
		document.item.pdDocument.close()
	}

	private void addMetadata() {
		ByteArrayOutputStream xmpOut = new ByteArrayOutputStream()
		def xml = new MarkupBuilder(xmpOut.newWriter())

		xml.document(marginTop: "${document.margin.top}", marginBottom: "${document.margin.bottom}", marginLeft: "${document.margin.left}", marginRight: "${document.margin.right}") {

			delegate = xml
			resolveStrategy = Closure.DELEGATE_FIRST

			document.children.each { child ->
				if (child.getClass() == Paragraph) {
					paragraph(marginTop: "${child.margin.top}", marginBottom: "${child.margin.bottom}", marginLeft: "${child.margin.left}", marginRight: "${child.margin.right}") {
						child.children.findAll { it.getClass() == Image }.each {
							image()
						}
					}
				}
				else {
					table(columns: child.columns, width: child.width, borderSize: child.border.size) {
						child.rows.each {
							def cells = it.cells
							row() {
								cells.each {
									cell(width: "${it.width ?: 0}")
								}
							}
						}
					}
				}
			}
		}

		def catalog = document.item.pdDocument.documentCatalog
		PDMetadata metadata = new PDMetadata(document.item.pdDocument as PDDocument, new ByteArrayInputStream(xmpOut.toByteArray()), false)
		catalog.metadata = metadata
	}

}