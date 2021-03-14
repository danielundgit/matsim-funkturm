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
import org.geotools.feature.SchemaException;
import org.locationtech.jts.geom.Geometry;
import org.matsim.analysis.GridCreator;
import org.matsim.analysis.RunAnalysis;
import org.matsim.analysis.RunComparison;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
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
import java.util.Arrays;
import java.util.Map;

import static org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks;

/**
* @author ikaddoura
*/

public final class RunBerlinScenario {

	// Cases to look at:
	private static final String BASE = "base";
	private static final String DEGES = "deges";
	private static final String CITIZEN = "citiz";
	private static final String INDEX = "_t2";

	private static final String[] MODE = new String[]{BASE, DEGES, CITIZEN};
	private static final boolean[] RUNSIM = new boolean[]{false, false, false};
	private static final boolean[] RUNANALYSIS = new boolean[]{false, false, false};
	private static final boolean[] RUNCOMPARE = new boolean[]{true, true, false}; // select at least 2 "true" to compare

	private static final Integer[] areaADF = new Integer[]{115, 120, 122, 123, 130, 136, 138, 139, 140};
//	private static final Integer[] areaADF = new Integer[]{194, 164, 191, 189, 488, 178, 177, 176};
//	private static final String[] areaADF = new String[]{"04200311", "04200207", "04400725", "04400726", "04500937", "04300415", "04300414", "04300413"};

	private static final Logger log = Logger.getLogger(RunBerlinScenario.class );

	public static void main(String[] args) throws IOException, SchemaException {

		for(int mode = 0; mode < MODE.length; mode++) {
			if (RUNSIM[mode]) {
				for (String arg : args) {
					log.info(arg);
				}

				if (args.length == 0) {
					// set 1pct or 10 pct
					args = new String[]{"scenarios/berlin-v5.4-10pct/input/berlin-v5.4-10pct.config.xml"};
				}

				Config config = prepareConfig(args);
				config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
				// set iterations
				config.controler().setLastIteration(1);
				config.controler().setOutputDirectory("./funkturm_" + MODE[mode] + INDEX + "/output");
//				config.controler().setWriteEventsInterval(20);
				Scenario scenario = prepareScenario(config);

				if (MODE[mode] != BASE) {
					new NetworkMod(MODE[mode]).modify(scenario);

//					uncommented to build plans file correctly for export
//					for (Person pp : scenario.getPopulation().getPersons().values()) {
//						for (Plan plan : pp.getPlans()) {
//							PopulationUtils.resetRoutes(plan);
//						}
//					}
//					new PopulationWriter(scenario.getPopulation()).write("berlin-v5.4-plans-"+ MODE[mode] +".xml.gz");


					System.out.println("CHANGED PLANS!");
					config.network().setInputFile("berlin-v5.4-network-" + MODE[mode] + ".xml.gz");
					config.plans().setInputFile("berlin-v5.4-plans-"+ MODE[mode] +".xml.gz");
//					scenario = prepareScenario(config);
				}

				Controler controler = prepareControler(scenario);
//				new TimeAllocationMutatorReRoute().get();

//				controler.run();
			}
		}

		// prepare polygons (replacing LOR shp, having issues)
		Map<Integer, Geometry> polyADF = new GridCreator().getPolyMap();

		/** Analysis section */
		for(int mode = 0; mode < MODE.length; mode++) {
			if (RUNANALYSIS[mode]) {
				RunAnalysis analysis = new RunAnalysis(MODE[mode] + INDEX, polyADF);
				analysis.exampleCounts(true);
				analysis.getResidentDensity(true);
				analysis.getADFpersons(true);
				analysis.writeOut(polyADF);
				analysis.writeToFile(polyADF, "gridADF", "csv");
				System.out.println("\n\n###### Hallo! ######\n\n");
			}
		}

		/** Comparison section */
		for(int mode = 0; mode < MODE.length-1; mode++) {
			if (RUNCOMPARE[mode]) {
				if(RUNCOMPARE[mode+1]) {
					RunComparison compare = new RunComparison(MODE[mode] + INDEX, MODE[mode+1] + INDEX, polyADF);
					compare.runAllComparisons();
				}
				if(RUNCOMPARE[MODE.length-1-mode]){
					if(!MODE[MODE.length - 1 - mode].equals(MODE[mode])){
						RunComparison compare = new RunComparison(MODE[mode] + INDEX, MODE[MODE.length-1-mode] + INDEX, polyADF);
						compare.runAllComparisons();
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
		config.plansCalcRoute().setInsertingAccessEgressWalk( true );
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

}

