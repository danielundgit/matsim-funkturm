package org.matsim.analysis;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class RunFileComparison {
    private static BufferedWriter bufferedWriter;
    private static BufferedReader bufferedReader;

    private static String input1 = "funkturm_base_t7/output/base_t7-traffic-resident-relation.csv";       // ADFbase-berlin_5.4_10pct_300
    private static String input2 = "ADFbase-berlin_5.4_10pct_300/output/base-traffic-resident-relation.csv";
    private static String input3= "";
    private static String outputPath= "comparisons/funkturm_1_t7/all_traffic-counts";
    private static String type= ".csv";
    private static String regex= ",";
    private static int lookupvar = 2;

    public static void main(String[] args) throws IOException {

        String link = "131820";
        input1 = "scenarios/berlin-v5.4-10pct/input/count-files-to-compare/"+link+"-base.csv";       // ADFbase-berlin_5.4_10pct_300
        input2 = "scenarios/berlin-v5.4-10pct/input/count-files-to-compare/"+link+"-deges.csv";
        input3= "scenarios/berlin-v5.4-10pct/input/count-files-to-compare/"+link+"-citiz.csv";
        outputPath= "comparisons/ADF1-berlin_5.4_10pct_300/traffic-counts-"+link;

        Map<Integer, List<Double>> table = new HashMap<>();
        List<Double> line = new ArrayList<>();
        String[] input = new String[]{input1, input2, input3};
        String[] values;
        int max = 3;

        for(int ii=0; ii<3; ii++) {
            try {
                bufferedReader = new BufferedReader(new FileReader(input[ii]));
                String str;
                int ll = 0;
                bufferedReader.readLine();
                while ((str = bufferedReader.readLine()) != null) {
                    values = str.split(regex);
                    if(ii==0){
                        line = new ArrayList<>();
                        table.put(ll, line);
                    }
                    else{
                        line = table.get(ll);
                    }
                    line.add(Double.parseDouble(values[lookupvar]));
                    table.put(ll++, line);
                }
            } catch (FileNotFoundException e) {
                System.out.println("Read less than 3 files! " + e);
                max-=1;
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            }
        }


        try {
            FileWriter fileWriter = new FileWriter(outputPath + type);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write("zone;base;deges;citiz");
            bufferedWriter.newLine();
            for (int ll = 0; ll < table.size(); ll++) {
                bufferedWriter.write((ll + 1) + ";" + table.get(ll).get(0));
                for (int ii = 1; ii < max; ii++) {
                    bufferedWriter.write(";" + table.get(ll).get(ii));
                }
                bufferedWriter.newLine();
            }
            bufferedWriter.close();
        } catch (IndexOutOfBoundsException ee){
            System.out.println("Less than 3 files! " +ee);
        } catch (IOException ee) {
            throw new RuntimeException(ee);
        }


        try {
            FileWriter fileWriter = new FileWriter(outputPath + "_rel"+type);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write("zone;base;base/deges;base/citiz");
            bufferedWriter.newLine();
            for(int ll = 0; ll < table.size(); ll++) {
                bufferedWriter.write((ll+1)+";"+ table.get(ll).get(0));
                for (int ii = 1; ii < max; ii++) {
                    if (table.get(ll).get(0) != 0. && table.get(ll).get(ii) != 0.) {
                        double value = table.get(ll).get(0) / table.get(ll).get(ii);
                        bufferedWriter.write(";" + value);
                    } else {
                        bufferedWriter.write(";" + table.get(ll).get(ii));
                    }
                }
                bufferedWriter.newLine();
            }
            bufferedWriter.close();
        } catch (IndexOutOfBoundsException ee){
            System.out.println("Less than 3 files! " +ee);
        } catch (IOException ee){
            throw new RuntimeException(ee);
        }


        try {
            FileWriter fileWriter = new FileWriter(outputPath+"_diff"+type);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write("zone;base;base-deges;base-citiz"); bufferedWriter.newLine();
            for(int ll = 0; ll < table.size(); ll++) {
                bufferedWriter.write((ll+1)+";"+ table.get(ll).get(0));
                for (int ii = 1; ii < max; ii++) {
                    double value = table.get(ll).get(0) - table.get(ll).get(ii);
                    bufferedWriter.write(";" + value);
                }
                bufferedWriter.newLine();
            }
            bufferedWriter.close();
        } catch (IndexOutOfBoundsException ee){
            System.out.println("Less than 3 files! " +ee);
        } catch (IOException ee) {
            throw new RuntimeException(ee);
        }
    }
}

