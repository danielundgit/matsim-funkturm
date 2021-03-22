package org.matsim.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.TreeMap;

public class ScoreComparison {

    private static BufferedWriter bufferedWriter;
    Config baseConfig; Scenario baseScenario;
    Config compareConfig; Scenario compareScenario;
    String outputPath = "comparisons/";
    String file = "ScoreDistanceDifferences.txt";
    List<Plan> originalPlans = null;
    List<Plan> modifiedPlans = null;
    TreeMap<String,Double[]> scoreTree = null;

    public ScoreComparison(Config base, Config compare){
        baseConfig = base; compareConfig = compare;
        baseScenario = ScenarioUtils.loadScenario(baseConfig);
        compareScenario = ScenarioUtils.loadScenario(compareConfig);
        outputPath = outputPath+file;
    }

    public ScoreComparison(Config cBase, Config cCompare, Scenario sBase, Scenario sCompare, String fileName){
        baseConfig = cBase; compareConfig = cCompare;
        baseScenario = sBase; compareScenario = sCompare;
        outputPath = outputPath+fileName;
    }

    public ScoreComparison(Config cBase, Config cCompare, Scenario sBase, Scenario sCompare, String fileName, List<Plan> pBase, List<Plan> pCompare){
        baseConfig = cBase; compareConfig = cCompare;
        baseScenario = sBase; compareScenario = sCompare;
        outputPath = outputPath+fileName;
        originalPlans = pBase; modifiedPlans = pCompare;
    }

    public void compare() {

        Double[] originalScore = calculateScore(originalPlans);
        Double[] modifiedScore = calculateScore(modifiedPlans);
        scoreTree.put("baseScore", originalScore);
        scoreTree.put("compScore", originalScore);
        
        Double ScoreDifference = modifiedScore[0] - originalScore[0];
        Double meanScoreDifference = modifiedScore[1] - originalScore[1];

        System.out.println("OriginalScore: " + originalScore[0] + "\n ModifiedScore: " + modifiedScore[0]);
        System.out.println("OriginalMeanScore p.p.: " + originalScore[1] + "\n ModifiedMeanScore p.p.: " + modifiedScore[1]);

        try {
            FileWriter fileWriter = new FileWriter(outputPath);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write( "OriginalScore in [utility]: " + originalScore[0] + "\n ModifiedScore in [utility]: " + modifiedScore[0]
                    + "\n Score difference in [utility]: " + ScoreDifference +  "\n");
            bufferedWriter.write( "OriginalMeanScore per person in [utility]: " + originalScore[1] + "\n ModifiedMeanScore per person in [utility]: " + modifiedScore[1]
                    + "\n mean Score difference in [utility]: " + meanScoreDifference + "\n");
            bufferedWriter.close();
        } catch(IOException ee){
            throw new RuntimeException(ee);
        }
    }

    public TreeMap<String,Double[]> getScoreComparison(){
        return scoreTree;
    }

    //method calculates how much travel Score agents aggregate [0] and the mean Score per plan [1]
    //The method uses as input a list of selected plans
    private static Double[] calculateScore(List<Plan> Plans) {
        Double[] scores = new Double[2];
        Double score = 0.;
        double ctr = 0.;

        for (Plan plan : Plans){
            score += plan.getScore();
            ctr++;
        }
        scores[0]= score; scores[1]= score /ctr;
        return scores;
    }

}
