package org.hucompute.textimager.biofid;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.google.common.io.Files;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.commons.cli.*;
import org.apache.uima.UIMAException;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCopier;
import org.apache.uima.util.CasIOUtils;
import org.texttechnologylab.annotation.type.Taxon;
import org.xml.sax.SAXException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class EvaluateWithAnnotated {
	
	private static PrintWriter conllWriter = null;
	private static Path outPath;
	private static ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
	private static ArrayList<Future<?>> serializationTasks = new ArrayList<>();
	
	public static void main(String[] args) throws ResourceInitializationException, FileNotFoundException, ParseException, InterruptedException {
		ImmutableList<String> params = ImmutableList.copyOf(args);
		
		Options options = new Options();
		options.addOption("h", false, "Print this message.");
		options.addOption("i", true, "Path to the input XMIs.");
		options.addOption("t", true, "Path to the taxon file.");
		options.addOption("w", true, "If given, write the resulting XMIs to this path.");
		options.addOption("s", false, "Toggles strict IOB-evaluation. If set True, B- and I- will be included in scoring.");
		options.addOption("p", true, "Print the results as three column conll file to the given location.");
		
		// FIXME: Fix the wrong CLI version causing a NoSuchMethodError
//		DefaultParser defaultParser = new DefaultParser();
//		CommandLine commandLine = defaultParser.parse(options, args);

//		final AnalysisEngine naiveTaggerEngine = AnalysisEngineFactory.createEngine(AnalysisEngineFactory.createEngineDescription(NaiveStringbasedTaxonTagger.class,
//				NaiveStringbasedTaxonTagger.PARAM_SOURCE_LOCATION, commandLine.getOptionValue("t"),
//				NaiveStringbasedTaxonTagger.PARAM_USE_LOWERCASE, true));
		
		int index = params.indexOf("-h");
		if (index > -1) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java -cp $CP org.hucompute.textimager.biofid.EvaluateWithAnnotated",
					"Annotate and evaluate a set of given XMIs that already contain 'Taxon' annotations.",
					options,
					"",
					true);
			return;
		}
		
		index = params.indexOf("-t");
		final AnalysisEngine naiveTaggerEngine = AnalysisEngineFactory.createEngine(AnalysisEngineFactory.createEngineDescription(NaiveStringbasedTaxonTagger.class,
				NaiveStringbasedTaxonTagger.PARAM_SOURCE_LOCATION, params.get(index + 1),
				NaiveStringbasedTaxonTagger.PARAM_USE_LOWERCASE, true));
		
		boolean strict = params.indexOf("-s") > -1;
		AtomicInteger truePositivies = new AtomicInteger(0);
		AtomicInteger falsePositivies = new AtomicInteger(0);
		AtomicInteger trueNegatives = new AtomicInteger(0);
		AtomicInteger falseNegatives = new AtomicInteger(0);
		
		index = params.indexOf("-p");
		boolean print = index > -1;
		if (print) {
			conllWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(params.get(index + 1)), StandardCharsets.UTF_8));
		}
		
		index = params.indexOf("-w");
		boolean write = index > -1;
//		boolean write = commandLine.hasOption("w");
		if (write) {
//			outPath = Paths.get(commandLine.getOptionValue("w"));
			outPath = Paths.get(params.get(index + 1));
		}
		
		try {
			boolean lastLineWasEmpty = true;
			index = params.indexOf("-i");
			Iterable<File> files = Files.fileTraverser().breadthFirst(new File(params.get(index + 1)));
			for (File file : Streams.stream(files).filter(File::isFile).collect(Collectors.toList())) {
				try {
					JCas aJCas = JCasFactory.createJCas();
					CasIOUtils.load(java.nio.file.Files.newInputStream(file.toPath()), null, aJCas.getCas(), true);
					if (aJCas.getAnnotationIndex(Taxon.class).size() <= 0)
						continue;
					
					JCas bJCas = JCasFactory.createJCas();
					CasCopier.copyCas(aJCas.getCas(), bJCas.getCas(), true);
					bJCas.removeAllIncludingSubtypes(Taxon.type);
					
					SimplePipeline.runPipeline(bJCas, naiveTaggerEngine);
					
					// Indexing
					ArrayList<Sentence> aSentences = Lists.newArrayList(JCasUtil.select(aJCas, Sentence.class));
					ArrayList<Sentence> bSentences = Lists.newArrayList(JCasUtil.select(bJCas, Sentence.class));
					
					Map<Sentence, Collection<Taxon>> aSentenceCoveredTaxa = JCasUtil.indexCovered(aJCas, Sentence.class, Taxon.class);
					
					Map<Token, Collection<Taxon>> aTokenCoveringTaxa = JCasUtil.indexCovering(aJCas, Token.class, Taxon.class);
					Map<Token, Collection<Taxon>> bTokenCoveringTaxa = JCasUtil.indexCovering(bJCas, Token.class, Taxon.class);
					
					Map<Sentence, Collection<Token>> aSentenceCoveringToken = JCasUtil.indexCovered(aJCas, Sentence.class, Token.class);
					Map<Sentence, Collection<Token>> bSentenceCoveringToken = JCasUtil.indexCovered(bJCas, Sentence.class, Token.class);
					
					for (int i = 0; i < aSentences.size(); i++) {
						if (aSentenceCoveredTaxa.getOrDefault(aSentences.get(i), new ArrayList<>()).isEmpty()) {
							continue;
						}
						
						ArrayList<Token> aTokens = Lists.newArrayList(aSentenceCoveringToken.get(aSentences.get(i)));
						ArrayList<Token> bTokens = Lists.newArrayList(bSentenceCoveringToken.get(bSentences.get(i)));
						
						for (int j = 0; j < aTokens.size(); j++) {
							Token aToken = aTokens.get(j);
							Token bToken = bTokens.get(j);
							
							boolean a = aTokenCoveringTaxa.containsKey(aToken);
							boolean b = bTokenCoveringTaxa.containsKey(bToken);
							
							boolean aBegin = true;
							if (a) {
								aBegin = Lists.newArrayList(aTokenCoveringTaxa.get(aToken)).get(0).getBegin() == aToken.getBegin();
							}
							
							boolean bBegin = true;
							if (b) {
								bBegin = Lists.newArrayList(bTokenCoveringTaxa.get(bToken)).get(0).getBegin() == bToken.getBegin();
							}
							
							if (!strict || (aBegin && bBegin)) {
								if (a && b)
									truePositivies.incrementAndGet();
								if (a && !b)
									falseNegatives.incrementAndGet();
								if (!a && !b)
									trueNegatives.incrementAndGet();
								if (!a && b)
									falsePositivies.incrementAndGet();
							}
							
							if (print && (a || b)) {
								lastLineWasEmpty = false;
								conllWriter.println(String.format("%s\t%s\t%s", aToken.getCoveredText(), getIOB(aBegin, a ? "TAX" : "O"), getIOB(bBegin, b ? "TAX" : "O")));
							}
						}
						if (print && !lastLineWasEmpty)
							conllWriter.println();
						lastLineWasEmpty = true;
					}
					
					if (Objects.nonNull(outPath)) {
//						try (FileOutputStream fileOutputStream = new FileOutputStream(Paths.get(outPath.toString(), file.getName()).toFile())) {
//							XmiCasSerializer.serialize(bJCas.getCas(), fileOutputStream);
//						} catch (SAXException | IOException e) {
//							e.printStackTrace();
//						}
						serializationTasks.add(cachedThreadPool.submit(new XmiCasSerializerRunnable(bJCas, Paths.get(outPath.toString(), file.getName()).toFile())));
					}
				} catch (UIMAException | UIMARuntimeException | IOException e) {
					e.printStackTrace();
				}
				if (print && !lastLineWasEmpty)
					conllWriter.println();
				lastLineWasEmpty = true;
				
				float precision = truePositivies.get() / ((float) truePositivies.get() + falsePositivies.get());
				float recall = truePositivies.get() / ((float) truePositivies.get() + falseNegatives.get());
				float f1 = 2 * (precision * recall) / (precision + recall);
				int sum = trueNegatives.get() + truePositivies.get() + falseNegatives.get() + falsePositivies.get();
				System.out.printf("\rPrecision: %01.3f, Recall: %01.3f, F1: %01.3f,\ttotal: %d, tp: %d, fp: %d, tn: %d, fn: %d",
						precision, recall, f1, sum, truePositivies.get(), falsePositivies.get(), trueNegatives.get(), falseNegatives.get());
			}
		} finally {
			System.out.println("Waiting for serialization to finish..");
			cachedThreadPool.shutdown();
		}
		System.out.println("Done.");
	}
	
	private static String getIOB(boolean begin, String label) {
		return begin ? "B-" + label : "I-" + label;
	}
}
