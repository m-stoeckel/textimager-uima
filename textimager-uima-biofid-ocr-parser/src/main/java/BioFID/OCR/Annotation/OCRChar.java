package BioFID.OCR.Annotation;

import org.xml.sax.Attributes;

/**
 * Created on 21.01.2019.
 */
public class OCRChar extends OCRAnnotation
{
	public final int top;
	public final int bottom;
	public final int left;
	public final int right;

	public OCRChar(Attributes attributes)
	{
		this.top = Util.parseInt(attributes.getValue("t"));
		this.bottom = Util.parseInt(attributes.getValue("b"));
		this.left = Util.parseInt(attributes.getValue("l"));
		this.right = Util.parseInt(attributes.getValue("r"));
	}
}
