package org.hucompute.textimager.biofid;

import com.google.common.io.Files;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.core.tokit.RegexSegmenter;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.util.CasIOUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.texttechnologylab.annotation.type.Taxon;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@DisplayName("NaiveStringbasedTaxonTagger Test")
public class TestNaiveStringbasedTaxonTagger {
	
	@Test
	@DisplayName("Test")
	public void test() throws UIMAException, IOException, SAXException {
//		String documentText = "Der Alnion glutinosae-Verband ist wahrscheinlich besser der XXI. Klasse der Querceto-Fagetea anzuschließen und darin der Ordnung der Populetalia unterzustellen.\n";

//		String modelLocation = "/home/s3676959/Documents/BioFID/models/Taxa_1-Skip-N-Gram.bin";
		String sourceLocation = "/resources/public/stoeckel/BioFID/taxa.txt";
//		NaiveSkipGramModel.buildModel(modelLocation, sourceLocation, "de", true);

//		final AnalysisEngine regexSegmenterEngine = AnalysisEngineFactory.createEngine(AnalysisEngineFactory.createEngineDescription(RegexSegmenter.class,
//				RegexSegmenter.PARAM_LANGUAGE, "de",
//				RegexSegmenter.PARAM_TOKEN_BOUNDARY_REGEX, "[\\s\n\\-]+"));
		final AnalysisEngine breakIteratorSegmenterEngine = AnalysisEngineFactory.createEngine(AnalysisEngineFactory.createEngineDescription(BreakIteratorSegmenter.class,
				BreakIteratorSegmenter.PARAM_LANGUAGE, "de",
				BreakIteratorSegmenter.PARAM_SPLIT_AT_APOSTROPHE, true));
		final AnalysisEngine naiveTaggerEngine = AnalysisEngineFactory.createEngine(AnalysisEngineFactory.createEngineDescription(NaiveStringbasedTaxonTagger.class,
				NaiveStringbasedTaxonTagger.PARAM_SOURCE_LOCATION, sourceLocation,
				NaiveStringbasedTaxonTagger.PARAM_USE_LOWERCASE, true));
		
		
		ExecutorService executorService = Executors.newCachedThreadPool();
//		for (File file : new File("/home/s3676959/Documents/BioFID/data/BioFID_TXT_raw_18.02/").listFiles()) {
		File file = new File("/resources/public/ahmed/BIOfid/BioFID_XMI_TI_28.03/9031034.xmi");
		{
			JCas jCas = JCasFactory.createJCas();
			CasIOUtils.load(java.nio.file.Files.newInputStream(file.toPath()), null, jCas.getCas(), true);
			
//			System.out.printf("Tagging %d sentences with %d tokens..", JCasUtil.select(jCas, Sentence.class).size(), JCasUtil.select(jCas, Token.class).size());
			SimplePipeline.runPipeline(jCas, naiveTaggerEngine);
			XmiCasSerializer.serialize(jCas.getCas(), new FileOutputStream(new File("/home/s3676959/Documents/BioFID/temp.xmi")));
//			executorService.submit(new XmiCasSerializerRunnable(jCas, new File("/home/s3676959/Documents/BioFID/temp.xmi")));

//			System.out.println(JCasUtil.select(jCas, Token.class).stream().map(Annotation::getCoveredText).collect(Collectors.joining(" ")));
			System.out.println(JCasUtil.select(jCas, Taxon.class).stream().map(taxon -> String.format("%s@(%d, %d)", taxon.getCoveredText(), taxon.getBegin(), taxon.getEnd())).collect(Collectors.joining(", ")));
		}
		executorService.shutdown();
	}
	
}
