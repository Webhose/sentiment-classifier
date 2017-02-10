package com.webhose.reviewSentiment;

import com.google.common.io.Resources;
import edu.stanford.nlp.classify.Classifier;
import edu.stanford.nlp.classify.ColumnDataClassifier;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.objectbank.ObjectBank;

import java.io.IOException;
import java.text.NumberFormat;


public class App
{
    private static ColumnDataClassifier cdc;

    public static void main( String[] args ) throws IOException {
        // Constructing the ColumnDataClassifier Object with the properties file
        cdc = new ColumnDataClassifier(Resources.getResource("review-sentiment.prop").getPath());

        // Declare and Construct the General Classifier with the general train file
        Classifier<String,String> generalCl = cdc.makeClassifier(cdc.readTrainingExamples(Resources.getResource("general.train").getPath()));

        // Declare and Construct the Domain-Specific Classifier with the general train file
        Classifier<String,String> hotelsCl = cdc.makeClassifier(cdc.readTrainingExamples(Resources.getResource("booking.train").getPath()));

        // General Classifier self test (using the 20% data-set from various sources)
        System.out.println("General Classifier stats:");
        System.out.println(setScore("general.test", generalCl));
        System.out.println();

        // Domain-Specific Classifier self test (using the 20% data-set from booking.com)
        System.out.println("Domain-Specific Classifier stats:");
        System.out.println(setScore("booking.test", hotelsCl));
        System.out.println();

        // Compare both of the classifiers with the estranged data-set (using the data from expedia.com)
        System.out.println("Comparison Results:");

        System.out.println("General Classifier score:");
        System.out.println(setScore("expedia.test", generalCl));
        System.out.println();

        System.out.println("Domain-Specific Classifier score:");
        System.out.println(setScore("expedia.test", hotelsCl));
        System.out.println();

    }

    private static String setScore(String testFileName, Classifier<String,String> cl) {
        String results = "";

        // Calculate the score of 'positive' class
        int tp = 0;
        int fn = 0;
        int fp = 0;

        for (String line : ObjectBank.getLineIterator(Resources.getResource(testFileName).getPath(), "utf-8")) {
            try {
                Datum<String, String> d = cdc.makeDatumFromLine(line);
                String sentiment = getSentimentFromText(line.replace(d.label()+"\t", ""), cl);
                // true-positive
                if (d.label().equals("positive") && sentiment.equals("positive")) {
                    tp++;
                }
                // false-positive
                else if (d.label().equals("positive") && sentiment.equals("negative")) {
                    fp++;
                }
                // false-negative
                else if (d.label().equals("negative") && sentiment.equals("positive")) {
                    fn++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        NumberFormat percentFormatter = NumberFormat.getPercentInstance();
        percentFormatter.setMinimumFractionDigits(1);
        double precision = (double)tp/(double)(tp+fp);
        double recall = (double)tp/(double)(tp+fn);
        results += "\nPositive Results:\n";
        results += "Precision: " + percentFormatter.format(precision) + "\n";
        results += "Recall: " + percentFormatter.format(recall) + "\n";
        results += "F1: " + (2*precision*recall)/(precision+recall) + "\n";

        // Calculate the score of 'negative' class
        tp = 0;
        fn = 0;
        fp = 0;
        for (String line : ObjectBank.getLineIterator(Resources.getResource(testFileName).getPath(), "utf-8")) {
            try {
                Datum<String, String> d = cdc.makeDatumFromLine(line);
                String sentiment = getSentimentFromText(line.replace(d.label()+"\t", ""), cl);
                // true-positive
                if (d.label().equals("negative") && sentiment.equals("negative")) {
                    tp++;
                }
                // false-positive
                else if (d.label().equals("negative") && sentiment.equals("positive")) {
                    fp++;
                }
                // false-negative
                else if (d.label().equals("positive") && sentiment.equals("negative")) {
                    fn++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        percentFormatter.setMinimumFractionDigits(1);
        precision = (double)tp/(double)(tp+fp);
        recall = (double)tp/(double)(tp+fn);
        results += "\nNegative Results:\n";
        results += "Precision: " + percentFormatter.format(precision) + "\n";
        results += "Recall: " + percentFormatter.format(recall) + "\n";
        results += "F1: " + (2*precision*recall)/(precision+recall) + "\n";

        return results;
    }

    private static String getSentimentFromText(String text, Classifier<String,String> cl)  throws Exception {
        Datum<String, String> d = cdc.makeDatumFromLine("\t" + text);
        return cl.classOf(d);
    }
}
