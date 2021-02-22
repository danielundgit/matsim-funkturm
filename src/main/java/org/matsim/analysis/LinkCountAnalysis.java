package org.matsim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LinkCountAnalysis implements LinkEnterEventHandler {
    // in Datei schreiben
    private final BufferedWriter bufferedWriter;

    /** (1) Used for classic printResult */
    private final List<LinkEnterEvent> events = new ArrayList<>();
    private final double[] linkA = new double[30];
    private final double[] linkB = new double[30];

    /** (2) Used for chronological printResult, for each direction one column */
    private final Map<Double, Id<Vehicle>> Amap = new HashMap<>();
    private final Map<Double, Id<Vehicle>> Bmap = new HashMap<>();
    private final List<Double> getAtime = new ArrayList<>();
    private final List<Double> getBtime = new ArrayList<>();

    /** (3) Used for chronological printResult, for both directions one column */
    private final Map<Double, Id<Vehicle>> combimap = new HashMap<>();
    private final List<Double> combitimes = new ArrayList<>();
    private final List<String> direction = new ArrayList<>();
    int it_direction = 0;

    public LinkCountAnalysis(String outputfile){

        try {
            FileWriter fileWriter = new FileWriter(outputfile);
            bufferedWriter = new BufferedWriter(fileWriter);
        } catch(IOException ee){
            throw new RuntimeException(ee);
        }
    }

    private int getSlot(double time){
        return (int) time/3600;
    }

    @Override
    public void handleEvent(LinkEnterEvent enterKMA){

        /**
         Links **entering** KMA: From A: 54738 / From B: 97508
         Links **leaving** KMA:  From A: 126333 / From B: 99708
         */

        if(enterKMA.getLinkId().equals(Id.createLinkId("126333")) || enterKMA.getLinkId().equals(Id.createLinkId("99708"))){
            int slot = getSlot(enterKMA.getTime());
            combimap.put(enterKMA.getTime(), enterKMA.getVehicleId());
            combitimes.add(enterKMA.getTime());
            if(enterKMA.getLinkId().equals(Id.createLinkId("126333"))){
                this.linkA[slot]++;
                Amap.put(enterKMA.getTime(), enterKMA.getVehicleId());
                getAtime.add(enterKMA.getTime());

                direction.add(it_direction, "A");
            }
            else if (enterKMA.getLinkId().equals(Id.createLinkId("99708"))){
                this.linkB[slot]++;
                Bmap.put(enterKMA.getTime(), enterKMA.getVehicleId());
                getBtime.add(enterKMA.getTime());

                direction.add(it_direction, "B");
            }
            it_direction++;
            events.add(enterKMA);
        }

    }

    /** (1) */
    public void printResult() {
        try {
            bufferedWriter.write("Hour\tFrom A\tFrom B\tTotal:\t" + events.size());
            bufferedWriter.newLine();
            System.out.println("Hour\tFrom A\tFrom B\tTotal\n");
            for (int i = 0; i < 24; i++) {
                double A_volume = this.linkA[i];
                double B_volume = this.linkB[i];
                bufferedWriter.write(i+1 + "\t" + A_volume + "\t" + B_volume + "\t" + (A_volume+B_volume));
                bufferedWriter.newLine();
                //System.out.println("volume on link 6 from " + i + " to " + (i+1) + "o clock = " + volume);
                System.out.println(i+1 + "\t\t" + A_volume + "\t\t\t" + B_volume + "\t\t\t" + (A_volume+B_volume) + "\n");
            }
            bufferedWriter.close();
        } catch (IOException ee){
            throw new RuntimeException(ee);
        }
    }

    /** (2) */
    public void printResult_timesAndIds() {
        try {
            bufferedWriter.write("Time\tFrom A\tFrom B\tTotal:\t" + events.size());
            bufferedWriter.newLine();

            System.out.println("Time\t\tVehicle from A\t\tVehicle from B");

            for (int i = 0; i < (Amap.size()+Bmap.size()); i++) {
                double Atime = 30*3600, Btime =30*3600;
                boolean Acheck = false, Bcheck = false;
                //System.out.println(getAtime);
                //System.out.println(getBtime);
                if(i < Amap.size()) {
                    Atime = getAtime.get(i);
                    Acheck=true;
                }
                if (i < Bmap.size()) {
                    Btime = getBtime.get(i);
                    Bcheck=true;
                }

                //bufferedWriter.newLine();
                if(Acheck && Bcheck) {
                    if (Atime < Btime) {
                        bufferedWriter.write(Atime + "\t" + Amap.get(Atime));
                        System.out.println(Atime + "\t" + Amap.get(Atime) + "\n");
                    } else {
                        bufferedWriter.write(Btime + "\t\t\t\t\t\t" + Bmap.get(Btime));
                        System.out.println(Btime + "\t\t\t\t\t" + Bmap.get(Btime) + "\n");
                    }
                    bufferedWriter.newLine();
                }
                else if(Acheck){
                    bufferedWriter.write(String.valueOf(Atime) + Amap.get(Atime));
                    System.out.println(Atime + "\t\t" + Amap.get(Atime) + "\n");
                    bufferedWriter.newLine();
                }
                else if (Bcheck) {
                    bufferedWriter.write("\t\t\t\t\t\t\t\t" + Btime + Bmap.get(Btime));
                    System.out.println("\t\t\t\t\t\t\t" + Btime + "\t\t\t" + Bmap.get(Btime) + "\n");
                    bufferedWriter.newLine();
                }
                else{
                    continue;
                }

            }
            bufferedWriter.close();
        } catch (IOException ee){
            throw new RuntimeException(ee);
        }
    }

    /** (3) */
    public void printResult_timesAndIds_combi() {
        try {
            bufferedWriter.write("Time\t\tVehicleID\t\t\t\t\tFrom:\t\tTotal:\t" + events.size());
            bufferedWriter.newLine();

            System.out.println("Time\t\tVehicleID\t\t\t\t\tFrom:\t\tTotal:\t" + events.size());

            for (int i = 0; i < (combimap.size()); i++) {
                bufferedWriter.write(combitimes.get(i) + "\t\t" + combimap.get(combitimes.get(i)) + "\t\t" + direction.get(i));
                System.out.println(combitimes.get(i) + "\t\t" + combimap.get(combitimes.get(i)) + "\t\t" + direction.get(i));
                bufferedWriter.newLine();
            }
            bufferedWriter.close();
        } catch (IOException ee){
            throw new RuntimeException(ee);
        }
    }

    public List<LinkEnterEvent> writeListOfEvents() {
        return this.events;
    }

    public Map<Double, Id<Vehicle>> gettimevehiclemap(){
        return combimap;
    }

    public List<Double> getCombitimes(){
        return combitimes;
    }
}
