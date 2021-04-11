/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.analysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.noise.NoiseOfflineCalculation;
import org.matsim.contrib.noise.ProcessNoiseImmissions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.Arrays;

import static org.matsim.run.RunBerlinScenario.*;

/**
 * 
 * @author ikaddoura
 *
 */
public class RunOfflineNoiseAnalysis {
	private static final Logger log = Logger.getLogger(RunOfflineNoiseAnalysis.class);

//	private final static String runDirectory = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/output-berlin-v5.4-1pct/";
//private final static String runDirectory = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/output-berlin-v5.4-1pct/";
	private final static String runId = "berlin-v5.4-1pct";

	String selectDefault;
	String lookupDir;
	GridCreator grid;

	public RunOfflineNoiseAnalysis(String mode, GridCreator inputGrid){
		selectDefault = mode;
		lookupDir = PRE+selectDefault+INDEX+"/output/";
		grid = inputGrid;
	}

	public void calculate() throws IOException, SAXException, XPathExpressionException {

//		String outputDirectory = "./scenarios/";
		String outputDirectory = lookupDir;

		String tunnelLinkIdFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-10pct/input/berlin-v5.1.tunnel-linkIDs.csv";
		double receiverPointGap = 100.;
		double timeBinSize = 3600.;


//		Network modNetwork = NetworkUtils.createNetwork(configs.get(selectDefault));
//		Network modNetwork = NetworkUtils.readNetwork(lookupDir+"/berlin-v5.4-"+ RunBerlinScenario.PCT+"pct.output_network.xml.gz");
//		NetworkWriter networkWriter = new NetworkWriter(modNetwork);
//		networkWriter.write(lookupDir+"/networkTestCRS.xml");

//		Config config = ConfigUtils.createConfig(new NoiseConfigGroup());
		Config config = ConfigUtils.loadConfig(configs.get(selectDefault).getContext());
		Config config2 = configs.get(selectDefault);
//		config.global().setCoordinateSystem("EPSG:31468");

////		config.network().setInputFile(runDirectory + runId + ".output_network.xml.gz");
//		config.network().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz");
//		config.network().setInputFile(lookupDir+"/berlin-v5.4-"+ RunBerlinScenario.PCT+"pct.output_network.xml.gz");
//		config.network().setInputFile(lookupDir+"networkTestCRS.xml");
//		config.network().setInputCRS("EPSG:31468");
////		config.plans().setInputFile(runDirectory + runId + ".output_plans.xml.gz");
//		config.plans().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-1pct/input/berlin-v5.5-1pct.plans.xml.gz");

//		config.plans().setInputFile(lookupDir+"berlin-v5.4-"+ RunBerlinScenario.PCT+"pct."+RunBerlinScenario.ITER+"plans.xml.gz");
//		config.plans().setInputCRS("EPSG:31468");
//		config.controler().setOutputDirectory(runDirectory);
		config.controler().setOutputDirectory("./"+ lookupDir);
//		config.controler().setRunId(runId+"_"+selectDefault);
//*/
		// adjust the default noise parameters

//		config.addModule(new NoiseConfigGroup());

//		config.global().setCoordinateSystem("EPSG:31468");
/*
		NoiseConfigGroup noiseParameters = (NoiseConfigGroup) config.getModules().get(NoiseConfigGroup.GROUP_NAME);
		noiseParameters.setReceiverPointGap(receiverPointGap);
/*
//		double xMin = 4573258.;
//		double yMin = 5801225.;
//		double xMax = 4620323.;
//		double yMax = 5839639.;
		double xMin = grid.X_MIN;
		double yMin = grid.Y_MIN;
		double xMax = grid.X_MAX;
		double yMax = grid.Y_MAX;

		noiseParameters.setReceiverPointsGridMinX(xMin);
		noiseParameters.setReceiverPointsGridMinY(yMin);
		noiseParameters.setReceiverPointsGridMaxX(xMax);
		noiseParameters.setReceiverPointsGridMaxY(yMax);
///*
		String[] consideredActivitiesForDamages = {"home*", "work*", "leisure*", "shopping*", "other*"};
		noiseParameters.setConsideredActivitiesForDamageCalculationArray(consideredActivitiesForDamages);

		// ################################

		noiseParameters.setUseActualSpeedLevel(false);
		noiseParameters.setAllowForSpeedsOutsideTheValidRange(false);
		noiseParameters.setScaleFactor(100.);
		noiseParameters.setComputePopulationUnits(true);
		noiseParameters.setComputeNoiseDamages(true);
		noiseParameters.setInternalizeNoiseDamages(false);
		noiseParameters.setComputeCausingAgents(false);
		noiseParameters.setThrowNoiseEventsAffected(true);
		noiseParameters.setThrowNoiseEventsCaused(false);

		String[] hgvIdPrefixes = { "freight" };
		noiseParameters.setHgvIdPrefixesArray(hgvIdPrefixes);

		noiseParameters.setTunnelLinkIdFile(tunnelLinkIdFile);
		noiseParameters.setTimeBinSizeNoiseComputation(timeBinSize);

		noiseParameters.setConsiderNoiseBarriers(true);
//		noiseParameters.setConsiderNoiseBarriers("/Users/ihab/Documents/workspace/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-buildings/osm-buildings-dissolved.geojson");
		noiseParameters.setNoiseBarriersFilePath("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-buildings/osm-buildings-dissolved.geojson");
		noiseParameters.setReceiverPointsCSVFileCoordinateSystem("EPSG:31468");
*/

//		((NetworkImpl)((MutableScenario)((ScenarioLoaderImpl)this).scenario).network).attributes.getAttribute("coordinateReferenceSystem");
//		((NetworkReaderMatsimV2)reader.delegate).currentAttributes;
//		((AttributesXmlReaderDelegate)((NetworkReaderMatsimV2)reader.delegate).attributesDelegate).currentAttributes;
//		((NetworkImpl)reader.network).attributes;
//		((NetworkImpl)((NetworkFactoryImpl)((NetworkImpl)this.network).factory).network).attributes;


//		DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
//		DocumentBuilder b = null;
//		Document doc;
//		try {
//			b = f.newDocumentBuilder();
//		} catch (ParserConfigurationException e) {
//			e.printStackTrace();
//		}
//
//		doc = b.parse(new File(config.network().getInputFile()));
//
//
//		XPath xPath = XPathFactory.newInstance().newXPath();
//		Node crsNode;
//		crsNode = (Node) xPath.compile("/attributes/coordinateReferenceSystem").evaluate(doc, XPathConstants.NODE);
//
//		crsNode.setTextContent("EPSG:31468");
//
//		Transformer tf=null;
//		try {
//			tf = TransformerFactory.newInstance().newTransformer();
//		} catch (TransformerConfigurationException e) {
//			e.printStackTrace();
//		}
//		tf.setOutputProperty(OutputKeys.INDENT, "yes");
//		tf.setOutputProperty(OutputKeys.METHOD, "xml");
//		tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
//
//		DOMSource domSource = new DOMSource(doc);
//		StreamResult sr = new StreamResult(new File(config.network().getInputFile()));
//		try {
//			tf.transform(domSource, sr);
//		} catch (TransformerException e) {
//			e.printStackTrace();
//		}

//		System.out.println(config.network().getInputCRS());


		Scenario scenario = ScenarioUtils.loadScenario(config);
//		Scenario scenaro = ScenarioUtils.

		scenario.getConfig().controler().setRunId("/"+runId);

		System.out.println("STEP");
		NoiseOfflineCalculation noiseCalculation = new NoiseOfflineCalculation(scenario, outputDirectory);
		try {
			noiseCalculation.run();
		} catch (NullPointerException e) {
			log.warn(e+"\n######\n"+Arrays.toString(e.getStackTrace()));
		}

		
		// some processing of the output data
		String outputFilePath = outputDirectory + "noise-analysis/";
//		ProcessNoiseImmissions process = new ProcessNoiseImmissions(outputFilePath + "immissions/", outputFilePath + "receiverPoints/receiverPoints.csv", noiseParameters.getReceiverPointGap());
		ProcessNoiseImmissions process = new ProcessNoiseImmissions(outputFilePath + "immissions/", outputFilePath + "receiverPoints/receiverPoints.csv", receiverPointGap);
		try {
			process.run();
		} catch (NullPointerException e) {
			log.warn(e+"\n######\n"+Arrays.toString(e.getStackTrace()));
		}
				
		final String[] labels = { "damages_receiverPoint" };
		final String[] workingDirectories = { outputFilePath + "/damages_receiverPoint/" };

		MergeNoiseCSVFile merger = new MergeNoiseCSVFile() ;
		merger.setReceiverPointsFile(outputFilePath + "receiverPoints/receiverPoints.csv");
		merger.setOutputDirectory(outputFilePath);
//		merger.setTimeBinSize(noiseParameters.getTimeBinSizeNoiseComputation());
		merger.setTimeBinSize(timeBinSize);
		merger.setWorkingDirectory(workingDirectories);
		merger.setLabel(labels);
		merger.run();
		
		log.info("Done.");
	}
}
		

