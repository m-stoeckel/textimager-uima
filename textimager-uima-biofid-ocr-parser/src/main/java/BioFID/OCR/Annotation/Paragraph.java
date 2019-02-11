package BioFID.OCR.Annotation;

import com.google.common.base.Strings;
import org.apache.uima.jcas.JCas;
import org.texttechnologylab.annotation.ocr.OCRParagraph;
import org.xml.sax.Attributes;

public class Paragraph extends Annotation {
	
	public final int leftIndent;
	public final int rightIndent;
	public final int startIndent;
	public final int lineSpacing;
	public final alignment align;
	
	private enum alignment {
		Left, Center, Right, Justified
	}
	
	public Paragraph(int leftIndent, int rightIndent, int startIndent, int lineSpacing, String align) {
		this.leftIndent = leftIndent;
		this.rightIndent = rightIndent;
		this.startIndent = startIndent;
		this.lineSpacing = lineSpacing;
		this.align = alignment.valueOf(align);
	}
	
	public Paragraph(Attributes attributes) {
		this.leftIndent = Util.parseInt(attributes.getValue("leftIndent"));
		this.rightIndent = Util.parseInt(attributes.getValue("rightIndent"));
		this.startIndent = Util.parseInt(attributes.getValue("startIndent"));
		this.lineSpacing = Util.parseInt(attributes.getValue("lineSpacing"));
		this.align = Strings.isNullOrEmpty(attributes.getValue("align"))
				? alignment.Left
				: alignment.valueOf(attributes.getValue("align"));
	}
	
	@Override
	public OCRParagraph wrap(JCas jCas, int offset) {
		OCRParagraph ocrParagraph = new OCRParagraph(jCas, start + offset, end + offset);
		ocrParagraph.setLeftIndent(leftIndent);
		ocrParagraph.setRightIndent(rightIndent);
		ocrParagraph.setStartIndent(startIndent);
		ocrParagraph.setLineSpacing(lineSpacing);
		ocrParagraph.setAlign(align.toString());
		return ocrParagraph;
	}
}
