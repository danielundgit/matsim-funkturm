package org.matsim.analysis;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.geotools.MGC;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GridCreator {

    int rows = 5;
    int cols = 5;
    int X_MIN = 4585305; // west
    int X_MAX = 4588090; // east
    int Y_MIN = 5818060; // south (!)
    int Y_MAX = 5821530; // north (!)

    public GridCreator() {
        createGrid();
    }

    public GridCreator(int rows, int columns) {
        this.rows = rows;
        this.cols = columns;
        createGrid();
    }

    public GridCreator(int length) {
        rows = length;
        cols = length;
        createGrid();
    }

    int width = X_MAX-X_MIN;
    int height = Y_MAX-Y_MIN;
    Map<Coord, Coord> cells = null;
    Map<Integer, Geometry> polyMap = null;

    public void createGrid() {

        System.out.println("### Creating the grid... ###");

        int cellwidth = width/cols;
        int cellheight = height/rows;
        int extra_x = width%cols;       System.out.println("## Width of each cell is "+cellwidth+" with "+extra_x+" extra on last cell! ##");
        int extra_y = height%rows;      System.out.println("## Height of each cell is "+cellheight+" with "+extra_y+" extra on last cell! ##");

        cells = new HashMap<>();

        for(int r=0; r<=rows; r++){
            for(int c=0; c<=cols; c++) {
                if(c==cols && r==rows){
                    cells.put(new Coord(r,c), new Coord(X_MIN+cellwidth*cols+extra_x, Y_MIN+cellheight*rows+extra_y));
                }
                else if(r==rows){
                    cells.put(new Coord(r,c), new Coord(X_MIN+cellwidth*c, Y_MIN+cellheight*rows+extra_y));
                }
                else if(c==cols){
                    cells.put(new Coord(r,c), new Coord(X_MIN+cellwidth*cols+extra_x, Y_MIN+cellheight*r));
                }
                else{
                    cells.put(new Coord(r,c),new Coord(X_MIN+cellwidth*c,Y_MIN+cellheight*r));
                }
            }
        }
        System.out.println("### DONE! ###");
//        printGrid(cells);
        polyMap = getPolygons(cells);

    }

    public Map<Coord, Coord> getCoordMap(){
        return cells;
    }

    public Map<Integer, Geometry> getPolyMap(){
        return polyMap;
    }

    public Map<Integer, Geometry> getPolygons(Map<Coord,Coord> gridADF) {
        Map<Integer, Geometry> polyADF = new HashMap<>();
        int id = 1;
        GeometryFactory gf = new GeometryFactory();

        for(int r=0; r<rows; r++) {
            for (int c = 0; c < cols; c++) {
                Coordinate[] coordinates = new Coordinate[]{
                        MGC.coord2Coordinate(gridADF.get(new Coord(r,c))),              // bottom-left corner
                        MGC.coord2Coordinate(gridADF.get(new Coord(r,c+1))),         // bottom-right corner
                        MGC.coord2Coordinate(gridADF.get(new Coord(r+1,c+1))),    // top-right corner
                        MGC.coord2Coordinate(gridADF.get(new Coord(r+1,c))),         // top-left corner
                        MGC.coord2Coordinate(gridADF.get(new Coord(r,c)))               // back: bottom-left corner
                };
//                polyADF.put(id++,gf.createPolygon(coordinates));
                polyADF.put(id++, gf.createPolygon(coordinates).convexHull());
            }
        }

//        getPolyShape(gridADF);

        return polyADF;
    }

    // intent to write shape file
/*
    public void getPolygonShape(Polygon polygon) throws IOException {
//        SimpleFeatureType TYPE = dataStore.getSchema(typeName);
        File newFile = new File("output.shp");
        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put("url", URLs.fileToUrl(newFile));
        params.put("create spatial index", Boolean.TRUE);

        ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);


//        newDataStore.createSchema(TYPE);
        Transaction transaction = new DefaultTransaction("create");

        String typeName = newDataStore.getTypeNames()[0];
        SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

        if (featureSource instanceof SimpleFeatureStore) {
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

            featureStore.setTransaction(transaction);
            try {
                featureStore.addFeatures(collection);

                // Now add the hexagon
                featureStore.addFeatures(DataUtilities.collection(hexagon));
                transaction.commit();
            } catch (Exception problem) {
                problem.printStackTrace();
                transaction.rollback();
                System.exit(-1);
            } finally {
                transaction.close();
            }
        } else {
            System.out.println(typeName + " does not support read/write access");
            System.exit(1);
        }

    }
*/

    // second intent
//
//    public void getPolyShape(Map<Coord, Coord> gridADF) throws SchemaException, IOException {
//
//
//        /*
//         * We create a FeatureCollection into which we will put each Feature created from a record
//         * in the input csv data file
//         */
////        SimpleFeatureCollection collection = FeatureCollections.newCollection();
//        String tipoShape = "Polygon";
//        SimpleFeatureType featureType = DataUtilities.createType(tipoShape,
//                "the_geom:" + tipoShape + ":srid=31468," + "number:Integer");
//        DefaultFeatureCollection collection = new DefaultFeatureCollection("internal", featureType);
//
//        /*
//         * GeometryFactory will be used to create the geometry attribute of each feature (a Point
//         * object for the location)
//         */
//        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
//
//        int id = 1;
//        GeometryFactory gf = new GeometryFactory();
//        for(int r=0; r<rows; r++) {
//            for (int c = 0; c < cols; c++) {
//                Coordinate[] coordinates = new Coordinate[]{
//                        MGC.coord2Coordinate(gridADF.get(new Coord(r,c))),
//                        MGC.coord2Coordinate(gridADF.get(new Coord(r,c+1))),
//                        MGC.coord2Coordinate(gridADF.get(new Coord(r+1,c+1))),
//                        MGC.coord2Coordinate(gridADF.get(new Coord(r+1,c))),
//                        MGC.coord2Coordinate(gridADF.get(new Coord(r,c)))
//                };
//                Polygon polygon = gf.createPolygon(coordinates);
//                featureBuilder.add(polygon);
//                featureBuilder.add(id++);
//                SimpleFeature feature = featureBuilder.buildFeature(null);
//                collection.add(feature);
//            }
//        }
//
//        /*
//         * Get an output file name and create the new shapefile
//         */
//        File shpFile = new File("scenarios/berlin-v5.4-1pct/input/gridADF.shp");
//
//        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
//        Map<String, Serializable> params = new HashMap<>();
//        params.put("url", shpFile.toURI().toURL());
//        params.put("create spatial index", Boolean.TRUE);
//        ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
//        newDataStore.createSchema(featureType);
//
//        /*
//         * You can comment out this line if you are using the createFeatureType method (at end of
//         * class file) rather than DataUtilities.createType
//         */
////        newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);
//
//        //
//
//        /*
//         * Write the features to the shapefile
//         */
//        Transaction transaction = new DefaultTransaction("create");
//
//        String typeName = newDataStore.getTypeNames()[0];
//        SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
//
//        if (featureSource instanceof SimpleFeatureStore) {
//            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
//            featureStore.setTransaction(transaction);
//            featureStore.addFeatures(collection);
//            transaction.commit();
//            transaction.close();
//        } else {
//            System.out.println(typeName + " does not support read/write access");
//            System.exit(1);
//        }
//    }
//

    public void printGrid(Map<Coord, Coord> grid){
        BufferedWriter writer = null;
        System.out.println("### Print the grid! ###");

        try {
            // Set name of output file *csv, *txt, ... again from root folder
            File file = new File("scenarios/berlin-v5.4-1pct/input/gridADF.txt");

            FileWriter fileWriter = new FileWriter(file);
            writer = new BufferedWriter(fileWriter);

            System.out.println("row\tcol\tx_start\ty_start");
            writer.write("row\tcol\tx_start\ty_start");
            for(int r=0; r<=rows; r++){
                for(int c=0; c<=cols; c++){
                    writer.newLine();
                    System.out.print(r + "\t" + c + "\t");
                    writer.write(r + "\t" + c + "\t");
                    System.out.println(grid.get(new Coord(r,c)).getX() + "\t" + grid.get(new Coord(r,c)).getY());
                    writer.write(grid.get(new Coord(r,c)).getX() + "\t" + grid.get(new Coord(r,c)).getY());
                }
            }

        }catch (IOException e){
            e.printStackTrace();
        }

        try {
            if(writer!=null) {
                writer.close();
                System.out.println("### DONE! ###");
            }
        } catch (IOException ioException) {
            System.out.println("Error!");
        }
    }

    public void printGridLines(Map<String, Integer>[][] grid){
        BufferedWriter writer = null;

        System.out.println("### Print grid including lines (e.g. for Via) ###");

        try {
            // Set name of output file *csv, *txt, ... again from root folder
            File file = new File("gruppeB_TXSandCSV/grid4via.txt");

            FileWriter fileWriter = new FileWriter(file);
            writer = new BufferedWriter(fileWriter);
            writer.write("x_start\ty_start\tx_end\ty_end");
            writer.newLine();
            for(int r=0; r<rows; r++) {
                System.out.println("### horizontal line "+r+" ###");
                System.out.println(X_MIN + "\t" + grid[r][0].get("y_start") + "\t" + X_MAX + "\t" + grid[r][0].get("y_end"));
                writer.write(X_MIN + "\t" + grid[r][0].get("y_start") + "\t" + X_MAX + "\t" + grid[r][0].get("y_end"));
                writer.newLine();
            }
            for(int c=0; c<cols; c++){
                System.out.println("### vertical line "+c+" ###");
                System.out.println(grid[0][c].get("x_start")+"\t"+Y_MIN+"\t"+grid[0][c].get("x_end")+"\t"+Y_MAX);
                writer.write(grid[0][c].get("x_start")+"\t"+Y_MIN+"\t"+grid[0][c].get("x_end")+"\t"+Y_MAX);
                writer.newLine();
            }

        }catch (IOException e){
            e.printStackTrace();
        }

        try {
            if(writer!=null) {
                writer.close();
                System.out.println("### DONE! ###");
            }
        } catch (IOException ioException) {
            System.out.println("Error!");
        }
    }
}
