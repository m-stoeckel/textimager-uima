package biofid.engine.agreement;

import com.google.common.collect.ImmutableSortedSet;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.statistics.agreement.unitizing.KrippendorffAlphaUnitizingAgreement;
import org.dkpro.statistics.agreement.unitizing.UnitizingAnnotationStudy;
import org.texttechnologylab.annotation.AbstractNamedEntity;
import org.texttechnologylab.annotation.NamedEntity;


/**
 * Inter-annotator agreement engine using {@link UnitizingAnnotationStudy UnitizingAnnotationStudies} and
 * {@link KrippendorffAlphaUnitizingAgreement KrippendorffAlphaUnitizingAgreement}.
 * <p/>
 * Creates one <i>local</i> {@link UnitizingAnnotationStudy UnitizingAnnotationStudy} for each CAS to be processed and
 * concatenates the results in a single <i>global</i> study for which the Krippendorff-Alpha-Agreement is computed.
 * <p/>
 *
 * @see KrippendorffAlphaUnitizingAgreement
 */
public class TTLabUnitizingIAACollectionProcessingEngine extends UnitizingIAACollectionProcessingEngine {
	/**
	 * An array of flags which are to be included in the category name.<br>
	 * Default: none.<br>
	 * Choices: <ul>
	 * <li>{@link TTLabUnitizingIAACollectionProcessingEngine#METAPHOR}</li>
	 * <li>{@link TTLabUnitizingIAACollectionProcessingEngine#METONYM}</li>
	 * <li>{@link TTLabUnitizingIAACollectionProcessingEngine#SPECIFIC}</li>
	 * </ul>
	 */
	public static final String PARAM_INCLUDE_FLAGS = "pIncludeFlags";
	@ConfigurationParameter(
			name = PARAM_INCLUDE_FLAGS,
			mandatory = false
	)
	private String[] pIncludeFlags;
	private ImmutableSortedSet<String> includeFlags = ImmutableSortedSet.of();
	
	public static final String METAPHOR = "Metaphor";
	public static final String METONYM = "Metonym";
	public static final String SPECIFIC = "Specific";
	
	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		if (pIncludeFlags != null && pIncludeFlags.length > 0)
			includeFlags = ImmutableSortedSet.copyOf(pIncludeFlags);
	}
	
	@Override
	protected String getCatgoryName(Annotation annotation) {
		String category = super.getCatgoryName(annotation);
		if (annotation instanceof NamedEntity) {
			NamedEntity namedEntity = (NamedEntity) annotation;
			if (includeFlags.contains(METAPHOR) && namedEntity.getMetaphor())
				category += "-" + METAPHOR;
			if (includeFlags.contains(METONYM) && namedEntity.getMetonym())
				category += "-" + METONYM;
//			if (includeFlags.contains(SPECIFIC)) // FIXME
//				category += "-" + SPECIFIC;
		} else if (annotation instanceof AbstractNamedEntity) {
			AbstractNamedEntity namedEntity = (AbstractNamedEntity) annotation;
			if (includeFlags.contains(METAPHOR) && namedEntity.getMetaphor())
				category += "-" + METAPHOR;
			if (includeFlags.contains(METONYM) && namedEntity.getMetonym())
				category += "-" + METONYM;
//			if (includeFlags.contains(SPECIFIC)) // FIXME
//				category += "-" + SPECIFIC;
		}
		return category;
	}
}
