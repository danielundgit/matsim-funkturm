package org.matsim.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TimeDistanceComparison {

    private static BufferedWriter bufferedWriter;
    Config baseConfig; Scenario baseScenario;
    Config compareConfig; Scenario compareScenario;
    String outputPath = "TimeDistanceDifferences.txt";

    public TimeDistanceComparison(Config base, Config compare){
        baseConfig = base; compareConfig = compare;
        baseScenario = ScenarioUtils.loadScenario(baseConfig);
        compareScenario = ScenarioUtils.loadScenario(compareConfig);
    }

    public TimeDistanceComparison(Config cBase, Config cCompare, Scenario sBase, Scenario sCompare){
        baseConfig = cBase; compareConfig = cCompare;
        baseScenario = sBase; compareScenario = sCompare;
    }


    public void prepare() {

        List<Plan> originalPlans = getSelectedPlans(baseScenario);
        List<Plan> modifiedPlans = getSelectedPlans(compareScenario);

        Double[] originalTime = calculateTIme(originalPlans);
        Double[] modifiedTime = calculateTIme(modifiedPlans);

        Double[] originalDistance = calculateDistance(originalPlans);
        Double[] modifiedDistance = calculateDistance(modifiedPlans);

        Double timeDifference = modifiedTime[0] - originalTime[0];
        Double distanceDifference = modifiedDistance[0] - originalDistance[0];
        Double meanTimeDifference = modifiedTime[1] - originalTime[1];
        Double meanDistanceDifference = modifiedDistance[1] - originalDistance[1];

        System.out.println("OriginalTime: " + originalTime[0]/3600 + "\n ModifiedTime: " + modifiedTime[0]/3600
                + "\n OriginalDistance: " + originalDistance[0]/1000 + "\n ModifiedDistance: " + modifiedDistance[0]/1000);


        try {
            FileWriter fileWriter = new FileWriter(outputPath);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write( "OriginalTime in hours: " + originalTime[0]/3600 + "\n ModifiedTime in hours: " + modifiedTime[0]/3600
                    + "\n OriginalDistance in kilometers: " + originalDistance[0]/1000 + "\n ModifiedDistance in kilometers: " + modifiedDistance[0]/1000
                    + "\n Time difference in hours: " + timeDifference /3600 + "\n Distance differnce in kilometers: " + distanceDifference/1000 + "\n");
            bufferedWriter.write( "OriginalMeanTime in hours: " + originalTime[1]/3600 + "\n ModifiedMeanTime in hours: " + modifiedTime[1]/3600
                    + "\n OriginalMeanDistance in kilometers: " + originalDistance[1]/1000 + "\n ModifiedMeanDistance in kilometers: " + modifiedDistance[1]/1000
                    + "\n mean Time difference in hours: " + meanTimeDifference /3600 + "\n mean Distance difference in kilometers: " + meanDistanceDifference/1000 + "\n");
            bufferedWriter.close();
        } catch(IOException ee){
            throw new RuntimeException(ee);
        }
    }
    //Method returns a list of selected plans that are executed
    private static List<Plan> getSelectedPlans(Scenario scenario){
        List<Plan> selectedPlans = new ArrayList<>();
        Population population = scenario.getPopulation();

        for (Person p:population.getPersons().values()){
            selectedPlans.add(p.getSelectedPlan());
        }

        return selectedPlans;
    }

    //method calculates how much travel time agents aggregate [0] and the mean time per plan [1]
    //The method uses as input a list of plans
    private static Double[] calculateTIme(List<Plan> Plans) {
        Double[] times = new Double[2];
        Double time = 0.;
        double ctr = 0.;

        for (Plan plan : Plans){
            List<PlanElement> planElements = plan.getPlanElements();
            for (PlanElement planElement: planElements) {
                if(planElement instanceof Leg){
                    time += (((Leg)planElement).getRoute()).getTravelTime();
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
        Double distance = 0.;
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
