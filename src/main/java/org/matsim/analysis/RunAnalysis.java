package org.matsim.analysis;

import org.geotools.data.FeatureReader;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.locationtech.jts.geom.*;
import org.matsim.api.core.v01.Coord;
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
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class RunAnalysis {
//    final String pathLOR = "scenarios/berlin-v5.4-1pct/input/LOR.shp";
    // Newer LOR, recquires new LOR definition
    final String pathLOR = "scenarios/berlin-v5.4-1pct/input/LOR_PLR.shp";
    String selectDefault = "base";
    final String configFile = "/output/berlin-v5.4-1pct.output_config.xml";
    final String countsFile ="/funkturmCounts.xml.gz";
    Config config; Scenario scenario;
    Map<Integer, Polygon> shapes = null;

    String[] countLinks = new String[]{"degesLink_7", "degesLink_24", "77419", "41441", "86578", "68014", "149457",
            "97427", "94289", "129176", "100186", "citizLink_28", "citizLink_27", "citizLink_21"};
    String[] areaADF = new String[]{};

    public RunAnalysis(String select, String[] lookupList){
        selectDefault = select;
        config = ConfigUtils.loadConfig("funkturm_"+select+configFile);
        config.network().setInputFile("berlin-v5.4-1pct.output_network.xml.gz");
        config.plans().setInputFile("berlin-v5.4-1pct.output_plans.xml.gz");
        scenario = ScenarioUtils.loadScenario(config);
        areaADF = lookupList;
    }

    public RunAnalysis(String select, Map<Integer, Polygon> shapeMap){
        selectDefault = select;
        config = ConfigUtils.loadConfig("funkturm_"+select+configFile);
        config.network().setInputFile("berlin-v5.4-1pct.output_network.xml.gz");
        config.plans().setInputFile("berlin-v5.4-1pct.output_plans.xml.gz");
        scenario = ScenarioUtils.loadScenario(config);
        shapes = shapeMap;
    }

    public RunAnalysis(String select){
        selectDefault = select;
        config = ConfigUtils.loadConfig("funkturm_"+select+configFile);
        config.network().setInputFile("berlin-v5.4-1pct.output_network.xml.gz");
        config.plans().setInputFile("berlin-v5.4-1pct.output_plans.xml.gz");
        scenario = ScenarioUtils.loadScenario(config);
        List<String> allLOR = new ArrayList<>();
//      Newer LOR system (01/2021)
        //        for(int i=1;i<=542;i++){
        for(int i=1;i<=447;i++){
            allLOR.add(String.valueOf(i));
        }
        areaADF = (String[]) allLOR.toArray();
    }

    public RunAnalysis(){
        config = ConfigUtils.loadConfig("funkturm_"+selectDefault+configFile);
        config.network().setInputFile("berlin-v5.4-1pct.output_network.xml.gz");
        config.plans().setInputFile("berlin-v5.4-1pct.output_plans.xml");
        scenario = ScenarioUtils.loadScenario(config);
    }


    public Counts<Link> exampleCounts(boolean considerAllADFlinks) throws IOException {
        if(considerAllADFlinks){
            System.out.println("Get all links inside defined LOR area (default: ADF LOR)");
            List<String> linksADF = new ArrayList<>();
            if(shapes!=null){
                for(Id<Link> linkId:scenario.getNetwork().getLinks().keySet()){
                    Link link = scenario.getNetwork().getLinks().get(linkId);
                    Point linkADF = MGC.xy2Point(link.getCoord().getX(),link.getCoord().getY());
                    for(Polygon zone: shapes.values()) {
                        if (zone.contains(linkADF)) {
                            if(link.getCapacity()!=0)
                                linksADF.add(linkId.toString());
                        }
                    }
                }
            }
            else {
                Hashtable<String, Geometry> lors = getLOR();
                for (Id<Link> linkId : scenario.getNetwork().getLinks().keySet()) {
                    Link link = scenario.getNetwork().getLinks().get(linkId);
                    Point linkADF = MGC.xy2Point(link.getCoord().getX(), link.getCoord().getY());
                    for (Geometry lor : lors.values()) {
                        if (lor.contains(linkADF)) {
                            if (link.getCapacity() != 0)
                                linksADF.add(linkId.toString());
                        }
                    }
                }
            }
            countLinks = linksADF.toArray(new String[linksADF.size()]);
        }
        Counts<Link> counts = new Counts<>();
        for(String cc:countLinks){
            Id<Link> linkId = Id.createLinkId(cc);
            String countName = "count_"+cc;
            if(scenario.getNetwork().getLinks().containsKey(linkId)){
                Count<Link> countId = counts.createAndAddCount(linkId,countName);
                for(int hh=1; hh<25; hh++){
                    countId.createVolume(hh,0.);
                }
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
        Map<Id<Link>, ? extends Link> links = scenario.getNetwork().getLinks();

        for(String lor:lors.keySet()) {
            trafficCounts.put(lor, 0);
            System.out.println(String.format("Start analyzing LOR %d of %d", trafficCounts.size(), lors.size()));
            for (Person pp : population.getPersons().values()) {
                boolean passedZone = false;
                for(Leg leg: PopulationUtils.getLegs(pp.getSelectedPlan())){
                    if(leg.getMode().contains("car") || leg.getMode().contains("freight") || leg.getMode().contains("ride")) {
                        if (leg.getRoute() != null) {
                            List<String> routeElements = new ArrayList<>();
                            routeElements.add(leg.getMode());
                            routeElements.addAll(Arrays.asList(leg.getRoute().getRouteDescription().split(" ").clone()));

                            for(int re = 1; re < routeElements.size(); re++){
                                Link link = links.get(Id.createLinkId(routeElements.get(re)));
                                 if(link == null){
                                     Coord coord_s = links.get(Id.createLinkId(leg.getRoute().getStartLinkId())).getCoord();
                                     Coord coord_e = links.get(Id.createLinkId(leg.getRoute().getStartLinkId())).getCoord();
                                     if(lors.get(lor).contains(MGC.coord2Point(coord_s)) || lors.get(lor).contains(MGC.coord2Point(coord_e))){
                                         passedZone = true;
                                         break;
                                     }
                                 }
                                 else if(lors.get(lor).contains(MGC.coord2Point(link.getCoord()))){
                                     passedZone = true;
                                     break;
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
            String lorId = feature.getID();
            for(String lorADF : areaADF){
                if(lorId.contains(lorADF)){
                    Geometry lorGeo = (Geometry) feature.getDefaultGeometry();
                    allLOR.put(lorId,lorGeo);
                }
            }

        }

        return allLOR;
    }

    public Map<Id<Person>, Person> getADFpersons() throws IOException {
        Population population = scenario.getPopulation();
        Hashtable<String, Geometry> lors = getLOR();
        Map<Double, Double> coordinatesH = new HashMap<>();
        if(lors.size()>9){
            System.out.println("Warning: The covered area of personsADF seems to be bigger than the predefined ADF area");
        }
        Map<Id<Person>, Person> personsADF = new HashMap<>();
        for (Person pp : population.getPersons().values()) {
            Activity homeActivity = PopulationUtils.getFirstActivity(pp.getSelectedPlan());
            Point home = MGC.xy2Point(homeActivity.getCoord().getX(),homeActivity.getCoord().getY());
            for(Geometry lor : lors.values()) {
                if (lor.contains(home)) {
                    personsADF.put(pp.getId(), pp);
                    coordinatesH.put(homeActivity.getCoord().getX(),homeActivity.getCoord().getY());
                }
            }
        }
        writeToFile(coordinatesH, "coordinatesOfHomesADF");
        return personsADF;
    }

    public void writeOut(Object o){
        System.out.println(o);
        return;
    }

    public void writeToFile(Object o, String name){
        String outputPath = "funkturm_"+selectDefault+"/output/"+selectDefault+"-"+name+".csv";
        try {
            FileWriter fileWriter = new FileWriter(outputPath);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write("LOR;value_"+name+"_"+selectDefault); bufferedWriter.newLine();
//            bufferedWriter.write("key_"+name+"_"+selectDefault+"LOR;value_"+name+"_"+selectDefault); bufferedWriter.newLine();
            for(Object key:((Map<?, ?>) o).keySet()){
                bufferedWriter.write(key+";"+((Map<?, ?>) o).get(key));
                bufferedWriter.newLine();
            }
            bufferedWriter.close();
        } catch(IOException ee){
            throw new RuntimeException(ee);
        }
    }

}
