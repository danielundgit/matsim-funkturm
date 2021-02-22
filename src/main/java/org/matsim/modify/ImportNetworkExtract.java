package org.matsim.modify;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.CollectionUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ImportNetworkExtract {

    final static String csvNetworkPath = "freizeitnetz-csv.csv";
    final static String existingNetworkPath = "";
    final static String newNetworkPath = "freizeitnetz-network.xml";

    public static void main(String[] args) throws IOException {
            Node fromNode, toNode;
            Id<Node> nodeId;
            Link link;
            Id<Link> linkId;
            double length, freespeed = 1., capacity = 1., permlanes = 0.;

            Network network = NetworkUtils.createNetwork();
            if(existingNetworkPath!=""){network = NetworkUtils.readNetwork(existingNetworkPath);}

            Map<Coord,Node> nodeMap = new HashMap<>();
            for(Node nn:network.getNodes().values()){
                nodeMap.put(nn.getCoord(), nn);
            }
            Set<String> modes = CollectionUtils.stringToSet("car,freight,ride");

            BufferedReader bufferedReader = null;

            try {
                bufferedReader = new BufferedReader(new FileReader(csvNetworkPath));
                String str;
                bufferedReader.readLine();
                int i = 0;
                while ((str = bufferedReader.readLine()) != null) {
                    str = str.replace("\"",""); // have to tidy " created by QGis...
                    String[] col = str.split(";");

                    /**
                     * Order [0-9]: id, fromX, fromY, toX, toY, length, freespeed, capacity, permlanes, modes
                     * */
                    // link ID
                    linkId = Id.createLinkId(col[0]);

                    // fromNode and toNode, create or match existing nodes if applicable
                    Coord coord_start; Coord coord_end;
                    if(col.length>1) {
                        coord_start = roundCoord(new Coord(Double.parseDouble(col[1]), Double.parseDouble(col[2])));
                        coord_end = roundCoord(new Coord(Double.parseDouble(col[3]), Double.parseDouble(col[4])));
                    }
                    else{
                        System.out.println("Missing coordinates! Check link in csv or shp!");
                        continue;
                    }

                    coord_start = checkExistingCoord(coord_start,nodeMap);
                    coord_end = checkExistingCoord(coord_end,nodeMap);

                    if(!nodeMap.containsKey(coord_start)) {
                        nodeId = Id.createNodeId("Node_"+i++);
                        fromNode = NetworkUtils.createAndAddNode(network, nodeId, coord_start);
                        System.out.println("New Node: "+fromNode);
                        nodeMap.put(coord_start,fromNode);
                    }
                    else {
                        fromNode = nodeMap.get(coord_start);
                    }
                    if(!nodeMap.containsKey(coord_end)) {
                        nodeId = Id.createNodeId("Node_"+i++);
                        toNode = NetworkUtils.createAndAddNode(network, nodeId, coord_end);
                        System.out.println("New Node: "+toNode);
                        nodeMap.put(coord_end,toNode);
                    }
                    else {
                        toNode = nodeMap.get(coord_end);
                    }

                    // calculate own length to have control on this
                    length = getLength(fromNode, toNode);

                    // "additional" attributes, if used
//                    if(col[6] != null){freespeed = Double.parseDouble(col[6]);}
//                    if(col[7] != null){capacity = Double.parseDouble(col[7]);}
//                    if(col[8] != null){permlanes = Double.parseDouble(col[8]);}

                    // plug all together
                    link = NetworkUtils.createAndAddLink(network, linkId, fromNode, toNode, length, freespeed, capacity, permlanes);

                    // optionally set modes
                    //  String[] mode = col[9].substring(1, col.length-2).split(",");
                    //  modes.addAll(Arrays.asList(mode.clone()));
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
            System.out.println("Imported Nodes and Links! Call NetworkWriter...");
            writeIntoNetwork(network,newNetworkPath);
            return;
    }

    private static double getLength(Node start, Node end) {
        Coord a = start.getCoord();
        Coord b = end.getCoord();
        double x_dist = Math.abs(a.getX() - b.getX());
        double y_dist = Math.abs(a.getY() - b.getY());
        // sqrt-distance + 5% extra to be safe (may be commented out)
//        System.out.println("x: " + x_dist);
//        System.out.println("y: " + y_dist);
        double dist = Math.sqrt((x_dist * x_dist) + (y_dist * y_dist)); // * 1.05;
//        System.out.println(dist);
        return dist;
    }

    private static Coord roundCoord(Coord cd){
        int x = (int) cd.getX();
        int y = (int) cd.getY();
        return new Coord(x,y);
    }

    private static Coord checkExistingCoord(Coord cd, Map<Coord, Node> nodeMap){
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

    private static void writeIntoNetwork(Network outputNetwork, String outputPath){
        new NetworkWriter(outputNetwork).write(outputPath);
    }

}
