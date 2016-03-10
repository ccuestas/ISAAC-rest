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
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.jersey.test.JerseyTestNg;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import gov.vha.isaac.MetaData;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.commit.CommitService;
import gov.vha.isaac.ochre.api.constants.DynamicSememeConstants;
import gov.vha.isaac.ochre.api.externalizable.BinaryDataReaderService;
import gov.vha.isaac.ochre.api.index.IndexServiceBI;
import gov.vha.isaac.rest.LocalJettyRunner;
import gov.vha.isaac.rest.api1.RestPaths;

/**
 * {@link RestTest}
 * Testing framework for doing full cycle testing - this launches the REST server in a grizzly container, and makes REST requests via a loop
 * back call.
 * 
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class RestTest extends JerseyTestNg.ContainerPerClassTest
{
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
		Response response = target(RestPaths.conceptPathComponent + RestPaths.versionComponent +
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
	
	/**
	 * This test validates that both the JSON and XML serializers are working correctly with returns that contain
	 * nested array data, and various implementation types of the dynamic sememe types.
	 */
	@Test
	public void testArraySememeReturn()
	{
		Response response = target(RestPaths.sememePathComponent + RestPaths.byAssemblageComponent +
				DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION.getPrimordialUuid()).request()
					.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get();
		
		checkFail(response);
		
		response = target(RestPaths.sememePathComponent + RestPaths.byAssemblageComponent +
				DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION.getPrimordialUuid()).request()
					.header(Header.Accept.toString(), MediaType.APPLICATION_JSON).get();
		
		checkFail(response);
	}
	/**
	 * This test validates that both the JSON and XML serializers are working correctly with returns that contain
	 * concept data.
	 */
	@Test
	public void testIdReturn()
	{
		final String url = RestPaths.idPathComponent + RestPaths.idTranslateComponent +
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
		final String url = RestPaths.conceptPathComponent + RestPaths.versionComponent +
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
		final String url = RestPaths.logicGraphPathComponent + RestPaths.versionComponent +
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
	 * LogicGraph data and validates that the returned Response XML contains a valid RestSememeLogicGraphVersion
	 */
	@Test
	public void testRestSememeLogicGraphVersionReturn()
	{
		final String url = RestPaths.logicGraphPathComponent + RestPaths.versionComponent +
				DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION.getPrimordialUuid().toString();
	
		Object returnedEntity = target(url).request().get().getEntity();
		
		System.out.println("testRestSememeLogicGraphVersionReturn(): " + url + " returned a " + returnedEntity.getClass().getName() + " " + returnedEntity);
	}
	
	/**
	 * This test validates that both the JSON and XML serializers are working correctly with returns that contain
	 * taxonomy data.
	 */
	@Test
	public void testTaxonomyReturn()
	{
		final String url = RestPaths.taxonomyPathComponent + RestPaths.versionComponent;

		Response response = target(url).request()
					.header(Header.Accept.toString(), MediaType.APPLICATION_XML).get();
	//	System.out.println(target(url).request().get().toString());
		
		checkFail(response);

		response = target(url).request()
					.header(Header.Accept.toString(), MediaType.APPLICATION_JSON).get();
		
		checkFail(response);
	}
	
	
	@Test
	public void testSearchAssemblageRestriction1()
	{
		//Check with UUID
		final String url = RestPaths.searchPathComponent + RestPaths.sememesComponent;

		String result = checkFail(target(url)
				.queryParam("treatAsString", "false")
				.queryParam("query","3")
				.queryParam("sememeAssemblageId", DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION.getUUID().toString())
				.queryParam("sememeAssemblageId", MetaData.AMT_MODULE.getNid() + "")
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
				.readEntity(String.class);
		
		Assert.assertTrue(result.contains(DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMN_DEFAULT_VALUE.getPrimordialUuid().toString()));
		
		//Check with nid
		result = checkFail(target(url)
				.queryParam("treatAsString", "false")
				.queryParam("query","3")
				.queryParam("sememeAssemblageId", DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION.getNid() + "")
				.queryParam("sememeAssemblageId", MetaData.AMT_MODULE.getNid() + "")
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
				.readEntity(String.class);
		
		Assert.assertTrue(result.contains(DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMN_DEFAULT_VALUE.getPrimordialUuid().toString()));
		
		//Check with sequence
		//Check with nid
		result = checkFail(target(url)
				.queryParam("treatAsString", "false")
				.queryParam("query","3")
				.queryParam("sememeAssemblageId", DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION.getConceptSequence() + "")
				.queryParam("sememeAssemblageId", MetaData.AMT_MODULE.getNid() + "")
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
				.readEntity(String.class);
		
		Assert.assertTrue(result.contains(DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMN_DEFAULT_VALUE.getPrimordialUuid().toString()));
		
		//sanity check search
		result = checkFail(target(url)
				.queryParam("treatAsString", "false")
				.queryParam("query","55")
				.queryParam("sememeAssemblageId", DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION.getNid() + "")
				.queryParam("sememeAssemblageId", MetaData.AMT_MODULE.getNid() + "")
				.request().header(Header.Accept.toString(), MediaType.APPLICATION_XML).get())
				.readEntity(String.class);
		
		Assert.assertFalse(result.contains(DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMN_DEFAULT_VALUE.getPrimordialUuid().toString()));
	}
}
