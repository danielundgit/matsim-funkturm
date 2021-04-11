package org.matsim.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class TimeDistanceComparison {

    private static BufferedWriter bufferedWriter;
    Config baseConfig; Scenario baseScenario;
    Config compareConfig; Scenario compareScenario;
    String outputPath = "comparisons/";
    String file = "TimeDistanceDifferences.txt";
    List<Plan> originalPlans = null;
    List<Plan> modifiedPlans = null;

    public TimeDistanceComparison(Config base, Config compare){
        baseConfig = base; compareConfig = compare;
        baseScenario = ScenarioUtils.loadScenario(baseConfig);
        compareScenario = ScenarioUtils.loadScenario(compareConfig);
        outputPath = outputPath+file;
    }

    public TimeDistanceComparison(Config cBase, Config cCompare, Scenario sBase, Scenario sCompare, String fileName){
        baseConfig = cBase; compareConfig = cCompare;
        baseScenario = sBase; compareScenario = sCompare;
        outputPath = outputPath+fileName;
    }

    public TimeDistanceComparison(Config cBase, Config cCompare, Scenario sBase, Scenario sCompare, String fileName, List<Plan> pBase, List<Plan> pCompare){
        baseConfig = cBase; compareConfig = cCompare;
        baseScenario = sBase; compareScenario = sCompare;
        outputPath = outputPath+fileName;
        originalPlans = pBase; modifiedPlans = pCompare;
    }


    public void prepare() {

        Double[] originalTime = calculateTime(originalPlans);
        Double[] modifiedTime = calculateTime(modifiedPlans);

        Double[] originalDistance = calculateDistance(originalPlans);
        Double[] modifiedDistance = calculateDistance(modifiedPlans);

        double timeDifference = modifiedTime[0] - originalTime[0];
        double distanceDifference = modifiedDistance[0] - originalDistance[0];
        double meanTimeDifference = modifiedTime[1] - originalTime[1];
        double meanDistanceDifference = modifiedDistance[1] - originalDistance[1];

        System.out.println("OriginalTime: " + originalTime[0]/3600 + "\n ModifiedTime: " + modifiedTime[0]/3600
                + "\n OriginalDistance: " + originalDistance[0]/1000 + "\n ModifiedDistance: " + modifiedDistance[0]/1000);


        try {
            FileWriter fileWriter = new FileWriter(outputPath);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write( "OriginalTime in hours: " + originalTime[0]/3600 + "\n ModifiedTime in hours: " + modifiedTime[0]/3600
                    + "\n OriginalDistance in kilometers: " + originalDistance[0]/1000 + "\n ModifiedDistance in kilometers: " + modifiedDistance[0]/1000
                    + "\n Time difference in hours: " + timeDifference /3600 + "\n Distance difference in kilometers: " + distanceDifference/1000 + "\n");
            bufferedWriter.write( "OriginalMeanTime in hours: " + originalTime[1]/3600 + "\n ModifiedMeanTime in hours: " + modifiedTime[1]/3600
                    + "\n OriginalMeanDistance in kilometers: " + originalDistance[1]/1000 + "\n ModifiedMeanDistance in kilometers: " + modifiedDistance[1]/1000
                    + "\n mean Time difference in hours: " + meanTimeDifference /3600 + "\n mean Distance difference in kilometers: " + meanDistanceDifference/1000 + "\n");
            bufferedWriter.close();
        } catch(IOException ee){
            throw new RuntimeException(ee);
        }
    }

    //method calculates how much travel time agents aggregate [0] and the mean time per plan [1]
    //The method uses as input a list of plans
    private static Double[] calculateTime(List<Plan> Plans) {
        Double[] times = new Double[2];
        double time = 0.;
        double ctr = 0.;

        for (Plan plan : Plans){
            List<PlanElement> planElements = plan.getPlanElements();
            for (PlanElement planElement: planElements) {
                if(planElement instanceof Leg){
                    time += (((Leg)planElement).getRoute()).getTravelTime().seconds();
                    ctr++;
                }
            }
        }
        times[0]=time; times[1]=time/ctr;
        return times;
    }

    //method calculates how much travel distance agents aggregate [0] and the mean distance per plan [1]
    //The method uses as input a list of plans
    private static Double[] calculateDistance(List<Plan> Plans) {
        Double[] distances = new Double[2];
        double distance = 0.;
        double ctr = 0.;

        for (Plan plan : Plans){
            List<PlanElement> planElements = plan.getPlanElements();
            for (PlanElement planElement: planElements) {
                if(planElement instanceof Leg){
                    distance+= (((Leg)planElement).getRoute()).getDistance();
                    ctr++;
                }
            }
        }
        distances[0]=distance; distances[1]=distance/ctr;
        return distances;
    }

}
