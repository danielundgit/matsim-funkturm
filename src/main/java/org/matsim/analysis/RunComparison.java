package org.matsim.analysis;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.run.RunBerlinScenario;

import java.io.*;
import java.util.*;

public class RunComparison {
//    final String pathLOR = "scenarios/berlin-v5.4-1pct/input/LOR.shp";
    String baseDefault = "base";
    String compareDefault = "base";
    String defaultPre = "funkturm_";
    final String configFile = "/output/berlin-v5.4-"+ RunBerlinScenario.PCT+"pct.output_config.xml";
    Config baseConfig; Scenario baseScenario;
    Config compareConfig; Scenario compareScenario;
    Map<Integer, Geometry> shapes = null;
    private static BufferedWriter bufferedWriter;
    private static BufferedReader bufferedReader;
    TreeMap<String,Double[]> scoreTree = null;
    TreeMap<String,Double[]> timeTree = null;
    TreeMap<String,Double[]> distanceTree = null;
    String pathRoot = "comparisons/"+baseDefault+"VS"+compareDefault+"_";
    String fileRoot = "/"+baseDefault+"VS"+compareDefault+"_";

    public RunComparison(String base, String compare, Map<Integer, Geometry> shapeMap){
        baseDefault = base;
        compareDefault = compare;

        pathRoot = "comparisons/"+RunBerlinScenario.PRE+RunBerlinScenario.PCT+RunBerlinScenario.INDEX;
        File dir = new File(pathRoot);
        if (!dir.exists()){
            dir.mkdirs();
        }
        shapes = shapeMap;
    }

    public RunComparison(String actualPre, String base, String compare, Map<Integer, Geometry> shapeMap){
        defaultPre = actualPre;
        baseDefault = base;
        baseConfig = ConfigUtils.loadConfig(actualPre + base + configFile);
        baseConfig.network().setInputFile("berlin-v5.4-"+ RunBerlinScenario.PCT+"pct.output_network.xml.gz");
        baseConfig.plans().setInputFile("berlin-v5.4-"+ RunBerlinScenario.PCT+"pct."+RunBerlinScenario.ITER+"plans.xml.gz");
        baseScenario = ScenarioUtils.loadScenario(baseConfig);

        compareDefault = compare;
        compareConfig = ConfigUtils.loadConfig(actualPre+ compare + configFile);
        compareConfig.network().setInputFile("berlin-v5.4-"+ RunBerlinScenario.PCT+"pct.output_network.xml.gz");
        compareConfig.plans().setInputFile("berlin-v5.4-"+ RunBerlinScenario.PCT+"pct."+RunBerlinScenario.ITER+"plans.xml.gz");
        compareScenario = ScenarioUtils.loadScenario(compareConfig);

        shapes = shapeMap;
    }

    public RunComparison(String actualPre, String base, String compare){
        defaultPre = actualPre;
        baseDefault = base;
        compareDefault = compare;
        baseConfig = ConfigUtils.loadConfig(actualPre+ base + configFile);
        baseConfig.network().setInputFile("berlin-v5.4-"+ RunBerlinScenario.PCT+"pct.output_network.xml.gz");
        baseConfig.plans().setInputFile("berlin-v5.4-"+ RunBerlinScenario.PCT+"pct."+RunBerlinScenario.ITER+"plans.xml.gz");
        compareConfig = ConfigUtils.loadConfig(actualPre+ compare + configFile);
        compareConfig.network().setInputFile("berlin-v5.4-"+ RunBerlinScenario.PCT+"pct.output_network.xml.gz");
        compareConfig.plans().setInputFile("berlin-v5.4-"+ RunBerlinScenario.PCT+"pct."+RunBerlinScenario.ITER+"plans.xml.gz");
        baseScenario = ScenarioUtils.loadScenario(baseConfig);
        compareScenario = ScenarioUtils.loadScenario(compareConfig);
    }

    public RunComparison(){
        System.out.println("This comparison 'base vs. base' might not be what you are looking for!");
        baseConfig = ConfigUtils.loadConfig(defaultPre+ baseDefault + configFile);
        baseScenario = ScenarioUtils.loadScenario(baseConfig);
        compareConfig = baseConfig; compareScenario = baseScenario;
    }

    public void runAllComparisons(boolean onlyADFresidents) {

        fileRoot = "/"+baseDefault+"VS"+compareDefault+"_";

        System.out.println("############\n############\nUse all comparison modes "+baseDefault+" vs. "+compareDefault+
                ". Existing files will be overwritten!");

        System.out.println("Processing plans from case "+baseDefault.toUpperCase());
        List<Plan> originalPlans = getSelectedPlans(baseDefault, onlyADFresidents);
        System.out.println("Processing plans from case "+compareDefault.toUpperCase());
        List<Plan> modifiedPlans = getSelectedPlans(compareDefault, onlyADFresidents);

        System.out.println("["+baseDefault.toUpperCase()+"]\t"+originalPlans.size()+" relevant legs found!");
        System.out.println("["+compareDefault.toUpperCase()+"]\t"+originalPlans.size()+" relevant legs found!");

        if(originalPlans.size()!=0 && modifiedPlans.size()!= 0) {
            traveltimes(originalPlans, modifiedPlans, pathRoot+ fileRoot + "times.txt");
            traveldistances(originalPlans, modifiedPlans, pathRoot+ fileRoot + "distances.txt");
            scores(originalPlans, modifiedPlans, pathRoot+ fileRoot + "scores.txt");
        }
        else{
            System.out.println("ERROR! Comparison not possible, because one case has no plans in defined area!");
        }

    }


    //Method returns a list of selected plans that are executed
    public List<Plan> getSelectedPlans(String mode, boolean onlyADFresidents){

        List<Plan> selectedPlans = new ArrayList<>();
        Scenario scenario = RunBerlinScenario.scenarios.get(mode);
        Population population = RunBerlinScenario.populations.get(mode);
        Map<Id<Link>, ? extends Link> links = scenario.getNetwork().getLinks();

        if(shapes!=null) {

            if(onlyADFresidents) {
                System.out.println("Take plans from persons with home activity inside defined area");
                if(!fileRoot.contains("onlyADFler")) {
                    fileRoot = "/onlyADFler_"+fileRoot.substring(1);
                }
            }
            else { System.out.println("Take plans from \"all\" persons who passed through the defined area at some point, including start/end-legs");}
            System.out.println("This usually takes some time ... ");
            int ctr=0;

            for (Person pp : RunBerlinScenario.persons.get(mode)) {
                if(onlyADFresidents) {
                    Activity homeActivity = PopulationUtils.getFirstActivity(pp.getSelectedPlan());
                    for (int zone : shapes.keySet()) {
                        if (shapes.get(zone).contains(MGC.coord2Point(homeActivity.getCoord()))) {
                            selectedPlans.add(pp.getSelectedPlan());
                            break;
                        }
                    }
                }
                else {
                    for (Leg leg : PopulationUtils.getLegs(pp.getSelectedPlan())) {
                        boolean passedZone = false;
                        if (leg.getMode().contains("car") || leg.getMode().contains("freight") || leg.getMode().contains("ride")) {
                            if (leg.getRoute() != null) {
                                List<String> routeElements = new ArrayList<>();
                                routeElements.add(leg.getMode());
                                routeElements.addAll(Arrays.asList(leg.getRoute().getRouteDescription().split(" ").clone()));
                                for(String re : routeElements){
                                    Link link = links.get(Id.createLinkId(re));
                                    if (link != null) {
                                        for (Geometry zone: shapes.values()) {
                                            if (zone.contains(MGC.coord2Point(link.getCoord()))) {
                                                selectedPlans.add(pp.getSelectedPlan());
                                                passedZone =true;
                                                break;
                                            }
                                        }
                                        if (passedZone) {
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if(++ctr%1111==0) {System.out.printf("%d/%d plans processed!%n", ctr, population.getPersons().size());}
            }
        }
        else {
            System.out.println("There is no defined area. Output will be all selected plans!");
            for (Person p : RunBerlinScenario.persons.get(mode)) {
                selectedPlans.add(p.getSelectedPlan());
            }
        }
        return selectedPlans;
    }

    public void traveltimes(List<Plan> pBase, List<Plan> pCompare, String outputPath) {

        String bd = baseDefault.toUpperCase();
        String cd = compareDefault.toUpperCase();

        Double[] originalTime = calculateTime(pBase);
        Double[] modifiedTime = calculateTime(pCompare);
        timeTree = new TreeMap<>();
        timeTree.put("baseTTime", originalTime);
        timeTree.put("compTTime", modifiedTime);

        double timeDifference = modifiedTime[0] - originalTime[0];
        double meanTimeDifference = modifiedTime[1] - originalTime[1];

        System.out.println("OriginalTime: " + originalTime[0]/3600 + " [h]\n ModifiedTime: " + modifiedTime[0]/3600 + " [h]");

        try {
            FileWriter fileWriter = new FileWriter(outputPath);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write( "## Time "+ bd +" in hours: " + originalTime[0]/3600 + "\n## Time "+ cd +" in hours: " + modifiedTime[0]/3600
                    + "\n## Time difference ("+ bd + "-"+ cd +") in hours: " + timeDifference /3600 + "\n");
            bufferedWriter.write( "##Mean time"+ bd +" in hours: " + originalTime[1]/3600 + "\n## Mean time "+ cd +"in hours: " + modifiedTime[1]/3600
                    + "\n## Mean Time ("+ bd + "-"+ cd +") difference in hours: " + meanTimeDifference /3600 + "\n");
            bufferedWriter.close();
        } catch(IOException ee){
            throw new RuntimeException(ee);
        }
    }

    public void traveldistances(List<Plan> pBase, List<Plan> pCompare, String outputPath) {

        Double[] originalDistance = calculateDistance(pBase);
        Double[] modifiedDistance = calculateDistance(pCompare);
        distanceTree = new TreeMap<>();
        distanceTree.put("baseTDistance", originalDistance);
        distanceTree.put("compTDistance", modifiedDistance);

        double distanceDifference = modifiedDistance[0] - originalDistance[0];
        double meanDistanceDifference = modifiedDistance[1] - originalDistance[1];

        System.out.println("OriginalDistance: " + originalDistance[0]/1000 + " [km]\n ModifiedDistance: " + modifiedDistance[0]/1000 + " [km]");

        try {
            FileWriter fileWriter = new FileWriter(outputPath);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write( "OriginalDistance in kilometers: " + originalDistance[0]/1000 + "\n ModifiedDistance in kilometers: " + modifiedDistance[0]/1000
                    + "\n Distance difference in kilometers: " + distanceDifference/1000 + "\n");
            bufferedWriter.write( "OriginalMeanDistance in kilometers: " + originalDistance[1]/1000 + "\n ModifiedMeanDistance in kilometers: " + modifiedDistance[1]/1000
                    + "\n mean Distance difference in kilometers: " + meanDistanceDifference/1000 + "\n");
            bufferedWriter.close();
        } catch(IOException ee){
            throw new RuntimeException(ee);
        }
    }

    public void scores(List<Plan> pBase, List<Plan> pCompare, String outputPath) {

        Double[] originalScore = calculateScore(pBase);
        Double[] modifiedScore = calculateScore(pCompare);
        scoreTree = new TreeMap<>();
        scoreTree.put("baseScore", originalScore);
        scoreTree.put("compScore", modifiedScore);

        double ScoreDifference = modifiedScore[0] - originalScore[0];
        double meanScoreDifference = modifiedScore[1] - originalScore[1];

        System.out.println("OriginalScore: " + originalScore[0] + " [util]\n ModifiedScore: " + modifiedScore[0] + " [util]");
        System.out.println("OriginalMeanScore p.p.: " + originalScore[1] + " [util]\n ModifiedMeanScore p.p.: " + modifiedScore[1] + " [util]");

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

    //method calculates how much travel Score agents aggregate [0] and the mean Score per plan [1]
    //The method uses as input a list of selected plans
    private static Double[] calculateScore(List<Plan> Plans) {
        Double[] scores = new Double[2];
        double score = 0.;
        double ctr = 0.;

        for (Plan plan : Plans){
            score += plan.getScore();
            ctr++;
        }
        scores[0]= score; scores[1]= score /ctr;
        return scores;
    }

    public TreeMap<String,Double[]> getTimeComparison(){
        return timeTree;
    }

    public TreeMap<String,Double[]> getDistanceComparison(){
        return distanceTree;
    }

    public TreeMap<String,Double[]> getScoreComparison(){
        return scoreTree;
    }

}
