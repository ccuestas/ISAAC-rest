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
package gov.vha.isaac.rest.api1.data.sememe;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.rest.api.data.Expandables;
import gov.vha.isaac.rest.api1.data.RestStampedVersion;

/**
 * 
 * {@link RestSememeVersion}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@XmlRootElement
public class RestSememeVersion 
{
	/**
	 * The data that was not expanded as part of this call (but can be)
	 */
	@XmlElement
	Expandables expandables;
	
	/**
	 * The sememe chronology for this concept.  Depending on the expand parameter, may be empty.
	 */
	@XmlElement
	RestSememeChronology sememeChronology;
	
	
	/**
	 * The StampedVersion details for this version of this sememe.
	 */
	@XmlElement
	RestStampedVersion sememeVersion;

	protected RestSememeVersion()
	{
		//For jaxb
	}
	
	public RestSememeVersion(SememeVersion<?> sv)
	{
		sememeChronology = new RestSememeChronology(sv);
		sememeVersion = new RestStampedVersion(sv);
	}
}
