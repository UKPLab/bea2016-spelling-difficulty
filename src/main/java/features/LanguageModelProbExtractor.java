/*******************************************************************************
 * Copyright 2016
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package features;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;

import de.tudarmstadt.ukp.dkpro.tc.api.exception.TextClassificationException;
import de.tudarmstadt.ukp.dkpro.tc.api.features.DocumentFeatureExtractor;
import de.tudarmstadt.ukp.dkpro.tc.api.features.Feature;
import de.tudarmstadt.ukp.dkpro.tc.api.features.FeatureExtractorResource_ImplBase;
import edu.berkeley.nlp.lm.NgramLanguageModel;
import edu.berkeley.nlp.lm.io.LmReaders;
import edu.berkeley.nlp.lm.io.MakeKneserNeyArpaFromText;
import edu.berkeley.nlp.lm.io.MakeLmBinaryFromArpa;

public class LanguageModelProbExtractor<W>
    extends FeatureExtractorResource_ImplBase
    implements DocumentFeatureExtractor
{
    public static final String LM_PROB = "LanguageModelProbability";

    public static final String PARAM_IGNORE_CASE = "IgnoreCase";
    @ConfigurationParameter(name = PARAM_IGNORE_CASE, mandatory = true, defaultValue = "true")
    protected boolean ignoreCase;
    NgramLanguageModel<W> lm;

    public static final String PARAM_PATH_TO_BINARYLM = "BinaryLMFile";
    @ConfigurationParameter(name = PARAM_PATH_TO_BINARYLM, mandatory = true)
    protected static String lmfile;

    @SuppressWarnings("unchecked")
    @Override
    public List<Feature> extract(JCas jcas)
        throws TextClassificationException
    {
        List<Feature> featList = new ArrayList<Feature>();

        double lmProb = 0.0;

	//add start and end symbols
        String word = "#" + jcas.getDocumentText() + "$";

	//lm score is calculated based on character trigrams 
        lmProb = this.lm.scoreSentence((List<W>) getNgrams(word, 3));

        featList.addAll(Arrays.asList(new Feature(LM_PROB, lmProb)));

        return featList;
    }

    @Override
    public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
        throws ResourceInitializationException
    {
        super.initialize(aSpecifier, aAdditionalParams);
        // bigram model based on basic Vocabulary
	//generated by Berkeleylm, Kneser-Ney smoothing, order 5
        this.lm = LmReaders.readLmBinary(lmfile);
        return true;
    }

    public static ArrayList<String> getNgrams(String word, int n)
    {
        ArrayList<String> ngrams = new ArrayList<String>();

        for (int i = 0; i < (word.length() - (n - 1)); i++) {
            String ngram = word.substring(i, i + n);
            ngrams.add(ngram);
        }
        return ngrams;
    }

    public static void createCharacterBasedLM(File inputFile, String outputDir)
        throws IOException
    {
        String name = inputFile.getName();
        String trigramFile = outputDir + "/" + name + ".3grm";
        String lmFile = outputDir + "/" + name + ".lm";
        String binaryFile = lmFile + ".binary";
        generateTrigramFormat(inputFile, trigramFile);
        generateBinaryLm(trigramFile, lmFile, binaryFile);

    }

    // generateTrigramFormat(new DkproContext().getWorkspace("corpora/BasicItalian/")
    // + "/basicItalian.txt", new DkproContext().getWorkspace("corpora/BasicItalian")
    // + "/basicItalian.3grm.txt");
    // generateTrigramFormat(new DkproContext().getWorkspace("corpora/BasicCzech/")
    // + "/basicCzech.txt", new DkproContext().getWorkspace("corpora/BasicCzech")
    // + "/basicCzech.3grm.txt");
    // generateTrigramFormat(new DkproContext().getWorkspace("corpora/BasicGerman/")
    // + "/basicGerman_lowercase.txt",
    // new DkproContext().getWorkspace("corpora/BasicGerman") + "/basicGerman.3grm.txt");
    // String path = new DkproContext().getWorkspace("corpora").getAbsolutePath() + "/";
    // // generateBinaryLm(path + "BasicItalian/basicItalian.3grm.txt", path
    // + "BasicItalian/basicItalian.3grm.lm", path
    // + "BasicItalian/basicItalian.3grm.binary");
    // generateBinaryLm(path + "BasicCzech/basicCzech.3grm.txt", path
    // + "BasicCzech/basicCzech.3grm.lm", path + "BasicCzech/basicCzech.3grm.binary");
    // generateBinaryLm(path + "BasicGerman/basicGerman.3grm.txt", path
    // + "BasicGerman/basicGerman.3grm.lm", path + "BasicGerman/basicGerman.3grm.binary");

    // check output
    // NgramLanguageModel<String> lm = LmReaders.readLmBinary(path
    // + "BasicItalian/basicItalian.3grm.binary");
    // System.out.println(lm.scoreSentence(Arrays
    // .asList("#so sop opr pra rat att ttu tut utt tto to$".trim().split(" "))));
    // System.out.println(lm.scoreSentence(Arrays.asList("#pi ian ano no$".trim().split(" "))));
    //
    // NgramLanguageModel<String> lm2 = LmReaders.readLmBinary(path
    // + "BasicGerman/basicGerman.3grm.binary");
    // System.out.println(lm2.scoreSentence(Arrays
    // .asList("#vi vie iel ell lle lei eic ich cht ht$".trim().split(" "))));
    // System.out.println(lm2.scoreSentence(Arrays.asList("#ge ena nau au$".trim().split(" "))));

     private static void generateTrigramFormat(File inputFile, String trigramFile)
     throws IOException
     {

        BufferedReader br = new BufferedReader(new FileReader(inputFile));
        BufferedWriter bw = new BufferedWriter(new FileWriter(trigramFile));

        String line = "";
        while ((line = br.readLine()) != null) {
            line = line.toLowerCase();
            line = "#" + line + "$";
            ArrayList<String> ngrams = getNgrams(line, 3);

            for (String ngram : ngrams) {
                bw.write(ngram + " ");
            }
            bw.write("\n");
        }
        br.close();
        bw.close();

    }

    private static void generateBinaryLm(String vocfile, String lmfile, String binaryfile)
    {
        MakeKneserNeyArpaFromText.main(new String[] { "5", lmfile, vocfile });
        MakeLmBinaryFromArpa.main(new String[] { lmfile, binaryfile });
    }

}
