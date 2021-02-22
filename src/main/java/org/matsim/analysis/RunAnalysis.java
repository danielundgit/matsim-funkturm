package org.matsim.analysis;

import org.geotools.data.FeatureReader;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RunAnalysis {
    final String pathLOR = "scenarios/berlin-v5.4-1pct/input/LOR.shp";
    String selectDefault = "base";
    final String configFile = "/output/berlin-v5.4-1pct.output_config.xml";
    final String countsFile ="/funkturmCounts.xml.gz";
    Config config; Scenario scenario;

    String[] countLinks = new String[]{"degesLink_7", "degesLink_24", "77419", "41441", "86578", "68014", "149457",
            "97427", "94289", "129176", "100186", "citizLink_28", "citizLink_27", "citizLink_21"};

    public RunAnalysis(String select){
        selectDefault = select;
        config = ConfigUtils.loadConfig("funkturm_"+select+configFile);
        config.network().setInputFile("berlin-v5.4-1pct.output_network.xml.gz");
        scenario = ScenarioUtils.loadScenario(config);
    }

    public RunAnalysis(){
        config = ConfigUtils.loadConfig("funkturm_"+selectDefault+configFile);
        config.network().setInputFile("berlin-v5.4-1pct.output_network.xml.gz");
        scenario = ScenarioUtils.loadScenario(config);
    }

    public Counts<Link> exampleCounts(){
        Counts<Link> counts = new Counts<>();
        for(String cc:countLinks){
            Id<Link> linkId = Id.createLinkId(cc);
            String countName = "count_"+cc;
            if(scenario.getNetwork().getLinks().containsKey(linkId)){
                counts.createAndAddCount(linkId,countName);
            }
        }
        String outputCounts = "funkturm_"+selectDefault+countsFile;
        System.out.println(String.format("Write %d of %d counts into file %s !", counts.getCounts().size(), countLinks.length, outputCounts));
        new CountsWriter(counts).write(outputCounts);

        return counts;
    }

    public Map<String, Double> getResidentDensity() throws IOException {
        Hashtable<String, Geometry> lors = getLOR();
        double area, counts = 0.;
        Map<String, Double> density = new HashMap<>();
        Population population = scenario.getPopulation();
        for(String lor:lors.keySet()) {
            for (Person pp : population.getPersons().values()) {
                Activity homeActivity = PopulationUtils.getFirstActivity(pp.getSelectedPlan());
                if(density.get(lor) != null) {
                    counts = density.get(lor);
                }
                if(lors.get(lor).contains(MGC.coord2Point(homeActivity.getCoord()))){
                    density.put(lor,counts+1.);
                }
            }
            area = lors.get(lor).getArea();
            density.put(lor,density.get(lor)/area);
        }
        return density;
    }


    public Map<String, Integer> doTrafficCounts() throws IOException {
        Hashtable<String, Geometry> lors = getLOR();
        Map<String, Integer> trafficCounts = new HashMap<>(lors.size());
        Population population = scenario.getPopulation();

        for(String lor:lors.keySet()) {
            for (Person pp : population.getPersons().values()) {
                boolean passedZone = false;
                for(Leg leg: PopulationUtils.getLegs(pp.getSelectedPlan())){
                    if(leg.getMode().contains("car") || leg.getMode().contains("freight") || leg.getMode().contains("ride")) {
                        if (leg.getRoute() != null) {
                            List<String> routeElements = new ArrayList<>();
                            routeElements.add(leg.getMode());
                            routeElements.addAll(Arrays.asList(leg.getRoute().getRouteType().split(" ").clone()));
                            for(int re = 1; re < routeElements.size(); re++){
                                 Link link = scenario.getNetwork().getLinks().get(Id.createLinkId(routeElements.get(re)));
                                 if(lors.get(lor).contains(MGC.coord2Point(link.getCoord()))){
                                     passedZone = true;
                                 }
                             }
                         }
                    }
                    if(passedZone)
                    trafficCounts.put(lor, trafficCounts.get(lor)+1);
                }
            }
        }
        return trafficCounts;
    }


    public Hashtable<String,Geometry> getLOR() throws IOException {
        Hashtable<String,Geometry> allLOR = new Hashtable<>();
        FileDataStore store = FileDataStoreFinder.getDataStore(new File(pathLOR));
        FeatureReader<SimpleFeatureType, SimpleFeature> reader = store.getFeatureReader();
        List<SimpleFeature> features = new ArrayList<>();

        for( ; reader.hasNext(); ){
            SimpleFeature result = reader.next();
            features.add(result);
        }
        reader.close();

        for(SimpleFeature feature:features) {
            Geometry lorGeo = (Geometry) feature.getDefaultGeometry();
            String lorId = feature.getID();
            allLOR.put(lorId,lorGeo);
        }

        return allLOR;
    }

    public void writeOut(Object o){
        System.out.println(o);
        return;
    }


}
