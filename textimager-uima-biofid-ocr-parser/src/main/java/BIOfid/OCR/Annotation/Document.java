package BIOfid.OCR.Annotation;

import biofid.utility.Util;
import com.google.common.base.Strings;
import org.xml.sax.Attributes;

public class Document {
	
	// Mandatory
	private String version;
	private String producer;
	
	// Optional
	private Integer pagesCount = 1;
	private String mainLanguage;
	private String languages;
	
	public Document(Attributes attributes) {
		this.version = attributes.getValue("version");
		this.producer = attributes.getValue("producer");
		this.pagesCount = Util.parseInt(attributes.getValue("pagesCount"));
		this.mainLanguage = Strings.nullToEmpty(attributes.getValue("mainLanguage"));
		this.languages = Strings.nullToEmpty(attributes.getValue("languages"));
	}
}
