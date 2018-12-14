import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.Anomaly;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.util.JCasUtil.*;

public class BioFIDOCRPageParserTest {

    boolean printAnnotations = false;

    @Test
    public void testTokenizationTable() throws UIMAException {
        // Input
        String xml = "";
        try (BufferedReader br = new BufferedReader(new FileReader(new File("src/test/resources/9088917/9088369/9031004/0046_9028573.xml")))) {
            xml = br.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.printf("Text length: %d\n", xml.length());

        // Create a new Engine Description.
        testTokenization(xml);
    }

    @Test
    public void testTokenizationText() throws UIMAException {
        // Input
        String xml = "";
        try (BufferedReader br = new BufferedReader(new FileReader(new File("src/test/resources/9088917/9088369/9031004/0047_9028574.xml")))) {
            xml = br.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.printf("Text length: %d\n", xml.length());
        testTokenization(xml);

    }

    private void testTokenization(String xml) throws UIMAException {
        // Create a new Engine Description.
        AnalysisEngineDescription pageParser = createEngineDescription(BioFIDOCRPageParser.class, BioFIDOCRPageParser.INPUT_XML, xml, BioFIDOCRPageParser.PARAM_MIN_TOKEN_CONFIDENCE, 90);

        // Create a new JCas - "Holder"-Class for Annotation.
        JCas inputCas = JCasFactory.createJCas();

        // Pipeline
lo        SimplePipeline.runPipeline(inputCas, pageParser);

        System.out.println();
        System.out.flush();
        for (Token token : select(inputCas, Token.class)) {
            List<Anomaly> anomalies = selectCovered(inputCas, Anomaly.class, token.getBegin(), token.getEnd());
            System.out.printf("%s", token.getText());
            if (!anomalies.isEmpty()) {
                System.out.printf("\tAnomaly<%s>", anomalies.stream().map(Anomaly::getDescription).collect(Collectors.joining(";")));
            } else if (printAnnotations) {
                List<NamedEntity> nes = selectCovered(inputCas, NamedEntity.class, token.getBegin(), token.getEnd());
                if (!nes.isEmpty()) {
                    System.out.printf("\tAnnotation<%s>", nes.stream().map(NamedEntity::getValue).collect(Collectors.joining(";")));
                }
            }
            System.out.println();
            System.out.flush();
        }
    }
}
