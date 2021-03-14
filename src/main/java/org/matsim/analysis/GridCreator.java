package org.matsim.analysis;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
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

    public GridCreator(){
        createGrid();
    }
    public GridCreator(int rows, int columns){
        this.rows = rows;
        this.cols = columns;
        createGrid();
    }
    public GridCreator(int length){
        rows = length;
        cols = length;
        createGrid();
    }

    int width = X_MAX-X_MIN;
    int height = Y_MAX-Y_MIN;
    Map<Coord, Coord> cells = null;
    Map<Integer, Polygon> polyMap = null;

    public void createGrid(){

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
        printGrid(cells);
        polyMap = getPolygons(cells);

        return;
    }

    public Map<Coord, Coord> getCoordMap(){
        return cells;
    }

    public Map<Integer, Polygon> getPolyMap(){
        return polyMap;
    }

    public Map<Integer, Polygon> getPolygons(Map<Coord,Coord> gridADF){
        Map<Integer, Polygon> polyADF = new HashMap<>();
        int id = 1;
        GeometryFactory gf = new GeometryFactory();
        Coordinate[] coordinates = new Coordinate[5];
        for(int r=0; r<rows; r++) {
            for (int c = 0; c < cols; c++) {
                coordinates = new Coordinate[]{
                        MGC.coord2Coordinate(gridADF.get(new Coord(r,c))),
                        MGC.coord2Coordinate(gridADF.get(new Coord(r,c+1))),
                        MGC.coord2Coordinate(gridADF.get(new Coord(r+1,c+1))),
                        MGC.coord2Coordinate(gridADF.get(new Coord(r+1,c))),
                        MGC.coord2Coordinate(gridADF.get(new Coord(r,c)))
                };
                polyADF.put(id++,gf.createPolygon(coordinates));

            }
        }
        return polyADF;
    }

    public void printGrid(Map<Coord, Coord> grid){
        BufferedWriter writer = null;
        System.out.println("### Print the grid! ###");

        try {
            // Set name of output file *csv, *txt, ... again from root folder
            File file = new File("scenarios/berlin-v5.4-1pct/input/gridADF.txt");
            if(!file.exists()){
                file.createNewFile();
            }

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

    public void printGridLines_via(Map<String, Integer>[][] grid){
        // Set URL or local Path as inputFile (from root folder, usually "Gruppe_B")

        BufferedWriter writer = null;

        System.out.println("### Try to print the grid for VIA! ###");

        try {
            // Set name of output file *csv, *txt, ... again from root folder
            File file = new File("gruppeB_TXSandCSV/grid4via.txt");
            if(!file.exists()){
                file.createNewFile();
            }

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
