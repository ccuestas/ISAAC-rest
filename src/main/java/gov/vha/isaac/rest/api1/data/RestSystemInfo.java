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
package gov.vha.isaac.rest.api1.data;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import gov.vha.isaac.ochre.api.ConfigurationService;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.rest.ApplicationConfig;
import gov.vha.isaac.rest.api1.data.systeminfo.RestDependencyInfo;
import gov.vha.isaac.rest.api1.data.systeminfo.RestLicenseInfo;

/**
 * {@link RestSystemInfo}
 * 
 * This class carries back various system information about this deployment.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 * 
 * 
 */
@XmlRootElement
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestSystemInfo
{
	private transient static Logger log_ = LogManager.getLogger();
	/**
	 * The full version number of this API.  Note, this is an array, because in the future
	 * the API may simultaneously support versions such as [1.3, 2.0] for reverse compatibility.
	 * 
	 * The agreement with Komet is that we do "Major.Minor.Revision"
	 * The Major version only changes in concert with the rest API paths changing from /1/ to /2/ for example.
	 * The Minor version is changed whenever we change a previously existing API or data structure - such that it 
	 * may break existing code in KOMET.  Note, you can add new APIs / properties to existing data structures without 
	 * breaking KOMET.
	 * The Revision is changed whenever we make a change that modifies the API, but only in a way that won't impact
	 * existing KOMET functionality - such as adding a new API, adding a new data structure, adding a field to an existing
	 * data structure. 
	 */
	@XmlElement
	public String[] supportedAPIVersions = new String[] {"1.4.1"};
	
	/**
	 * REST API Implementation Version - aka the version number of the software running here.
	 */
	@XmlElement
	public String apiImplementationVersion;
	
	/**
	 * The version number of the database being used by this instance.
	 */
	@XmlElement
	public RestDependencyInfo isaacDbDependency;

	/**
	 * Source Code Management URL that contains the source code for the software running here.
	 */
	@XmlElement
	public String scmUrl;
	
	/**
	 * The version of ISAAC that the rest service is running on top of.
	 */
	@XmlElement
	public String isaacVersion;
	
	/**
	 * Software Licenses
	 */
	@XmlElement
	public List<RestLicenseInfo> appLicenses = new ArrayList<>();
	
	/**
	 * Database Licenses
	 */
	@XmlElement
	public List<RestLicenseInfo> dbLicenses = new ArrayList<>();
	
	/**
	 * The source content that was built into the underlying database.
	 */
	@XmlElement
	public List<RestDependencyInfo> dbDependencies = new ArrayList<>();

	public RestSystemInfo()
	{
		//Read in other information from the package (pom.properties file during normal runtime, pom.xml files if running in a dev env)
		try
		{
			loadIsaacMetdata();
			loadDbMetadata();
		}
		catch (Exception ex)
		{
			log_.error("Unexpected error reading app configuration information", ex);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "SystemInfo [supportedAPIVersions=" + Arrays.toString(supportedAPIVersions) + ", apiImplementationVersion="
				+ apiImplementationVersion + ", isaacDbDependency=" + isaacDbDependency + ", scmUrl=" + scmUrl 
				+ ", isaacVersion="
				+ isaacVersion
				+ ", appLicenses=" + appLicenses + ", dbLicenses=" + dbLicenses
				+ ", dbDependencies=" + dbDependencies + "]";
	}
	
	// Read the DB metadata
	private void loadDbMetadata() throws IOException {
		//Read the db metadata
		AtomicBoolean readDbMetadataFromProperties = new AtomicBoolean(false);
		AtomicBoolean readDbMetadataFromPom = new AtomicBoolean(false);
		
		java.nio.file.Path dbLocation = LookupService.get().getService(ConfigurationService.class).getChronicleFolderPath().getParent();
		//find the pom.properties file in the hierarchy
		File dbMetadata = new File(dbLocation.toFile(), "META-INF");
		AtomicReference<String> metadataVersion = new AtomicReference<String>("");
		if (dbMetadata.isDirectory())
		{
			Files.walkFileTree(dbMetadata.toPath(), new SimpleFileVisitor<java.nio.file.Path>()
			{
				/**
				 * @see java.nio.file.SimpleFileVisitor#visitFile(java.lang.Object, java.nio.file.attribute.BasicFileAttributes)
				 */
				@Override
				public FileVisitResult visitFile(java.nio.file.Path path, BasicFileAttributes attrs) throws IOException
				{
					File f = path.toFile();
					if (f.isFile() && f.getName().toLowerCase().equals("pom.properties"))
					{
						Properties p = new Properties();
						p.load(new FileReader(f));

						isaacDbDependency =
								new RestDependencyInfo(
										p.getProperty("project.groupId"),
										p.getProperty("project.artifactId"),
										p.getProperty("project.version"),
										p.getProperty("resultArtifactClassifier"),
										p.getProperty("chronicles.type"));
						
						metadataVersion.set(p.getProperty("isaac-metadata.version"));
						readDbMetadataFromProperties.set(true);
						return readDbMetadataFromPom.get() ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
					} else if (f.isFile() && f.getName().toLowerCase().equals("pom.xml")) {
						DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
						DocumentBuilder builder;
						Document dDoc = null;
						XPath xPath = XPathFactory.newInstance().newXPath();

						try {
							builder = domFactory.newDocumentBuilder();

							dDoc = builder.parse(f);
							
							{
								NodeList dbLicensesNodes = ((NodeList) xPath.evaluate("/project/licenses/license/name", dDoc, XPathConstants.NODESET));

								log_.debug("Found {} license names in DB pom.xml", dbLicensesNodes.getLength());
								for (int i = 0; i < dbLicensesNodes.getLength(); i++) {
									Node currentLicenseNameNode = dbLicensesNodes.item(i);
									String name = currentLicenseNameNode.getTextContent();

									RestLicenseInfo license =
											new RestLicenseInfo(
													name,
													((Node)xPath.evaluate("/project/licenses/license[name='" + name + "']/url", dDoc, XPathConstants.NODE)).getTextContent(),
													((Node)xPath.evaluate("/project/licenses/license[name='" + name + "']/comments", dDoc, XPathConstants.NODE)).getTextContent());
									dbLicenses.add(license);

									log_.debug("Extracted license \"{}\" from DB pom.xml: {}", name, license.toString());
								}
							}
							
							{
								NodeList dbDependenciesNodes = ((NodeList) xPath.evaluate("/project/dependencies/dependency/artifactId", dDoc, XPathConstants.NODESET));

								log_.debug("Found {} dependency artifactIds in DB pom.xml", dbDependenciesNodes.getLength());
								for (int i = 0; i < dbDependenciesNodes.getLength(); i++) {
									Node currentDbDependencyArtifactIdNode = dbDependenciesNodes.item(i);
									
									String artifactId = currentDbDependencyArtifactIdNode.getTextContent();
									String groupId = ((Node)xPath.evaluate("/project/dependencies/dependency[artifactId='" + artifactId + "']/groupId", dDoc, XPathConstants.NODE)).getTextContent();
									String version = ((Node)xPath.evaluate("/project/dependencies/dependency[artifactId='" + artifactId + "']/version", dDoc, XPathConstants.NODE)).getTextContent();
						
									String classifier = null;
									try {
										classifier = ((Node)xPath.evaluate("/project/dependencies/dependency[artifactId='" + artifactId + "']/classifier", dDoc, XPathConstants.NODE)).getTextContent();
									} catch (Throwable t) {
										log_.debug("Problem reading \"classifier\" element for {}", artifactId);
									}
									String type = null;
									try {
										type = ((Node)xPath.evaluate("/project/dependencies/dependency[artifactId='" + artifactId + "']/type", dDoc, XPathConstants.NODE)).getTextContent();
									} catch (Throwable t) {
										log_.debug("Problem reading \"type\" element for {}", artifactId);
									}

									RestDependencyInfo dependencyInfo = new RestDependencyInfo(groupId, artifactId, version, classifier, type);
									dbDependencies.add(dependencyInfo);

									log_.debug("Extracted dependency \"{}\" from DB pom.xml: {}", artifactId, dependencyInfo.toString());
								}
							}
						} catch (XPathExpressionException | SAXException | ParserConfigurationException e) {
							throw new IOException(e);
						}

						readDbMetadataFromPom.set(true);
						return readDbMetadataFromProperties.get() ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
					}

					return FileVisitResult.CONTINUE;
				}
			});
		}

		if (!readDbMetadataFromProperties.get())
		{
			log_.warn("Failed to read the metadata about the database from the database package.");
		}
		else
		{
			//Due to a quirk in how the DB poms are set up, we need to fill in this property
			for (RestDependencyInfo dependency : dbDependencies)
			{
				if (dependency.version != null && "${isaac-metadata.version}".equals(dependency.version))
				{
					dependency.version = metadataVersion.get();
					break;
				}
			}
			log_.debug("Successfully read db properties from maven config files.  dbGroupId: {} dbArtifactId: {} dbVersion: {} dbClassifier: {} dbType: {}", 
					isaacDbDependency.groupId, isaacDbDependency.artifactId, isaacDbDependency.version, isaacDbDependency.classifier, isaacDbDependency.type);
		}
	}
	
	private void loadIsaacMetdata() throws ParserConfigurationException, SAXException, IOException, DOMException, XPathExpressionException {
		//read the ISAAC metadata
		AtomicBoolean readIsaacAppMetadata = new AtomicBoolean(false);

		//if running from eclipse - our launch folder should be "ISAAC-rest".
		File f = new File("").getAbsoluteFile();
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = domFactory.newDocumentBuilder();

		if (ApplicationConfig.getInstance().getServletContext() != null)
		{
			InputStream is = ApplicationConfig.getInstance().getServletContext().getResourceAsStream("/META-INF/maven/gov.vha.isaac.rest/isaac-rest/pom.xml");
			if (is == null)
			{
				log_.warn("Can't locate pom.xml file in deployment to read metadata");
			}
			else
			{
				readFromPomFile(builder.parse(is));
				readIsaacAppMetadata.set(true);
			}
		}
		else if (f.getName().endsWith("ISAAC-rest"))  //context null, are we running in a local jetty runner?
		{
			File pom = new File(f, "pom.xml");
			if (pom.isFile())
			{
				readFromPomFile(builder.parse(pom));
				readIsaacAppMetadata.set(true);
			}
		}
		else
		{
			log_.warn("No Servlet Context available to utilize to locate the metadata!");
		}
		
		
		if (!readIsaacAppMetadata.get())
		{
			log_.warn("Failed to read the metadata about the ISAAC-rest instance");
		}
	}
	
	private void readFromPomFile(Document dDoc) throws DOMException, XPathExpressionException
	{
		XPath xPath = XPathFactory.newInstance().newXPath();
		isaacVersion = ((Node) xPath.evaluate("/project/properties/isaac.version", dDoc, XPathConstants.NODE)).getTextContent();
		scmUrl = ((Node) xPath.evaluate("/project/scm/url", dDoc, XPathConstants.NODE)).getTextContent();
		apiImplementationVersion = ((Node) xPath.evaluate("/project/version", dDoc, XPathConstants.NODE)).getTextContent();
		
		log_.debug("API implementation version: {} scmUrl: {} isaacVersion: {}", apiImplementationVersion, scmUrl, isaacVersion);
		
		NodeList appLicensesNodes = ((NodeList) xPath.evaluate("/project/licenses/license/name", dDoc, XPathConstants.NODESET));

		log_.debug("Found {} license names", appLicensesNodes.getLength());
		for (int i = 0; i < appLicensesNodes.getLength(); i++) {
			Node currentLicenseNameNode = appLicensesNodes.item(i);
			String name = currentLicenseNameNode.getTextContent();
			
			RestLicenseInfo appLicenseInfo =
					new RestLicenseInfo(
							name,
							((Node)xPath.evaluate("/project/licenses/license[name='" + name + "']/url", dDoc, XPathConstants.NODE)).getTextContent(),
							((Node)xPath.evaluate("/project/licenses/license[name='" + name + "']/comments", dDoc, XPathConstants.NODE)).getTextContent());
			appLicenses.add(appLicenseInfo);
			
			log_.debug("Extracted license \"{}\" from ISAAC pom.xml: {}", name, appLicenseInfo.toString());
		}
	}
}
