package org.matsim.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

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
        compareConfig = ConfigUtils.loadConfig("funkturm_"+ compare + configFile);
        compareConfig.network().setInputFile("berlin-v5.4-1pct.output_network.xml.gz");
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
        TimeDistanceComparison tdc = new TimeDistanceComparison(this.baseConfig, this.compareConfig, this.baseScenario, this.compareScenario);
        tdc.prepare();
    }

}
