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
package gov.vha.isaac.rest.api1.data.concept;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.util.AlphanumComparator;
import gov.vha.isaac.rest.ExpandUtil;
import gov.vha.isaac.rest.api.data.Expandable;
import gov.vha.isaac.rest.api.data.Expandables;
import gov.vha.isaac.rest.api1.RestPaths;
import gov.vha.isaac.rest.api1.data.RestIdentifiedObject;
import gov.vha.isaac.rest.session.RequestInfo;
import gov.vha.isaac.rest.session.RequestParameters;

/**
 * 
 * {@link RestConceptChronology}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@XmlRootElement
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestConceptChronology implements Comparable<RestConceptChronology>
{
	/**
	 * The concept sequence identifier of this concept
	 */
	@XmlElement
	public int conceptSequence;
	
	/**
	 * The "best" description for this concept.  This is selected based on the attributes within the session for 
	 * stamp and language coordinates - or - if none present - the server default.
	 */
	@XmlElement
	public String description;
	
	/**
	 * The data that was not expanded as part of this call (but can be)
	 */
	@XmlElement
	Expandables expandables;
	
	/**
	 * The identifier data for the object
	 */
	@XmlElement
	public RestIdentifiedObject identifiers;
	
	/**
	 * The list of concept versions.  Depending on the expand parameter, may be empty, the latest only, or all versions.
	 */
	@XmlElement
	public List<RestConceptVersion> versions;

	protected RestConceptChronology()
	{
		//for JaxB
	}
	
	@SuppressWarnings("rawtypes") 
	public RestConceptChronology(ConceptChronology<? extends ConceptVersion> cc, boolean includeAllVersions, boolean includeLatestVersion)
	{
		conceptSequence = cc.getConceptSequence();
		identifiers = new RestIdentifiedObject(cc.getUuidList());
		
		description = readBestDescription(cc.getNid());
		
		if (includeAllVersions || includeLatestVersion)
		{
			expandables = null;
			versions = new ArrayList<RestConceptVersion>();
			if (includeAllVersions)
			{
				for (ConceptVersion cv : cc.getVersionList())
				{
					versions.add(new RestConceptVersion(cv, false, false, false, false, false, false, false));
				}
			}
			else // if (includeLatestVersion)
			{
				@SuppressWarnings("unchecked")
				Optional<LatestVersion<ConceptVersion>> latest = 
						((ConceptChronology)cc).getLatestVersion(ConceptVersion.class, RequestInfo.get().getStampCoordinate());
				if (latest.isPresent())
				{
					versions.add(new RestConceptVersion(latest.get().value(), false, false, false, false, false, false, false));
				}
			}
		}
		else
		{
			versions = null;
			if (RequestInfo.get().returnExpandableLinks())
			{
				expandables = new Expandables(
					new Expandable(ExpandUtil.versionsAllExpandable,
						RestPaths.conceptVersionsAppPathComponent + cc.getConceptSequence() + "/"),
					new Expandable(ExpandUtil.versionsLatestOnlyExpandable,
						RestPaths.conceptVersionAppPathComponent + cc.getConceptSequence() + "/"
						+ "?" + RequestParameters.coordToken + "=" + RequestInfo.get().getCoordinatesToken().getSerialized()));
			}
			else
			{
				expandables = null;
			}
		}
	}

	/**
	 * Utility method to find the 'best' description for the concept at hand.
	 * @param conceptNid
	 * @return
	 */
	public static String readBestDescription(int conceptNid)
	{
		Optional<LatestVersion<DescriptionSememe<?>>> descriptionOptional = Optional.empty();
		
		if (RequestInfo.get().useFsn())
		{
			descriptionOptional = RequestInfo.get().getLanguageCoordinate().getFullySpecifiedDescription(
				Get.sememeService().getDescriptionsForComponent(conceptNid).collect(Collectors.toList()), RequestInfo.get().getStampCoordinate());
		}
		
		if (!descriptionOptional.isPresent())
		{
			descriptionOptional = RequestInfo.get().getLanguageCoordinate().getPreferredDescription(
				Get.sememeService().getDescriptionsForComponent(conceptNid).collect(Collectors.toList()), RequestInfo.get().getStampCoordinate());
		}
		
		if (descriptionOptional.isPresent())
		{
			if (descriptionOptional.get().contradictions().isPresent())
			{
				//Prefer active descriptions over inactive, if there was a contradiction (which means they tied the sort - have the same time)
				//common for a replacement description to have the same time as the retired one.
				if (descriptionOptional.get().value().getState() == State.ACTIVE)
				{
					return descriptionOptional.get().value().getText();
				}
				else
				{
					for (DescriptionSememe<?> ds : descriptionOptional.get().contradictions().get())
					{
						if (ds.getState() == State.ACTIVE)
						{
							return ds.getText();
						}
					}
				}
			}
			return descriptionOptional.get().value().getText();
		}
		else
		{
			return null;
		}
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(RestConceptChronology o)
	{
		return AlphanumComparator.compare(description,  o.description,  true);
	}
}
