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
    final String pathLOR = "scenarios/berlin-v5.4-1pct/input/LOR.shp";
    String selectDefault = "base";
    final String configFile = "/output/berlin-v5.4-1pct.output_config.xml";
    final String countsFile ="/output/berlin-v5.4-1pct.output_counts.xml.gz";
    Config config; Scenario scenario;
    Map<Integer, Geometry> shapes = null;

    String[] countLinks = new String[]{"degesLink_7", "degesLink_24", "77419", "41441", "86578", "68014", "149457",
            "97427", "94289", "129176", "100186", "citizLink_28", "citizLink_27", "citizLink_21"};
    Integer[] areaADF = new Integer[]{};

    public RunAnalysis(String select, Integer[] lookupList) throws IOException {
        selectDefault = select;
        config = ConfigUtils.loadConfig("funkturm_"+select+configFile);
        config.network().setInputFile("berlin-v5.4-1pct.output_network.xml.gz");
        config.plans().setInputFile("berlin-v5.4-1pct.output_plans.xml.gz");
        scenario = ScenarioUtils.loadScenario(config);
        areaADF = lookupList;
        shapes = getLOR();
    }

    public RunAnalysis(String select, Map<Integer, Geometry> shapeMap){
        selectDefault = select;
        config = ConfigUtils.loadConfig("funkturm_"+select+configFile);
        config.network().setInputFile("berlin-v5.4-1pct.output_network.xml.gz");
        config.plans().setInputFile("berlin-v5.4-1pct.output_plans.xml.gz");
        scenario = ScenarioUtils.loadScenario(config);
        shapes = shapeMap;
    }

    public RunAnalysis(String select) throws IOException {
        selectDefault = select;
        config = ConfigUtils.loadConfig("funkturm_"+select+configFile);
        config.network().setInputFile("berlin-v5.4-1pct.output_network.xml.gz");
        config.plans().setInputFile("berlin-v5.4-1pct.output_plans.xml.gz");
        scenario = ScenarioUtils.loadScenario(config);
        System.out.println("You did not select a specific area! It will try to analyze all shapes [default: "+pathLOR+"]\n" +
                "The analysis may take some time ...");
        shapes = getLOR();
    }

    public RunAnalysis() throws IOException {
        System.out.println("You did not select a specific case! It will take the preset [default: "+selectDefault+"]");
        config = ConfigUtils.loadConfig("funkturm_"+selectDefault+configFile);
        config.network().setInputFile("berlin-v5.4-1pct.output_network.xml.gz");
        config.plans().setInputFile("berlin-v5.4-1pct.output_plans.xml");
        scenario = ScenarioUtils.loadScenario(config);
        System.out.println("You did not select a specific area! It will try to analyze all shapes [default: "+pathLOR+"]\n" +
                "The analysis may take some time ...");
        shapes = getLOR();
    }

    public Counts<Link> exampleCounts(boolean considerAllADFlinks) throws IOException {
        if(considerAllADFlinks){
            System.out.println("Get all links inside defined LOR area (default: ADF LOR)");
            List<String> linksADF = new ArrayList<>();
            if(shapes!=null){
                for(Id<Link> linkId:scenario.getNetwork().getLinks().keySet()){
                    Link link = scenario.getNetwork().getLinks().get(linkId);
                    Point linkADF = MGC.xy2Point(link.getCoord().getX(),link.getCoord().getY());
                    for(Geometry zone: shapes.values()) {
                        if (zone.contains(linkADF)) {
                            if(!linkId.toString().contains("pt")) {
                                if (
                                        (link.getCapacity() != 0
//                                        && link.getFreespeed() > 5.
//                                        && link.getLength() > 150.
                                        )
//                                        || linkId.toString().contains(selectDefault)
                                ){
//                                    Set<Id<Link>> neighborLinks = scenario.getNetwork().getNodes().get(link.getFromNode().getId()).getInLinks().keySet();
//                                    int cc = 0;
//                                    for(Id<Link> neighborLink:neighborLinks){
//                                        if(linksADF.contains(neighborLink.toString())) {
//                                            cc++;
//                                        }
//                                    }
//                                    if(cc==0)
                                    linksADF.add(linkId.toString());
                                }
                            }
                        }
                    }
                }
            }
            else {
                System.out.println("For some reason, the loaded shapes are null! No exampleCounts created!");
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

    public Map<Integer, Double> getResidentDensity(boolean writeToFile) {

        double area, counts = 0.;
        Map<Integer, Double> density = new HashMap<>();

        Population population = scenario.getPopulation();
        for(int zone:shapes.keySet()) {
            for (Person pp : population.getPersons().values()) {
                Activity homeActivity = PopulationUtils.getFirstActivity(pp.getSelectedPlan());
                if(density.get(zone) != null) {counts = density.get(zone);}
                if(shapes.get(zone).contains(MGC.coord2Point(homeActivity.getCoord()))) {density.put(zone,counts+1.);}
            }
            area = shapes.get(zone).getArea()/1000000; //hopefully to get from m^2 to km^2
            density.put(zone,density.get(zone)/area);
        }

        writeOut(density);
        if(writeToFile){
            writeToFile(density, "resident density", "csv");
        }
        return density;
    }

    public Map<Integer, Integer> doTrafficCounts() {
        Map<Integer, Integer> trafficCounts = new HashMap<>(shapes.size());
        Population population = scenario.getPopulation();
        Map<Id<Link>, ? extends Link> links = scenario.getNetwork().getLinks();
        for(Integer zone:shapes.keySet()) {
            trafficCounts.put(zone, 0);
            System.out.println(String.format("Start analyzing LOR %d of %d", trafficCounts.size(), shapes.size()));
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
                                     if(shapes.get(zone).contains(MGC.coord2Point(coord_s)) || shapes.get(zone).contains(MGC.coord2Point(coord_e))){
                                         passedZone = true;
                                         break;
                                     }
                                 }
                                 else if(shapes.get(zone).contains(MGC.coord2Point(link.getCoord()))){
                                     passedZone = true;
                                     break;
                                 }
                             }
                         }
                    }
                    if(passedZone)
                    trafficCounts.put(zone, trafficCounts.get(zone)+1);
                }
            }
        }
        return trafficCounts;
    }

// try to write/get something as a shape file
//    public Map<Integer,Geometry> getShape() throws IOException {
//        Map<Integer,Geometry> allLOR = new Hashtable<>();
//        FileDataStore store = FileDataStoreFinder.getDataStore(new File("C:/Users/djp/Desktop/TUB/MATSim/matsim-2021/git/matsim-funkturm/funkturm_deges_t2/output/deges_t2-gridADF.csv"));
//        FeatureReader<SimpleFeatureType, SimpleFeature> reader = store.getFeatureReader();
//        List<SimpleFeature> features = new ArrayList<>();
//
//        for( ; reader.hasNext(); ){
//            SimpleFeature result = reader.next();
//            features.add(result);
//        }
//        reader.close();
//
//        for(SimpleFeature feature:features) {
//            int lorId = Integer.parseInt(feature.getID());
//            Geometry lorGeo = (Geometry) feature.getDefaultGeometry();
//            allLOR.put(lorId,lorGeo);   // cast as Polygon, lorId to Integer through split regex [1]
//        }
//        return allLOR;
//    }

    public Map<Id<Person>, Person> getADFpersons(boolean writeToFile) {

        Population population = scenario.getPopulation();
        Map<Double, Double> coordinatesH = new HashMap<>();

        if(shapes.size()>9){System.out.println("Warning: The covered area of personsADF seems to be bigger than the predefined ADF area");}

        Map<Id<Person>, Person> personsADF = new HashMap<>();
        for (Person pp : population.getPersons().values()) {
            Activity homeActivity = PopulationUtils.getFirstActivity(pp.getSelectedPlan());
            Point home = MGC.xy2Point(homeActivity.getCoord().getX(),homeActivity.getCoord().getY());
            for(Geometry zone : shapes.values()) {
                if (zone.contains(home)) {
                    personsADF.put(pp.getId(), pp);
                    coordinatesH.put(homeActivity.getCoord().getX(),homeActivity.getCoord().getY());
                }
            }
        }
        writeOut(coordinatesH);
        if(writeToFile) { writeToFile(coordinatesH, "coordinatesOfHomesADF", "csv"); }
        return personsADF;
    }

    public Map<Integer,Geometry> getLOR() throws IOException {

        Map<Integer,Geometry> allLOR = new HashMap<>();
        FileDataStore store = FileDataStoreFinder.getDataStore(new File(pathLOR));
        FeatureReader<SimpleFeatureType, SimpleFeature> reader = store.getFeatureReader();
        List<SimpleFeature> features = new ArrayList<>();

        for( ; reader.hasNext(); ){
            SimpleFeature result = reader.next();
            features.add(result);
        }
        reader.close();

        for(SimpleFeature feature:features) {
            String[] sLorId = feature.getID().split("\\.");
            // Make sure that the syntax of PLR IDs is appopriate for the next step
            int lorId = Integer.parseInt(sLorId[1]);
            if(areaADF!=null) {
                for (int lorADF : areaADF) {
                    if (lorADF == lorId) {
                        Geometry lorGeo = (Geometry) feature.getDefaultGeometry();
                        allLOR.put(lorId, lorGeo);
                        break;
                    }
                }
            }
            else {
                Geometry lorGeo = (Geometry) feature.getDefaultGeometry();
                allLOR.put(lorId, lorGeo);
            }

        }
        return allLOR;
    }

    public void writeOut(Object o){
        System.out.println(o);
        return;
    }

    public void writeToFile(Object o, String name, String extension){
        String outputPath = "funkturm_"+selectDefault+"/output/"+selectDefault+"-"+name+"."+extension;
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
