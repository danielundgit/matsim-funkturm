package org.matsim.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.ArrayList;
import java.util.List;

public class RunComparison {
//    final String pathLOR = "scenarios/berlin-v5.4-1pct/input/LOR.shp";
    String baseDefault = "base";
    String compareDefault = "base";
    final String configFile = "/output/berlin-v5.4-1pct.output_config.xml";
    Config baseConfig; Scenario baseScenario;
    Config compareConfig; Scenario compareScenario;

    public RunComparison(String base, String compare){
        baseDefault = base;
        compareDefault = compare;
        baseConfig = ConfigUtils.loadConfig("funkturm_"+ base + configFile);
        baseConfig.network().setInputFile("berlin-v5.4-1pct.output_network.xml.gz");
        baseConfig.plans().setInputFile("berlin-v5.4-1pct.output_plans.xml.gz");
        compareConfig = ConfigUtils.loadConfig("funkturm_"+ compare + configFile);
        compareConfig.network().setInputFile("berlin-v5.4-1pct.output_network.xml.gz");
        compareConfig.plans().setInputFile("berlin-v5.4-1pct.output_plans.xml.gz");
        baseScenario = ScenarioUtils.loadScenario(baseConfig);
        compareScenario = ScenarioUtils.loadScenario(compareConfig);
    }

    public RunComparison(){
        System.out.println("This comparison 'base vs. base' might not be what you are looking for!");
        baseConfig = ConfigUtils.loadConfig("funkturm_"+ baseDefault + configFile);
        baseScenario = ScenarioUtils.loadScenario(baseConfig);
        compareConfig = baseConfig; compareScenario = baseScenario;
    }

    public void runTimeDistanceComparison() {
        String outputFile = baseDefault+"-"+compareDefault+"_TimeDistanceDifferences.txt";

//        List<Plan> originalPlans = getSelectedPlans(baseScenario);
//        List<Plan> modifiedPlans = getSelectedPlans(compareScenario);

        TimeDistanceComparison tdc = new TimeDistanceComparison(this.baseConfig, this.compareConfig, this.baseScenario, this.compareScenario, outputFile);
        tdc.prepare();
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

}
