package org.matsim.modify;


import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.utils.collections.CollectionUtils;

import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.*;


public class NetworkMod {

    private String outputNetwork = "scenarios/berlin-v5.4-1pct/input/berlin-v5.4-network-degesV2.xml.gz";
    private String outputPopulation = "scenarios/berlin-v5.4-1pct/input/berlin-v5.4-plans-deges.xml.gz";
    private String inputLines = "C:/Users/djp/Documents/deges_lines-v3.csv";
    private String cleaningShape = "C:/Users/djp/Documents/cleaning-shape_2.shp";
    private String csvOutputPath = System.getProperty("user.dir")+"/scenarios/berlin-v5.4-1pct/output/deges_added-Nodes.csv";
//    private String outputNetwork;
//    private String inputLines;
//    private String cleaningShape;
//    private String csvOutputPath = System.getProperty("user.dir")+"/scenarios/berlin-v5.4-1pct/output/citiz_added-NodesV2.csv";
    private String changeId = "deges";

    Map<Coord, Node> nodeMap = new HashMap<>();

    public NetworkMod(String analysisCase){
        this.changeId = analysisCase;
        this.outputNetwork = "scenarios/berlin-v5.4-1pct/input/berlin-v5.4-network-"+analysisCase+".xml.gz";
        this.inputLines = "C:/Users/djp/Documents/"+analysisCase+"_lines-v3.csv";
        this.cleaningShape = "C:/Users/djp/Documents/"+analysisCase+"/input/cleaning-shape_2.shp";
        this.csvOutputPath = System.getProperty("user.dir")+"/scenarios/berlin-v5.4-1pct/modOutput/"+analysisCase+"_added-Nodes.csv";
        this.outputPopulation = "scenarios/berlin-v5.4-1pct/input/berlin-v5.4-plans-"+analysisCase+".xml.gz";
    }
    public NetworkMod(){

    }

    // linkIDs to delete additionally
    static int[] LINKS_DEL = {5637,5638,131346,131347,142993,142994,142995,142996,142997,142998,9886,9887,8572,8573,46146,134490,35093,113981};

    public void modify(Scenario scenario) throws IOException {
        Network network = scenario.getNetwork();
        new MatsimNetworkReader(network);
        Network outNetwork = NetworkUtils.createNetwork();

        // linkID to delete additionally
//        int[] LINKS_DEL = {5637,5638,131346,131347,142993,142994,142995,142996,142997,142998,9886,9887,8572,8573,46146,134490,35093};

//        ArrayList<Id<Link>> links_delete = new ArrayList<>(LINKS_DEL.length);
//
//        Collection<SimpleFeature> simpleFeatures = new ShapeFileReader().readFileAndInitialize(cleaningShape);
//        Map<String, Geometry> zones = new HashMap<>();
//        for (SimpleFeature simpleFeature : simpleFeatures) {
//            zones.put((String) simpleFeature.getAttribute("id"), (Geometry) simpleFeature.getDefaultGeometry());
//        }
//        Geometry cShape = zones.get("cleaning-shape_2.1");
//
//        for(Link ll:network.getLinks().values()){
//            Point startNode = MGC.coord2Point(ll.getFromNode().getCoord());
//            Point endNode = MGC.coord2Point(ll.getToNode().getCoord());
//            if((cShape.contains(startNode) || cShape.contains(endNode)) && ll.getAttributes().getAttribute("type") == "motorway_link"){
//                links_delete.add(ll.getId());
//            }
//        }

        excludeNetworkElements(network, outNetwork);
//        for (int i : LINKS_DEL
//        ) {
//            Node fromNode = network.getLinks().get(Id.createLinkId(Integer.toString(i))).getFromNode();
//            Node toNode = network.getLinks().get(Id.createLinkId(Integer.toString(i))).getToNode();
//            Id<Link> linkId = Id.createLinkId(Integer.toString(i));
//
//            fromNode.removeOutLink(linkId);
//            toNode.removeInLink(linkId);
//
////            network.getLinks().remove(linkId);
//            network.getLinks().get(linkId).setCapacity(0.);
//
//            if(fromNode.getInLinks().size()+fromNode.getOutLinks().size() == 0){
//                network.getNodes().remove(fromNode);
//            }
//            if(toNode.getInLinks().size()+toNode.getOutLinks().size() == 0){
//                network.getNodes().remove(toNode);
//            }
//        }
        System.out.println("\n"+changeId+" nodes/links:");
        importNetworkElements(inputLines, outNetwork, nodeMap);
        reorderNetworkElements(outNetwork);
        printoutCsv(nodeMap, csvOutputPath);

//        adaptRoutes(scenario, network, outNetwork);
//        adaptActivities(scenario, outNetwork);

        new NetworkWriter(outNetwork).write(this.outputNetwork);

        for (Person pp : scenario.getPopulation().getPersons().values()) {
            for (Plan plan : pp.getPlans()) {
                PopulationUtils.resetRoutes(plan);
            }
        }
        new PopulationWriter(scenario.getPopulation()).write(outputPopulation);

//        for(Person pp: scenario.getPopulation().getPersons().values()) {
//            for (Plan plan : pp.getPlans()) {
//                PopulationUtils.resetRoutes(plan);
//            }
//        }

//        new PopulationWriter(scenario.getPopulation()).write(scenario.getConfig().plans().getInputFile());
        System.out.println("DONE!");


//        NetworkSimplifier networkSimplifier = new NetworkSimplifier();
//        networkSimplifier.run(network);
//        new NetworkWriter(outNetwork).write(this.outputNetwork);
    }

    private void excludeNetworkElements(Network network, Network newNetwork) {
        ArrayList<Id<Link>> links_delete = new ArrayList<>();

        for(Link ll: network.getLinks().values()){
            if(checkInsideArea(ll)){
                links_delete.add(ll.getId());
            }
        }
        for(int ll : LINKS_DEL){
            links_delete.add(Id.createLinkId(ll));
        }

        for (Node nn : network.getNodes().values()){
//            for(Id<Link> linkId : links_delete){
//                if(nn.getInLinks().containsKey(linkId)){
//                    nn.removeInLink(linkId);
//                }
//                else if(nn.getOutLinks().containsKey(linkId)){
//                    nn.removeOutLink(linkId);
//                }
//            }
//            if(!(nn.getOutLinks().size()+nn.getInLinks().size() == 0)){
//                NetworkUtils.createAndAddNode(newNetwork,nn.getId(),nn.getCoord());
//                nodeMap.put(roundCoord(nn.getCoord()), nn);
//            } else{
//                System.out.println("# # # Excluded node " + nn.toString() + " # # #");
//            }

            // workaround to keep all nodes. Comment if not needed (and include version above again)
            NetworkUtils.createAndAddNode(newNetwork,nn.getId(),nn.getCoord());
            nodeMap.put(roundCoord(nn.getCoord()), nn);


        }
        for(Id<Link> llId: network.getLinks().keySet()){
            if(!links_delete.contains(llId)){
                Link ll = network.getLinks().get(llId);
                NetworkUtils.createAndAddLink(newNetwork, llId, ll.getFromNode(), ll.getToNode(), ll.getLength(),
                        ll.getFreespeed(), ll.getCapacity(), ll.getNumberOfLanes()).setAllowedModes(ll.getAllowedModes());
//                network.getLinks().get(llId).setCapacity(0.);
//                network.getLinks().get(llId).setFreespeed(0.);
//                System.out.println("# # # Excluded link " + llId.toString() + " # # #");
            }
            else{
                Link ll = network.getLinks().get(llId);
                NetworkUtils.createAndAddLink(newNetwork, llId, ll.getFromNode(), ll.getToNode(), ll.getLength(),
                    ll.getFreespeed(), 0., ll.getNumberOfLanes()).setAllowedModes(new HashSet<>());
//                System.out.println("# # # Excluded link " + llId.toString() + " # # #");
                System.out.println("# # # Freespeed, capacity and numLanes of link " + llId.toString() + " = 0. !!  # # #");
            }
        }
    }

    private boolean checkInsideArea(Link ll){
        Polygon polygon = new Polygon();
        polygon.addPoint(4587275,5818986); polygon.addPoint(4587123,5819870);
        polygon.addPoint(4586847,5819638);polygon.addPoint(4586465,5819077);
        Coord startCoord = ll.getFromNode().getCoord();
        Coord endCoord = ll.getToNode().getCoord();
        if(polygon.contains(startCoord.getX(),startCoord.getY()) || polygon.contains(endCoord.getX(), endCoord.getY())) {
            if(NetworkUtils.getType(ll) != null && NetworkUtils.getType(ll).equals("motorway_link")){
                return true;
            }
        }
        return false;
    }

    private void importNetworkElements(String filePath, Network newNetwork, Map<Coord, Node> nodeMap) throws IOException {

        Node fromNode, toNode;
        Id<Node> nodeId;
        Link link;
        Id<Link> linkId;
        double length, freespeed, capacity, permlanes = 0.;
        Set<String> modes = CollectionUtils.stringToSet("car,freight,ride");

        BufferedReader bufferedReader = null;

        try {
            bufferedReader = new BufferedReader(new FileReader(filePath));
            String str;
            bufferedReader.readLine();
            int i = 0;
            while ((str = bufferedReader.readLine()) != null) {
                String[] col = str.split(";");

                /**
                 * Order col[0-9]: id, fromX, fromY, toX, toY, length, freespeed, capacity, permlanes, modes
                 * */
                Coord coord_start = roundCoord(new Coord(Double.parseDouble(col[1]), Double.parseDouble(col[2])));
                Coord coord_end = roundCoord(new Coord(Double.parseDouble(col[3]), Double.parseDouble(col[4])));

                coord_start = checkExistingCoord(coord_start,nodeMap);
                coord_end = checkExistingCoord(coord_end,nodeMap);

                if(!nodeMap.containsKey(coord_start)) {
                    nodeId = Id.createNodeId(col[0].substring(0,5)+"Node_"+i++);
                    fromNode = NetworkUtils.createAndAddNode(newNetwork, nodeId, coord_start);
                    System.out.println("New Node: "+fromNode);
                    nodeMap.put(coord_start,fromNode);
                }
                else {
                    fromNode = nodeMap.get(coord_start);
                }
                if(!nodeMap.containsKey(coord_end)) {
                    nodeId = Id.createNodeId(col[0].substring(0,5)+"Node_"+i++);
                    toNode = NetworkUtils.createAndAddNode(newNetwork, nodeId, coord_end);
                    System.out.println("New Node: "+toNode);
                    nodeMap.put(coord_end,toNode);
                }
                else {
                    toNode = nodeMap.get(coord_end);
                }

                linkId = Id.createLinkId(col[0]);
                length = getLength(fromNode, toNode);
                freespeed = Double.parseDouble(col[6]);
                capacity = Double.parseDouble(col[7]);
                permlanes = Double.parseDouble(col[8]);
//                String[] mode = col[9].substring(1, col.length-2).split(",");
//                modes.addAll(Arrays.asList(mode.clone()));

                link = NetworkUtils.createAndAddLink(newNetwork, linkId, fromNode, toNode, length, freespeed, capacity, permlanes);
                link.setAllowedModes(modes);

                System.out.println("From Node: "+fromNode);
                System.out.println("To Node: "+toNode);
                System.out.println("New Link: "+link);
                System.out.println("#######################");
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
        System.out.println("Imported Nodes!");
        return;
    }

    private double getLength(Node start, Node end) {
        Coord a = start.getCoord();
        Coord b = end.getCoord();
        double x_dist = Math.abs(a.getX() - b.getX());
        double y_dist = Math.abs(a.getY() - b.getY());
        // sqrt-distance + 10% extra to be safe (may be commented)
//        System.out.println("x: " + x_dist);
//        System.out.println("y: " + y_dist);
        double dist = Math.sqrt((x_dist * x_dist) + (y_dist * y_dist)); // * 1.1;
//        System.out.println(dist);
        return dist;
    }

    private Coord roundCoord(Coord cd){
        int x = (int) cd.getX();
        int y = (int) cd.getY();
        return new Coord(x,y);
    }

    private Coord checkExistingCoord(Coord cd, Map<Coord, Node> nodeMap){
        for(int x=-1; x<=1; x++){
            for(int y=-1; y<=1; y++){
                Coord checkCoord = new Coord(cd.getX()+x, cd.getY()+y);
                if(nodeMap.containsKey(checkCoord)){
                    return checkCoord;
                }
            }
        }
        return cd;
    }

    private void printoutCsv(Map<Coord, Node> nodeHashMap, String outputPath){
        try {
            FileWriter fileWriter = new FileWriter(outputPath);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write("X;Y;NodeId;inLinks;outLinks"); bufferedWriter.newLine();
            for(Coord cc :nodeHashMap.keySet()){
                bufferedWriter.write(cc.getX()+";"+cc.getY()+";");
                bufferedWriter.write(nodeHashMap.get(cc).getId() +";");
                bufferedWriter.write(nodeHashMap.get(cc).getInLinks().size()+" in;"+nodeHashMap.get(cc).getInLinks().size()+" out");
                bufferedWriter.newLine();
            }
            bufferedWriter.close();
        } catch(IOException ee){
            throw new RuntimeException(ee);
        }
    }

    private void reorderNetworkElements(Network newNetwork){
        if(changeId == "deges") {
            Id<Link> linkId = Id.createLinkId("degesLink_32");
            newNetwork.getLinks().get(Id.createLinkId(7619)).setFromNode(newNetwork.getLinks().get(linkId).getToNode());
            newNetwork.getLinks().get(Id.createLinkId(29191)).setToNode(newNetwork.getLinks().get(linkId).getFromNode());

            linkId = Id.createLinkId("degesLink_38");
            newNetwork.getLinks().get(Id.createLinkId(24150)).setToNode(newNetwork.getLinks().get(linkId).getFromNode());
            newNetwork.getLinks().get(Id.createLinkId(41431)).setFromNode(newNetwork.getLinks().get(linkId).getToNode());

            linkId = Id.createLinkId("degesLink_36");
            newNetwork.getLinks().get(Id.createLinkId(57000)).setFromNode(newNetwork.getLinks().get(linkId).getToNode());
            newNetwork.getLinks().get(Id.createLinkId(56999)).setToNode(newNetwork.getLinks().get(linkId).getToNode());
        } else if(changeId == "citiz") {
            Id<Link> linkId = Id.createLinkId("citizLink_16");
            newNetwork.getLinks().get(Id.createLinkId(24150)).setToNode(newNetwork.getLinks().get(linkId).getFromNode());
            newNetwork.getLinks().get(Id.createLinkId(41431)).setFromNode(newNetwork.getLinks().get(linkId).getToNode());

            linkId = Id.createLinkId("citizLink_10");
            newNetwork.getLinks().get(Id.createLinkId(41439)).setToNode(newNetwork.getLinks().get(linkId).getFromNode());
        } else{
            System.out.println("No correspondent reorderSet found! Change ID probably not set/set wrong!");
        }
    }

    private void adaptActivities(Scenario scenario, Network newNetwork){
        int[] blackList = {26584,78176,81897,81898,113981,81925,80096,134583,15369,32494,131590,131587,47471,92218,
                130741,78175,5637,5638,131346,131347,142993,142994,142995,142996,142997,142998,9886,9887,8572,8573,
                46146,134490,35093,80087,149674,137210,147564};
        for(Person pp: scenario.getPopulation().getPersons().values()) {
            for (Plan plan : pp.getPlans()) {
                for (PlanElement pe : plan.getPlanElements()) {
                    if(pe instanceof Activity){
                        Activity activity = (Activity) pe;
                        for (int linkId : blackList) {
                            //System.out.println(leg.getRoute());
                            if (Id.createLinkId(linkId).equals(activity.getLinkId())) {
                                System.out.println(activity);
                                System.out.println("FOUND AN AFFECTED LINK ON ACTIVITY! " + linkId);
                                double radio = 5.;
                                boolean linkFound = false;
                                while (!linkFound) {
                                    Collection<Node> nearestNodes = NetworkUtils.getNearestNodes(newNetwork, activity.getCoord(), radio);
                                    for (Node replaceNode : nearestNodes) {
                                        System.out.println(String.format("Candidate node %s with distance %f m from activity Coord %s",
                                                replaceNode.getId(),
                                                NetworkUtils.getEuclideanDistance(replaceNode.getCoord(), activity.getCoord()),
                                                activity.getCoord()));
                                        if (!replaceNode.getId().toString().contains("pt")) {
                                            for (Id<Link> replaceLink : replaceNode.getInLinks().keySet()) {
                                                if(replaceLink.toString().contains(changeId)){
                                                    linkFound = true;
                                                }
                                                else if (!replaceLink.toString().contains("pt")) {
                                                    for(int compareLink : blackList){
                                                        if(compareLink == Integer.valueOf(replaceLink.toString())){
                                                            linkFound = false;
                                                            break;
                                                        }
                                                        else{
                                                            linkFound = true;
                                                        }
                                                    }
                                                }
                                                if(linkFound) {
                                                    System.out.println(String.format("Replace activity linkId %s by %s", activity.getLinkId(), replaceLink));
                                                    activity.setLinkId(replaceLink);
                                                    for (PlanElement pe2 : plan.getPlanElements()) {
                                                        if(pe2 instanceof Leg){
                                                            Leg leg = (Leg) pe2;
                                                            if(leg.getRoute().getStartLinkId().equals(activity.getLinkId())){
                                                                System.out.println(String.format("Replace startLinkId %s by %s", activity.getLinkId(), replaceLink));
                                                                leg.getRoute().setStartLinkId(replaceLink);
                                                            }
                                                            if(leg.getRoute().getEndLinkId().equals(activity.getLinkId())){
                                                                System.out.println(String.format("Replace endLinkId %s by %s", activity.getLinkId(), replaceLink));
                                                                leg.getRoute().setEndLinkId(replaceLink);
                                                            }
                                                        }
                                                    }
                                                    break;
                                                }
                                            }
                                        }
                                        if(linkFound){
                                            break;
                                        }
                                    }
                                    System.out.println("Set new radio to "+(radio+10.));
                                    radio += 10.;
                                }
                            }
                        }
                    }
                }
            }
        }
        for (Person pp : scenario.getPopulation().getPersons().values()) {
            for (Plan plan : pp.getPlans()) {
                PopulationUtils.resetRoutes(plan);
            }
        }
        new PopulationWriter(scenario.getPopulation()).write(outputPopulation);
    }

    private void adaptRoutes(Scenario scenario, Network network, Network newNetwork){
        int[] blackList = {26584,78176,81897,81898,113981,81925,80096,134583,15369,32494,131590,131587,47471,92218,
                130741,78175,5637,5638,131346,131347,142993,142994,142995,142996,142997,142998,9886,9887,8572,8573,
                46146,134490,35093,80087,149674,137210,147564};
//        List<String> passedLinks = new ArrayList<String>();
        new PopulationReader(scenario);
//        Population newPop = PopulationUtils.createPopulation(scenario.getConfig(), newNetwork);
//        AttributesUtils.copyTo(scenario.getPopulation().getAttributes(), newPop.getAttributes());
//        PopulationFactory popFactory = newPop.getFactory();
//        ActivityFacilities actFacilities = scenario.getActivityFacilities();
//        RouteFactories rFactories = popFactory.getRouteFactories();

        Config config = scenario.getConfig();
//        config.subtourModeChoice().

        for(Person pp: scenario.getPopulation().getPersons().values()){
            for(Plan plan:pp.getPlans()){
                boolean correctLinks = true;
//                for(Leg leg:PopulationUtils.getLegs(plan)){
                for(PlanElement pe: plan.getPlanElements()){
                    List<String> passedLinks = new ArrayList<>();
                    if(pe instanceof Leg) {
                        Leg leg = (Leg) pe;

//                        if (leg.getMode().contains("car") || leg.getMode().contains("freight") || leg.getMode().contains("ride")) {
                            if (leg.getRoute() != null) {
                                passedLinks.addAll(Arrays.asList(leg.getRoute().getRouteDescription().split(" ")));
                                passedLinks.add(String.valueOf(leg.getRoute().getStartLinkId()));
                                passedLinks.add(String.valueOf(leg.getRoute().getEndLinkId()));
                                for (int linkId : blackList) {
                                    //System.out.println(leg.getRoute());
                                    if (passedLinks.contains(String.valueOf(linkId))) {
//                                if (leg.getRoute().getRouteDescription().contains(String.valueOf(linkId))) {
                                        //                                if (!leg.getRoute().getRouteType().contains(String.valueOf(linkId))) {
                                        System.out.println(leg.getRoute().getRouteType().equals(String.valueOf(linkId)));
                                        System.out.println(plan);
                                        System.out.println("FOUND AN AFFECTED LINK ON LEG! " + linkId);

                                        PopulationUtils.resetRoutes(plan);

                                        /*

                                        ControlerConfigGroup.RoutingAlgorithmType routingAlgorithmType = config.controler().getRoutingAlgorithmType();



                                        TripRouter tripRouter = new TripRouter();

                                        Provider<TripRouter> tripRouterProvider = (Provider<TripRouter>) Types.javaxProviderOf(tripRouter.getClass());

                                        new ReRoute(scenario.getActivityFacilities(),tripRouterProvider, config.global()).handlePlan(plan);

                                        NetworkUtils.getNearestLinkExactly(ou)

                                        SwissRailRaptor swissRailRaptor = new SwissRailRaptor().


                                         */

//                                    plan.getType();
//                                    leg.getRoute().getStartLinkId();
//                                    leg.getMode(); leg.getRoute().getRouteType();
//
//
//
//
//                                    Activity activity = null;
//                                    activity.getFacilityId();
//                                    activity.getLinkId();
//                                    FacilitiesUtils.decideOnLink(FacilitiesUtils.wrapActivity(activity),network);
//                                    FacilitiesUtils.setLinkID(FacilitiesUtils.wrapActivity(activity),
//                                            FacilitiesUtils.decideOnLink(FacilitiesUtils.wrapActivity(activity),network).getId());
//                                    Facility facility = FacilitiesUtils.wrapActivity(activity);
//                                    facility.getLinkId();

//                                    FacilitiesUtils.decideOnLink();


//                                    List<PlanElement> planElements = plan.getPlanElements();
//                                    PlanElement planElement = planElements.get(0);
//
//
//
//                                    ReRoute reRoute = new ReRoute();
//                                    PlanStrategy planStrategy = reRoute.get();
//
//                                    ((java.util.ArrayList)((HashMap.Node)((HashMap)controler.config.strategy.parameterSetsPerType).entrySet().toArray()[0]).getValue()).get(5);
//                                    TripStructureUtils.Trip trip = TripStructureUtils.getActivities();
//                                    trip.getLegsOnly();
//                                    TripRouter tripRouter = new TripRouter();
//
//                                    tripRouter.

                                        correctLinks = false;
                                        break;
                                    }
                                }
                            }
                            else{
//                                passedLinks.add(leg.)
                            }
//                        }
                    }
                    else{
                        Activity activity = (Activity) pe;
                        passedLinks.add(String.valueOf(activity.getLinkId()));
                        for (int linkId : blackList) {
                            //System.out.println(leg.getRoute());
                            if (passedLinks.contains(String.valueOf(linkId))) {
//                                if (leg.getRoute().getRouteDescription().contains(String.valueOf(linkId))) {
                                //                                if (!leg.getRoute().getRouteType().contains(String.valueOf(linkId))) {
                                System.out.println(activity);
                                System.out.println("FOUND AN AFFECTED LINK ON ACTIVITY! " + linkId);

                                activity.setLinkId(NetworkUtils.getNearestLinkExactly(newNetwork, activity.getCoord()).getId());
//                                ActivityFacility facility = actFacilities.getFacilities().get(activity.getFacilityId());
//                                ActivityFacility newFacility = actFacilities.getFactory().createActivityFacility(facility.getId(),facility.getCoord(), NetworkUtils.getNearestLinkExactly(newNetwork,facility.getCoord()).getId());
//                                actFacilities.getFacilities().put(facility.getId(),newFacility);

                                PopulationUtils.resetRoutes(plan);
                                correctLinks = false;
                                break;
                            }
                        }
                    }
                    if(!correctLinks) {
                        break;
                    }
                }
//                if(correctLinks){
//                    if(newPop.getPersons().containsKey(pp)){
//                       newPop.getPersons().get(pp).addPlan(plan);
//                    }
//                    else {
//                        Person person = popFactory.createPerson(pp.getId());
//                        person.addPlan(plan);
//                        newPop.addPerson(person);
//                    }
//                }
            }

        }
        System.out.println("DONE!");
//        new PopulationWriter(newPop).write(outputPopulation);
    }

}
