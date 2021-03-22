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
import org.matsim.run.RunBerlinScenario;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.*;
import java.util.*;

public class RunAnalysis {
//    final String pathLOR = "scenarios/berlin-v5.4-1pct/input/LOR.shp";
    // Newer LOR, requires new LOR definition
    final String pathLOR = "scenarios/berlin-v5.4-1pct/input/LOR.shp";
    String selectDefault = "base";
    String defaultPre = "funkturm_";
    final String configFile = "/output/berlin-v5.4-"+ RunBerlinScenario.PCT+"pct.output_config.xml";
    final String countsFile ="/output/berlin-v5.4-"+ RunBerlinScenario.PCT+"pct.output_counts.xml.gz";
    Config config; Scenario scenario;
    public static Map<Integer, Geometry> shapes;
    public static Map<Integer, Double> density;
    public static Map<Integer, Integer> trafficCounts;
    public static Map<Integer, Double> relativeMap;
    public static Map<Id<Person>, Person> personsADF;
    public static Map<Integer,Geometry> allLOR;
    Population population = null;
    Collection<? extends Person> allPersons = null;

    String[] countLinks = new String[]{"degesLink_7", "degesLink_24", "77419", "41441", "86578", "68014", "149457",
            "97427", "94289", "129176", "100186", "citizLink_28", "citizLink_27", "citizLink_21"};
    Integer[] areaADF = new Integer[]{};

    public RunAnalysis(String mode, Map<Integer, Geometry> shapeMap){
        selectDefault = mode;
        scenario = RunBerlinScenario.scenarios.get(mode);
        population = RunBerlinScenario.populations.get(mode);
        allPersons = RunBerlinScenario.persons.get(mode);
        shapes = shapeMap;
    }

    public RunAnalysis(String actualPre, String select, Integer[] lookupList) throws IOException {
        selectDefault = select;
        defaultPre = actualPre;
        config = ConfigUtils.loadConfig(defaultPre+select+configFile);
        config.network().setInputFile("berlin-v5.4-"+ RunBerlinScenario.PCT+"pct.output_network.xml.gz");
        config.plans().setInputFile("berlin-v5.4-"+ RunBerlinScenario.PCT+"pct."+RunBerlinScenario.ITER+"plans.xml.gz");
        scenario = ScenarioUtils.loadScenario(config);
        areaADF = lookupList;
        shapes = getLOR();
    }

    public RunAnalysis(String actualPre, String select, Map<Integer, Geometry> shapeMap){
        defaultPre = actualPre;
        selectDefault = select;
        config = ConfigUtils.loadConfig(defaultPre+select+configFile);
        config.network().setInputFile("berlin-v5.4-"+ RunBerlinScenario.PCT+"pct.output_network.xml.gz");
        config.plans().setInputFile("berlin-v5.4-"+ RunBerlinScenario.PCT+"pct."+RunBerlinScenario.ITER+"plans.xml.gz");
        scenario = ScenarioUtils.loadScenario(config);
        shapes = shapeMap;
    }

    public RunAnalysis(String select) throws IOException {
        selectDefault = select;
        config = ConfigUtils.loadConfig("funkturm_"+select+configFile);
        config.network().setInputFile("berlin-v5.4-"+ RunBerlinScenario.PCT+"pct.output_network.xml.gz");
        config.plans().setInputFile("berlin-v5.4-"+ RunBerlinScenario.PCT+"pct."+RunBerlinScenario.ITER+"plans.xml.gz");
        scenario = ScenarioUtils.loadScenario(config);
        System.out.println("You did not select a specific area! It will try to analyze all shapes [default: "+pathLOR+"]\n" +
                "The analysis may take some time ...");
        shapes = getLOR();
    }

    public RunAnalysis() throws IOException {
        System.out.println("You did not select a specific case! It will take the preset [default: "+selectDefault+"]");
        config = ConfigUtils.loadConfig("funkturm_"+selectDefault+configFile);
        config.network().setInputFile("berlin-v5.4-1pct.output_network.xml.gz");
        config.plans().setInputFile("berlin-v5.4-1pct."+RunBerlinScenario.ITER+"plans.xml");
        scenario = ScenarioUtils.loadScenario(config);
        System.out.println("You did not select a specific area! It will try to analyze all shapes [default: "+pathLOR+"]\n" +
                "The analysis may take some time ...");
        shapes = getLOR();
    }

    public void exampleCounts(boolean considerAllADFlinks) {
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
                                if ((link.getCapacity() != 0
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
        String outputCounts = RunBerlinScenario.PRE+selectDefault+RunBerlinScenario.INDEX+countsFile;
        System.out.printf("Write %d of %d counts into file %s !%n", counts.getCounts().size(), countLinks.length, outputCounts);
        new CountsWriter(counts).write(outputCounts);
    }

    public Map<Integer, Double> residentDensity(boolean writeToFile) {

        double area, counts;
        density = new HashMap<>();

//        if(allPersons==null) {
//            population = scenario.getPopulation();
//            allPersons = population.getPersons().values();
//        }
        for(int zone:shapes.keySet()) {
            counts = 0.;
            for (Person pp : allPersons) {
                Activity homeActivity = PopulationUtils.getFirstActivity(pp.getSelectedPlan());
                if(density.get(zone) != null) {
                    counts = density.get(zone);
                }
                if(shapes.get(zone).contains(MGC.coord2Point(homeActivity.getCoord()))) {
                    density.put(zone,counts+1.);
                }
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

    public Map<Integer, Integer> trafficCounts(boolean writeToFile) {
        trafficCounts = new HashMap<>(shapes.size());
//        if(allPersons==null) {
//            population = scenario.getPopulation();
//            allPersons = population.getPersons().values();
//        }
        Map<Id<Link>, ? extends Link> links = scenario.getNetwork().getLinks();
        for(Integer zone:shapes.keySet()) {
            trafficCounts.put(zone, 0);
            System.out.printf("Start analyzing LOR %d of %d%n", trafficCounts.size(), shapes.size());
            for (Person pp : allPersons) {
                boolean passedZone = false;
                for(Leg leg: PopulationUtils.getLegs(pp.getSelectedPlan())){
                    if(leg.getMode().contains("car") || leg.getMode().contains("freight") || leg.getMode().contains("ride")) {
                        if (leg.getRoute() != null) {
                            List<String> routeElements = new ArrayList<>();
                            routeElements.add(leg.getMode());
                            routeElements.addAll(Arrays.asList(leg.getRoute().getRouteDescription().split(" ").clone()));
                            //TODO: set Integer[25]{0,0,0,0,...,0,0,0} matches
                            for(int re = 1; re < routeElements.size(); re++){
                                Link link = links.get(Id.createLinkId(routeElements.get(re)));
                                 if(link == null){
                                     Coord coord_s = links.get(Id.createLinkId(leg.getRoute().getStartLinkId())).getCoord();
                                     Coord coord_e = links.get(Id.createLinkId(leg.getRoute().getStartLinkId())).getCoord();
                                     if(shapes.get(zone).contains(MGC.coord2Point(coord_s)) || shapes.get(zone).contains(MGC.coord2Point(coord_e))){
                                         passedZone = true; //TODO: put matches {0,0,1,0,0,...,0,0,0}
                                         break;
                                     }
                                 }
                                 else if(shapes.get(zone).contains(MGC.coord2Point(link.getCoord()))){
                                     passedZone = true;
                                     break;
                                 }
                             }
                            //TODO: Note: Here we would have matches {0,0,1,0,1,0,0,1,1,0,0,...,1,0,0}
                         }
                    }
                    if(passedZone) //TODO: No if condition
                    trafficCounts.put(zone, trafficCounts.get(zone)+1); //TODO: For all zones: trafficCounts.put(zone, ...get(zone)+matches[zone]); !!
                }
            }
        }
        writeOut(trafficCounts);
        if(writeToFile){
            writeToFile(trafficCounts, "traffic-counts", "csv");
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

    public Map<Id<Person>, Person> personsADF(boolean writeToFile) {

        personsADF = new HashMap<>();
//        if(allPersons==null) {
//            population = scenario.getPopulation();
//            allPersons = population.getPersons().values();
//        }
        Map<Double, Double> coordinatesH = new HashMap<>();

        if(shapes.size()>9){System.out.println("Warning: The covered area of personsADF seems to be bigger than the predefined ADF area");}

        for (Person pp : allPersons) {
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

    public Map<Integer, Double> trafficPerResidentDensity(boolean writeToFile) throws IOException {

        Map<Integer, Integer> trafficCounts = getTrafficCounts();
        Map<Integer, Double> density = getDensity();
        relativeMap = new HashMap<>();
        if(trafficCounts==null){
            trafficCounts = readIntIntMap(RunBerlinScenario.PRE+selectDefault+"/output/"+selectDefault+"-traffic-counts.csv");
            if(trafficCounts==null){
                trafficCounts = trafficCounts(true);
            }
        }
        if(density==null){
            density =readIntDouMap(RunBerlinScenario.PRE+selectDefault+"/output/"+selectDefault+"-resident density.csv");
            if(density==null){
                density = residentDensity(true);
            }
        }
        for(int kk:trafficCounts.keySet()){
            double relativeValue = Double.parseDouble(trafficCounts.get(kk).toString());
            relativeMap.put(kk, relativeValue/density.get(kk));
        }
        writeOut(trafficCounts);
        if(writeToFile){
            writeToFile(trafficCounts, "traffic-resident-relation", "csv");
        }
        return relativeMap;
    }

    public Map<Integer,Double> readIntDouMap(String file) throws IOException {
        Map<Integer,Double> map = new HashMap<>();
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(file));
            String line;
            bufferedReader.readLine();
            while ((line = bufferedReader.readLine()) != null) {
                map.put(Integer.parseInt(line.split(";")[0]),Double.parseDouble(line.split(";")[1]));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }
        return map;
    }

    public Map<Integer,Integer> readIntIntMap(String file) throws IOException {
        Map<Integer,Integer> map = new HashMap<>();
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(file));
            String line;
            bufferedReader.readLine();
            while ((line = bufferedReader.readLine()) != null) {
                map.put(Integer.parseInt(line.split(";")[0]),Integer.parseInt(line.split(";")[1]));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }
        return map;
    }

    public Map<Integer,Geometry> getLOR() throws IOException {

        allLOR = new HashMap<>();
        FileDataStore store = FileDataStoreFinder.getDataStore(new File(pathLOR));
        FeatureReader<SimpleFeatureType, SimpleFeature> reader = store.getFeatureReader();
        List<SimpleFeature> features = new ArrayList<>();

        while (reader.hasNext()) {
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

    public Map<Integer, Double> getDensity() {
        if(density==null){
            System.out.println("Resident density is null! Run residentDensity() before!");
        }
        return density;
    }

    public Map<Integer, Integer> getTrafficCounts() {
        if(trafficCounts==null){
            System.out.println("Traffic counts is null! Run trafficCounts() before!");
        }
        return trafficCounts;
    }

    public Map<Integer, Double> getTrafficPerDensity() {
        if(density==null){
            System.out.println("trafficPerResidentDensity is null! Run trafficPerResidentDensity() before!");
        }
        return relativeMap;
    }

    public Map<Id<Person>, Person> getPersonsADF() {
        if(personsADF==null){
            System.out.println("personsADF is null! Run personsADF() before!");
        }
        return personsADF;
    }

    public Map<Integer, Geometry> getAllLOR() {
        if(allLOR==null){
            System.out.println("allLOR is null! Run getLOR() before!");
        }
        return allLOR;
    }

    public void writeOut(Object o){
        System.out.println(o);
    }

    public void writeToFile(Object o, String name, String extension){
        String outputPath = RunBerlinScenario.PRE+selectDefault+RunBerlinScenario.INDEX+"/output/"+selectDefault+"-"+name+"."+extension;
        try {
            FileWriter fileWriter = new FileWriter(outputPath);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write("LOR/AREA/X;value_"+name+"_"+selectDefault); bufferedWriter.newLine();
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
