/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.run;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.analysis.GridCreator;
import org.matsim.analysis.RunAnalysis;
import org.matsim.analysis.RunComparison;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.modify.NetworkMod;

import java.io.IOException;
import java.util.*;

import static org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks;

/**
* @author ikaddoura
*/

public final class RunBerlinScenario {

	// Cases to look at:
	private static final String BASE = "base";
	private static final String DEGES = "deges";
	private static final String CITIZEN = "citiz";
	public static final String INDEX = "_t2"; // default: [depends on output folder, e.g. _t7] -berlin_5.4_10pct_300
	public static final String PRE = "funkturm_";	// default: funkturm_  ADF
	public static final String PCT = "1";	// default: 1	10
	public static final String ITER = "output_"; // default: output_   100.
	static String configFile = "/output/berlin-v5.4-"+ RunBerlinScenario.PCT+"pct.output_config.xml";

	// TRUE = Set task, correspondent order as String[] MODE
	private static final String[] MODE = new String[]{BASE, DEGES, CITIZEN};
	private static final boolean[] RUNSIM = new boolean[]{false, false, false};
	private static final boolean[] RUNANALYSIS = new boolean[]{true, true, true};
	private static final boolean[] RUNCOMPARE = new boolean[]{true, true, true}; // select at least 2 "true" to compare

//	private static final Integer[] areaADF = new Integer[]{115, 120, 122, 123, 130, 136, 138, 139, 140};
//	private static final Integer[] areaADF = new Integer[]{194, 164, 191, 189, 488, 178, 177, 176};
//	private static final String[] areaADF = new String[]{"04200311", "04200207", "04400725", "04400726", "04500937", "04300415", "04300414", "04300413"};
	// get map to save all results from analysis
	public static Map<String, List<Map<?,?>>> anaResultsMap;
	public static List<Map<?,?>> anaResultsList;

	// Store config, scneario, population, persons into a static map, to avoid reload the same twice or more times
	public static Map<String, Config> configs = new HashMap<>();
	public static Map<String, Scenario> scenarios = new HashMap<>();
	public static Map<String, Population> populations = new HashMap<>();
	public static Map<String, Collection<? extends Person>> persons = new HashMap<>();

	private static final Logger log = Logger.getLogger(RunBerlinScenario.class );

	public static void main(String[] args) throws IOException {

		/* Run section */
		for(int mm = 0; mm < MODE.length; mm++) {
			if (RUNSIM[mm]) {
				for (String arg : args) {
					log.info(arg);
				}

				if (args.length == 0) {
					// set 1pct or 10 pct
					args = new String[]{"scenarios/berlin-v5.4-"+PCT+"pct/input/berlin-v5.4-"+PCT+"pct.config.xml"};
				}

				Config config = prepareConfig(args);
				config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
				// set iterations
				config.controler().setLastIteration(1);
				config.controler().setOutputDirectory("./"+ PRE + MODE[mm] + INDEX + "/output");
				config.controler().setWriteEventsInterval(50);
				Scenario scenario = prepareScenario(config);

				if (!MODE[mm].equals(BASE)) {
					new NetworkMod(MODE[mm]).modify(scenario);

//					uncommented to build plans file correctly for export
//					for (Person pp : scenario.getPopulation().getPersons().values()) {
//						for (Plan plan : pp.getPlans()) {
//							PopulationUtils.resetRoutes(plan);
//						}
//					}
//					new PopulationWriter(scenario.getPopulation()).write("berlin-v5.4-plans-"+ MODE[mode] +".xml.gz");

					System.out.println("CHANGED PLANS!");
					config.network().setInputFile("berlin-v5.4-network-" + MODE[mm] + ".xml.gz");
					config.plans().setInputFile("berlin-v5.4-plans-"+ MODE[mm] +".xml.gz");
					scenario = prepareScenario(config);
				}

				Controler controler = prepareControler(scenario);
				controler.run();
			}
		}

		//workaround to avoid issues creating jar
		for (String arg : args) {
			log.info(arg);
		}

		if (args.length == 0) {
			// set 1pct or 10 pct
			args = new String[]{"/output/berlin-v5.4-"+ RunBerlinScenario.PCT+"pct.output_config.xml"};
		}

		// prepare polygons (replacing LOR shp due to issues)
		Map<Integer, Geometry> polyADF = new GridCreator().getPolyMap();

		/* Analysis section */
		for(int mm = 0; mm < MODE.length; mm++) {
			if (RUNANALYSIS[mm]) {
				System.out.println("\n\n###### Start analysis ["+MODE[mm]+"] ! ######");
				prepareFiles(mm);
				anaResultsList = new ArrayList<>();
//				RunAnalysis analysis = new RunAnalysis(PRE,MODE[mode] + INDEX, polyADF);
				RunAnalysis analysis = new RunAnalysis(MODE[mm], polyADF);

				analysis.exampleCounts(true);
				anaResultsList.add(analysis.residentDensity(true));
				anaResultsList.add(analysis.personsADF(true));
				anaResultsList.add(analysis.trafficCounts(true));
				anaResultsList.add(analysis.trafficPerResidentDensity(true));
//				analysis.writeOut(polyADF);
				analysis.writeToFile(polyADF, "gridADF", "csv");
				if(anaResultsMap==null){
					anaResultsMap = new HashMap<>();
				}
				anaResultsMap.put(MODE[mm],anaResultsList);
				System.out.println("\n\n###### End analysis ["+MODE[mm]+"] ! ######");
			}
		}

		/* Comparison section */
		for(int mm = 0; mm < MODE.length-1; mm++) {
			if (RUNCOMPARE[mm]) {
				if(RUNCOMPARE[mm +1]) {
					prepareFiles(mm);
					prepareFiles(mm +1);
					System.out.println("\n\n###### Compare ["+MODE[mm]+"] to ["+MODE[mm +1]+"] ! ######");
					RunComparison compare = new RunComparison(MODE[mm],MODE[mm +1],polyADF);
//					RunComparison compare = new RunComparison(PRE, MODE[mode] + INDEX, MODE[mode+1] + INDEX, polyADF);
					compare.runAllComparisons(true);
					compare.runAllComparisons(false);
				}
				if(RUNCOMPARE[MODE.length-1- mm]){
					if(!MODE[MODE.length - 1 - mm].equals(MODE[mm])){
						prepareFiles(mm);
						prepareFiles(MODE.length- mm -1);
						System.out.println("\n\n###### Compare ["+MODE[mm]+"] to ["+MODE[MODE.length- mm -1]+"] ! ######");
//						RunComparison compare = new RunComparison(PRE,MODE[mode] + INDEX, MODE[MODE.length-1-mode] + INDEX, polyADF);
						RunComparison compare = new RunComparison(MODE[mm],MODE[MODE.length- mm -1],polyADF);
						compare.runAllComparisons(true);
						compare.runAllComparisons(false);
					}
					else{
						break;
					}
				}

			}
		}
	}

	public static Controler prepareControler( Scenario scenario ) {
		// note that for something like signals, and presumably drt, one needs the controler object
		
		Gbl.assertNotNull(scenario);
		
		final Controler controler = new Controler( scenario );
		
		if (controler.getConfig().transit().isUsingTransitInMobsim()) {
			// use the sbb pt raptor router
			controler.addOverridingModule( new AbstractModule() {
				@Override
				public void install() {
					install( new SwissRailRaptorModule() );
				}
			} );
		} else {
			log.warn("Public transit will be teleported and not simulated in the mobsim! "
					+ "This will have a significant effect on pt-related parameters (travel times, modal split, and so on). "
					+ "Should only be used for testing or car-focused studies with a fixed modal split.  ");
		}
		
		// use the (congested) car travel time for the teleported ride mode
		controler.addOverridingModule( new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding( TransportMode.ride ).to( networkTravelTime() );
				addTravelDisutilityFactoryBinding( TransportMode.ride ).to( carTravelDisutilityFactoryKey() );

			}
		} );


		return controler;
	}
	
	public static Scenario prepareScenario( Config config ) {
		Gbl.assertNotNull( config );
		
		// note that the path for this is different when run from GUI (path of original config) vs.
		// when run from command line/IDE (java root).  :-(    See comment in method.  kai, jul'18
		// yy Does this comment still apply?  kai, jul'19

		final Scenario scenario = ScenarioUtils.loadScenario( config );

		return scenario;
	}
	
	public static Config prepareConfig( String [] args ) {
		OutputDirectoryLogging.catchLogEntries();
		
		String[] typedArgs = Arrays.copyOfRange( args, 1, args.length );

		final Config config = ConfigUtils.loadConfig( args[ 0 ] ); // I need this to set the context
		
		config.controler().setRoutingAlgorithmType( FastAStarLandmarks );

		//config of modeChoice

		config.subtourModeChoice().setProbaForRandomSingleTripMode( 0.5 );
		
		config.plansCalcRoute().setRoutingRandomness( 3. );
		config.plansCalcRoute().removeModeRoutingParams(TransportMode.ride);
		config.plansCalcRoute().removeModeRoutingParams(TransportMode.pt);
		config.plansCalcRoute().removeModeRoutingParams(TransportMode.bike);
		config.plansCalcRoute().removeModeRoutingParams("undefined");
	
		config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles( true );
				
		// vsp defaults
		config.vspExperimental().setVspDefaultsCheckingLevel( VspExperimentalConfigGroup.VspDefaultsCheckingLevel.info );
		config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.accessEgressModeToLink);
		config.qsim().setUsingTravelTimeCheckInTeleportation( true );
		config.qsim().setTrafficDynamics( TrafficDynamics.kinematicWaves );
				
		// activities:
		for ( long ii = 600 ; ii <= 97200; ii+=600 ) {
			config.planCalcScore().addActivityParams( new ActivityParams( "home_" + ii + ".0" ).setTypicalDuration( ii ) );
			config.planCalcScore().addActivityParams( new ActivityParams( "work_" + ii + ".0" ).setTypicalDuration( ii ).setOpeningTime(6. * 3600. ).setClosingTime(20. * 3600. ) );
			config.planCalcScore().addActivityParams( new ActivityParams( "leisure_" + ii + ".0" ).setTypicalDuration( ii ).setOpeningTime(9. * 3600. ).setClosingTime(27. * 3600. ) );
			config.planCalcScore().addActivityParams( new ActivityParams( "shopping_" + ii + ".0" ).setTypicalDuration( ii ).setOpeningTime(8. * 3600. ).setClosingTime(20. * 3600. ) );
			config.planCalcScore().addActivityParams( new ActivityParams( "other_" + ii + ".0" ).setTypicalDuration( ii ) );
		}
		config.planCalcScore().addActivityParams( new ActivityParams( "freight" ).setTypicalDuration( 12.*3600. ) );

		ConfigUtils.applyCommandline( config, typedArgs ) ;

		return config ;
	}

	static void prepareFiles(int mm){
		String mode = MODE[mm];
		if(configs.get(mode)==null){
			Config config = ConfigUtils.loadConfig(PRE+mode+INDEX+configFile);
			config.network().setInputFile("berlin-v5.4-"+ RunBerlinScenario.PCT+"pct.output_network.xml.gz");
			config.plans().setInputFile("berlin-v5.4-"+ RunBerlinScenario.PCT+"pct."+RunBerlinScenario.ITER+"plans.xml.gz");
			configs.put(mode, config);
			Scenario scenario = ScenarioUtils.loadScenario(config);
			scenarios.put(mode, scenario);
			Population population = scenario.getPopulation();
			populations.put(mode, population);
			Collection<? extends Person> allPersons = population.getPersons().values();
			persons.put(mode, allPersons);
		}
		else if(scenarios.get(mode)==null){
			configs.get(mode).network().setInputFile("berlin-v5.4-"+ RunBerlinScenario.PCT+"pct.output_network.xml.gz");
			configs.get(mode).plans().setInputFile("berlin-v5.4-"+ RunBerlinScenario.PCT+"pct."+RunBerlinScenario.ITER+"plans.xml.gz");
			Scenario scenario = ScenarioUtils.loadScenario(configs.get(mode));
			scenarios.put(mode, scenario);
			Population population = scenario.getPopulation();
			populations.put(mode, population);
			Collection<? extends Person> allPersons = population.getPersons().values();
			persons.put(mode, allPersons);
		}
		else if(populations.get(mode)==null){
			Population population = scenarios.get(mode).getPopulation();
			populations.put(mode, population);
			Collection<? extends Person> allPersons = population.getPersons().values();
			persons.put(mode, allPersons);
		}
		else {
			Collection<? extends Person> allPersons = populations.get(mode).getPersons().values();
			persons.put(mode, allPersons);
		}
	}

}

