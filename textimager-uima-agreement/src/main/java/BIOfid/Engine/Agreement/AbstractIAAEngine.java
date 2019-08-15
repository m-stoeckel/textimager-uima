package BIOfid.Engine.Agreement;

import BIOfid.Utility.CountMap;
import com.google.common.collect.ImmutableSet;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.internal.ExtendedLogger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.statistics.agreement.ICategorySpecificAgreement;
import org.jetbrains.annotations.NotNull;
import org.texttechnologielab.annotation.type.Fingerprint;

import java.util.*;

/**
 * Abstract base class for all inter-annotator agreement engines.
 */
public abstract class AbstractIAAEngine extends JCasConsumer_ImplBase {
	
	/**
	 * An array of fully qualified names of classes, that extend {@link Annotation},
	 * which are to be considered in the agreement computation.
	 * Adding classes which extend other given classes might return unexpected results.
	 */
	public static final String PARAM_ANNOTATION_CLASSES = "pAnnotationClasses";
	@ConfigurationParameter(name = PARAM_ANNOTATION_CLASSES, mandatory = false)
	private String[] pAnnotationClasses;
	ImmutableSet<Class<? extends Annotation>> annotationClasses = ImmutableSet.of(Annotation.class);
	
	/**
	 * Defines the relation of the given annotators:
	 * <ul>
	 * <li>{@link AbstractIAAEngine#WHITELIST}: only the listed annotators will be considered.</li>
	 * <li>{@link AbstractIAAEngine#BLACKLIST}: all listed annotators will be excluded.</li>
	 * </ul>
	 */
	public static final String PARAM_ANNOTATOR_RELATION = "pRelation";
	@ConfigurationParameter(
			name = PARAM_ANNOTATOR_RELATION,
			defaultValue = "true",
			mandatory = false,
			description = "Decides weather to white- to or blacklist the given annotators."
	)
	Boolean pWhitelisting;
	public static final Boolean WHITELIST = true;
	public static final Boolean BLACKLIST = false;
	
	public static final String PARAM_ANNOTATOR_LIST = "pAnnotatorList";
	@ConfigurationParameter(name = PARAM_ANNOTATOR_LIST, mandatory = false)
	private String[] pAnnotatorList;
	ImmutableSet<String> listedAnnotators = ImmutableSet.of();
	
	/**
	 * The minimal number of views in each CAS.
	 */
	public static final String PARAM_MIN_VIEWS = "pDiscardSingleView";
	@ConfigurationParameter(
			name = PARAM_MIN_VIEWS,
			mandatory = false,
			defaultValue = "2"
	)
	protected Integer pMinViews;
	
	/**
	 * If true, only consider annotations coverd by a {@link Fingerprint}.
	 */
	public static final String PARAM_FILTER_FINGERPRINTED = "pFilterFingerprinted";
	@ConfigurationParameter(
			name = PARAM_FILTER_FINGERPRINTED,
			defaultValue = "true"
	)
	protected Boolean pFilterFingerprinted;
	
	/**
	 * If true, print annotation statistics
	 */
	public static final String PARAM_PRINT_STATS = "pPrintStatistics";
	@ConfigurationParameter(
			name = PARAM_PRINT_STATS,
			mandatory = false,
			defaultValue = "true"
	)
	Boolean pPrintStatistics;
	
	/**
	 * Possible aggregation methods for the calculation of the inter-annotator agreement values.
	 */
	public static final String PARAM_MULTI_CAS_HANDLING = "pMultiCasHandling";
	@ConfigurationParameter(
			name = PARAM_MULTI_CAS_HANDLING,
			mandatory = false,
			defaultValue = COMBINED
	)
	String pMultiCasHandling;
	
	/**
	 * {@link AbstractIAAEngine#PARAM_MULTI_CAS_HANDLING} choice. Process each of the CAS separately and output their
	 * inter-annotator agreement.
	 */
	public static final String SEPARATE = "SEPARATE";
	
	/**
	 * {@link AbstractIAAEngine#PARAM_MULTI_CAS_HANDLING} choice. Collect all annotations from each cas in a single study.
	 */
	public static final String COMBINED = "COMBINED";
	
	/**
	 * {@link AbstractIAAEngine#PARAM_MULTI_CAS_HANDLING} choice. Process each and collect them into a single study afterwards.
	 */
	public static final String BOTH = "BOTH";
	
	protected ExtendedLogger logger;
	
	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		logger = getLogger();
		ArrayList<Class<? extends Annotation>> classArrayList = new ArrayList<>();
		
		// If class names were passed as parameters, update the annotationClasses set
		if (pAnnotationClasses != null && pAnnotationClasses.length > 0) {
			for (String pAnnotationClass : pAnnotationClasses) {
				// Get a class from its class name and cast it to Class<? extends Annotation>
				try {
					Class<?> aClass = Class.forName(pAnnotationClass);
					if (Annotation.class.isAssignableFrom(aClass)) {
						classArrayList.add((Class<? extends Annotation>) aClass);
					}
				} catch (ClassNotFoundException e) {
					logger.warn(e.getMessage());
				}
			}
			// If any class could be found, update the set
			if (classArrayList.size() > 0)
				annotationClasses = ImmutableSet.copyOf(classArrayList);
		}
		
		// Set the list of excluded annotators
		if (pAnnotatorList != null && pAnnotatorList.length > 0) {
			listedAnnotators = ImmutableSet.copyOf(pAnnotatorList);
		}
		logger.info("Computing inter-annotator agreement for subclasses of " + annotationClasses.toString());
		if (!listedAnnotators.isEmpty()) {
			logger.info(String.format("%s annotators with ids: " + listedAnnotators.toString(), pWhitelisting ? "Whitelisting" : "Blacklisting"));
		}
	}
	
	/**
	 * Create a set of annotations, that are overlapped by another annotation
	 *
	 * @param viewCas     The cas containing the annotations.
	 * @param aClass      The class of the overlapped annotations.
	 * @param annotations A collection of all annotations.
	 * @return A HashSet of all annotations that are overlapped by any other annotation.
	 */
	@NotNull
	protected HashSet<Annotation> getOverlappedAnnotations(JCas viewCas, Class<? extends Annotation> aClass, Collection<? extends Annotation> annotations) {
		HashSet<Annotation> overlappedAnnotations = new HashSet<>();
		for (Annotation annotation : annotations) {
			JCasUtil.subiterate(viewCas, aClass, annotation, false, false).forEach(item -> {
				if (annotation.getType().equals(item.getType()))
					overlappedAnnotations.add(item);
			});
		}
		return overlappedAnnotations;
	}
	
	protected void printStudyResults(ICategorySpecificAgreement agreement, TreeSet<String> categories, Collection<String> annotators) {
		for (String category : categories) {
			System.out.printf("%s\t%f\n", category, agreement.calculateCategoryAgreement(category));
		}
		System.out.println();
	}
	
	protected void printStudyResultsAndStatistics(ICategorySpecificAgreement agreement, CountMap<String> categoryCount, HashMap<String, CountMap<String>> annotatorCategoryCount, TreeSet<String> categories, Collection<String> annotators) {
		for (String category : categories) {
			System.out.printf("%s\t%d\t%f\n", category, categoryCount.get(category), agreement.calculateCategoryAgreement(category));
		}
		System.out.println();
		
		// Print annotation statistics for each annotator and all categories
		if (pPrintStatistics) {
			System.out.print("Annotation statistics:\nAnnotator");
			for (String annotator : annotators) {
				System.out.printf("\t#%s", annotator);
			}
			System.out.println();
			
			System.out.print("Total");
			for (String annotator : annotators) {
				System.out.printf("\t%d", annotatorCategoryCount.get(annotator).values().stream().reduce(Integer::sum).orElse(0));
			}
			System.out.println();
			
			for (String category : categories) {
				System.out.printf("%s", category);
				for (String annotator : annotators) {
					System.out.printf("\t%d", annotatorCategoryCount.get(annotator).get(category));
				}
				System.out.println();
			}
		}
	}
	
	protected String getCatgoryName(Annotation annotation) {
		return annotation.getType().getName();
	}
}
