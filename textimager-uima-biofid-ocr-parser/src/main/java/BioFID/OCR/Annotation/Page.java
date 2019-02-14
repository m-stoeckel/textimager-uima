package BioFID.OCR.Annotation;

import BioFID.Util;
import org.apache.uima.jcas.JCas;
import org.texttechnologylab.annotation.ocr.OCRpage;
import org.xml.sax.Attributes;

public class Page extends Annotation {
	private Integer width;
	private Integer height;
	private Integer resolution;
	private boolean originalCoords;
	
	public String pageId;
	public Integer pageNumber;
	
	public Page(Attributes attributes) {
		this.width = Util.parseInt(attributes.getValue("width"));
		this.height = Util.parseInt(attributes.getValue("height"));
		this.resolution = Util.parseInt(attributes.getValue("resolution"));
		this.originalCoords = Util.parseBoolean(attributes.getValue("originalCoords"));
	}
	
	@Override
	public OCRpage wrap(JCas jCas, int offset) {
		OCRpage ocrPage = new OCRpage(jCas, start + offset, end + offset);
		ocrPage.setWidth(width);
		ocrPage.setHeight(height);
		ocrPage.setResolution(resolution);
		ocrPage.setPageId(pageId);
		ocrPage.setPageNumber(pageNumber);
		return ocrPage;
	}
}
