package BioFID;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.google.common.io.Files;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.CasIOUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.texttechnologielab.annotation.type.Fingerprint;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.uima.fit.util.JCasUtil.select;

/**
 * @author Manuel Stoeckel
 * Created on 11.03.19
 */
public class TransferAnnotations extends AbstractRunner {
	private static String sFrom;
	private static String sTo;
	private static InputType inputType;
	
	enum InputType {
		PATH, REGEX, LIST
	}
	
	public static void main(String[] args) {
		try {
			getParams(args);
			
			int index;
			index = Integer.max(params.indexOf("-f"), params.indexOf("--from"));
			if (index > -1) {
				sFrom = params.get(index + 1);
			} else {
				throw new MissingArgumentException("Missing --from!\n");
			}
			
			index = Integer.max(params.indexOf("-t"), params.indexOf("--to"));
			if (index > -1) {
				sTo = params.get(index + 1);
			} else {
				throw new MissingArgumentException("Missing --to\n");
			}
			
			index = Integer.max(params.indexOf("-i"), params.indexOf("--input-type"));
			if (index > -1) {
				switch (params.get(index + 1)) {
					case "r":
					case "regex":
					case "R":
					case "REGEX":
						inputType = InputType.REGEX;
						break;
					case "l":
					case "list":
					case "L":
					case "LIST":
						inputType = InputType.LIST;
						break;
					case "p":
					case "path":
					case "P":
					case "PATH":
						inputType = InputType.PATH;
						break;
					default:
						throw new IllegalArgumentException(String.format("'%s' is not a valid InputType! Valid types are: PATH, REGEX, LIST.", params.get(index + 1)));
				}
			} else {
				throw new MissingArgumentException("Missing --input-type\n");
			}
			
			ArrayList<File> fromList = new ArrayList<>();
			ArrayList<File> toList = new ArrayList<>();
			
			// Get unordered lists
			getFileLists(fromList, toList);
			
			HashSet<ImmutablePair<File, File>> pairs = mapFilesByName(fromList, toList);
			
			alignAnnotations(pairs);

//			System.out.printf("Aligned %d perfect matches!", count);
		} catch (IllegalArgumentException | MissingArgumentException | IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void alignAnnotations(HashSet<ImmutablePair<File, File>> pairs) {
		for (ImmutablePair<File, File> pair : pairs) {
			File fromFile = pair.getLeft();
			File toFile = pair.getRight();
			
			try {
				JCas fromCas = JCasFactory.createJCas();
				CasIOUtil.readXmi(fromCas, fromFile);
				
				JCas toCas = JCasFactory.createJCas();
				CasIOUtil.readXmi(toCas, toFile);
				if (Objects.isNull(fromCas.getDocumentText()) || Objects.isNull(toCas.getDocumentText()))
					continue;
				
				HashSet<TOP> fingerprinted = select(fromCas, Fingerprint.class).stream().map(Fingerprint::getReference).collect(Collectors.toCollection(HashSet::new));
				List<NamedEntity> namedEntities = select(fromCas, NamedEntity.class).stream().filter(fingerprinted::contains).collect(Collectors.toList());
				
				int count = 0;
				int allCount = 0;
				
				if (fromCas.getDocumentText().equals(toCas.getDocumentText())) {
					for (NamedEntity ne : namedEntities) {
						toCas.addFsToIndexes(ne);
					}
				} else {
					int lastTargetOffset = 0;
					int lastSourceOffset = 0;
					for (NamedEntity neSource : namedEntities) {
						int index = toCas.getDocumentText().indexOf(neSource.getCoveredText(), lastTargetOffset);
						if (index > -1) {
							count++;
							NamedEntity neTarget = new NamedEntity(toCas, index, index + neSource.getCoveredText().length());
							neTarget.setValue(neSource.getValue());
							neTarget.setIdentifier(neSource.getIdentifier());
							toCas.addFsToIndexes(neTarget);
							if (lastSourceOffset < neSource.getBegin() && lastTargetOffset == neTarget.getBegin()) {
								lastTargetOffset = neTarget.getBegin() + 1; // FIXME!!
							} else {
								lastTargetOffset = neTarget.getBegin();
							}
							lastSourceOffset = neSource.getBegin();
						}
						allCount++;
					}
				}
				System.out.printf("Found %d/%d NEs in '%s'.\n", count, allCount, fromFile.getName());
			} catch (UIMAException | IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void getFileLists(ArrayList<File> fromList, ArrayList<File> toList) throws IOException {
		if (inputType == InputType.REGEX) {
			throw new NotImplementedException("InputType.REGEX is not implemented yet.");
		} else if (inputType == InputType.PATH) {
			File fromNode = Paths.get(sFrom).toAbsolutePath().toFile();
			File toNode = Paths.get(sTo).toAbsolutePath().toFile();
			
			if (fromNode.isFile())
				throw new IllegalArgumentException(String.format("'%s' is not a valid directory path!", sFrom));
			if (toNode.isFile())
				throw new IllegalArgumentException(String.format("'%s' is not a valid directory path!", sTo));
			
			fromList.addAll(Streams.stream(Files.fileTraverser().breadthFirst(fromNode)).filter(File::isFile).collect(Collectors.toList()));
			toList.addAll(Streams.stream(Files.fileTraverser().breadthFirst(toNode)).filter(File::isFile).collect(Collectors.toList()));
		} else { // InputType.LIST
			File fromNode = new File(sFrom);
			File toNode = new File(sTo);
			
			if (!fromNode.exists() || !fromNode.isFile())
				throw new IllegalArgumentException(String.format("'%s' is not a valid file path!", sFrom));
			if (!toNode.exists() || !toNode.isFile())
				throw new IllegalArgumentException(String.format("'%s' is not a valid file path!", sTo));
			
			fromList.addAll(Files.readLines(fromNode, StandardCharsets.UTF_8).stream().map(File::new).collect(Collectors.toCollection(ArrayList::new)));
			toList.addAll(Files.readLines(toNode, StandardCharsets.UTF_8).stream().map(File::new).collect(Collectors.toCollection(ArrayList::new)));
		}
	}
	
	private static HashSet<ImmutablePair<File, File>> mapFilesByName(ArrayList<File> fromList, ArrayList<File> toList) {
		Map<String, File> map = toList.stream().collect(Collectors.toMap(File::getName, f -> f));
		ArrayList<String> missed = new ArrayList<>();
		
		HashSet<ImmutablePair<File, File>> pairs = new HashSet<>();
		
		for (File from : fromList) {
			if (map.containsKey(from.getName())) {
				pairs.add(ImmutablePair.of(from, map.get(from.getName())));
			} else {
				missed.add(from.getPath());
			}
		}
		
		System.out.printf("Matched %d pairs:\n", pairs.size());
		pairs.forEach(System.out::println);
		System.out.flush();
		
		System.err.printf("Missed %d pairs:\n", missed.size());
		System.err.println(missed);
		
		return pairs;
	}
}
