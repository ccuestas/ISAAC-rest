/**
 * Copyright Notice
 *
 * This is a work of the U.S. Government and is not subject to copyright
 * protection in the United States. Foreign copyrights may apply.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.rest.testng;

import static gov.vha.isaac.ochre.api.constants.Constants.DATA_STORE_ROOT_LOCATION_PROPERTY;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.jersey.test.JerseyTestNg;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.vha.isaac.MetaData;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronologyType;
import gov.vha.isaac.ochre.api.commit.CommitService;
import gov.vha.isaac.ochre.api.constants.DynamicSememeConstants;
import gov.vha.isaac.ochre.api.coordinate.PremiseType;
import gov.vha.isaac.ochre.api.coordinate.StampPrecedence;
import gov.vha.isaac.ochre.api.coordinate.TaxonomyCoordinate;
import gov.vha.isaac.ochre.api.externalizable.BinaryDataReaderService;
import gov.vha.isaac.ochre.api.index.IndexServiceBI;
import gov.vha.isaac.ochre.api.logic.NodeSemantic;
import gov.vha.isaac.ochre.model.configuration.LanguageCoordinates;
import gov.vha.isaac.ochre.model.configuration.LogicCoordinates;
import gov.vha.isaac.ochre.model.configuration.StampCoordinates;
import gov.vha.isaac.ochre.model.configuration.TaxonomyCoordinates;
import gov.vha.isaac.rest.ApplicationConfig;
import gov.vha.isaac.rest.ExpandUtil;
import gov.vha.isaac.rest.LocalJettyRunner;
import gov.vha.isaac.rest.api1.RestPaths;
import gov.vha.isaac.rest.api1.data.RestCoordinatesToken;
import gov.vha.isaac.rest.api1.data.RestSystemInfo;
import gov.vha.isaac.rest.api1.data.coordinate.RestTaxonomyCoordinate;
import gov.vha.isaac.rest.api1.data.enumerations.RestObjectChronologyType;
import gov.vha.isaac.rest.api1.data.sememe.RestSememeLogicGraphVersion;
import gov.vha.isaac.rest.api1.data.sememe.RestSememeVersions;
import gov.vha.isaac.rest.api1.data.systeminfo.RestIdentifiedObjectsResult;
import gov.vha.isaac.rest.session.RequestParameters;
import gov.vha.isaac.rest.tokens.CoordinatesToken;
import gov.vha.isaac.rest.tokens.CoordinatesTokens;

/**
 * {@link RestTest}
 * Testing framework for doing full cycle testing - this launches the REST server in a grizzly container, and makes REST requests via a loop
 * back call.
 * 
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class RestTest extends JerseyTestNg.ContainerPerClassTest
{
	private final static String taxonomyCoordinateRequestPath = RestPaths.coordinateAPIsPathComponent + RestPaths.taxonomyCoordinatePathComponent;
	private final static String stampCoordinateRequestPath = RestPaths.coordinateAPIsPathComponent + RestPaths.stampCoordinatePathComponent;
	private final static String languageCoordinateRequestPath = RestPaths.coordinateAPIsPathComponent + RestPaths.languageCoordinatePathComponent;
	private final static String logicCoordinateRequestPath = RestPaths.coordinateAPIsPathComponent + RestPaths.logicCoordinatePathComponent;
	private final static String descriptionSearchRequestPath = RestPaths.searchAPIsPathComponent + RestPaths.descriptionsComponent;
	private final static String taxonomyRequestPath = RestPaths.taxonomyAPIsPathComponent + RestPaths.versionComponent;

	private final static String sememeSearchRequestPath = RestPaths.searchAPIsPathComponent + RestPaths.sememesComponent;
	private final static String prefixSearchRequestPath = RestPaths.searchAPIsPathComponent + RestPaths.prefixComponent;
	private final static String byRefSearchRequestPath = RestPaths.searchAPIsPathComponent + RestPaths.byReferencedComponentComponent;

	private final static String sememeSearchrequestPath = RestPaths.searchAPIsPathComponent + RestPaths.sememesComponent;

	private final static String conceptDescriptionsRequestPath = RestPaths.conceptAPIsPathComponent +  RestPaths.descriptionsComponent;
	private final static String conceptVersionRequestPath = RestPaths.conceptAPIsPathComponent +  RestPaths.versionComponent;

	private static final String coordinatesTokenRequestPath = RestPaths.coordinateAPIsPathComponent + RestPaths.coordinatesTokenComponent;

	private final static String sememeByAssemblageRequestPath = RestPaths.sememeAPIsPathComponent + RestPaths.byAssemblageComponent;

	private final static String sememeByReferencedComponentRequestPath = RestPaths.sememeAPIsPathComponent + RestPaths.byReferencedComponentComponent;

	@Override
	protected Application configure()
	{
		try
		{
			System.out.println("Launching Jersey within Grizzley for tests");
			new File("target/test.data").mkdirs();
			System.setProperty(DATA_STORE_ROOT_LOCATION_PROPERTY, "target/test.data");
			return LocalJettyRunner.configureJerseyServer();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	@BeforeClass
	public void testDataLoad()
	{
		//Load in the test data
		try
		{
			while (!ApplicationConfig.getInstance().isIsaacReady())
			{
				Thread.sleep(50);
			}

			BinaryDataReaderService reader = Get.binaryDataReader(Paths.get("target", "data", "IsaacMetadataAuxiliary.ibdf"));
			CommitService commitService = Get.commitService();
			reader.getStream().forEach((object) -> {
				commitService.importNoChecks(object);
			});

			Get.startIndexTask((Class<IndexServiceBI>[])null).get();
		}
		catch (FileNotFoundException | InterruptedException | ExecutionException e)
		{
			Assert.fail("Test data file not found", e);
		}
		Assert.assertTrue(Get.conceptDescriptionText(MetaData.ASSEMBLAGE.getConceptSequence()).equals("assemblage (ISAAC)"));
	}

	@Test
	public void testMe()
	{
		//TODO write many more interesting tests, using some sort of pattern like this....
		Response response = target(conceptVersionRequestPath +
				DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION.getPrimordialUuid()).request().get();

		checkFail(response);

		//System.out.println(response.readEntity(String.class));
	}

	private Response checkFail(Response response)
	{
		if (response.getStatus() != Status.OK.getStatusCode())
		{
			Assert.fail("Response code " + response.getStatus() + " - " + Status.fromStatusCode(response.getStatus())
			+ response.readEntity(String.class));
		}
		return response;
	}
	private WebTarget target(String url, Map<String, Object> testParameters)
	{
		WebTarget target = target(url);
		if (testParameters != null) {
			for (Map.Entry<String, Object> testParameter : testParameters.entrySet()) {
				target = target.queryParam(testParameter.getKey(), testParameter.getValue());
			}
		}

		return target;
	}

	private static Map<String, Object> buildParams(@SuppressWarnings("unchecked") Map.Entry<String, Object>...parameters)
	{
		Map<String, Object> map = new HashMap<>();
		for (Map.Entry<String, Object> testParameter : parameters) {
			map.put(testParameter.getKey(), testParameter.getValue());
		}

		return map;
	}
	private static Map.Entry<String, Object> param(String key, Object value) {
		Map<String, Object> map = new HashMap<>();
		map.put(key,  value);
		return map.entrySet().iterator().next();
	}

	/**
	 * This test validates that the XML serializers, sememe by-assemblage API and pagination are working correctly 
	 */
	@Test
	public void testPaginatedSememesByAssemblage()
	{
		String xpathExpr = "/restSememeVersions/results/sememeChronology/sememeSequence";

		// Test to confirm that requested maxPageSize of results returned
		for (int pageSize : new int[] { 1, 5, 10 }) {
			Response response = target(
					sememeByAssemblageRequestPath + DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION.getPrimordialUuid())
					.queryParam(RequestParameters.expand, "chronology")
					.queryParam(RequestParameters.maxPageSize, pageSize)
					.request()
					.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get();

			String resultXmlString = checkFail(response).readEntity(String.class);

			NodeList nodeList = XMLUtils.getNodeList(resultXmlString, xpathExpr);

			Assert.assertTrue(nodeList != null && nodeList.getLength() == pageSize);
		}

		// Test to confirm that pageNum works
		{
			// Get first page of 10 results
			Response response = target(
					sememeByAssemblageRequestPath + DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION.getPrimordialUuid())
					.queryParam(RequestParameters.expand, "chronology")
					.queryParam(RequestParameters.maxPageSize, 10)
					.request()
					.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get();
			String resultXmlString = checkFail(response).readEntity(String.class);
			NodeList nodeList = XMLUtils.getNodeList(resultXmlString, xpathExpr);
			String idOfTenthResultOfFirstTenResultPage = nodeList.item(9).getTextContent();

			// Get 10th page of 1 result
			response = target(
					sememeByAssemblageRequestPath + DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION.getPrimordialUuid())
					.queryParam(RequestParameters.expand, "chronology")
					.queryParam(RequestParameters.pageNum, 10)
					.queryParam(RequestParameters.maxPageSize, 1)
					.request()
					.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get();

			resultXmlString = checkFail(response).readEntity(String.class);
			nodeList = XMLUtils.getNodeList(resultXmlString, xpathExpr);
			String idOfOnlyResultOfTenthResultPage = nodeList.item(0).getTextContent();

			Assert.assertTrue(idOfTenthResultOfFirstTenResultPage.equals(idOfOnlyResultOfTenthResultPage));
		}
	}

	/**
	 * This test validates that the XML serializers, description search API and pagination are working correctly 
	 */
	@Test
	public void testPaginatedSearchResults()
	{
		String xpathExpr = "/restSearchResults/results/matchNid";

		// Test to confirm that requested maxPageSize of results returned
		for (int pageSize : new int[] { 2, 3, 8 }) {
			String resultXmlString = checkFail(target(descriptionSearchRequestPath)
					.queryParam(RequestParameters.query,"dynamic*")
					.queryParam(RequestParameters.expand, "uuid")
					.queryParam(RequestParameters.maxPageSize, pageSize)
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);

			NodeList nodeList = XMLUtils.getNodeList(resultXmlString, xpathExpr);

			Assert.assertTrue(nodeList != null && nodeList.getLength() == pageSize);
		}

		// Test to confirm that pageNum works
		{
			// Get first page of 7 results
			String resultXmlString = checkFail(target(descriptionSearchRequestPath)
					.queryParam(RequestParameters.query,"dynamic*")
					.queryParam(RequestParameters.expand, "uuid")
					.queryParam(RequestParameters.maxPageSize, 7)
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);
			NodeList nodeList = XMLUtils.getNodeList(resultXmlString, xpathExpr);
			String idOf7thResultOfFirst7ResultPage = nodeList.item(6).getTextContent();

			// Get 7th page of 1 result
			resultXmlString = checkFail(target(descriptionSearchRequestPath)
					.queryParam(RequestParameters.query,"dynamic*")
					.queryParam(RequestParameters.expand, "uuid")
					.queryParam(RequestParameters.pageNum, 7)
					.queryParam(RequestParameters.maxPageSize, 1)
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);
			nodeList = XMLUtils.getNodeList(resultXmlString, xpathExpr);
			String idOfOnlyResultOf7thResultPage = nodeList.item(0).getTextContent();

			Assert.assertTrue(idOf7thResultOfFirst7ResultPage.equals(idOfOnlyResultOf7thResultPage));
		}
	}

	/**
	 * This test validates that both the JSON and XML serializers are working correctly with returns that contain
	 * nested array data, and various implementation types of the dynamic sememe types.
	 */
	@Test
	public void testArraySememeReturn()
	{
		Response response = target(sememeByAssemblageRequestPath +
				DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION.getPrimordialUuid()).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get();

		checkFail(response);

		response = target(sememeByAssemblageRequestPath +
				DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION.getPrimordialUuid()).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_JSON).get();

		checkFail(response);
	}

	@Test
	public void testReferencedDetailsExpansion()
	{
		Response response = target(sememeByReferencedComponentRequestPath +
				DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION.getPrimordialUuid()).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get();

		String result = checkFail(response).readEntity(String.class);

		Assert.assertFalse(result.contains("<conceptDescription>preferred (ISAAC)</conceptDescription>"));
		Assert.assertFalse(result.contains("</referencedComponentNidObjectType>"));
		Assert.assertFalse(result.contains("<referencedComponentNidDescription>dynamic sememe extension definition (ISAAC)</referencedComponentNidDescription>"));

		response = target(sememeByReferencedComponentRequestPath +
				DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION.getPrimordialUuid())
				.queryParam("expand", "chronology,referencedDetails,nestedSememes")
				.queryParam("includeDescriptions", "true")
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get();

		result = checkFail(response).readEntity(String.class);

		Assert.assertTrue(result.contains("<conceptDescription>preferred (ISAAC)</conceptDescription>"));
		Assert.assertTrue(result.contains("</referencedComponentNidObjectType>"));
		Assert.assertTrue(result.contains("<referencedComponentNidDescription>dynamic sememe extension definition (ISAAC)</referencedComponentNidDescription>"));
	}


	/**
	 * This test validates that both the JSON and XML serializers are working correctly with returns that contain
	 * concept data.
	 */
	@Test
	public void testIdReturn()
	{
		final String url = RestPaths.idAPIsPathComponent + RestPaths.idTranslateComponent +
				DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION.getPrimordialUuid().toString();

		Response response = target(url).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get();

		checkFail(response);

		response = target(url).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_JSON).get();

		checkFail(response);
	}
	/**
	 * This test validates that both the JSON and XML serializers are working correctly with returns that contain
	 * concept data.
	 */
	@Test
	public void testConceptReturn()
	{
		final String url = conceptVersionRequestPath +
				DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION.getPrimordialUuid().toString();

		Response response = target(url).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get();

		checkFail(response);

		response = target(url).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_JSON).get();

		checkFail(response);
	}
	/**
	 * This test validates that both the JSON and XML serializers are working correctly with returns that contain
	 * LogicGraph data.
	 */
	@Test
	public void testLogicGraphReturn()
	{
		final String url = RestPaths.logicGraphAPIsPathComponent + RestPaths.versionComponent +
				DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION.getPrimordialUuid().toString();

		Response response = target(url).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get();

		checkFail(response);

		response = target(url).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_JSON).get();

		checkFail(response);
	}

	/**
	 * This test validates that the JSON serializer is working correctly with returns that contain
	 * LogicGraph data and validates that the returned Response JSON contains a valid RestSememeLogicGraphVersion
	 */
	@Test
	public void testRestSememeLogicGraphVersionReturn()
	{
		final String url = RestPaths.logicGraphAPIsPathComponent + RestPaths.versionComponent +
				DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION.getPrimordialUuid().toString();
		Response returnedResponse = target(url)
				//.queryParam(RequestParameters.expand,"version")
				.request().get();

		String output = returnedResponse.readEntity(String.class);

		ObjectMapper mapper = new ObjectMapper();

		JsonNode rootNode = null;
		try {
			//System.out.println("testRestSememeLogicGraphVersionReturn() parsing json " + output);

			rootNode = mapper.readValue(output, JsonNode.class);

			//System.out.println("testRestSememeLogicGraphVersionReturn() parsed json as " + rootNode.getNodeType() + "\n" + rootNode);
		} catch (IOException e) {
			Assert.fail("testRestSememeLogicGraphVersionReturn() FAILED parsing json.  Caught " + e.getClass().getName() + " " + e.getLocalizedMessage());
		}

		JsonNode rootType = rootNode.get("@class");

		if (rootType.isMissingNode()) {
			Assert.fail("testRestSememeLogicGraphVersionReturn() parsed json object missing @class member");
		} else if (! rootType.asText().equals(RestSememeLogicGraphVersion.class.getName())) {
			Assert.fail("testRestSememeLogicGraphVersionReturn() parsed json of unexpected object type " + rootType.asText());
		} else {
			//System.out.println("testRestSememeLogicGraphVersionReturn() parsed " + rootType.asText() + " object");
		}

		final String referencedConceptDescriptionFieldName = "referencedConceptDescription";
		final String referencedConceptDescriptionExpectedValue = "dynamic sememe extension definition (ISAAC)";
		if (! rootNode.has(referencedConceptDescriptionFieldName)) {
			Assert.fail("testRestSememeLogicGraphVersionReturn() parsed RestSememeLogicGraphVersion with no referencedConceptDescription");
		}
		if (rootNode.get(referencedConceptDescriptionFieldName) == null) {
			Assert.fail("testRestSememeLogicGraphVersionReturn() parsed RestSememeLogicGraphVersion with null referencedConceptDescription");
		}
		JsonNode referencedConceptDescriptionNode = rootNode.get(referencedConceptDescriptionFieldName);
		if (! referencedConceptDescriptionNode.asText().equals(referencedConceptDescriptionExpectedValue)) {
			Assert.fail("testRestSememeLogicGraphVersionReturn() parsed RestSememeLogicGraphVersion with unexpected referencedConceptDescription=\"" + referencedConceptDescriptionNode.asText() + "\"");
		}
		//System.out.println("testRestSememeLogicGraphVersionReturn() parsed RestSememeLogicGraphVersion with referencedConceptDescription of type " + referencedConceptDescriptionNode.getNodeType());

		final String rootLogicNodeFieldName = "rootLogicNode";		
		final String nodeSemanticNodeFieldName = "nodeSemantic";
		if (rootNode.with(rootLogicNodeFieldName).with(nodeSemanticNodeFieldName).get("name") == null || ! rootNode.with(rootLogicNodeFieldName).with(nodeSemanticNodeFieldName).get("name").asText().equals(NodeSemantic.DEFINITION_ROOT.name())) {
			Assert.fail("testRestSememeLogicGraphVersionReturn() parsed RestSememeLogicGraphVersion with missing or invalid " + rootLogicNodeFieldName + ": \"" + rootNode.with(rootLogicNodeFieldName).with(nodeSemanticNodeFieldName).get("name") + "\"!=\"" + NodeSemantic.DEFINITION_ROOT.name() + "\"");
		}
	}

	/**
	 * This test validates that both the JSON and XML serializers are working correctly with returns that contain
	 * taxonomy data.
	 */
	@Test
	public void testTaxonomyReturn()
	{

		Response response = target(taxonomyRequestPath).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get();
		//	System.out.println(target(taxonomyRequestUrl).request().get().toString());

		checkFail(response);

		response = target(taxonomyRequestPath).request()
				.header(Header.Accept.toString(), MediaType.APPLICATION_JSON).get();

		checkFail(response);
	}


	@Test
	public void testSearchAssemblageRestriction1()
	{
		//Check with UUID

		String result = checkFail(target(sememeSearchRequestPath)
				.queryParam(RequestParameters.treatAsString, "false")
				.queryParam(RequestParameters.query,"3")
				.queryParam(RequestParameters.sememeAssemblageId, DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION.getUUID().toString())
				.queryParam(RequestParameters.sememeAssemblageId, MetaData.AMT_MODULE.getNid() + "")
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
				.readEntity(String.class);

		Assert.assertTrue(result.contains(DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMN_DEFAULT_VALUE.getPrimordialUuid().toString()));

		//Check with nid
		result = checkFail(target(sememeSearchRequestPath)
				.queryParam(RequestParameters.treatAsString, "false")
				.queryParam(RequestParameters.query,"3")
				.queryParam(RequestParameters.sememeAssemblageId, DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION.getNid() + "")
				.queryParam(RequestParameters.sememeAssemblageId, MetaData.AMT_MODULE.getNid() + "")
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
				.readEntity(String.class);

		Assert.assertTrue(result.contains(DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMN_DEFAULT_VALUE.getPrimordialUuid().toString()));

		//Check with sequence
		//Check with nid
		result = checkFail(target(sememeSearchRequestPath)
				.queryParam(RequestParameters.treatAsString, "false")
				.queryParam(RequestParameters.query,"3")
				.queryParam(RequestParameters.sememeAssemblageId, DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION.getConceptSequence() + "")
				.queryParam(RequestParameters.sememeAssemblageId, MetaData.AMT_MODULE.getNid() + "")
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
				.readEntity(String.class);

		Assert.assertTrue(result.contains(DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMN_DEFAULT_VALUE.getPrimordialUuid().toString()));

		//sanity check search
		result = checkFail(target(sememeSearchRequestPath)
				.queryParam(RequestParameters.treatAsString, "false")
				.queryParam(RequestParameters.query,"55")
				.queryParam(RequestParameters.sememeAssemblageId, DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION.getNid() + "")
				.queryParam(RequestParameters.sememeAssemblageId, MetaData.AMT_MODULE.getNid() + "")
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
				.readEntity(String.class);

		Assert.assertFalse(result.contains(DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMN_DEFAULT_VALUE.getPrimordialUuid().toString()));
	}

	@Test
	public void testSearchExpandsUUID()
	{
		//Make sure it contains a random (type 4) UUID with this pattern...
		//<uuids>12604572-254c-49d2-8d9f-39d485af0fa0</uuids>
		final Pattern pXml = Pattern.compile(".*uuids.{9}-.{4}-4.{3}-.{4}-.{14}uuids.*", Pattern.DOTALL);

		//Test expand uuid on/off for each search type

		String result = checkFail(target(sememeSearchrequestPath)
				.queryParam(RequestParameters.treatAsString, "false")
				.queryParam(RequestParameters.query,"1")
				.queryParam(RequestParameters.expand, "uuid")
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
				.readEntity(String.class);
		Assert.assertTrue(pXml.matcher(result).matches());

		result = checkFail(target(sememeSearchrequestPath)
				.queryParam(RequestParameters.treatAsString, "false")
				.queryParam(RequestParameters.query,"1")
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
				.readEntity(String.class);
		Assert.assertFalse(pXml.matcher(result).matches());

		result = checkFail(target(descriptionSearchRequestPath)
				.queryParam(RequestParameters.query,"dynamic*")
				.queryParam(RequestParameters.expand, "uuid")
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
				.readEntity(String.class);
		Assert.assertTrue(pXml.matcher(result).matches());

		result = checkFail(target(descriptionSearchRequestPath)
				.queryParam(RequestParameters.query,"dynamic*")
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
				.readEntity(String.class);
		Assert.assertFalse(pXml.matcher(result).matches());

		result = checkFail(target(prefixSearchRequestPath)
				.queryParam(RequestParameters.query,"dynamic")
				.queryParam(RequestParameters.expand, "uuid")
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
				.readEntity(String.class);
		Assert.assertTrue(pXml.matcher(result).matches());

		result = checkFail(target(prefixSearchRequestPath)
				.queryParam(RequestParameters.query,"dynamic")
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
				.readEntity(String.class);
		Assert.assertFalse(pXml.matcher(result).matches());

		result = checkFail(target(byRefSearchRequestPath)
				.queryParam(RequestParameters.nid, MetaData.ISAAC_ROOT.getNid())
				.queryParam(RequestParameters.expand, "uuid")
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
				.readEntity(String.class);
		Assert.assertTrue(pXml.matcher(result).matches());

		result = checkFail(target(byRefSearchRequestPath)
				.queryParam(RequestParameters.nid, MetaData.ISAAC_ROOT.getNid())
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
				.readEntity(String.class);
		Assert.assertFalse(pXml.matcher(result).matches());

		//Spot check for JSON return support:
		//Make sure it contains a random (type 4) UUID with this pattern...
		// "uuids" : [ "bcf22234-a736-5f6b-9ce3-d016594ca5cd" ]
		final Pattern pJson = Pattern.compile(".*uuids.{15}-.{4}-4.{3}-.{4}-.{12}.*", Pattern.DOTALL);
		result = checkFail(target(prefixSearchRequestPath)
				.queryParam(RequestParameters.query,"dynamic")
				.queryParam(RequestParameters.expand, "uuid")
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_JSON).get())
				.readEntity(String.class);
		Assert.assertTrue(pJson.matcher(result).matches());

		result = checkFail(target(prefixSearchRequestPath)
				.queryParam(RequestParameters.query,"dynamic")
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_JSON).get())
				.readEntity(String.class);
		Assert.assertFalse(pJson.matcher(result).matches());
	}

	@Test
	public void testSearchExpandsRefConcept()
	{
		//Test expand uuid on/off for each search type

		String result = checkFail(target(sememeSearchRequestPath)
				.queryParam(RequestParameters.query, DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMN_NAME.getPrimordialUuid().toString())
				.queryParam("expand", ExpandUtil.referencedConcept)
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
				.readEntity(String.class);
		Assert.assertTrue(result.contains(DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION.getPrimordialUuid().toString()));

		result = checkFail(target(sememeSearchRequestPath)
				.queryParam(RequestParameters.query, DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMN_NAME.getPrimordialUuid().toString())
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
				.readEntity(String.class);
		Assert.assertFalse(result.contains(DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION.getPrimordialUuid().toString()));

		result = checkFail(target(descriptionSearchRequestPath)
				.queryParam(RequestParameters.query,"dynamic sememe Asse*")
				.queryParam(RequestParameters.expand, ExpandUtil.referencedConcept)
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
				.readEntity(String.class);
		Assert.assertTrue(result.contains(DynamicSememeConstants.get().DYNAMIC_SEMEME_ASSEMBLAGES.getPrimordialUuid().toString()));

		result = checkFail(target(descriptionSearchRequestPath)
				.queryParam(RequestParameters.query,"dynamic sememe Asse*")
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
				.readEntity(String.class);
		Assert.assertFalse(result.contains(DynamicSememeConstants.get().DYNAMIC_SEMEME_ASSEMBLAGES.getPrimordialUuid().toString()));

		result = checkFail(target(prefixSearchRequestPath)
				.queryParam(RequestParameters.query,"dynamic sememe Asse")
				.queryParam(RequestParameters.expand, ExpandUtil.referencedConcept)
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
				.readEntity(String.class);
		Assert.assertTrue(result.contains(DynamicSememeConstants.get().DYNAMIC_SEMEME_ASSEMBLAGES.getPrimordialUuid().toString()));

		result = checkFail(target(prefixSearchRequestPath)
				.queryParam(RequestParameters.query,"dynamic sememe Asse")
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
				.readEntity(String.class);
		Assert.assertFalse(result.contains(DynamicSememeConstants.get().DYNAMIC_SEMEME_ASSEMBLAGES.getPrimordialUuid().toString()));

		result = checkFail(target(byRefSearchRequestPath)
				.queryParam(RequestParameters.nid, MetaData.ISAAC_METADATA.getNid())
				.queryParam(RequestParameters.maxPageSize, "100")
				.queryParam(RequestParameters.expand, "uuid," + ExpandUtil.referencedConcept)
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_JSON).get())
				.readEntity(String.class);
		Assert.assertTrue(result.contains(MetaData.MODULE.getPrimordialUuid().toString()));

		result = checkFail(target(byRefSearchRequestPath)
				.queryParam(RequestParameters.nid, MetaData.ISAAC_METADATA.getNid())
				.queryParam(RequestParameters.maxPageSize, "100")
				.queryParam(RequestParameters.expand, "uuid")
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_JSON).get())
				.readEntity(String.class);
		Assert.assertFalse(result.contains(MetaData.MODULE.getPrimordialUuid().toString()));
	}

	@Test
	public void testSearchExpandsRefConceptVersion()
	{
		//Test expand uuid on/off for each search type

		String result = checkFail(target(descriptionSearchRequestPath)
				.queryParam(RequestParameters.query,"dynamic sememe Asse*")
				.queryParam(RequestParameters.expand, ExpandUtil.referencedConcept + "," + ExpandUtil.versionsLatestOnlyExpandable)
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
				.readEntity(String.class);
		Assert.assertTrue(result.contains("<state>ACTIVE</state>"));

		result = checkFail(target(descriptionSearchRequestPath)
				.queryParam(RequestParameters.query,"dynamic sememe Asse*")
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
				.readEntity(String.class);
		Assert.assertFalse(result.contains("<state>ACTIVE</state>"));
	}

	@Test
	public void testSearchRecursiveRefComponentLookup()
	{
		String result = checkFail(target(sememeSearchrequestPath)
				.queryParam(RequestParameters.query, MetaData.PREFERRED.getNid() + "")
				.queryParam(RequestParameters.treatAsString, "true")
				.queryParam(RequestParameters.maxPageSize, 500)
				.queryParam(RequestParameters.expand, ExpandUtil.referencedConcept)
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
				.readEntity(String.class);
		Assert.assertTrue(result.contains(DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION.getPrimordialUuid().toString()));

		result = checkFail(target(sememeSearchrequestPath)
				.queryParam(RequestParameters.query, MetaData.PREFERRED.getNid() + "")
				.queryParam(RequestParameters.treatAsString, "true")
				.queryParam(RequestParameters.maxPageSize, 500)
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
				.readEntity(String.class);
		Assert.assertFalse(result.contains(DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION.getPrimordialUuid().toString()));
	}

	@Test
	public void testDescriptionsFetch()
	{
		String result = checkFail(target(conceptDescriptionsRequestPath + MetaData.USER.getConceptSequence())
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
				.readEntity(String.class);

		String[] temp = result.split("<restSememeDescriptionVersion>");
		//[0] is header junk
		//[1] is the first dialect
		//[2] is the second dialect

		Assert.assertTrue(temp.length == 3);
		for (int i = 1; i < 3; i++)
		{
			String[] temp2 = temp[i].split("<dialects>");
			String preDialect = temp2[0];
			String dialect = temp2[1];
			Assert.assertEquals(temp2.length, 2);

			//Validate that the important bit of the description sememe are put together properly
			Assert.assertTrue(preDialect.contains("<assemblageSequence>" + MetaData.ENGLISH_DESCRIPTION_ASSEMBLAGE.getConceptSequence() + "</assemblageSequence>"), "Wrong language");
			Assert.assertTrue(preDialect.contains("<referencedComponentNid>" + MetaData.USER.getNid() + "</referencedComponentNid>"), "Wrong concept");
			Assert.assertTrue(preDialect.contains("<caseSignificanceConceptSequence>" + MetaData.DESCRIPTION_NOT_CASE_SENSITIVE.getConceptSequence() 
			+ "</caseSignificanceConceptSequence>"), "Wrong case sentivity");
			Assert.assertTrue(preDialect.contains("<languageConceptSequence>" + MetaData.ENGLISH_LANGUAGE.getConceptSequence() 
			+ "</languageConceptSequence>"), "Wrong language");
			Assert.assertTrue((preDialect.contains("<text>user</text>") || preDialect.contains("<text>user (ISAAC)</text>")), "Wrong text " + preDialect);
			Assert.assertTrue((preDialect.contains("<descriptionTypeConceptSequence>" + MetaData.SYNONYM.getConceptSequence() + "</descriptionTypeConceptSequence>") 
					|| preDialect.contains("<descriptionTypeConceptSequence>" + MetaData.FULLY_SPECIFIED_NAME.getConceptSequence() + "</descriptionTypeConceptSequence>")), 
					"Wrong description type");

			//validate that the dialect bits are put together properly
			Assert.assertTrue(dialect.contains("<assemblageSequence>" + MetaData.US_ENGLISH_DIALECT.getConceptSequence() + "</assemblageSequence>"), "Wrong dialect");
			Assert.assertTrue(dialect.contains("<data xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xsi:type=\"xs:int\">" + MetaData.PREFERRED.getNid() + "</data>"), "Wrong value");
		}
	}

	@Test
	public void testCoordinateTokenRoundTrip() throws Exception
	{
		TaxonomyCoordinate taxonomyCoordinate =
				TaxonomyCoordinates.getStatedTaxonomyCoordinate(
						StampCoordinates.getDevelopmentLatest(),
						LanguageCoordinates.getUsEnglishLanguagePreferredTermCoordinate(),
						LogicCoordinates.getStandardElProfile());
		CoordinatesToken t = CoordinatesTokens.getOrCreate(
				taxonomyCoordinate.getStampCoordinate(),
				taxonomyCoordinate.getLanguageCoordinate(),
				taxonomyCoordinate.getLogicCoordinate(),
				taxonomyCoordinate.getTaxonomyType()
				);

		String token = t.getSerialized();

		CoordinatesToken read = CoordinatesTokens.getOrCreate(token);
		Assert.assertTrue(token.equals(read.getSerialized()));
	}


	@Test
	public void testCoordinatesToken()
	{
		/*
		 * These tests display the following values on fail
		 * Each test should initialize or set-to-null each of the following values
		 */
		Map<String, Object> parameters = new HashMap<>();
		WebTarget target = null;
		String result = null;
		RestCoordinatesToken retrievedToken = null;
		CoordinatesToken defaultTokenObject = null;
		RestCoordinatesToken defaultToken = null;
		try {
			defaultToken = new RestCoordinatesToken(CoordinatesTokens.getDefaultCoordinatesToken());
			defaultTokenObject = CoordinatesTokens.getOrCreate(defaultToken.token);

			// Test no parameters against default token
			parameters.clear();
			result = checkFail((target = target(coordinatesTokenRequestPath))
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);
			retrievedToken = XMLUtils.unmarshalObject(RestCoordinatesToken.class, result);
			Assert.assertTrue(retrievedToken.equals(defaultToken));

			// Test default token passed as argument against default token
			result = checkFail((target = target(
					coordinatesTokenRequestPath,
					parameters = buildParams(param(RequestParameters.coordToken, defaultToken.token))))
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);
			retrievedToken = XMLUtils.unmarshalObject(RestCoordinatesToken.class, result);
			Assert.assertTrue(retrievedToken.equals(defaultToken));

			// Test default token passed as argument against default token
			result = checkFail((target = target(
					coordinatesTokenRequestPath,
					parameters = buildParams(param(RequestParameters.coordToken, defaultToken.token))))
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);
			retrievedToken = XMLUtils.unmarshalObject(RestCoordinatesToken.class, result);
			Assert.assertTrue(retrievedToken.equals(defaultToken));

			// Compare retrieved coordinates with default generated by default token
			parameters.clear();
			result = checkFail((target = target(taxonomyCoordinateRequestPath))
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);
			RestTaxonomyCoordinate retrievedTaxonomyCoordinate = XMLUtils.unmarshalObject(RestTaxonomyCoordinate.class, result);
			RestTaxonomyCoordinate defaultTaxonomyCoordinate = new RestTaxonomyCoordinate(defaultTokenObject.getTaxonomyCoordinate());
			try {
				Assert.assertTrue(retrievedTaxonomyCoordinate.equals(defaultTaxonomyCoordinate));
			} catch (Error e) {
				System.out.println(retrievedTaxonomyCoordinate);
				System.out.println(defaultTaxonomyCoordinate);
				throw e;
			}

			// Test passing a custom parameter
			// First ensure that the parameters we pass are not already in the default
			try {
				Assert.assertTrue(defaultTokenObject.getStampCoordinate().getStampPrecedence() != StampPrecedence.TIME);
				Assert.assertTrue(defaultTokenObject.getTaxonomyCoordinate().getTaxonomyType() != PremiseType.INFERRED);
			} catch (Error e) {
				System.out.println(defaultTokenObject.getLogicCoordinate());
				System.out.println(defaultTokenObject.getTaxonomyCoordinate());
				throw e;
			}

			result = checkFail((target = target(
					coordinatesTokenRequestPath,
					parameters = buildParams(
							param(RequestParameters.precedence, StampPrecedence.TIME.name()),
							param(RequestParameters.stated, "false"))))
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);
			retrievedToken = XMLUtils.unmarshalObject(RestCoordinatesToken.class, result);
			Assert.assertTrue(CoordinatesTokens.getOrCreate(retrievedToken.token).getStampPrecedence() == StampPrecedence.TIME);
			Assert.assertTrue(CoordinatesTokens.getOrCreate(retrievedToken.token).getTaxonomyType() == PremiseType.INFERRED);

			// Test using a customized token with getStampPrecedence() == StampPrecedence.TIME
			// and getTaxonomyType() == PremiseType.INFERRED
			result = checkFail((target = target(
					taxonomyCoordinateRequestPath,
					parameters = buildParams(param(RequestParameters.coordToken, retrievedToken.token))))
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);
			retrievedTaxonomyCoordinate = XMLUtils.unmarshalObject(RestTaxonomyCoordinate.class, result);
			Assert.assertTrue(retrievedTaxonomyCoordinate.stampCoordinate.precedence.getEnumId() == StampPrecedence.TIME.ordinal());
			Assert.assertTrue(retrievedTaxonomyCoordinate.stated == false);
		} catch (Throwable error) {
			System.out.println("Failing target: " + target);
			System.out.println("Failing parameters: " + parameters);
			System.out.println("Failing result XML: " + result);
			System.out.println("Failing retrievedToken: " + retrievedToken);
			System.out.println("Failing defaultToken: " + defaultToken);
			System.out.println("Failing defaultTokenObject: " + defaultTokenObject);
			throw new Error(error);
		}
	}

	@Test
	public void testGetCoordinates()
	{
		/*
		 * These tests display the following values on fail
		 * Each test should initialize or set-to-null each of the following values
		 */
		Map<String, Object> parameters = new HashMap<>();
		WebTarget target = null;
		String xpath = null;
		Node node = null;
		NodeList nodeList = null;
		String result = null;
		String requestUrl = null;
		try {
			// RestTaxonomyCoordinate
			boolean taxonomyCoordinateStated;
			result = checkFail(
					(target = target(
							requestUrl = taxonomyCoordinateRequestPath,
							parameters = buildParams(param(RequestParameters.stated, "false"))))
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);
			xpath = "/restTaxonomyCoordinate/stated";
			node = XMLUtils.getNodeFromXml(result, xpath);
			nodeList = null;
			taxonomyCoordinateStated = Boolean.valueOf(node.getTextContent());
			Assert.assertTrue(taxonomyCoordinateStated == false);

			result = checkFail(
					(target = target(
							requestUrl = taxonomyCoordinateRequestPath,
							parameters = buildParams(param(RequestParameters.stated, "true"))))
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);
			node = XMLUtils.getNodeFromXml(result, xpath);
			nodeList = null;
			taxonomyCoordinateStated = Boolean.valueOf(node.getTextContent());
			Assert.assertTrue(taxonomyCoordinateStated == true);

			// RestStampCoordinate
			result = checkFail(
					(target = target(
							requestUrl = stampCoordinateRequestPath,
							parameters = buildParams(
									param(RequestParameters.time, 123456789),
									param(RequestParameters.precedence, StampPrecedence.TIME),
									param(RequestParameters.modules, MetaData.AMT_MODULE.getConceptSequence() + "," + MetaData.ISAAC_MODULE.getConceptSequence() + "," + MetaData.SNOMED_CT_CORE_MODULE.getConceptSequence()),
									param(RequestParameters.allowedStates, State.INACTIVE.getAbbreviation() + "," + State.PRIMORDIAL.getAbbreviation()))))
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);
			xpath = "/restStampCoordinate/time";
			node = XMLUtils.getNodeFromXml(result, xpath);
			nodeList = null;
			long stampCoordinateTime = Long.parseLong(node.getTextContent());
			Assert.assertTrue(stampCoordinateTime == 123456789);
			xpath = "/restStampCoordinate/modules";
			List<Integer> stampCoordinateModules = new ArrayList<>();
			node = null;
			nodeList = XMLUtils.getNodeSetFromXml(result, xpath);
			for (int i = 0; i < nodeList.getLength(); ++i) {
				stampCoordinateModules.add(Integer.valueOf(nodeList.item(i).getTextContent()));
			}
			Assert.assertTrue(stampCoordinateModules.size() == 3);
			Assert.assertTrue(stampCoordinateModules.contains(MetaData.AMT_MODULE.getConceptSequence()));
			Assert.assertTrue(stampCoordinateModules.contains(MetaData.ISAAC_MODULE.getConceptSequence()));
			Assert.assertTrue(stampCoordinateModules.contains(MetaData.SNOMED_CT_CORE_MODULE.getConceptSequence()));

			xpath = "/restStampCoordinate/allowedStates/enumId";
			List<Integer> allowedStates = new ArrayList<>();
			node = null;
			nodeList = XMLUtils.getNodeSetFromXml(result, xpath);
			for (int i = 0; i < nodeList.getLength(); ++i) {
				allowedStates.add(Integer.valueOf(nodeList.item(i).getTextContent()));
			}
			Assert.assertTrue(allowedStates.size() == 2);
			Assert.assertTrue(allowedStates.contains(0));
			Assert.assertTrue(allowedStates.contains(2));

			// LanguageCoordinate
			// language
			result = checkFail(
					(target = target(
							requestUrl = languageCoordinateRequestPath,
							parameters = buildParams(param(RequestParameters.language, MetaData.ENGLISH_LANGUAGE.getConceptSequence()))))
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);
			xpath = "/restLanguageCoordinate/language";
			node = XMLUtils.getNodeFromXml(result, xpath);
			nodeList = null;
			int languageCoordinateLangSeq = Integer.parseInt(node.getTextContent());
			Assert.assertTrue(languageCoordinateLangSeq == MetaData.ENGLISH_LANGUAGE.getConceptSequence());

			// descriptionTypePrefs
			result = checkFail(
					(target = target(
							requestUrl = languageCoordinateRequestPath,
							parameters = buildParams(param(RequestParameters.descriptionTypePrefs, "fsn,synonym"))))
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);
			xpath = "/restLanguageCoordinate/descriptionTypePreferences";
			nodeList = XMLUtils.getNodeSetFromXml(result, xpath);
			node = null;
			Assert.assertTrue(nodeList.getLength() == 2);
			Assert.assertTrue(Integer.parseUnsignedInt(nodeList.item(0).getTextContent()) == MetaData.FULLY_SPECIFIED_NAME.getConceptSequence());
			Assert.assertTrue(Integer.parseUnsignedInt(nodeList.item(1).getTextContent()) == MetaData.SYNONYM.getConceptSequence());

			// descriptionTypePrefs (reversed)
			result = checkFail(
					(target = target(
							requestUrl = languageCoordinateRequestPath,
							parameters = buildParams(param(RequestParameters.descriptionTypePrefs, "synonym,fsn"))))
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);
			xpath = "/restLanguageCoordinate/descriptionTypePreferences";
			nodeList = XMLUtils.getNodeSetFromXml(result, xpath);
			node = null;
			Assert.assertTrue(nodeList.getLength() == 2);
			Assert.assertTrue(Integer.parseUnsignedInt(nodeList.item(0).getTextContent()) == MetaData.SYNONYM.getConceptSequence());
			Assert.assertTrue(Integer.parseUnsignedInt(nodeList.item(1).getTextContent()) == MetaData.FULLY_SPECIFIED_NAME.getConceptSequence());

			// Get token with specified non-default descriptionTypePrefs (SYNONYM,FSN)
			// then test token passed as argument along with RequestParameters.stated parameter
			result = checkFail(
					(target = target(
							requestUrl = coordinatesTokenRequestPath,
							parameters = buildParams(param(RequestParameters.descriptionTypePrefs, "synonym,fsn"))))
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);
			final RestCoordinatesToken synonymDescriptionPreferredToken = (RestCoordinatesToken) XMLUtils.unmarshalObject(RestCoordinatesToken.class, result);
			// Get token with specified default descriptionTypePrefs (FSN,SYNONYM)
			parameters.clear();
			parameters.put(RequestParameters.descriptionTypePrefs, "fsn,synonym");
			result = checkFail(
					(target = target(requestUrl = coordinatesTokenRequestPath, parameters))
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);
			final RestCoordinatesToken fsnDescriptionPreferredToken = (RestCoordinatesToken) XMLUtils.unmarshalObject(RestCoordinatesToken.class, result);

			// confirm that constructed token has descriptionTypePrefs ordered as in
			// parameters used to construct token
			result = checkFail(
					(target = target(
							requestUrl = languageCoordinateRequestPath,
							parameters = buildParams(param(RequestParameters.coordToken, synonymDescriptionPreferredToken.token))))
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);
			xpath = "/restLanguageCoordinate/descriptionTypePreferences";
			nodeList = XMLUtils.getNodeSetFromXml(result, xpath);
			node = null;
			Assert.assertTrue(nodeList.getLength() == 2);
			Assert.assertTrue(Integer.parseUnsignedInt(nodeList.item(0).getTextContent()) == MetaData.SYNONYM.getConceptSequence());
			Assert.assertTrue(Integer.parseUnsignedInt(nodeList.item(1).getTextContent()) == MetaData.FULLY_SPECIFIED_NAME.getConceptSequence());

			// test token passed as argument along with RequestParameters.stated parameter
			// ensure that descriptionTypePrefs order specified in token is maintained
			result = checkFail(
					(target = target(
							requestUrl = languageCoordinateRequestPath,
							parameters = buildParams(
									param(RequestParameters.coordToken, synonymDescriptionPreferredToken.token),
									param(RequestParameters.stated, "true"))))
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);
			xpath = "/restLanguageCoordinate/descriptionTypePreferences";
			nodeList = XMLUtils.getNodeSetFromXml(result, xpath);
			node = null;
			Assert.assertTrue(nodeList.getLength() == 2);
			Assert.assertTrue(Integer.parseUnsignedInt(nodeList.item(0).getTextContent()) == MetaData.SYNONYM.getConceptSequence());
			Assert.assertTrue(Integer.parseUnsignedInt(nodeList.item(1).getTextContent()) == MetaData.FULLY_SPECIFIED_NAME.getConceptSequence());

			// test passed descriptionTypePrefs on taxonomy
			// test synonym as preference
			result = checkFail(
					(target = target(
							requestUrl = taxonomyRequestPath,
							parameters = buildParams(
									param("childDepth", 1),
									param(RequestParameters.descriptionTypePrefs, "synonym,fsn"))))
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);
			xpath = "/restConceptVersion/children/conChronology[conceptSequence=" + MetaData.HEALTH_CONCEPT.getConceptSequence() + "]/description";
			node = XMLUtils.getNodeFromXml(result, xpath);
			nodeList = null;
			Assert.assertTrue(node != null && node.getNodeType() == Node.ELEMENT_NODE);
			Assert.assertTrue(node.getTextContent().equals(MetaData.HEALTH_CONCEPT.getConceptDescriptionText()));

			// test fsn as preference using token
			result = checkFail(
					(target = target(
							requestUrl = taxonomyRequestPath,
							parameters = buildParams(
									param("childDepth", 1),
									param(RequestParameters.coordToken, fsnDescriptionPreferredToken.token))))
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);
			xpath = "/restConceptVersion/children/conChronology[conceptSequence=" + MetaData.HEALTH_CONCEPT.getConceptSequence() + "]/description";
			node = XMLUtils.getNodeFromXml(result, xpath);
			nodeList = null;
			Assert.assertTrue(node != null && node.getNodeType() == Node.ELEMENT_NODE);
			Assert.assertTrue(node.getTextContent().equals(MetaData.HEALTH_CONCEPT.getConceptDescriptionText() + " (ISAAC)"));

			// test fsn as preference
			result = checkFail(
					(target = target(
							requestUrl = taxonomyRequestPath,
							parameters = buildParams(
									param("childDepth", 1),
									param(RequestParameters.descriptionTypePrefs, "fsn,synonym"))))
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);
			xpath = "/restConceptVersion/children/conChronology[conceptSequence=" + MetaData.HEALTH_CONCEPT.getConceptSequence() + "]/description";
			node = XMLUtils.getNodeFromXml(result, xpath);
			nodeList = null;
			Assert.assertTrue(node != null && node.getNodeType() == Node.ELEMENT_NODE);
			Assert.assertTrue(node.getTextContent().equals(MetaData.HEALTH_CONCEPT.getConceptDescriptionText() + " (ISAAC)"));

			// LogicCoordinate
			result = checkFail(
					(target = target(
							requestUrl = logicCoordinateRequestPath,
							parameters = buildParams(param(RequestParameters.classifier, MetaData.SNOROCKET_CLASSIFIER.getConceptSequence()))))
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);
			xpath = "/restLogicCoordinate/classifier";
			node = XMLUtils.getNodeFromXml(result, xpath);
			nodeList = null;
			int logicCoordinateClassifierSeq = Integer.parseInt(node.getTextContent());
			Assert.assertTrue(logicCoordinateClassifierSeq == MetaData.SNOROCKET_CLASSIFIER.getConceptSequence());
		} catch (Throwable error) {
			System.out.println("Failing request target: " + target);
			System.out.println("Failing request URL: " + requestUrl);
			System.out.println("Failing request parameters: " + parameters);
			System.out.println("Failing result XPath: " + xpath);
			System.out.println("Failing result Node: " + XMLUtils.toString(node));
			System.out.println("Failing result NodeList: " + XMLUtils.toString(nodeList));
			System.out.println("Failing result XML: " + result);

			throw error;
		}
	}

	@Test
	public void testSystemAPIs()
	{
		/*
		 * These tests display the following values on fail
		 * Each test should initialize or set-to-null each of the following values
		 */
		Map<String, Object> parameters = new HashMap<>();
		WebTarget target = null;
		String result = null;
		String requestUrl = null;
		RestIdentifiedObjectsResult identifiedObjectsResult = null;
		RestSystemInfo systemInfo = null;
		try {
			// Get a sememe chronology by assemblage and extract one of its UUIDs
			result = checkFail(
			(target = target(
					requestUrl = sememeByAssemblageRequestPath + DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION.getPrimordialUuid(),
					parameters = buildParams(
							param(RequestParameters.expand, "chronology"), // Expand the chronology
							param(RequestParameters.maxPageSize, 1)))) // Request exactly 1 result
			.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
			.readEntity(String.class);
			RestSememeVersions sememeVersions = XMLUtils.unmarshalObject(RestSememeVersions.class, result);
			UUID sememeUuid = sememeVersions.results.get(0).sememeChronology.identifiers.uuids.get(0);

			// Test objectChronologyType of specified sememe UUID
			result = checkFail(
					(target = target(
							requestUrl = RestPaths.systemAPIsPathComponent + RestPaths.objectChronologyTypeComponent + sememeUuid.toString()))
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);
			RestObjectChronologyType objectChronologyType = XMLUtils.unmarshalObject(RestObjectChronologyType.class, result);
			// Test RestObjectChronologyType name
			Assert.assertTrue(objectChronologyType.toString().equalsIgnoreCase(ObjectChronologyType.SEMEME.name()));
			// Test RestObjectChronologyType enumId ordinal
			Assert.assertTrue(objectChronologyType.getEnumId() == ObjectChronologyType.SEMEME.ordinal());

			// Test objectChronologyType of specified concept UUID
			result = checkFail(
					(target = target(
							requestUrl = RestPaths.systemAPIsPathComponent + RestPaths.objectChronologyTypeComponent + MetaData.ISAAC_ROOT.getPrimordialUuid().toString()))
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);
			objectChronologyType = XMLUtils.unmarshalObject(RestObjectChronologyType.class, result);
			// Test RestObjectChronologyType name
			Assert.assertTrue(objectChronologyType.toString().equalsIgnoreCase(ObjectChronologyType.CONCEPT.name()));
			// Test RestObjectChronologyType enumId ordinal
			Assert.assertTrue(objectChronologyType.getEnumId() == ObjectChronologyType.CONCEPT.ordinal());

			// Test SystemInfo
			result = checkFail(
					(target = target(
							requestUrl = RestPaths.systemAPIsPathComponent + RestPaths.systemInfoComponent))
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);
			systemInfo = XMLUtils.unmarshalObject(RestSystemInfo.class, result);
			Assert.assertTrue(systemInfo.supportedAPIVersions.length > 0 && ! StringUtils.isBlank(systemInfo.supportedAPIVersions[0]));
//			Assert.assertTrue(! StringUtils.isBlank(systemInfo.apiImplementationVersion));
//			Assert.assertTrue(! StringUtils.isBlank(systemInfo.isaacVersion));
//			Assert.assertTrue(! StringUtils.isBlank(systemInfo.scmUrl));
//			Assert.assertNotNull(systemInfo.isaacDbDependency);
//			Assert.assertTrue(! StringUtils.isBlank(systemInfo.isaacDbDependency.groupId));
//			Assert.assertTrue(! StringUtils.isBlank(systemInfo.isaacDbDependency.artifactId));
//			Assert.assertTrue(! StringUtils.isBlank(systemInfo.isaacDbDependency.type));
//			Assert.assertTrue(! StringUtils.isBlank(systemInfo.isaacDbDependency.version));
//			Assert.assertTrue(systemInfo.appLicenses.size() > 0);
//			for (RestLicenseInfo licenseInfo : systemInfo.appLicenses) {
//				Assert.assertTrue(! StringUtils.isBlank(licenseInfo.name));
//				Assert.assertTrue(! StringUtils.isBlank(licenseInfo.url));
//				Assert.assertTrue(! StringUtils.isBlank(licenseInfo.comments));
//			}
//			for (RestDependencyInfo dependencyInfo : systemInfo.dbDependencies) {
//				Assert.assertTrue(! StringUtils.isBlank(dependencyInfo.groupId));
//				Assert.assertTrue(! StringUtils.isBlank(dependencyInfo.artifactId));
//				Assert.assertTrue(! StringUtils.isBlank(dependencyInfo.type));
//				Assert.assertTrue(! StringUtils.isBlank(dependencyInfo.version));
//			}
//			for (RestLicenseInfo licenseInfo : systemInfo.dbLicenses) {
//				Assert.assertTrue(! StringUtils.isBlank(licenseInfo.name));
//				Assert.assertTrue(! StringUtils.isBlank(licenseInfo.url));
//				Assert.assertTrue(! StringUtils.isBlank(licenseInfo.comments));
//			}		
			
			// Test identifiedObjectsComponent request of specified sememe UUID
			result = checkFail(
					(target = target(
							requestUrl = RestPaths.systemAPIsPathComponent + RestPaths.identifiedObjectsComponent + sememeUuid.toString()))
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);
			identifiedObjectsResult = XMLUtils.unmarshalObject(RestIdentifiedObjectsResult.class, result);
			// Test RestSememeChronology
			Assert.assertTrue(identifiedObjectsResult.sememe.identifiers.uuids.contains(sememeUuid));
			Assert.assertNull(identifiedObjectsResult.concept);
			
			// Test identifiedObjectsComponent request of specified concept UUID
			result = checkFail(
					(target = target(
							requestUrl = RestPaths.systemAPIsPathComponent + RestPaths.identifiedObjectsComponent + MetaData.ISAAC_ROOT.getPrimordialUuid().toString()))
					.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
					.readEntity(String.class);
			identifiedObjectsResult = XMLUtils.unmarshalObject(RestIdentifiedObjectsResult.class, result);
			// Test RestSememeChronology
			Assert.assertTrue(identifiedObjectsResult.concept.identifiers.uuids.contains(MetaData.ISAAC_ROOT.getPrimordialUuid()));
			Assert.assertNull(identifiedObjectsResult.sememe);

			// Iterate and test first 10000 sequence numbers until a value found which corresponds to both concept and sememe
			boolean foundSequenceCorrespondingToBothConceptAndSememe = false;
			for (int sequence = 1; sequence < 10000; ++sequence) {
				if (Get.sememeService().hasSememe(sequence) && Get.conceptService().hasConcept(sequence)) {
					foundSequenceCorrespondingToBothConceptAndSememe = true;
					result = checkFail(
							(target = target(
									requestUrl = RestPaths.systemAPIsPathComponent + RestPaths.identifiedObjectsComponent + sequence))
							.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
							.readEntity(String.class);
					identifiedObjectsResult = XMLUtils.unmarshalObject(RestIdentifiedObjectsResult.class, result);
					// Test RestSememeChronology AND RestConceptChronology
					Assert.assertTrue(identifiedObjectsResult.concept.conceptSequence == sequence);
					Assert.assertTrue(identifiedObjectsResult.sememe.sememeSequence == sequence);

					break;
				}
			}
			Assert.assertTrue(foundSequenceCorrespondingToBothConceptAndSememe);
		} catch (Throwable error) {
			System.out.println("Failing request target: " + target);
			System.out.println("Failing request URL: " + requestUrl);
			System.out.println("Failing request parameters: " + parameters);
			System.out.println("Failing result XML: " + result);
			System.out.println("Failing identified objects result: " + identifiedObjectsResult);
			System.out.println("Failing systemInfo result: " + systemInfo);

			throw error;
		}
	}
	
	public static void main(String[] argv) {
	}
}