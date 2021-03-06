package org.hucompute.textimager.uima.gnd.gazetteer;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.languagetool.LanguageToolSegmenter;
import org.junit.jupiter.api.Test;
import org.texttechnologylab.annotation.type.Person_HumanBeing;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestGNDGazetteer {

	@Test
	public void testGNDGazetteer() {
		try {
			final AnalysisEngine segmewnter = AnalysisEngineFactory.createEngine(LanguageToolSegmenter.class);

			final AnalysisEngine gazetterEngine = AnalysisEngineFactory.createEngine(AnalysisEngineFactory.createEngineDescription(
					GNDGazetteer.class,
					GNDGazetteer.PARAM_SOURCE_LOCATION, "/home/daniel/data/hiwi/gnd/gnd_sample.txt",
					GNDGazetteer.PARAM_TAGGING_TYPE_NAME, Person_HumanBeing.class.getName(),
					GNDGazetteer.PARAM_MAPPING_PROVIDER_LOCATION, "classpath:/org/hucompute/textimager/uima/gnd/gazetteer/lib/ner-default.map",
					GNDGazetteer.PARAM_USE_LOWERCASE, false,
					GNDGazetteer.PARAM_USE_STRING_TREE, true,
					GNDGazetteer.PARAM_USE_SENTECE_LEVEL_TAGGING, false,
					GNDGazetteer.PARAM_USE_LEMMATA, false,
					//GNDGazetteer.PARAM_TOKEN_BOUNDARY_REGEX, "(?=\\p{PUNCT})|(?<=\\p{PUNCT})|(\\s+)",
					GNDGazetteer.PARAM_TOKEN_BOUNDARY_REGEX, "(\\p{PUNCT})|(\\s+)",
					GNDGazetteer.PARAM_RETOKENIZE, true,
					GNDGazetteer.PARAM_SPLIT_HYPEN, false
			));

			runTest(segmewnter, gazetterEngine);
		} catch (UIMAException e) {
			e.printStackTrace();
			fail();
		}
	}

	private void runTest(AnalysisEngine segmenter, AnalysisEngine gazetterEngine) throws UIMAException {
		JCas jCas = JCasFactory.createText("Dies ist ein Test mit vielen Personennamen, wie z.B. 'Abd al-Mun'im 'Akifs oder auch G. Dorje. Dazu auch mehrdeutige Namen wie Abdul B. Ebenso wie C. R. Rinpoche, 'Jigs-med-dpa'-bo und G.-Drukpa.");
		jCas.setDocumentLanguage("de");

		StopWatch stopWatch = StopWatch.createStarted();
		SimplePipeline.runPipeline(jCas, segmenter, gazetterEngine);
		System.out.printf("Finished tagging in %dms.\n", stopWatch.getTime(TimeUnit.MILLISECONDS));

		System.out.printf("Found %d GND.\n", JCasUtil.select(jCas, Person_HumanBeing.class).size());
		System.out.println(JCasUtil.select(jCas, Person_HumanBeing.class).stream().map(element -> String.format("%s (%d, %d): %s", element.getCoveredText(), element.getBegin(), element.getEnd(), element.getValue())).collect(Collectors.joining("\n")));

		String[] expected = new String[] {
				// Text, Name
				"'Abd al-Mun'im 'Akifs", "'Abd al-Mun'im 'Akif¤",
				"G. Dorje", "'Gyur-med-rdo-rje¤",
				"Abdul B.", "",
				"C. R. Rinpoche" ,"'Chi-med rig-'dzin¤http://de.wikipedia.org/wiki/Chhimed_Rigdzin",
				"'Jigs-med-dpa'-bo", "'Jigs-med-dpa'-bo¤",
				"G.-Drukpa", "'Jigs-med pad-ma dbang-chen <'Brug-chen, XII.>¤",
		};

		String[] result = JCasUtil
				.select(jCas, Person_HumanBeing.class)
				.stream()
				.flatMap(p -> Arrays.stream(new String[]{p.getCoveredText(), p.getValue()}))
				.toArray(String[]::new);

		assertArrayEquals(expected, result);
	}

}
